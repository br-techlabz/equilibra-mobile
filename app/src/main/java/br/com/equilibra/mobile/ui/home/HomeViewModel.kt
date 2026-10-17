package br.com.equilibra.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val email: String? = null,
    val isLoggingOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val email = sessionManager.getEmail()
            _uiState.value = _uiState.value.copy(email = email)
        }
    }

    fun onLogout(onLoggedOut: () -> Unit) {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true, error = null)
            // Sempre limpa local mesmo se a rede falhar (idempotente)
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggingOut = false)
            onLoggedOut()
        }
    }
}
