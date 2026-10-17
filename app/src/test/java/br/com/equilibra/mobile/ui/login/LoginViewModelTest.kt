package br.com.equilibra.mobile.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.remote.ApiResult
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
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `validacao local impede login com email invalido`() = runTest {
        viewModel.onEmailChange("invalido")
        viewModel.onPasswordChange("12345678")
        viewModel.onLoginClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `login sucesso marca isSuccess`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Success(Unit)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onLoginClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `401 mostra credenciais incorretas`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.HttpError(401, "Unauthorized", null)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onLoginClick()
        advanceUntilIdle()

        assertEquals("E-mail ou senha incorretos", viewModel.uiState.value.generalError)
    }

    @Test
    fun `409 nao existe no login mas erro generico nao quebra`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.NetworkError(java.io.IOException("no internet"))

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onLoginClick()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalError)
        assertTrue(viewModel.uiState.value.generalError!!.contains("conexão", ignoreCase = true))
    }

    @Test
    fun `duplo clique nao dispara segundo login`() = runTest {
        var calls = 0
        coEvery { authRepository.login(any(), any()) } coAnswers {
            calls++
            // Simula delay
            kotlinx.coroutines.delay(100)
            ApiResult.Success(Unit)
        }

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("12345678")
        viewModel.onLoginClick()
        viewModel.onLoginClick() // segundo clique enquanto isLoading=true deve ser ignorado
        advanceUntilIdle()

        assertEquals(1, calls)
    }
}
