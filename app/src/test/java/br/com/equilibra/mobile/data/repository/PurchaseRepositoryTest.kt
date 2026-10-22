package br.com.equilibra.mobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.local.purchase.PurchaseDao
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.PurchaseApi
import br.com.equilibra.mobile.data.remote.dto.purchase.CreatePurchaseRequestDto
import br.com.equilibra.mobile.data.remote.dto.purchase.InstallmentDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto
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
 * Task 6.7 — Cache e sincronização de compras.
 *
 * Regras que este teste trava:
 * - API é a fonte da verdade: Room só muda depois de resposta bem sucedida
 * - sync por cartão remove do cache o que sumiu no servidor, sem tocar em
 *   compras de outros cartões
 * - falha (rede ou HTTP) preserva o cache anterior
 * - criar/cancelar não alteram o cache antes da confirmação do backend
 */
@RunWith(RobolectricTestRunner::class)
class PurchaseRepositoryTest {

    private lateinit var db: EquilibraDatabase
    private lateinit var dao: PurchaseDao
    private lateinit var api: PurchaseApi
    private lateinit var repository: PurchaseRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EquilibraDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.purchaseDao()
        api = mockk()
        repository = PurchaseRepository(api, dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun dto(
        id: String = "p1",
        cardId: String = "c1",
        status: String = "ACTIVE",
        purchaseDate: String = "2026-10-15",
        totalAmount: String = "100.00",
        installments: List<InstallmentDto> = listOf(
            InstallmentDto(1, 2, "50.01", "2026-11-01"),
            InstallmentDto(2, 2, "49.99", "2026-12-01")
        )
    ) = PurchaseDto(
        id = id,
        creditCardId = cardId,
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = totalAmount,
        purchaseDate = purchaseDate,
        purchaseAt = "2026-10-15T14:30:00Z",
        installmentCount = installments.size,
        cycleMonth = "2026-11-01",
        status = status,
        installments = installments
    )

    private fun seed(entity: PurchaseEntity) = runTest { dao.upsertPurchase(entity) }

    private fun entity(id: String = "p1", cardId: String = "c1", status: String = "ACTIVE") =
        PurchaseEntity(
            id = id,
            creditCardId = cardId,
            categoryId = "cat-1",
            description = "Antiga",
            totalAmount = "10.00",
            purchaseDate = "2026-09-01",
            purchaseAt = null,
            installmentCount = 1,
            cycleMonth = "2026-09-01",
            status = status,
            createdAt = "2026-09-01",
            updatedAt = "2026-09-01",
            syncedAt = null
        )

    // refresh

    @Test
    fun refreshByCardGravaComprasEParcelas() = runTest {
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.Success)
        val cache = dao.observeByCard("c1").first()
        assertEquals(1, cache.size)
        assertEquals("100.00", cache.single().totalAmount)
        assertEquals(listOf("50.01", "49.99"), dao.getInstallments("p1").map { it.principalAmount })
    }

