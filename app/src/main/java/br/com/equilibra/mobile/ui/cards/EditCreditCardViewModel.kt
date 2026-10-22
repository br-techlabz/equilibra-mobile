package br.com.equilibra.mobile.ui.cards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditCreditCardUiState(
    val card: CreditCardEntity? = null,
    val name: String = "",
    val nameError: String? = null,
    val institution: String = "",
    val institutionError: String? = null,
    val nickname: String = "",
    val nicknameError: String? = null,
    val limitInput: String = "",
    val limitError: String? = null,
    val closingDayInput: String = "",
    val closingDayError: String? = null,
    val dueDayInput: String = "",
    val dueDayError: String? = null,
    val timezone: String? = null,
    val timezoneError: String? = null,
    val isLoading: Boolean = false,      // salvando
    val isToggling: Boolean = false,     // ativando/desativando
    val isSuccess: Boolean = false,      // salvou
    val statusChanged: Boolean = false,  // ativar/desativar OK
    val generalError: String? = null,
    val confirmDeactivateVisible: Boolean = false,
    /** Cartão não encontrado (404) — tela mostra estado terminal sem form. */
    val notFound: Boolean = false
) {
    /** Preenche os inputs a partir da entity (cache ou resposta da API). */
    fun withCard(c: CreditCardEntity): EditCreditCardUiState = copy(
        card = c,
        name = c.name,
        institution = c.institution,
        nickname = c.nickname.orEmpty(),
        limitInput = c.totalLimit,
        closingDayInput = c.closingDay.toString(),
        dueDayInput = c.dueDay.toString(),
        timezone = c.timezone
    )
}

/**
 * Task 6.5 — Editar e ativar/desativar cartão.
 *
 * - Reutiliza o formulário/validador da Task 6.4
 * - Confirma desativação (diálogo) — compras e faturas existentes são
 *   preservadas no servidor; nada é apagado localmente (repository)
 * - Atualiza cache Room apenas após sucesso remoto
 * - Falha preserva estado anterior (form mantém valores, só mostra erro)
 */
@HiltViewModel
class EditCreditCardViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: String = savedStateHandle.get<String>(ARG_CARD_ID).orEmpty()

    private val _uiState = MutableStateFlow(EditCreditCardUiState())
    val uiState: StateFlow<EditCreditCardUiState> = _uiState.asStateFlow()

    init {
        // Pré-preenche do cache (Room-first) e sincroniza da API em seguida
        viewModelScope.launch {
            val cached = repository.getByIdCached(cardId)
            if (cached != null) {
                _uiState.update { it.withCard(cached) }
            }
            when (val result = repository.refreshById(cardId)) {
                is ApiResult.Success -> _uiState.update { it.withCard(result.data) }
                is ApiResult.HttpError -> {
                    if (result.code == 404) {
                        _uiState.update { it.copy(notFound = true, isLoading = false, isToggling = false) }
                    }
                    // outros erros: mantém os dados do cache e silencia
                    // (a lista/refresh global já relata erros)
                }
                else -> Unit
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onInstitutionChange(value: String) {
        _uiState.update { it.copy(institution = value, institutionError = null) }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, nicknameError = null) }
    }

    fun onLimitChange(value: String) {
        _uiState.update { it.copy(limitInput = value, limitError = null) }
    }

    fun onClosingDayChange(value: String) {
        _uiState.update { it.copy(closingDayInput = value, closingDayError = null) }
    }

    fun onDueDayChange(value: String) {
        _uiState.update { it.copy(dueDayInput = value, dueDayError = null) }
    }

    fun onTimezoneChange(value: String?) {
        _uiState.update { it.copy(timezone = value, timezoneError = null) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isLoading || state.isToggling) return

        val nameError = CreditCardFormValidator.validateName(state.name)
        val institutionError = CreditCardFormValidator.validateInstitution(state.institution)
        val nicknameError = CreditCardFormValidator.validateNickname(state.nickname)
        val limitError = CreditCardFormValidator.validateLimit(state.limitInput)
        val closingDayError = CreditCardFormValidator.validateClosingDay(state.closingDayInput)
        val dueDayError = CreditCardFormValidator.validateDueDay(state.dueDayInput)
        val timezoneError = CreditCardFormValidator.validateTimezone(state.timezone)

        if (listOfNotNull(nameError, institutionError, nicknameError, limitError, closingDayError, dueDayError, timezoneError).isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    institutionError = institutionError,
                    nicknameError = nicknameError,
                    limitError = limitError,
                    closingDayError = closingDayError,
                    dueDayError = dueDayError,
                    timezoneError = timezoneError
                )
            }
            return
        }

        val closingDay = CreditCardFormValidator.normalizeDay(state.closingDayInput)
        val dueDay = CreditCardFormValidator.normalizeDay(state.dueDayInput)
        if (closingDay == null || dueDay == null) return

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = repository.update(
                cardId,
                state.name.trim(),
                state.institution.trim(),
                state.nickname.trim().takeIf { it.isNotEmpty() },
                CreditCardFormValidator.normalizeLimit(state.limitInput),
                closingDay,
                dueDay,
                state.timezone?.takeIf { it.isNotBlank() }
            )) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, card = result.data) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Dados inválidos. Verifique os campos."
                        404 -> "Cartão não encontrado."
                        409 -> parseDetail(result.body) ?: "Conflito ao atualizar cartão."
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

    /** Ativar/desativar — desativação exige confirmação (compras/faturas preservadas). */
    fun onToggleActiveClick() {
        val card = _uiState.value.card ?: return
        if (_uiState.value.isToggling) return
        if (card.status == "ACTIVE") {
            _uiState.update { it.copy(confirmDeactivateVisible = true) }
        } else {
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
        val card = _uiState.value.card ?: return
        if (_uiState.value.isToggling) return

        _uiState.update { it.copy(isToggling = true, generalError = null) }

        viewModelScope.launch {
            val result = if (card.status == "ACTIVE") repository.deactivate(card.id) else repository.activate(card.id)
            when (result) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isToggling = false, statusChanged = true, card = result.data) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Operação não permitida."
                        404 -> "Cartão não encontrado."
                        409 -> parseDetail(result.body) ?: "Conflito: cartão em uso."
                        else -> "Erro ao alterar status (${result.code})."
                    }
                    // Restrições dobackend: exibe mensagem; estado local preservado
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

    private companion object {
        const val ARG_CARD_ID = "cardId"
    }
}
