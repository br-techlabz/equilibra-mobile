package br.com.equilibra.mobile.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    data object Loading : SplashUiState
    data object NavigateToHome : SplashUiState
    data object NavigateToLogin : SplashUiState
    data class Error(val message: String, val canRetry: Boolean = true) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        decide()
    }

    fun decide() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading

            // Sem token → login direto (sem chamada de rede)
            val token = sessionManager.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value = SplashUiState.NavigateToLogin
                return@launch
            }

            // Com token → valida sessão no backend (GET /users/me)
            // Se 401, o TokenAuthenticator tenta refresh automaticamente via cookie.
            // Se ainda 401 após refresh, o authenticator limpa a sessão → cai em erro auth.
            when (val result = authRepository.getCurrentUser()) {
                is ApiResult.Success -> {
                    // Sessão válida; garante que userId/email estão persistidos
                    sessionManager.saveUserId(result.data.id)
                    sessionManager.saveEmail(result.data.email)
                    _uiState.value = SplashUiState.NavigateToHome
                }
                is ApiResult.HttpError -> {
                    if (result.code == 401) {
                        // Token inválido/expirado e refresh falhou → login
                        _uiState.value = SplashUiState.NavigateToLogin
                    } else {
                        _uiState.value = SplashUiState.Error(
                            message = "Erro ao validar sessão (${result.code}). Tente novamente.",
                            canRetry = true
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    // Sem rede: mantém sessão local se houver token, mas informa
                    // Política segura: se não conseguimos validar, deixamos o usuário
                    // tentar novamente, mas não bloqueamos indefinidamente.
                    _uiState.value = SplashUiState.Error(
                        message = "Sem conexão. Verifique sua internet.",
                        canRetry = true
                    )
                }
                is ApiResult.UnknownError -> {
                    _uiState.value = SplashUiState.Error(
                        message = "Erro inesperado. Tente novamente.",
                        canRetry = true
                    )
                }
            }
        }
    }

    fun onRetry() = decide()

    fun onContinueOffline() {
        // Permite seguir para login se o usuário não quer retry agora
        _uiState.value = SplashUiState.NavigateToLogin
    }
}
