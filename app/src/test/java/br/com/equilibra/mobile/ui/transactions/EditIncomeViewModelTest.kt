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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditIncomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository

    private val incomeId = "i1"

    private fun cached(status: String = "ACTIVE", description: String = "Cache") = TransactionEntity(
        id = incomeId, type = "INCOME", status = status, description = description,
        occurredAt = "2026-10-05T12:00:00Z", amount = "100.00",
        destinationAccountId = "acc-2", categoryId = "cat-2", notes = "obs",
        createdAt = "2026-10-05T12:00:00Z", updatedAt = "2026-10-05T12:00:00Z"
    )

    private fun createViewModel(): EditIncomeViewModel =
        EditIncomeViewModel(repository, SavedStateHandle(mapOf("incomeId" to incomeId)))

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
    fun `prefill vem do cache e a api prevalece`() = runTest {
        coEvery { repository.getByIdCached(incomeId) } returns cached()
        coEvery { repository.refreshById("INCOME", incomeId) } returns
            ApiResult.Success(cached(description = "Servidor"))
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("Servidor", vm.formState.value.description)
        assertEquals("05/10/2026", vm.formState.value.occurredDate)
        assertEquals("acc-2", vm.formState.value.accountId)
        assertEquals("cat-2", vm.formState.value.categoryId)
        assertTrue(vm.editable.value)
    }

    @Test
    fun `receita cancelada nao permite edicao`() = runTest {
        coEvery { repository.getByIdCached(incomeId) } returns cached(status = "CANCELLED")
        coEvery { repository.refreshById("INCOME", incomeId) } returns
            ApiResult.Success(cached(status = "CANCELLED"))
        coEvery { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(cached())
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.editable.value)

        vm.onSaveClick()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sucesso chama updateIncome com valores normalizados`() = runTest {
        coEvery { repository.getByIdCached(incomeId) } returns cached()
        coEvery { repository.refreshById("INCOME", incomeId) } returns ApiResult.Success(cached())
        coEvery { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(cached(description = "Atualizada"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onDescriptionChange("Atualizada")
        vm.onAmountChange("250,50")
        vm.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.updateIncome(
                id = incomeId,
                description = "Atualizada",
                occurredAt = "2026-10-05T00:00:00Z",
                accountId = "acc-2",
                amount = "250.50",
                categoryId = "cat-2",
                notes = "obs",
                tagIds = null // intocadas → preserva tags existentes
            )
        }
        assertTrue(vm.isSuccess.value)
    }

    @Test
    fun `tags tocadas sao enviadas`() = runTest {
        coEvery { repository.getByIdCached(incomeId) } returns cached()
        coEvery { repository.refreshById("INCOME", incomeId) } returns ApiResult.Success(cached())
        coEvery { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(cached())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTagsChange(listOf("t9"))
        vm.onSaveClick()
        advanceUntilIdle()

        coVerify {
            repository.updateIncome(
                id = incomeId, description = any(), occurredAt = any(), accountId = any(),
                amount = any(), categoryId = any(), notes = any(), tagIds = listOf("t9")
            )
        }
    }

    @Test
    fun `erro http exibe mensagem e nao marca sucesso`() = runTest {
        coEvery { repository.getByIdCached(incomeId) } returns cached()
        coEvery { repository.refreshById("INCOME", incomeId) } returns ApiResult.Success(cached())
        coEvery { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) } returns
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
        coEvery { repository.getByIdCached(incomeId) } returns cached()
        coEvery { repository.refreshById("INCOME", incomeId) } returns ApiResult.Success(cached())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAmountChange("0")
        vm.onSaveClick()
        advanceUntilIdle()

        assertNotNull(vm.formState.value.amountError)
        coVerify(exactly = 0) { repository.updateIncome(any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}
