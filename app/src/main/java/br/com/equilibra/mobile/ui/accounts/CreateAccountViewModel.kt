package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountTypeDto
import br.com.equilibra.mobile.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CreateAccountUiState(
    val name: String = "",
    val nameError: String? = null,
    val type: String = AssetAccountTypeDto.CHECKING,
    val initialBalanceInput: String = "",
    val initialBalanceError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, generalError = null, isSuccess = false) }
    }

    fun onTypeChange(value: String) {
        _uiState.update { it.copy(type = value, generalError = null, isSuccess = false) }
    }

    fun onInitialBalanceChange(value: String) {
        _uiState.update { it.copy(initialBalanceInput = value, initialBalanceError = null, generalError = null, isSuccess = false) }
    }

    fun onCreateClick() {
        val nameError = when {
            _uiState.value.name.trim().isEmpty() -> "Informe o nome"
            _uiState.value.name.trim().length > 100 -> "Nome deve ter no máximo 100 caracteres"
            else -> null
        }

        val balanceError = validateMoney(_uiState.value.initialBalanceInput)

        if (nameError != null || balanceError != null) {
            _uiState.update { it.copy(nameError = nameError, initialBalanceError = balanceError) }
            return
        }

        if (_uiState.value.isLoading) return

        val nameSnapshot = _uiState.value.name.trim()
        val typeSnapshot = _uiState.value.type
        val balanceSnapshot = normalizeMoney(_uiState.value.initialBalanceInput)

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = repository.create(nameSnapshot, typeSnapshot, balanceSnapshot)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Dados inválidos. Verifique os campos."
                        422 -> parseDetail(result.body) ?: "Dados inválidos."
                        409 -> "Conta já existe."
                        else -> "Erro ao criar conta (${result.code})."
                    }
                    // Tenta mapear erros de campo
                    val fieldErrors = parseFieldErrors(result.body)
                    if (fieldErrors != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                nameError = fieldErrors["name"] ?: it.nameError,
                                initialBalanceError = fieldErrors["initialBalance"] ?: it.initialBalanceError,
                                generalError = if (fieldErrors["name"] == null && fieldErrors["initialBalance"] == null) msg else null
                            )
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

    private fun validateMoney(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "Informe o saldo inicial"
        // Aceita 1.234,56 ou 1234.56 ou 1234
        val normalized = trimmed.replace(".", "").replace(",", ".").let {
            // Se tinha vírgula, já trocou; se tinha ponto como separador de milhar, removeu
            // Mas se era "1,234.56" (ponto milhar + vírgula decimal) — não comum no Brasil
            // Simplifica: aceita apenas dígitos, ponto, vírgula e sinal negativo
            trimmed.replace(",", ".")
        }.replace(" ", "")
        // Valida com regex simples e tenta BigDecimal
        return try {
            val bd = BigDecimal(normalized)
            if (bd.scale() > 2) return "Máximo 2 casas decimais"
            null
        } catch (_: Exception) {
            "Valor inválido"
        }
    }

    private fun normalizeMoney(input: String): String {
        val trimmed = input.trim().replace(",", ".").replace(" ", "")
        return try {
            BigDecimal(trimmed).setScale(2).toPlainString()
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun parseDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    private fun parseFieldErrors(body: String?): Map<String, String>? {
        if (body.isNullOrBlank()) return null
        val map = mutableMapOf<String, String>()
        Regex("\"(name|initialBalance)\"\\s*:\\s*\"([^\"]+)\"").findAll(body).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        return if (map.isEmpty()) null else map
    }
}
