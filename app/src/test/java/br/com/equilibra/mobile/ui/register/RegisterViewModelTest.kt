package br.com.equilibra.mobile.ui.register

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.model.User
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `senhas diferentes mostra erro de confirmacao`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onConfirmPasswordChange("87654321")
        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `cadastro sucesso marca isSuccess`() = runTest {
        coEvery { authRepository.register(any(), any()) } returns ApiResult.Success(
            User("id1", "user@example.com", "2026-01-01T00:00:00Z")
        )

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onConfirmPasswordChange("12345678")
        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `409 mostra email ja cadastrado`() = runTest {
        coEvery { authRepository.register(any(), any()) } returns ApiResult.HttpError(409, "Conflict", null)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onConfirmPasswordChange("12345678")
        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.emailError)
        assertTrue(viewModel.uiState.value.emailError!!.contains("já cadastrado", ignoreCase = true))
    }

    @Test
    fun `erro de rede mostra mensagem de conexao`() = runTest {
        coEvery { authRepository.register(any(), any()) } returns ApiResult.NetworkError(java.io.IOException())

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onConfirmPasswordChange("12345678")
        viewModel.onRegisterClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalError)
    }
}
