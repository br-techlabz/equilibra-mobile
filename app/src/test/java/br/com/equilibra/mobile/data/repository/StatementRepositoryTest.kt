package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.statement.StatementDao
import br.com.equilibra.mobile.data.local.statement.StatementEntity
import br.com.equilibra.mobile.data.local.statement.StatementItemEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.StatementApi
import br.com.equilibra.mobile.data.remote.dto.statement.CloseStatementRequestDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementItemDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import java.io.IOException

/**
 * Task 7.2 — Cache e sincronização de faturas.
 *
 * Regras que este teste trava:
 * - API é a fonte da verdade: Room só muda depois de resposta bem sucedida
 * - refresh por cartão remove do cache o que sumiu no servidor, sem tocar
 *   nas faturas dos demais cartões
 * - current/detail/close gravam a resposta sem apagar o histórico
 * - falha (rede ou HTTP) preserva o cache anterior
 * - status e agregados são copiados do backend; o cliente não infere
 *   OVERDUE/PAID nem recalcula total/pago/pendente
 */
@RunWith(RobolectricTestRunner::class)
class StatementRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: StatementDao
    private lateinit var api: StatementApi
    private lateinit var repository: StatementRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.statementDao()
        api = mockk()
        repository = StatementRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun dto(
        id: String = "s1",
        cardId: String = "c1",
        status: String = "OPEN",
        cycleStart: String = "2026-10-03",
        cycleEnd: String = "2026-11-02",
        totalAmount: String = "100.00",
        paidAmount: String = "0.00",
        outstandingAmount: String = "100.00",
        items: List<StatementItemDto> = listOf(
            StatementItemDto("i1", "p1", "Notebook", 1, 2, "50.01", "2026-11-01"),
            StatementItemDto("i2", "p1", "Notebook", 2, 2, "49.99", "2026-12-01")
        )
    ) = StatementDto(
        id = id,
        creditCardId = cardId,
        cycleStart = cycleStart,
        cycleEnd = cycleEnd,
        closingAt = "2026-11-03T03:00:00Z",
        dueDate = "2026-11-10",
        status = status,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        items = items
    )

    private fun entity(
        id: String = "s1",
        cardId: String = "c1",
        cycleStart: String = "2026-09-03",
        status: String = "OPEN",
        dueDate: String = "2026-10-10"
    ) = StatementEntity(
        id = id,
        creditCardId = cardId,
        cycleStart = cycleStart,
        cycleEnd = "2026-10-02",
        closingAt = "2026-10-03T03:00:00Z",
        dueDate = dueDate,
        status = status,
        totalAmount = "10.00",
        paidAmount = "0.00",
        outstandingAmount = "10.00",
        syncedAt = null
    )

    private fun item(id: String = "i1", statementId: String = "s1") = StatementItemEntity(
        id = id,
        statementId = statementId,
        purchaseId = "p1",
        description = "Antiga",
        sequenceNumber = 1,
        totalInstallments = 1,
        amount = "10.00",
        cycleMonth = "2026-09-01"
    )

    private fun seed(entity: StatementEntity) = runTest { dao.upsertStatement(entity) }

    // refresh por cartão

    @Test
    fun refreshByCardGravaFaturasEItens() = runTest {
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.Success)
        val cache = dao.observeByCard("c1").first()
        assertEquals(1, cache.size)
        assertEquals("100.00", cache.single().totalAmount)
        assertEquals("0.00", cache.single().paidAmount)
        assertEquals("100.00", cache.single().outstandingAmount)
        assertEquals(listOf("50.01", "49.99"), dao.getItems("s1").map { it.amount })
    }

    @Test
    fun refreshByCardRemoveDoCartaoAsQueSumiramNoServidor() = runTest {
        seed(entity(id = "s1"))
        seed(entity(id = "s2", cycleStart = "2026-10-03"))
        coEvery { api.listByCard("c1") } returns Response.success(
            listOf(dto(id = "s2", cycleStart = "2026-10-03"))
        )

        repository.refreshByCard("c1")

        assertEquals(listOf("s2"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshByCardNaoTocaNasFaturasDeOutroCartao() = runTest {
        seed(entity(id = "outro", cardId = "c2"))
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        repository.refreshByCard("c1")

        assertEquals(listOf("outro"), dao.observeByCard("c2").first().map { it.id })
    }

    @Test
    fun refreshByCardComRedeForaPreservaCache() = runTest {
        seed(entity(id = "s1"))
        coEvery { api.listByCard("c1") } throws IOException("offline")

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(listOf("s1"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshByCardComHttpErrorPreservaCache() = runTest {
        seed(entity(id = "s1"))
        coEvery { api.listByCard("c1") } returns Response.error(500, "boom".toResponseBody())

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(listOf("s1"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshByCardRepetidoNaoDuplicaItens() = runTest {
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        repository.refreshByCard("c1")
        repository.refreshByCard("c1")

        assertEquals(2, dao.getItems("s1").size)
    }

    // fatura atual / detalhe

    @Test
    fun refreshCurrentGravaFaturaAtualSemApagarHistorico() = runTest {
        seed(entity(id = "historico"))
        coEvery { api.current("c1") } returns Response.success(dto(id = "atual", cycleStart = "2026-10-03"))

        val result = repository.refreshCurrent("c1")

        assertTrue(result is ApiResult.Success)
        assertEquals("atual", (result as ApiResult.Success).data.id)
        assertEquals(
            listOf("atual", "historico"),
            dao.observeByCard("c1").first().map { it.id }
        )
        assertEquals(2, dao.getItems("atual").size)
    }

    @Test
    fun refreshCurrentComErroPreservaCache() = runTest {
        seed(entity(id = "s1"))
        coEvery { api.current("c1") } returns Response.error(500, "boom".toResponseBody())

        val result = repository.refreshCurrent("c1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(listOf("s1"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshByIdGravaFaturaEItens() = runTest {
        coEvery { api.get("s1") } returns Response.success(dto(id = "s1"))

        val result = repository.refreshById("s1")

        assertTrue(result is ApiResult.Success)
        assertEquals("s1", dao.getById("s1")!!.id)
        assertEquals(listOf("50.01", "49.99"), dao.getItems("s1").map { it.amount })
    }

    @Test
    fun refreshByIdCom404PreservaCacheEPropaga() = runTest {
        seed(entity(id = "s1"))
        coEvery { api.get("s1") } returns Response.error(404, "nf".toResponseBody())

        val result = repository.refreshById("s1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(404, (result as ApiResult.HttpError).code)
        assertEquals("s1", dao.getById("s1")!!.id)
    }

    @Test
    fun refreshByIdComRedeForaPreservaCache() = runTest {
        seed(entity(id = "s1"))
        coEvery { api.get("s1") } throws IOException("offline")

        val result = repository.refreshById("s1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("s1", dao.getById("s1")!!.id)
    }

    // fechamento

    @Test
    fun closeEnviaCycleMonthESincronizaResposta() = runTest {
        seed(entity(id = "s1", status = "OPEN"))
        coEvery { api.close("c1", any()) } returns Response.success(dto(id = "s1", status = "CLOSED"))

        val result = repository.close("c1", "2026-11-01")

        assertTrue(result is ApiResult.Success)
        assertEquals("CLOSED", dao.getById("s1")!!.status)
        assertEquals(2, dao.getItems("s1").size)
        coVerify {
            api.close("c1", CloseStatementRequestDto(cycleMonth = "2026-11-01"))
        }
    }

    @Test
    fun closeComFalhaNaoMudaStatusLocal() = runTest {
        seed(entity(id = "s1", status = "OPEN"))
        coEvery { api.close("c1", any()) } returns Response.error(409, "fechada".toResponseBody())

        val result = repository.close("c1", "2026-11-01")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("OPEN", dao.getById("s1")!!.status)
    }

    @Test
    fun closeComRedeForaMantemCacheIntacto() = runTest {
        seed(entity(id = "s1", status = "OPEN"))
        coEvery { api.close("c1", any()) } throws IOException("offline")

        repository.close("c1", "2026-11-01")

        assertEquals("OPEN", dao.getById("s1")!!.status)
    }

    // leitura cache-first e autoridade do backend

    @Test
    fun leituraObservavelVemDoCacheSemChamarApi() = runTest {
        dao.upsertStatementWithItems(entity(id = "s1"), listOf(item(id = "i1")))

        assertEquals(listOf("s1"), repository.observeByCard("c1").first().map { it.id })
        assertEquals("s1", repository.observeLatestByCard("c1").first()?.id)
        assertEquals("s1", repository.observeStatement("s1").first()?.id)
        assertEquals(listOf("i1"), repository.observeItems("s1").first().map { it.id })
        assertEquals("s1", repository.getCached("s1")?.id)
        assertEquals(listOf("s1"), repository.getCachedByCard("c1").map { it.id })
        assertEquals(listOf("i1"), repository.getItemsCached("s1").map { it.id })

        coVerify(exactly = 0) {
            api.listByCard(any())
            api.current(any())
            api.get(any())
            api.close(any(), any())
        }
    }

    @Test
    fun naoInfereStatusVencidoNemAgregados() = runTest {
        // dueDate no passado e status OPEN: o backend continua sendo a única
        // autoridade. O repository não troca para OVERDUE nem recalcula nada.
        dao.upsertStatement(
            entity(id = "s1", status = "OPEN", dueDate = "2020-01-01")
                .copy(totalAmount = "100.00", paidAmount = "50.00", outstandingAmount = "50.00")
        )

        val cached = repository.observeByCard("c1").first().single()

        assertEquals("OPEN", cached.status)
        assertEquals("100.00", cached.totalAmount)
        assertEquals("50.00", cached.paidAmount)
        assertEquals("50.00", cached.outstandingAmount)
    }
}
