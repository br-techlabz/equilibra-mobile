package br.com.equilibra.mobile.ui.register

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

data class RegisterUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null, isSuccess = false) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null, generalError = null, isSuccess = false) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, generalError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onRegisterClick() {
        val emailError = EmailValidator.validate(_uiState.value.email)
        val passwordError = PasswordValidator.validate(_uiState.value.password)
        val confirmError = when {
            _uiState.value.confirmPassword.isEmpty() -> "Confirme a senha"
            _uiState.value.confirmPassword != _uiState.value.password -> "Senhas não conferem"
            else -> null
        }

        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, confirmPasswordError = confirmError)
            }
            return
        }

        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val normalizedEmail = EmailValidator.normalize(_uiState.value.email)
            val result = authRepository.register(normalizedEmail, _uiState.value.password)

            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> extractDetail(result.body) ?: "Dados inválidos. Verifique os campos."
                        409 -> "E-mail já cadastrado"
                        422 -> extractDetail(result.body) ?: "Dados inválidos"
                        else -> "Erro ao cadastrar (${result.code}). Tente novamente."
                    }
                    // Tenta distribuir erros de campo se houver
                    val fieldErrors = parseFieldErrors(result.body)
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
                        // 409 geralmente é e-mail duplicado
                        if (result.code == 409) {
                            _uiState.update { it.copy(isLoading = false, emailError = msg) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, generalError = msg) }
                        }
                    }
                }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, generalError = "Sem conexão. Verifique sua internet.") }
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

    private fun extractDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    private fun parseFieldErrors(body: String?): Map<String, String>? {
        if (body.isNullOrBlank()) return null
        val map = mutableMapOf<String, String>()
        Regex("\"(email|password)\"\\s*:\\s*\"([^\"]+)\"").findAll(body).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        return if (map.isEmpty()) null else map
    }
}
