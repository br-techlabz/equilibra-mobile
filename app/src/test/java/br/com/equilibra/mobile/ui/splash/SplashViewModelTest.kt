package br.com.equilibra.mobile.ui.splash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.session.SessionManager
import br.com.equilibra.mobile.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = mockk(relaxed = true)
        authRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sem token navega para login sem chamar API`() = runTest {
        every { sessionManager.getAccessToken() } returns null

        val vm = SplashViewModel(sessionManager, authRepository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is SplashUiState.NavigateToLogin)
        coVerify(exactly = 0) { authRepository.getCurrentUser() }
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `com token valido navega para home`() = runTest {
        every { sessionManager.getAccessToken() } returns "valid.jwt.token"
        coEvery { authRepository.getCurrentUser() } returns ApiResult.Success(
            User("id1", "user@example.com", "2026-01-01T00:00:00Z")
        )

        val vm = SplashViewModel(sessionManager, authRepository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is SplashUiState.NavigateToHome)
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `401 apos token invalido navega para login`() = runTest {
        every { sessionManager.getAccessToken() } returns "expired.jwt.token"
        coEvery { authRepository.getCurrentUser() } returns ApiResult.HttpError(401, "Unauthorized", null)

        val vm = SplashViewModel(sessionManager, authRepository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is SplashUiState.NavigateToLogin)
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `falha de rede mostra erro com retry`() = runTest {
        every { sessionManager.getAccessToken() } returns "valid.jwt.token"
        coEvery { authRepository.getCurrentUser() } returns ApiResult.NetworkError(java.io.IOException())

        val vm = SplashViewModel(sessionManager, authRepository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is SplashUiState.Error)
        assertTrue((state as SplashUiState.Error).canRetry)
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
    }
}
