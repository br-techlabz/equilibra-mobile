package br.com.equilibra.mobile.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateCreditCardUiState(
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
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isSuccess: Boolean = false
)

/**
 * Task 6.4 — CreateCreditCardViewModel
 *
 * Validações locais espelham as regras do backend; qualquer 400 mostra
 * também os erros de campo vindos do ProblemDetail ("errors":[{field,
 * message}]). Timezone é opcional: null deixa o backend aplicar o default.
 */
@HiltViewModel
class CreateCreditCardViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCreditCardUiState())
    val uiState: StateFlow<CreateCreditCardUiState> = _uiState.asStateFlow()

    private companion object {
        /** Campos do formulário que o backend pode apontar. */
        val KNOWN_FIELD_ERRORS = setOf("name", "institution", "nickname", "totalLimit", "closingDay", "dueDay", "timezone")
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, generalError = null, isSuccess = false) }
    }

    fun onInstitutionChange(value: String) {
        _uiState.update { it.copy(institution = value, institutionError = null, generalError = null, isSuccess = false) }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, nicknameError = null, generalError = null, isSuccess = false) }
    }

    fun onLimitChange(value: String) {
        _uiState.update { it.copy(limitInput = value, limitError = null, generalError = null, isSuccess = false) }
    }

    fun onClosingDayChange(value: String) {
        _uiState.update { it.copy(closingDayInput = value, closingDayError = null, generalError = null, isSuccess = false) }
    }

    fun onDueDayChange(value: String) {
        _uiState.update { it.copy(dueDayInput = value, dueDayError = null, generalError = null, isSuccess = false) }
    }

    fun onTimezoneChange(value: String?) {
        _uiState.update { it.copy(timezone = value, timezoneError = null, generalError = null, isSuccess = false) }
    }

    fun onCreateClick() {
        val s = _uiState.value
        val nameError = CreditCardFormValidator.validateName(s.name)
        val institutionError = CreditCardFormValidator.validateInstitution(s.institution)
        val nicknameError = CreditCardFormValidator.validateNickname(s.nickname)
        val limitError = CreditCardFormValidator.validateLimit(s.limitInput)
        val closingDayError = CreditCardFormValidator.validateClosingDay(s.closingDayInput)
        val dueDayError = CreditCardFormValidator.validateDueDay(s.dueDayInput)
        val timezoneError = CreditCardFormValidator.validateTimezone(s.timezone)

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

        if (s.isLoading) return

        val state = _uiState.value
        val name = state.name.trim()
        val institution = state.institution.trim()
        val nickname = state.nickname.trim().takeIf { it.isNotEmpty() }
        val totalLimit = CreditCardFormValidator.normalizeLimit(state.limitInput)
        val closingDay = CreditCardFormValidator.normalizeDay(state.closingDayInput)
        val dueDay = CreditCardFormValidator.normalizeDay(state.dueDayInput)
        val timezone = state.timezone?.takeIf { it.isNotBlank() }

        if (closingDay == null || dueDay == null) return  // já validado

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = repository.create(name, institution, nickname, totalLimit, closingDay, dueDay, timezone)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> "Dados inválidos. Verifique os campos."
                        422 -> parseDetail(result.body) ?: "Dados inválidos."
                        else -> "Erro ao criar cartão (${result.code})."
                    }
                    val fieldErrors = parseFieldErrors(result.body)
                    if (fieldErrors != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                nameError = fieldErrors["name"] ?: it.nameError,
                                institutionError = fieldErrors["institution"] ?: it.institutionError,
                                nicknameError = fieldErrors["nickname"] ?: it.nicknameError,
                                limitError = fieldErrors["totalLimit"] ?: it.limitError,
                                closingDayError = fieldErrors["closingDay"] ?: it.closingDayError,
                                dueDayError = fieldErrors["dueDay"] ?: it.dueDayError,
                                timezoneError = fieldErrors["timezone"] ?: it.timezoneError,
                                // Só mostra erro geral quando o backend não apontou campo conhecido
                                generalError = if (fieldErrors.keys.intersect(KNOWN_FIELD_ERRORS).isEmpty()) msg else null
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

    private fun parseDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    /**
     * Backend ProblemDetail para 400 de validação (GlobalExceptionHandler):
     * `"errors":[{"field":"..","message":".."},...]`
     */
    private fun parseFieldErrors(body: String?): Map<String, String>? {
        if (body.isNullOrBlank()) return null
        val map = mutableMapOf<String, String>()
        Regex("\"field\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"message\"\\s*:\\s*\"([^\"]+)\"").findAll(body).forEach {
            map[it.groupValues[1]] = it.groupValues[2]
        }
        return if (map.isEmpty()) null else map
    }
}