    @Test
    fun refreshRemoveDoCartaoOsQueSumiramNoServidor() = runTest {
        seed(entity(id = "p1"))
        seed(entity(id = "p2"))
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto(id = "p2")))

        repository.refreshByCard("c1")

        assertEquals(listOf("p2"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshNaoTocaNasComprasDeOutroCartao() = runTest {
        seed(entity(id = "outro", cardId = "c2"))
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        repository.refreshByCard("c1")

        assertEquals(listOf("outro"), dao.observeByCard("c2").first().map { it.id })
    }

    @Test
    fun refreshComRedeForaPreservaCache() = runTest {
        seed(entity(id = "p1"))
        coEvery { api.listByCard("c1") } throws IOException("offline")

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(listOf("p1"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshComHttpErrorPreservaCache() = runTest {
        seed(entity(id = "p1"))
        coEvery { api.listByCard("c1") } returns Response.error(500, "boom".toResponseBody())

        val result = repository.refreshByCard("c1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(listOf("p1"), dao.observeByCard("c1").first().map { it.id })
    }

    @Test
    fun refreshRepetidoNaoDuplicaParcelas() = runTest {
        coEvery { api.listByCard("c1") } returns Response.success(listOf(dto()))

        repository.refreshByCard("c1")
        repository.refreshByCard("c1")

        assertEquals(2, dao.getInstallments("p1").size)
    }

    // criar

    @Test
    fun criarGravaNoCacheDepoisDoSucesso() = runTest {
        coEvery { api.create(any()) } returns Response.success(dto(id = "novo"))

        val result = repository.create(
            description = "Notebook",
            purchaseDate = "2026-10-15",
            purchaseAt = "2026-10-15T14:30:00Z",
            totalAmount = "100.00",
            installmentCount = 2,
            creditCardId = "c1",
            categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.Success)
        assertEquals("novo", dao.getPurchaseById("novo")!!.id)
        assertEquals(2, dao.getInstallments("novo").size)
    }

    @Test
    fun criarComFalhaNaoGravaNada() = runTest {
        coEvery { api.create(any()) } returns Response.error(409, "limite".toResponseBody())

        val result = repository.create(
            description = "Notebook", purchaseDate = "2026-10-15",
            purchaseAt = "2026-10-15T14:30:00Z", totalAmount = "100.00",
            installmentCount = 2, creditCardId = "c1", categoryId = "cat-1"
        )

        assertTrue(result is ApiResult.HttpError)
        assertTrue(dao.observeByCard("c1").first().isEmpty())
    }

    @Test
    fun criarEnviaValorJaNormalizadoSemReinterpretar() = runTest {
        // Padrão do projeto: a normalização monetária é do formulário (validators
        // de UI, como CreditCardFormValidator.normalizeLimit); o repository só
        // transporta a String — nunca converte para Double.
        var enviado: CreatePurchaseRequestDto? = null
        coEvery { api.create(any()) } answers {
            enviado = firstArg()
            Response.success(dto(id = "novo"))
        }

        repository.create(
            description = "Notebook", purchaseDate = "2026-10-15",
            purchaseAt = "2026-10-15T14:30:00Z", totalAmount = "1234.50",
            installmentCount = 2, creditCardId = "c1", categoryId = "cat-1"
        )

        assertEquals("1234.50", enviado!!.totalAmount)
    }

    // cancelar

    @Test
    fun cancelarAtualizaStatusNoCacheAposConfirmacao() = runTest {
        seed(entity(id = "p1", status = "ACTIVE"))
        coEvery { api.cancel("p1") } returns Response.success(dto(id = "p1", status = "CANCELLED"))

        val result = repository.cancel("p1")

        assertTrue(result is ApiResult.Success)
        assertEquals("CANCELLED", dao.getPurchaseById("p1")!!.status)
    }

    @Test
    fun cancelarComFalhaNaoMudaStatusLocal() = runTest {
        seed(entity(id = "p1", status = "ACTIVE"))
        coEvery { api.cancel("p1") } returns Response.error(409, "inativa".toResponseBody())

        val result = repository.cancel("p1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals("ACTIVE", dao.getPurchaseById("p1")!!.status)
    }

    @Test
    fun cancelarComRedeForaMantemCacheIntacto() = runTest {
        seed(entity(id = "p1", status = "ACTIVE"))
        coEvery { api.cancel("p1") } throws IOException("offline")

        repository.cancel("p1")

        assertEquals("ACTIVE", dao.getPurchaseById("p1")!!.status)
    }

    // detalhe

    @Test
    fun refreshByIdGravaCompraEParcelas() = runTest {
        coEvery { api.get("p1") } returns Response.success(dto(id = "p1"))

        val result = repository.refreshById("p1")

        assertTrue(result is ApiResult.Success)
        assertEquals("p1", dao.getPurchaseById("p1")!!.id)
        assertEquals(listOf("50.01", "49.99"), dao.getInstallments("p1").map { it.principalAmount })
    }

    @Test
    fun refreshByIdCom404PreservaCacheEPropaga() = runTest {
        seed(entity(id = "p1"))
        coEvery { api.get("p1") } returns Response.error(404, "nf".toResponseBody())

        val result = repository.refreshById("p1")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(404, (result as ApiResult.HttpError).code)
        assertEquals("p1", dao.getPurchaseById("p1")!!.id)
    }

    @Test
    fun refreshByIdComRedeForaPreservaCache() = runTest {
        seed(entity(id = "p1"))
        coEvery { api.get("p1") } throws IOException("offline")

        val result = repository.refreshById("p1")

        assertTrue(result is ApiResult.NetworkError)
        assertEquals("p1", dao.getPurchaseById("p1")!!.id)
    }

    @Test
    fun refreshByIdRepetidoNaoDuplicaParcelas() = runTest {
        coEvery { api.get("p1") } returns Response.success(dto(id = "p1"))

        repository.refreshById("p1")
        repository.refreshById("p1")

        assertEquals(2, dao.getInstallments("p1").size)
    }

    // leitura

    @Test
    fun leituraObservavelVemDoCacheSemChamarApi() = runTest {
        seed(entity(id = "p1"))

        assertEquals(listOf("p1"), repository.observeByCard("c1").first().map { it.id })
        coVerify(exactly = 0) { api.listByCard(any()) }
    }
}
