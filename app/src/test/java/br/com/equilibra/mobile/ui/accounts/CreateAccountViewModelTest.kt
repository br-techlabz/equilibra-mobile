package br.com.equilibra.mobile.ui.accounts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import io.mockk.coEvery
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
class CreateAccountViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccountRepository
    private lateinit var viewModel: CreateAccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = CreateAccountViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nome vazio mostra erro`() = runTest {
        viewModel.onNameChange("")
        viewModel.onInitialBalanceChange("100.00")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `nome com mais de 100 caracteres mostra erro`() = runTest {
        viewModel.onNameChange("a".repeat(101))
        viewModel.onInitialBalanceChange("10.00")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `saldo vazio mostra erro`() = runTest {
        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.initialBalanceError)
    }

    @Test
    fun `saldo invalido mostra erro`() = runTest {
        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("abc")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.initialBalanceError)
    }

    @Test
    fun `saldo com mais de 2 casas decimais mostra erro`() = runTest {
        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("10.123")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.initialBalanceError)
    }

    @Test
    fun `criacao sucesso marca isSuccess`() = runTest {
        coEvery { repository.create(any(), any(), any()) } returns ApiResult.Success(
            AccountEntity("id1", "Conta X", "CHECKING", "100.00", "100.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        )

        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("100.00")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `erro 400 mostra generalError`() = runTest {
        coEvery { repository.create(any(), any(), any()) } returns ApiResult.HttpError(400, "Bad", null)

        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("100.00")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `erro de rede mostra mensagem de conexao`() = runTest {
        coEvery { repository.create(any(), any(), any()) } returns ApiResult.NetworkError(java.io.IOException())

        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("100.00")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.generalError!!.contains("conexão", ignoreCase = true))
    }

    @Test
    fun `duplo clique nao dispara segundo create`() = runTest {
        var calls = 0
        coEvery { repository.create(any(), any(), any()) } coAnswers {
            calls++
            kotlinx.coroutines.delay(200)
            ApiResult.Success(AccountEntity("id1", "Conta X", "CHECKING", "100.00", "100.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z"))
        }

        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("100.00")
        viewModel.onCreateClick()
        viewModel.onCreateClick() // segundo clique enquanto isLoading=true deve ser ignorado
        advanceUntilIdle()

        assertEquals(1, calls)
    }

    @Test
    fun `saldo com virgula e normalizado`() = runTest {
        coEvery { repository.create(any(), any(), any()) } returns ApiResult.Success(
            AccountEntity("id1", "Conta X", "CHECKING", "100.50", "100.50", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
        )

        viewModel.onNameChange("Conta X")
        viewModel.onInitialBalanceChange("100,50")
        viewModel.onCreateClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun `todos os tipos sao aceitos`() = runTest {
        val types = listOf("CHECKING", "SAVINGS", "CASH", "INVESTMENT", "DIGITAL", "OTHER")
        for (type in types) {
            coEvery { repository.create(any(), any(), any()) } returns ApiResult.Success(
                AccountEntity("id-$type", "Conta $type", type, "10.00", "10.00", true, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z")
            )
            viewModel.onTypeChange(type)
            viewModel.onNameChange("Conta $type")
            viewModel.onInitialBalanceChange("10.00")
            viewModel.onCreateClick()
            advanceUntilIdle()
            assertTrue("Tipo $type deve ter sucesso", viewModel.uiState.value.isSuccess)
        }
    }
}
