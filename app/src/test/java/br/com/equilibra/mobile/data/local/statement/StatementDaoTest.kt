package br.com.equilibra.mobile.data.local.statement

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Task 7.1 — Persistência das faturas de cartão.
 *
 * Regras que este teste trava:
 * - total/pago/pendente e itens permanecem String (centavos nunca viram Double)
 * - listagem por cartão é ordenada por cycleStart DESC (igual ao backend)
 * - itens são filhos da fatura e preservam a ordem de sequência
 * - trocar o status (fechar/pagar) não apaga os itens
 * - sincronização substitui os itens da fatura numa transação
 */
@RunWith(RobolectricTestRunner::class)
class StatementDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: StatementDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.statementDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun statement(
        id: String = "s1",
        cardId: String = "c1",
        cycleStart: String = "2026-10-03",
        cycleEnd: String = "2026-11-02",
        status: String = "OPEN",
        totalAmount: String = "100.00",
        paidAmount: String = "0.00",
        outstandingAmount: String = "100.00"
    ) = StatementEntity(
        id = id,
        creditCardId = cardId,
        cycleStart = cycleStart,
        cycleEnd = cycleEnd,
        closingAt = "2026-11-03T03:00:00Z",
        dueDate = "2026-11-10",
        status = status,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount
    )

    private fun item(
        id: String = "i1",
        statementId: String = "s1",
        purchaseId: String = "p1",
        seq: Int = 1,
        amount: String = "33.34",
        cycle: String = "2026-11-01"
    ) = StatementItemEntity(
        id = id,
        statementId = statementId,
        purchaseId = purchaseId,
        description = "Notebook",
        sequenceNumber = seq,
        totalInstallments = 3,
        amount = amount,
        cycleMonth = cycle
    )

    @Test
    fun upsertPreservaDecimaisComoString() = runTest {
        dao.upsertStatement(
            statement(
                totalAmount = "1234567.89",
                paidAmount = "0.01",
                outstandingAmount = "1234567.88"
            )
        )
        dao.upsertItem(item(amount = "411522.63"))

        val got = dao.getById("s1")!!
        assertEquals("1234567.89", got.totalAmount)
        assertEquals("0.01", got.paidAmount)
        assertEquals("1234567.88", got.outstandingAmount)
        assertEquals("411522.63", dao.getItems("s1").single().amount)
    }

    @Test
    fun listaPorCartaoVemDoCicloMaisRecenteParaOMaisAntigo() = runTest {
        dao.upsertStatements(
            listOf(
                statement(id = "s1", cycleStart = "2026-09-03", cycleEnd = "2026-10-02"),
                statement(id = "s2", cycleStart = "2026-10-03", cycleEnd = "2026-11-02"),
                statement(
                    id = "outro",
                    cardId = "c2",
                    cycleStart = "2026-11-03",
                    cycleEnd = "2026-12-02"
                )
            )
        )

        val daFatura = dao.observeByCard("c1").first()

        assertEquals(listOf("s2", "s1"), daFatura.map { it.id })
    }

    @Test
    fun faturaMaisRecenteFicaDisponivelParaLeituraCacheFirst() = runTest {
        dao.upsertStatements(
            listOf(
                statement(id = "s1", cycleStart = "2026-09-03", cycleEnd = "2026-10-02"),
                statement(id = "s2", cycleStart = "2026-10-03", cycleEnd = "2026-11-02"),
                statement(
                    id = "outro",
                    cardId = "c2",
                    cycleStart = "2026-11-03",
                    cycleEnd = "2026-12-02"
                )
            )
        )

        assertEquals("s2", dao.getLatestByCard("c1")?.id)
        assertEquals("s2", dao.observeLatestByCard("c1").first()?.id)
    }

    @Test
    fun itensFicamOrdenadosPorSequencia() = runTest {
        dao.upsertStatement(statement())
        dao.upsertItems(
            listOf(
                item(id = "i3", seq = 3, amount = "33.33", cycle = "2027-01-01"),
                item(id = "i1", seq = 1, amount = "33.34", cycle = "2026-11-01"),
                item(id = "i2", seq = 2, amount = "33.33", cycle = "2026-12-01")
            )
        )

        val itens = dao.getItems("s1")

        assertEquals(listOf(1, 2, 3), itens.map { it.sequenceNumber })
        assertEquals(listOf("33.34", "33.33", "33.33"), itens.map { it.amount })
        assertEquals("2026-11-01", itens.first().cycleMonth)
        assertEquals(listOf(1, 2, 3), dao.observeItems("s1").first().map { it.sequenceNumber })
    }

    @Test
    fun upsertComItensSubstituiItensDaVersaoAnterior() = runTest {
        dao.upsertStatementWithItems(
            statement(),
            listOf(
                item(id = "i1", seq = 1, amount = "33.34"),
                item(id = "i2", seq = 2, amount = "33.33"),
                item(id = "i3", seq = 3, amount = "33.33")
            )
        )

        dao.upsertStatementWithItems(
            statement(
                status = "PAID",
                paidAmount = "100.00",
                outstandingAmount = "0.00"
            ),
            listOf(item(id = "i1", seq = 1, amount = "100.00"))
        )

        val got = dao.getById("s1")!!
        assertEquals("PAID", got.status)
        assertEquals("100.00", got.paidAmount)
        assertEquals("0.00", got.outstandingAmount)

        val itens = dao.getItems("s1")
        assertEquals(1, itens.size)
        assertEquals("i1", itens.single().id)
        assertEquals("100.00", itens.single().amount)
    }

    @Test
    fun atualizarStatusSemItensMantemOsItens() = runTest {
        dao.upsertStatementWithItems(
            statement(),
            listOf(
                item(id = "i1", seq = 1, amount = "33.34"),
                item(id = "i2", seq = 2, amount = "33.33")
            )
        )

        // Fechamento/pagamento atualiza a fatura; não deve tocar nos itens.
        dao.upsertStatement(
            statement(
                status = "PAID",
                paidAmount = "100.00",
                outstandingAmount = "0.00"
            )
        )

        assertEquals("PAID", dao.getById("s1")!!.status)
        assertEquals(2, dao.getItems("s1").size)
    }

    @Test
    fun deleteByCardNotInRemoveFaturasAusentesEItensJunto() = runTest {
        dao.upsertStatements(listOf(statement(id = "s1"), statement(id = "s2")))
        dao.upsertItems(
            listOf(
                item(id = "i1", statementId = "s1", seq = 1),
                item(id = "i2", statementId = "s2", seq = 1)
            )
        )

        dao.deleteByCardNotInSafe("c1", listOf("s1"))

        assertEquals(listOf("s1"), dao.getByCard("c1").map { it.id })
        assertEquals(1, dao.getItems("s1").size)
        assertTrue(dao.getItems("s2").isEmpty())
    }

    @Test
    fun deleteByCardNotInComListaVaziaLimpaSoOCartao() = runTest {
        dao.upsertStatements(
            listOf(
                statement(id = "s1", cardId = "c1"),
                statement(id = "s2", cardId = "c1"),
                statement(id = "outro", cardId = "c2")
            )
        )

        dao.deleteByCardNotInSafe("c1", emptyList())

        assertTrue(dao.getByCard("c1").isEmpty())
        assertEquals(listOf("outro"), dao.getByCard("c2").map { it.id })
    }

    @Test
    fun deleteByIdRemoveItensEmCascata() = runTest {
        dao.upsertStatementWithItems(
            statement(),
            listOf(item(id = "i1", seq = 1), item(id = "i2", seq = 2))
        )

        dao.deleteById("s1")

        assertNull(dao.getById("s1"))
        assertTrue(dao.getItems("s1").isEmpty())
    }
}
