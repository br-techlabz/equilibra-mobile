package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 3.7 — Editar e ativar/desativar conta.
 *
 * - Reutiliza o formulário (AccountForm) das Tasks 3.6
 * - Confirma desativação (diálogo) pois pode afetar relatórios/transações
 * - Respeita restrições do backend (erros 400/409/422 exibidos)
 * - Atualiza cache Room apenas após sucesso remoto (repository já garante)
 * - Falha preserva estado anterior (form mantém valores, só mostra erro)
 */
data class EditAccountUiState(
    val account: AccountEntity? = null,
    val form: AccountFormState = AccountFormState(),
    val isLoading: Boolean = false,       // salvando
    val isToggling: Boolean = false,      // ativando/desativando
    val isSuccess: Boolean = false,       // salvou
    val statusChanged: Boolean = false,   // ativar/desativar OK
    val generalError: String? = null,
    val confirmDeactivateVisible: Boolean = false
)

@HiltViewModel
class EditAccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    private val _uiState = MutableStateFlow(EditAccountUiState())
    val uiState: StateFlow<EditAccountUiState> = _uiState.asStateFlow()

    init {
        // Pré-preenche do cache (Room-first)
        viewModelScope.launch {
            val cached = repository.getByIdCached(accountId)
            if (cached != null) {
                _uiState.update {
                    it.copy(
                        account = cached,
                        form = AccountFormState(
                            name = cached.name,
                            type = cached.type,
                            initialBalanceInput = cached.initialBalance
                        )
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(name = value, nameError = null),
                generalError = null,
                isSuccess = false,
                statusChanged = false
            )
        }
    }

    fun onTypeChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(type = value),
                generalError = null,
                isSuccess = false,
                statusChanged = false
            )
        }
    }

    fun onInitialBalanceChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(initialBalanceInput = value, initialBalanceError = null),
                generalError = null,
                isSuccess = false,
                statusChanged = false
            )
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isLoading || state.isToggling) return

        val nameError = AccountFormValidator.validateName(state.form.name)
        val balanceError = AccountFormValidator.validateMoney(state.form.initialBalanceInput)
        if (nameError != null || balanceError != null) {
            _uiState.update {
                it.copy(
                    form = it.form.copy(nameError = nameError, initialBalanceError = balanceError)
                )
            }
            return
        }

        val nameSnapshot = state.form.name.trim()
        val typeSnapshot = state.form.type
        val balanceSnapshot = AccountFormValidator.normalizeMoney(state.form.initialBalanceInput)

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = repository.update(accountId, nameSnapshot, typeSnapshot, balanceSnapshot)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = true, account = result.data)
                    }
                }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Dados inválidos. Verifique os campos."
                        404 -> "Conta não encontrada."
                        409 -> parseDetail(result.body) ?: "Conflito ao atualizar conta."
                        422 -> parseDetail(result.body) ?: "Dados inválidos."
                        else -> "Erro ao salvar (${result.code})."
                    }
                    // Falha preserva estado anterior — form mantém valores
                    _uiState.update { it.copy(isLoading = false, generalError = msg) }
                }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isLoading = false, generalError = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isLoading = false, generalError = "Erro inesperado. Tente novamente.") }
            }
        }
    }

    /**
     * Ativar/desativar — desativação exige confirmação (impacto em relatórios).
     */
    fun onToggleActiveClick() {
        val account = _uiState.value.account ?: return
        if (_uiState.value.isToggling) return
        if (account.active) {
            // Desativação → mostra diálogo de confirmação
            _uiState.update { it.copy(confirmDeactivateVisible = true) }
        } else {
            // Ativação não requer confirmação
            performToggle()
        }
    }

    fun onConfirmDeactivate() {
        _uiState.update { it.copy(confirmDeactivateVisible = false) }
        performToggle()
    }

    fun onDismissConfirmDeactivate() {
        _uiState.update { it.copy(confirmDeactivateVisible = false) }
    }

    private fun performToggle() {
        val account = _uiState.value.account ?: return
        if (_uiState.value.isToggling) return

        _uiState.update { it.copy(isToggling = true, generalError = null) }

        viewModelScope.launch {
            val result = if (account.active) {
                repository.deactivate(account.id)
            } else {
                repository.activate(account.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isToggling = false, statusChanged = true, account = result.data)
                    }
                }
                is ApiResult.HttpError -> {
                    // Restrições do backend: exibe mensagem; estado local preservado
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Operação não permitida."
                        404 -> "Conta não encontrada."
                        409 -> parseDetail(result.body) ?: "Conflito: conta em uso."
                        else -> "Erro ao alterar status (${result.code})."
                    }
                    _uiState.update { it.copy(isToggling = false, generalError = msg) }
                }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isToggling = false, generalError = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isToggling = false, generalError = "Erro inesperado. Tente novamente.") }
            }
        }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false, statusChanged = false) }
    }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun parseDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    companion object {
        const val ARG_ACCOUNT_ID = "accountId"
    }
}
