package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateIncomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: CreateIncomeViewModel

    private val savedEntity = TransactionEntity(
        id = "i-new", type = "INCOME", status = "ACTIVE", description = "Salário",
        occurredAt = "2026-10-05T00:00:00Z", amount = "5000.00",
        destinationAccountId = "acc-2", categoryId = "cat-2",
        createdAt = "2026-10-05T00:00:00Z", updatedAt = "2026-10-05T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = CreateIncomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm() {
        viewModel.onDescriptionChange("Salário")
        viewModel.onAmountChange("5000,00")
        viewModel.onDateChange("05/10/2026")
        viewModel.onAccountChange("acc-2")
        viewModel.onCategoryChange("cat-2")
        viewModel.onTagsChange(listOf("t1"))
        viewModel.onNotesChange("folha")
    }

    @Test
    fun `validacao falha nao chama api`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.descriptionError)
        coVerify(exactly = 0) { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sucesso envia valores normalizados para createIncome`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.createIncome(
                description = "Salário",
                occurredAt = "2026-10-05T00:00:00Z",
                accountId = "acc-2",
                amount = "5000.00",
                categoryId = "cat-2",
                notes = "folha",
                tagIds = listOf("t1")
            )
        }
        assertTrue(viewModel.isSuccess.value)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `erro http exibe mensagem com codigo`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(400, null, null)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertFalse(viewModel.isSuccess.value)
        assertTrue(viewModel.generalError.value!!.contains("400"))
    }

    @Test
    fun `erro de rede exibe mensagem`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.generalError.value!!.contains("Sem conexão"))
    }

    @Test
    fun `duplo toque nao duplica envio`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `valor negativo nao envia`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        viewModel.onDescriptionChange("Inválida")
        viewModel.onAmountChange("-10,00")
        viewModel.onDateChange("05/10/2026")
        viewModel.onAccountChange("acc-2")
        viewModel.onCategoryChange("cat-2")

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.amountError)
        coVerify(exactly = 0) { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sem notas envia null`() = runTest {
        coEvery { repository.createIncome(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        viewModel.onDescriptionChange("Salário")
        viewModel.onAmountChange("5000,00")
        viewModel.onDateChange("05/10/2026")
        viewModel.onAccountChange("acc-2")
        viewModel.onCategoryChange("cat-2")

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify {
            repository.createIncome(
                description = any(), occurredAt = any(), accountId = any(), amount = any(),
                categoryId = any(), notes = null, tagIds = emptyList()
            )
        }
        assertNull(viewModel.generalError.value)
    }
}
