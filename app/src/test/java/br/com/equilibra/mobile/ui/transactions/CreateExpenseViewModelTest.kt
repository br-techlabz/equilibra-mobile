package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
class CreateExpenseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: CreateExpenseViewModel

    private val savedEntity = TransactionEntity(
        id = "new-1", type = "EXPENSE", status = "ACTIVE", description = "Nova",
        occurredAt = "2026-10-01T00:00:00Z", amount = "10.50",
        sourceAccountId = "acc-1", categoryId = "cat-1",
        createdAt = "2026-10-01T00:00:00Z", updatedAt = "2026-10-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = CreateExpenseViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm() {
        viewModel.onDescriptionChange("Mercado")
        viewModel.onAmountChange("150,90")
        viewModel.onDateChange("01/10/2026")
        viewModel.onAccountChange("acc-1")
        viewModel.onCategoryChange("cat-1")
        viewModel.onTagsChange(listOf("t1", "t2"))
        viewModel.onNotesChange("compra do mês")
    }

    @Test
    fun `validacao falha nao chama api`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)

        viewModel.onSaveClick()
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertNotNull(form.descriptionError)
        assertNotNull(form.amountError)
        coVerify(exactly = 0) { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sucesso envia valores normalizados e marca sucesso`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.createExpense(
                description = "Mercado",
                occurredAt = "2026-10-01T00:00:00Z",
                accountId = "acc-1",
                amount = "150.90",
                categoryId = "cat-1",
                notes = "compra do mês",
                tagIds = listOf("t1", "t2")
            )
        }
        assertTrue(viewModel.isSuccess.value)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `erro http exibe mensagem e nao marca sucesso`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(400, null, null)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertFalse(viewModel.isSuccess.value)
        assertFalse(viewModel.isSaving.value)
        assertTrue(viewModel.generalError.value!!.contains("400"))
    }

    @Test
    fun `erro de rede exibe mensagem`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.generalError.value!!.contains("Sem conexão"))
    }

    @Test
    fun `duplo toque nao duplica envio`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        viewModel.onSaveClick() // segundo toque imediato
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sem tags envia lista vazia`() = runTest {
        coEvery { repository.createExpense(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        viewModel.onDescriptionChange("Nova")
        viewModel.onAmountChange("10.00")
        viewModel.onDateChange("01/10/2026")
        viewModel.onAccountChange("acc-1")
        viewModel.onCategoryChange("cat-1")

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify {
            repository.createExpense(
                description = "Nova",
                occurredAt = "2026-10-01T00:00:00Z",
                accountId = "acc-1",
                amount = "10.00",
                categoryId = "cat-1",
                notes = null,
                tagIds = emptyList()
            )
        }
    }
}
