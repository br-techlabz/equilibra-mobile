package br.com.equilibra.mobile.data.local.purchase

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
 * Task 6.6 — Persistência das compras de cartão.
 *
 * Regras que este teste trava:
 * - decimal permanece String (centavos nunca viram Double)
 * - parcelas são filhas da compra e preservam cada centavo distribuído
 * - listagem por cartão é ordenada por purchaseDate DESC (igual ao backend)
 * - trocar o status (cancelar) não apaga parcelas — Task 6.8 depende disso
 */
@RunWith(RobolectricTestRunner::class)
class PurchaseDaoTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: PurchaseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.purchaseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun purchase(
        id: String = "p1",
        cardId: String = "c1",
        purchaseDate: String = "2026-10-15",
        status: String = "ACTIVE",
        totalAmount: String = "100.00"
    ) = PurchaseEntity(
        id = id,
        creditCardId = cardId,
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = totalAmount,
        purchaseDate = purchaseDate,
        purchaseAt = "2026-10-15T14:30:00Z",
        installmentCount = 3,
        cycleMonth = "2026-11-01",
        status = status,
        createdAt = "2026-10-15T14:30:00Z",
        updatedAt = "2026-10-15T14:30:00Z"
    )

    private fun installment(
        id: String = "i1",
        purchaseId: String = "p1",
        seq: Int = 1,
        amount: String = "33.34",
        cycle: String = "2026-11-01"
    ) = InstallmentEntity(
        id = id,
        purchaseId = purchaseId,
        sequenceNumber = seq,
        totalInstallments = 3,
        principalAmount = amount,
        cycleMonth = cycle
    )

    @Test
    fun upsertEPreservaDecimalComoCentavo() = runTest {
        dao.upsertPurchase(purchase(totalAmount = "1234567.89"))
        dao.upsertInstallment(installment(amount = "411522.63"))

        val got = dao.getPurchaseById("p1")
        assertEquals("1234567.89", got!!.totalAmount)
        assertEquals("411522.63", dao.getInstallments("p1").single().principalAmount)
    }

    @Test
    fun parcelasFicamOrdenadasPorSequencia() = runTest {
        dao.upsertPurchase(purchase())
        dao.upsertInstallments(
            listOf(
                installment(id = "i3", seq = 3, amount = "33.33", cycle = "2027-01-01"),
                installment(id = "i1", seq = 1, amount = "33.34", cycle = "2026-11-01"),
                installment(id = "i2", seq = 2, amount = "33.33", cycle = "2026-12-01")
            )
        )

        val parcelas = dao.getInstallments("p1")

        assertEquals(listOf(1, 2, 3), parcelas.map { it.sequenceNumber })
        assertEquals(listOf("33.34", "33.33", "33.33"), parcelas.map { it.principalAmount })
        assertEquals("2026-11-01", parcelas.first().cycleMonth)
    }

    @Test
    fun listaPorCartaoVemDaMaisRecenteParaAMaisAntiga() = runTest {
        dao.upsertPurchases(
            listOf(
                purchase(id = "p1", purchaseDate = "2026-09-01"),
                purchase(id = "p2", purchaseDate = "2026-10-15"),
                purchase(id = "p3", purchaseDate = "2026-10-01"),
                purchase(id = "outro", cardId = "c2", purchaseDate = "2026-10-20")
            )
        )

        val doCartao = dao.observeByCard("c1").first()

        assertEquals(listOf("p2", "p3", "p1"), doCartao.map { it.id })
    }

    @Test
    fun cancelarMantemParcelasEPreservaShape() = runTest {
        dao.upsertPurchase(purchase(status = "ACTIVE"))
        dao.upsertInstallments(listOf(installment(id = "i1", seq = 1), installment(id = "i2", seq = 2, amount = "33.33"), installment(id = "i3", seq = 3, amount = "33.33")))

        dao.upsertPurchase(purchase(status = "CANCELLED"))

        val got = dao.getPurchaseById("p1")!!
        assertEquals("CANCELLED", got.status)
        assertEquals(3, dao.getInstallments("p1").size)
    }

    @Test
    fun purchaseAtNuloEAceito() = runTest {
        dao.upsertPurchase(purchase().copy(purchaseAt = null))

        assertNull(dao.getPurchaseById("p1")!!.purchaseAt)
    }

    @Test
    fun deleteNotInDoCartaoRemoveSoAsAusentesEParcelasJunto() = runTest {
        dao.upsertPurchases(listOf(purchase(id = "p1"), purchase(id = "p2")))
        dao.upsertInstallments(
            listOf(
                installment(id = "i1", purchaseId = "p1", seq = 1),
                installment(id = "i2", purchaseId = "p2", seq = 1)
            )
        )

        dao.deleteByCardNotInSafe("c1", listOf("p1"))

        assertEquals(listOf("p1"), dao.observeByCard("c1").first().map { it.id })
        assertEquals(1, dao.getInstallments("p1").size)
        assertTrue(dao.getInstallments("p2").isEmpty())
    }
}
