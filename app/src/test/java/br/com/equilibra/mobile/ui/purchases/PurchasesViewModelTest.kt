package br.com.equilibra.mobile.ui.purchases

import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import java.io.IOException

/**
 * Task 6.8 — Lista de compras de um cartão.
 *
 * - Filtra pelo cartão recebido (nunca mistura compras de outro cartão)
 * - Cache-first e depois sincroniza da API
 * - Falha de sync não apaga a lista já exibida (só sinaliza o erro)
 * - Cancelamento pede confirmação e só remove da lista após confirmação remota
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PurchasesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PurchaseRepository
    private lateinit var cards: MutableSharedFlow<List<PurchaseEntity>>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        cards = MutableSharedFlow(replay = 1)
        every { repository.observeByCard(any()) } returns cards
        coEvery { repository.refreshByCard(any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entity(id: String = "p1", status: String = "ACTIVE", cardId: String = "c1") =
        PurchaseEntity(
            id = id,
            creditCardId = cardId,
            categoryId = "cat-1",
            description = "Notebook",
            totalAmount = "100.00",
            purchaseDate = "2026-10-15",
            purchaseAt = "2026-10-15T14:30:00Z",
            installmentCount = 2,
            cycleMonth = "2026-11-01",
            status = status,
            createdAt = "2026-10-15T14:30:00Z",
            updatedAt = "2026-10-15T14:30:00Z"
        )

    private fun viewModel(cardId: String = "c1") = PurchasesViewModel(repository, cardId)

    @Test
    fun carregaDoCacheESincroniza() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("p1"), state.purchases.map { it.id })
        coVerify(exactly = 1) { repository.refreshByCard("c1") }
    }

    @Test
    fun listaVaziaMostraEstadoVazioSemErro() = runTest {        every { repository.observeByCard("c1") } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.purchases.isEmpty())
        assertEquals(null, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun falhaDeSyncPreservaComprasDoCache() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        coEvery { repository.refreshByCard("c1") } returns ApiResult.NetworkError(IOException("offline"))
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("p1"), state.purchases.map { it.id })
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Sem conexão"))
    }

    @Test
    fun erroDeSyncPodeSerDispensado() = runTest {
        coEvery { repository.refreshByCard("c1") } returns ApiResult.HttpError(500, "boom", null)
        val vm = viewModel()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        vm.dismissError()
        assertEquals(null, vm.uiState.value.error)
        assertTrue(vm.uiState.value.purchases.isEmpty())
    }

    @Test
    fun cancelamentoPedeConfirmacaoAntesDaRemota() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick("p1")
        advanceUntilIdle()

        assertEquals("p1", vm.uiState.value.confirmCancelId)
        coVerify(exactly = 0) { repository.cancel(any()) }
    }

    @Test
    fun cancelamentoConfirmadoChamaRemota() = runTest {
        val active = entity()
        every { repository.observeByCard("c1") } returns flowOf(listOf(active))
        coEvery { repository.cancel("p1") } returns ApiResult.Success(active.copy(status = "CANCELLED"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick("p1")
        vm.onConfirmCancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancel("p1") }
        assertEquals(null, vm.uiState.value.confirmCancelId)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun cancelamentoComFalhaMostraErroEMantemLista() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        coEvery { repository.cancel("p1") } returns ApiResult.HttpError(409, "conf", null)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick("p1")
        vm.onConfirmCancel()
        advanceUntilIdle()

        assertEquals(listOf("p1"), vm.uiState.value.purchases.map { it.id })
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isCancelling)
    }

    @Test
    fun cancelamentoDeRedePreservaListaESinaliza() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        coEvery { repository.cancel("p1") } returns ApiResult.NetworkError(IOException("offline"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick("p1")
        vm.onConfirmCancel()
        advanceUntilIdle()

        assertEquals(listOf("p1"), vm.uiState.value.purchases.map { it.id })
        assertTrue(vm.uiState.value.error!!.contains("Sem conexão"))
    }

    @Test
    fun dispensaConfirmacaoNaoCancela() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onCancelClick("p1")
        vm.onDismissCancel()
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.confirmCancelId)
        coVerify(exactly = 0) { repository.cancel(any()) }
    }

    @Test
    fun observaSomenteOCartaoRecebido() = runTest {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity(cardId = "c1")))
        val vm = viewModel("c1")
        advanceUntilIdle()

        // O filtro é do DAO (query por cartão); aqui garantimos que o VM
        // pede exatamente o cartão da rota e não uma lista global.
        coVerify(exactly = 1) { repository.observeByCard("c1") }
        coVerify(exactly = 0) { repository.observeByCard("c2") }
        assertEquals(listOf("c1"), vm.uiState.value.purchases.map { it.creditCardId })
    }
}
