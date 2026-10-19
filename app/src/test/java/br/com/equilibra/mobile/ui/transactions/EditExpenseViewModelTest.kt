package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditExpenseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository

    private val expenseId = "e1"

    private fun cached(status: String = "ACTIVE", description: String = "Cache") = TransactionEntity(
        id = expenseId, type = "EXPENSE", status = status, description = description,
        occurredAt = "2026-10-01T12:00:00Z", amount = "10.00",
        sourceAccountId = "acc-1", categoryId = "cat-1", notes = "obs",
        createdAt = "2026-10-01T12:00:00Z", updatedAt = "2026-10-01T12:00:00Z"
    )

    private fun updated() = cached().copy(description = "Atualizada", amount = "20.00")

    private fun createViewModel(): EditExpenseViewModel =
        EditExpenseViewModel(repository, SavedStateHandle(mapOf("expenseId" to expenseId)))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prefill vem do cache e o refresh da api prevalece`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns
            ApiResult.Success(cached(description = "Servidor"))
        val vm = createViewModel()
        advanceUntilIdle()

        // Cache preenche primeiro; API prevalece no estado final
        assertEquals("Servidor", vm.formState.value.description)
        assertEquals("01/10/2026", vm.formState.value.occurredDate)
        assertEquals("obs", vm.formState.value.notes)
        assertEquals("acc-1", vm.formState.value.accountId)
        assertEquals("cat-1", vm.formState.value.categoryId)
    }

    @Test
    fun `erro de rede sem cache expoe mensagem`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns null
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        val vm = createViewModel()
        advanceUntilIdle()

        assertNotNull(vm.generalError.value)
        assertTrue(vm.generalError.value!!.contains("conexão"))
    }

    @Test
    fun `despesa cancelada nao permite edicao`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached(status = "CANCELLED")
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns
            ApiResult.Success(cached(status = "CANCELLED"))
        coEvery { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(updated())
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.editable.value)
        assertNotNull(vm.generalError.value)

        // Salvar em despesa cancelada não chama a API
        vm.onSaveClick()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sucesso chama update com valores normalizados`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(updated())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onDescriptionChange("Atualizada")
        vm.onAmountChange("20,00")
        vm.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateExpense(
                id = expenseId,
                description = "Atualizada",
                occurredAt = "2026-10-01T00:00:00Z",
                accountId = "acc-1",
                amount = "20.00",
                categoryId = "cat-1",
                notes = "obs",
                // Tags intocadas → null preserva as existentes no backend
                tagIds = null
            )
        }
        assertTrue(vm.isSuccess.value)
    }

    @Test
    fun `tags tocadas sao enviadas no update`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(updated())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTagsChange(listOf("t1"))
        vm.onSaveClick()
        advanceUntilIdle()

        coVerify {
            repository.updateExpense(
                id = expenseId,
                description = any(),
                occurredAt = any(),
                accountId = any(),
                amount = any(),
                categoryId = any(),
                notes = any(),
                tagIds = listOf("t1")
            )
        }
    }

    @Test
    fun `erro http exibe mensagem e nao marca sucesso`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(409, null, null)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSaveClick()
        advanceUntilIdle()

        assertFalse(vm.isSuccess.value)
        assertTrue(vm.generalError.value!!.contains("409"))
    }

    @Test
    fun `validacao falha nao chama api`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onDescriptionChange("   ")
        vm.onSaveClick()
        advanceUntilIdle()

        assertNotNull(vm.formState.value.descriptionError)
        coVerify(exactly = 0) { repository.updateExpense(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // ---------- cancelamento (Task 4.10) ----------

    @Test
    fun `cancel chama api e marca cancelada na tela`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.cancelExpense(expenseId) } returns
            ApiResult.Success(cached(status = "CANCELLED"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelExpense(expenseId) }
        assertFalse(vm.editable.value)
        assertNotNull(vm.notice.value) // aviso de sucesso (não é erro)
        assertNull(vm.generalError.value)
    }

    @Test
    fun `cancel duplo nao repete envio`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.cancelExpense(expenseId) } returns
            ApiResult.Success(cached(status = "CANCELLED"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancel()
        vm.cancel() // segundo toque imediato
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelExpense(expenseId) }
    }

    @Test
    fun `cancel ja cancelada nao chama api`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached(status = "CANCELLED")
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns
            ApiResult.Success(cached(status = "CANCELLED"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.cancelExpense(any()) }
    }

    @Test
    fun `cancel com erro preserva status editavel`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.cancelExpense(expenseId) } returns ApiResult.HttpError(409, null, null)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        assertTrue(vm.editable.value) // não mudou para cancelada
        assertTrue(vm.generalError.value!!.contains("409"))
        assertNull(vm.notice.value)
    }

    @Test
    fun `cancel com erro de rede nao altera cache`() = runTest {
        coEvery { repository.getByIdCached(expenseId) } returns cached()
        coEvery { repository.refreshById("EXPENSE", expenseId) } returns ApiResult.Success(cached())
        coEvery { repository.cancelExpense(expenseId) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.cancel()
        advanceUntilIdle()

        assertTrue(vm.editable.value)
        assertTrue(vm.generalError.value!!.contains("Sem conexão"))
    }
}
