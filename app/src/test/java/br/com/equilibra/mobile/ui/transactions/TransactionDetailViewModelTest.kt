package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import br.com.equilibra.mobile.domain.model.transaction.TransactionDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Task 4.12 — Detalhe da transação.
 *
 * Cobre: carga por ID via endpoint do tipo, estados loading/erro/retry,
 * carga de nomes (conta/categoria), cancelamento por tipo com anti duplo
 * envio e sem ação para tipo sem endpoint de cancelamento.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository

    private val detailId = "e1"

    private fun detail(
        type: String = "EXPENSE",
        status: String = "ACTIVE",
        description: String = "Mercado"
    ) = TransactionDetail(
        id = detailId,
        type = type,
        status = status,
        description = description,
        occurredAt = "2026-10-01T12:00:00Z",
        amount = "42.90",
        sourceAccountId = "acc-1",
        destinationAccountId = null,
        categoryId = "cat-1",
        notes = "obs",
        createdAt = "2026-10-01T12:00:00Z",
        updatedAt = "2026-10-01T12:00:00Z",
        tagNames = listOf("mercado")
    )

    private fun createViewModel(type: String = "EXPENSE", id: String = detailId) =
        TransactionDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf("transactionType" to type, "transactionId" to id)
            )
        )

    private fun successState(vm: TransactionDetailViewModel) =
        vm.uiState.value as TransactionDetailUiState.Success

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.Success(detail())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- carga ----------

    @Test
    fun `carrega detalhe do tipo e id da rota`() = runTest {
        val vm = createViewModel(type = "INCOME", id = "i9")
        coEvery { repository.getDetail("INCOME", "i9") } returns
            ApiResult.Success(detail(type = "INCOME", description = "Salário"))
        advanceUntilIdle()

        assertEquals("Salário", successState(vm).detail.description)
        coVerify { repository.getDetail("INCOME", "i9") }
    }

    @Test
    fun `estado inicial e loading`() {
        val vm = createViewModel()

        assertTrue(vm.uiState.value is TransactionDetailUiState.Loading)
    }

    @Test
    fun `erro de rede vira Error com mensagem de conexao`() = runTest {
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.NetworkError(IOException("offline"))
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionDetailUiState.Error
        assertTrue(state.message.contains("conexão", ignoreCase = true))
    }

    @Test
    fun `erro http 404 informa codigo`() = runTest {
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.HttpError(404, null, null)
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as TransactionDetailUiState.Error
        assertTrue(state.message.contains("404"))
    }

    @Test
    fun `retry recarrega e sai do erro`() = runTest {
        coEvery { repository.getDetail(any(), any()) } returns
            ApiResult.NetworkError(IOException("offline"))
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { repository.getDetail(any(), any()) } returns ApiResult.Success(detail())
        vm.retry()
        advanceUntilIdle()

        assertEquals("Mercado", successState(vm).detail.description)
    }

    // ---------- retorno à tela (ON_RESUME) — bug: edição não atualizava o detalhe ----------

    @Test
    fun `primeiro resume nao recarrega pois init ja carregou`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onScreenResumed()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getDetail("EXPENSE", detailId) }
    }

    @Test
    fun `resume apos edicao recarrega e mostra dados atualizados`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // primeiro resume = entrada na tela, ignorado
        vm.onScreenResumed()
        advanceUntilIdle()

        // edição em outra rota alterou a descrição no backend
        coEvery { repository.getDetail(any(), any()) } returns
            ApiResult.Success(detail(description = "Mercado editado"))
        vm.onScreenResumed()
        advanceUntilIdle()

        assertEquals("Mercado editado", successState(vm).detail.description)
        coVerify(exactly = 2) { repository.getDetail(any(), any()) }
    }

    @Test
    fun `resume com estado de erro nao dispara recargaAutomatica`() = runTest {
        coEvery { repository.getDetail(any(), any()) } returns
            ApiResult.NetworkError(IOException("offline"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onScreenResumed()
        vm.onScreenResumed()
        advanceUntilIdle()

        // erro tem retry próprio; resume não deve empilhar chamadas
        coVerify(exactly = 1) { repository.getDetail(any(), any()) }
        assertTrue(vm.uiState.value is TransactionDetailUiState.Error)
    }

    // ---------- ação de cancelar ----------

    @Test
    fun `cancelar despesa usa endpoint de despesa e marca cancelada`() = runTest {
        coEvery { repository.cancelExpense(detailId) } returns ApiResult.Success(mockk(relaxed = true))
        val vm = createViewModel(type = "EXPENSE")
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelExpense(detailId) }
        assertEquals("CANCELLED", successState(vm).detail.status)
        assertFalse(successState(vm).detail.canCancel)
        assertNotNull(successState(vm).notice)
    }

    @Test
    fun `cancelar receita usa endpoint de receita`() = runTest {
        val vm = createViewModel(type = "INCOME", id = "i1")
        coEvery { repository.getDetail("INCOME", "i1") } returns
            ApiResult.Success(detail(type = "INCOME"))
        coEvery { repository.cancelIncome("i1") } returns ApiResult.Success(mockk(relaxed = true))
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelIncome("i1") }
        coVerify(exactly = 0) { repository.cancelExpense(any()) }
    }

    @Test
    fun `cancelar transferencia usa endpoint de transferencia`() = runTest {
        val vm = createViewModel(type = "TRANSFER", id = "tr1")
        coEvery { repository.getDetail("TRANSFER", "tr1") } returns
            ApiResult.Success(detail(type = "TRANSFER"))
        coEvery { repository.cancelTransfer("tr1") } returns ApiResult.Success(mockk(relaxed = true))
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelTransfer("tr1") }
    }

    @Test
    fun `cancelamento nao dispara duas vezes`() = runTest {
        val vm = createViewModel()
        coEvery { repository.cancelExpense(any()) } returns ApiResult.Success(mockk(relaxed = true))
        advanceUntilIdle()

        vm.cancel()
        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelExpense(any()) }
    }

    @Test
    fun `cancelamento de pagamento de fatura nao chama api`() = runTest {
        val vm = createViewModel(type = "STATEMENT_PAYMENT", id = "sp1")
        coEvery { repository.getDetail("STATEMENT_PAYMENT", "sp1") } returns
            ApiResult.Success(detail(type = "STATEMENT_PAYMENT"))
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.cancelExpense(any()) }
        coVerify(exactly = 0) { repository.cancelIncome(any()) }
        coVerify(exactly = 0) { repository.cancelTransfer(any()) }
    }

    @Test
    fun `falha ao cancelar preserva status ativo e informa erro`() = runTest {
        val vm = createViewModel()
        coEvery { repository.cancelExpense(any()) } returns ApiResult.HttpError(409, null, null)
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        val state = successState(vm)
        assertEquals("ACTIVE", state.detail.status)
        assertTrue(state.error!!.contains("409"))
    }

    @Test
    fun `limpar aviso e erro`() = runTest {
        val vm = createViewModel()
        coEvery { repository.cancelExpense(any()) } returns ApiResult.Success(mockk(relaxed = true))
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        vm.clearNotice()
        vm.clearError()

        assertNull(successState(vm).notice)
        assertNull(successState(vm).error)
    }
}
