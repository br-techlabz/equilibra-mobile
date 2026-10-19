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
class CreateTransferViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: CreateTransferViewModel

    private val savedEntity = TransactionEntity(
        id = "tr-new", type = "TRANSFER", status = "ACTIVE", description = "Poupança",
        occurredAt = "2026-10-10T00:00:00Z", amount = "250.75",
        sourceAccountId = "acc-1", destinationAccountId = "acc-2", categoryId = "cat-1",
        createdAt = "2026-10-10T00:00:00Z", updatedAt = "2026-10-10T00:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = CreateTransferViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fillValidForm() {
        viewModel.onDescriptionChange("Poupança")
        viewModel.onAmountChange("250,75")
        viewModel.onDateChange("10/10/2026")
        viewModel.onSourceAccountChange("acc-1")
        viewModel.onDestinationAccountChange("acc-2")
        viewModel.onCategoryChange("cat-1")
        viewModel.onNotesChange("reserva")
        viewModel.onTagsChange(listOf("t1"))
    }

    @Test
    fun `sucesso envia contrato com origem e destino`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.createTransfer(
                description = "Poupança",
                occurredAt = "2026-10-10T00:00:00Z",
                sourceAccountId = "acc-1",
                destinationAccountId = "acc-2",
                amount = "250.75",
                categoryId = "cat-1",
                notes = "reserva",
                tagIds = listOf("t1")
            )
        }
        assertTrue(viewModel.isSuccess.value)
        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `origem igual ao destino nao chama api`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()
        viewModel.onDestinationAccountChange("acc-1") // igual à origem

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.destinationAccountError)
        coVerify(exactly = 0) { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `validacao falha nao chama api`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.descriptionError)
        coVerify(exactly = 0) { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `erro http exibe mensagem e nao marca sucesso`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(409, null, null)
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertFalse(viewModel.isSuccess.value)
        assertTrue(viewModel.generalError.value!!.contains("409"))
    }

    @Test
    fun `erro de rede exibe mensagem`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.NetworkError(java.io.IOException("offline"))
        fillValidForm()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.generalError.value!!.contains("Sem conexão"))
    }

    @Test
    fun `duplo toque nao duplica envio`() = runTest {
        coEvery { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(savedEntity)
        fillValidForm()

        viewModel.onSaveClick()
        viewModel.onSaveClick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.createTransfer(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `trocar origem limpa erro de destino`() = runTest {
        fillValidForm()
        viewModel.onDestinationAccountChange("acc-1") // igual à origem
        viewModel.onSaveClick()
        advanceUntilIdle()
        assertNotNull(viewModel.formState.value.destinationAccountError)

        // A igualdade envolve origem e destino: mudar a origem limpa o erro
        viewModel.onSourceAccountChange("acc-3")

        assertNull(viewModel.formState.value.destinationAccountError)
    }
}
