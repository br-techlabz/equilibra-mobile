package br.com.equilibra.mobile.ui.purchases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.purchase.InstallmentEntity
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Task 6.9 — Detalhe da compra.
 *
 * - Lê parcelas e ciclo da resposta real da API (nunca infere campos)
 * - Compra cancelada continua acessível, com o status explícito
 * - Compra não encontrada (404) vira estado terminal, não erro genérico
 * - Falha de rede mostra o cache quando existe, senão erro recuperável
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PurchaseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun purchase(id: String = "p1", status: String = "ACTIVE") = PurchaseEntity(
        id = id,
        creditCardId = "c1",
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = "100.00",
        purchaseDate = "2026-10-15",
        purchaseAt = "2026-10-15T14:30:00Z",
        installmentCount = 3,
        cycleMonth = "2026-11-01",
        status = status,
        createdAt = "2026-10-15T14:30:00Z",
        updatedAt = "2026-10-15T14:30:00Z"
    )

    private fun installments() = listOf(
        InstallmentEntity("p1:1", "p1", 1, 3, "33.34", "2026-11-01"),
        InstallmentEntity("p1:2", "p1", 2, 3, "33.33", "2026-12-01"),
        InstallmentEntity("p1:3", "p1", 3, 3, "33.33", "2027-01-01")
    )

    private fun viewModel(id: String = "p1") = PurchaseDetailViewModel(
        repository,
        SavedStateHandle(mapOf("purchaseId" to id))
    )

    private fun stub(api: ApiResult<PurchaseEntity>, cached: PurchaseEntity? = null) {
        coEvery { repository.refreshById(any()) } returns api
        coEvery { repository.getCached(any()) } returns cached
        coEvery { repository.getInstallmentsCached(any()) } returns installments()
    }

    @Test
    fun carregaCompraEParcelasDaApi() = runTest {
        stub(ApiResult.Success(purchase()))
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PurchaseDetailUiState.Success)
        val s = state as PurchaseDetailUiState.Success
        assertEquals("Notebook", s.purchase.description)
        assertEquals("100.00", s.purchase.totalAmount)
        assertEquals(3, s.installments.size)
        assertEquals(listOf("33.34", "33.33", "33.33"), s.installments.map { it.principalAmount })
        assertEquals(listOf(1, 2, 3), s.installments.map { it.sequenceNumber })
    }

    @Test
    fun parcelasMantemValoresECiclosDoBackend() = runTest {
        stub(ApiResult.Success(purchase()))
        val vm = viewModel()
        advanceUntilIdle()

        val s = vm.uiState.value as PurchaseDetailUiState.Success
        val soma = s.installments.fold(java.math.BigDecimal.ZERO) { acc, i ->
            acc.add(i.principalAmount.toBigDecimal())
        }
        // o detalhe não recalcula nada: os valores somam o total informado
        assertEquals(0, soma.compareTo(s.purchase.totalAmount.toBigDecimal()))
        assertEquals(listOf("2026-11-01", "2026-12-01", "2027-01-01"), s.installments.map { it.cycleMonth })
    }

    @Test
    fun compraCanceladaEEExibidaComStatus() = runTest {
        stub(ApiResult.Success(purchase(status = "CANCELLED")))
        val vm = viewModel()
        advanceUntilIdle()

        val s = vm.uiState.value as PurchaseDetailUiState.Success
        assertEquals("CANCELLED", s.purchase.status)
        assertFalse(s.canCancel)
    }

    @Test
    fun compraAtivaPodeSerCancelada() = runTest {
        stub(ApiResult.Success(purchase()))
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue((vm.uiState.value as PurchaseDetailUiState.Success).canCancel)
    }

    @Test
    fun nãoEncontradaViraEstadoTerminal() = runTest {
        stub(ApiResult.HttpError(404, "nf", null))
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is PurchaseDetailUiState.NotFound)
    }

    @Test
    fun falhaDeRedeComCacheMostraCacheEAviso() = runTest {
        stub(ApiResult.NetworkError(IOException("offline")), cached = purchase())
        val vm = viewModel()
        advanceUntilIdle()

        val s = vm.uiState.value as PurchaseDetailUiState.Success
        assertEquals("Notebook", s.purchase.description)
        assertNotNull(s.warning)
    }

    @Test
    fun falhaDeRedeSemCacheMostraErroRecuperavel() = runTest {
        stub(ApiResult.NetworkError(IOException("offline")), cached = null)
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PurchaseDetailUiState.Error)
        assertTrue((state as PurchaseDetailUiState.Error).message.contains("Sem conexão"))
    }

    @Test
    fun retryRecarrega() = runTest {
        stub(ApiResult.NetworkError(IOException("offline")), cached = null)
        val vm = viewModel()
        advanceUntilIdle()

        coEvery { repository.refreshById("p1") } returns ApiResult.Success(purchase())
        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is PurchaseDetailUiState.Success)
    }

    @Test
    fun cancelarSoMudaStatusAposConfirmacaoRemota() = runTest {
        val ativa = purchase()
        stub(ApiResult.Success(ativa))
        coEvery { repository.cancel("p1") } returns ApiResult.Success(ativa.copy(status = "CANCELLED"))
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue((vm.uiState.value as PurchaseDetailUiState.Success).canCancel)

        vm.onCancelClick()
        advanceUntilIdle()
        // pede confirmação antes de qualquer chamada remota
        coVerify(exactly = 0) { repository.cancel(any()) }
        assertTrue(vm.uiState.value.let { it is PurchaseDetailUiState.Success && it.confirmCancelVisible })

        vm.onConfirmCancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancel("p1") }
        val s = vm.uiState.value as PurchaseDetailUiState.Success
        assertEquals("CANCELLED", s.purchase.status)
        assertFalse(s.canCancel)
    }

    @Test
    fun cancelamentoComFalhaMantemStatusEExibeErro() = runTest {
        stub(ApiResult.Success(purchase()))
        coEvery { repository.cancel("p1") } returns ApiResult.HttpError(409, "conf", null)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick()
        vm.onConfirmCancel()
        advanceUntilIdle()

        val s = vm.uiState.value as PurchaseDetailUiState.Success
        assertEquals("ACTIVE", s.purchase.status)
        assertNotNull(s.error)
        assertFalse(s.isCancelling)
    }

    @Test
    fun dispensaConfirmacaoNaoCancela() = runTest {
        stub(ApiResult.Success(purchase()))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick()
        vm.onDismissCancel()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.cancel(any()) }
        assertFalse((vm.uiState.value as PurchaseDetailUiState.Success).confirmCancelVisible)
    }
}
