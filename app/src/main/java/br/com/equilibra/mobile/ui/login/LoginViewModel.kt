package br.com.equilibra.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.AuthRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.validator.EmailValidator
import br.com.equilibra.mobile.domain.validator.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false,
    val canSubmit: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                generalError = null,
                isSuccess = false
            )
        }
        validate()
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                generalError = null,
                isSuccess = false
            )
        }
        validate()
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClick() {
        val emailError = EmailValidator.validate(_uiState.value.email)
        val passwordError = PasswordValidator.validate(_uiState.value.password)

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        if (_uiState.value.isLoading) return
        val normalizedEmail = EmailValidator.normalize(_uiState.value.email)
        val passwordSnapshot = _uiState.value.password
        _uiState.update { it.copy(isLoading = true, generalError = null, emailError = null, passwordError = null) }

        viewModelScope.launch {
            val result = authRepository.login(normalizedEmail, passwordSnapshot)

            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseFieldErrors(result.body) ?: "Dados inválidos. Verifique os campos."
                        401 -> "E-mail ou senha incorretos"
                        422 -> parseFieldErrors(result.body) ?: "Dados inválidos"
                        else -> "Erro ao entrar (${result.code}). Tente novamente."
                    }
                    // Se for 422 com campo, distribui; senão mensagem geral
                    if (result.code == 422 || result.code == 400) {
                        val fieldErrors = parseFieldErrorsMap(result.body)
                        if (fieldErrors != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    emailError = fieldErrors["email"] ?: it.emailError,
                                    passwordError = fieldErrors["password"] ?: it.passwordError,
                                    generalError = if (fieldErrors["email"] == null && fieldErrors["password"] == null) msg else null
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, generalError = msg) }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, generalError = msg) }
                    }
                }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, generalError = "Sem conexão. Verifique sua internet e tente novamente.") }
                is ApiResult.UnknownError -> _uiState.update { it.copy(isLoading = false, generalError = "Erro inesperado. Tente novamente.") }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun validate() {
        val s = _uiState.value
        _uiState.update { it.copy(canSubmit = s.email.isNotBlank() && s.password.isNotEmpty()) }
    }

    private fun parseFieldErrors(body: String?): String? {
        if (body.isNullOrBlank()) return null
        // Extrai "detail" ou "title" de ProblemDetail RFC7807
        return try {
            val detailMatch = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)
            detailMatch?.groupValues?.get(1)
        } catch (_: Exception) { null }
    }

    private fun parseFieldErrorsMap(body: String?): Map<String, String>? {
        if (body.isNullOrBlank()) return null
        return try {
            val map = mutableMapOf<String, String>()
            // Procura por "fieldErrors" ou "errors" array
            Regex("\"(email|password)\"\\s*:\\s*\"([^\"]+)\"").findAll(body).forEach {
                map[it.groupValues[1]] = it.groupValues[2]
            }
            if (map.isEmpty()) null else map
        } catch (_: Exception) { null }
    }
}
