package br.com.equilibra.mobile.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.account.ReconciliationResultDto
import br.com.equilibra.mobile.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Task 3.8 — Reconciliação de conta.
 *
 * - Data (LocalDate), hora (HH:mm) e saldo informado
 * - Exibe saldo esperado (currentBalance), saldo informado e diferença (BigDecimal)
 * - Confirma antes de enviar; envio único (guard)
 * - Não cria lançamentos localmente — o backend gera o ajuste (adjustmentType/createdTransactionId)
 * - Após sucesso, exibe o resultado retornado pela API
 */
data class ReconcileUiState(
    val account: AccountEntity? = null,
    val date: String = "",                 // YYYY-MM-DD
    val time: String = "",                 // HH:mm
    val informedBalanceInput: String = "",
    val dateError: String? = null,
    val timeError: String? = null,
    val balanceError: String? = null,
    val isSubmitting: Boolean = false,     // envia uma vez
    val confirmVisible: Boolean = false,   // diálogo de confirmação
    val result: ReconciliationResultDto? = null, // resultado da API
    val isSuccess: Boolean = false,
    val generalError: String? = null
) {
    /** Saldo esperado pelo backend (cache local da conta). */
    val expectedBalance: BigDecimal
        get() = account?.currentBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** Saldo informado (preview local). */
    val informed: BigDecimal?
        get() = informedBalanceInput.trim().replace(",", ".")
            .takeIf { it.isNotBlank() }?.toBigDecimalOrNull()

    /** Diferença preview = informado - esperado (BigDecimal, sem Double). */
    val previewDifference: BigDecimal?
        get() = informed?.subtract(expectedBalance)
}

@HiltViewModel
class ReconcileAccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    private val _uiState = MutableStateFlow(ReconcileUiState())
    val uiState: StateFlow<ReconcileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.getByIdCached(accountId)
            _uiState.update {
                it.copy(
                    account = cached,
                    date = LocalDate.now().toString(),
                    time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            }
        }
    }

    fun onDateChange(value: String) {
        _uiState.update { it.copy(date = value, dateError = null, generalError = null) }
    }

    fun onTimeChange(value: String) {
        _uiState.update { it.copy(time = value, timeError = null, generalError = null) }
    }

    fun onInformedBalanceChange(value: String) {
        _uiState.update { it.copy(informedBalanceInput = value, balanceError = null, generalError = null) }
    }

    /**
     * Valida e abre o diálogo de confirmação (mostra esperado/informado/diferença).
     */
    fun onReconcileClick() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val dateError = validateDate(state.date)
        val timeError = validateTime(state.time)
        val balanceError = validateMoney(state.informedBalanceInput)

        if (dateError != null || timeError != null || balanceError != null) {
            _uiState.update {
                it.copy(dateError = dateError, timeError = timeError, balanceError = balanceError)
            }
            return
        }

        _uiState.update { it.copy(confirmVisible = true) }
    }

    fun onConfirmSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        _uiState.update { it.copy(confirmVisible = false, isSubmitting = true, generalError = null) }

        val informed = AccountFormValidator.normalizeMoney(state.informedBalanceInput)

        viewModelScope.launch {
            val result = repository.reconcile(
                id = accountId,
                reconciliationDate = state.date,
                reconciliationTime = state.time,
                informedBalance = informed
            )
            when (result) {
                is ApiResult.Success -> {
                    // Backend gerou o ajuste — não criamos lançamento local.
                    // Recarrega conta do cache para saldo atualizado (best-effort).
                    val refreshed = repository.getByIdCached(accountId)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            result = result.data,
                            isSuccess = true,
                            account = refreshed ?: it.account
                        )
                    }
                }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400 -> parseDetail(result.body) ?: "Dados de reconciliação inválidos."
                        404 -> "Conta não encontrada."
                        422 -> parseDetail(result.body) ?: "Dados inválidos."
                        else -> "Erro ao reconciliar (${result.code})."
                    }
                    _uiState.update { it.copy(isSubmitting = false, generalError = msg) }
                }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isSubmitting = false, generalError = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isSubmitting = false, generalError = "Erro inesperado. Tente novamente.") }
            }
        }
    }

    fun onDismissConfirm() {
        _uiState.update { it.copy(confirmVisible = false) }
    }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    private fun validateDate(date: String): String? {
        if (date.isBlank()) return "Informe a data"
        return try {
            LocalDate.parse(date)
            null
        } catch (_: Exception) {
            "Data inválida"
        }
    }

    private fun validateTime(time: String): String? {
        if (!Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time)) return "Hora inválida (HH:mm)"
        return null
    }

    private fun validateMoney(input: String): String? {
        return AccountFormValidator.validateMoney(input)
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
