package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import br.com.equilibra.mobile.domain.model.transaction.TransactionDetail
import br.com.equilibra.mobile.domain.model.transaction.TransactionStatus
import br.com.equilibra.mobile.domain.model.transaction.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TransactionDetailUiState {
    data object Loading : TransactionDetailUiState()
    data class Success(
        val detail: TransactionDetail,
        /** Aviso não-erro (ex.: "Transação cancelada com sucesso"). */
        val notice: String? = null,
        val isCancelling: Boolean = false,
        val error: String? = null
    ) : TransactionDetailUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : TransactionDetailUiState()
}

/**
 * Task 4.12 — Detalhe da transação.
 *
 * - Navegação por ID: a rota traz `transactionType` + `transactionId` (o
 *   backend não tem GET /transactions/{id}); o detalhe real vem do endpoint
 *   do tipo, sempre da API — sem pré-preenchimento de cache.
 * - Ações expostas conforme tipo/status via [TransactionDetail.canEdit] e
 *   [TransactionDetail.canCancel]; cancelamento usa o endpoint do tipo,
 *   com anti duplo envio e só altera a tela após sucesso.
 * - Os campos editar/cancelar delegam às telas existentes (Tasks 4.6/4.8).
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])
    private val transactionType: String = checkNotNull(savedStateHandle["transactionType"])

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(TransactionDetailUiState.Loading)
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun clearNotice() {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success && current.notice != null) {
            _uiState.value = current.copy(notice = null)
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is TransactionDetailUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    /**
     * Cancela a transação (ação destrutiva; a tela confirma antes).
     * Sem endpoint para pagamento de fatura, nada é chamado. Em erro, o
     * detalhe permanece com o status anterior da API.
     */
    fun cancel() {
        val current = _uiState.value
        if (current !is TransactionDetailUiState.Success) return
        if (current.isCancelling) return
        if (!current.detail.canCancel) return

        _uiState.value = current.copy(isCancelling = true, error = null)

        viewModelScope.launch {
            when (val result = cancelByType()) {
                is ApiResult.Success -> {
                    _uiState.value = TransactionDetailUiState.Success(
                        detail = current.detail.copy(status = TransactionStatus.CANCELLED),
                        notice = "Transação cancelada com sucesso.",
                        isCancelling = false
                    )
                }
                is ApiResult.HttpError -> fail(current, "Erro ao cancelar (${result.code}). Transação não alterada.")
                is ApiResult.NetworkError -> fail(current, "Sem conexão. Transação não cancelada.")
                is ApiResult.UnknownError -> fail(current, "Erro inesperado ao cancelar.")
            }
        }
    }

    private suspend fun cancelByType(): ApiResult<TransactionEntity> = when (transactionType) {
        TransactionType.EXPENSE -> repository.cancelExpense(transactionId)
        TransactionType.INCOME -> repository.cancelIncome(transactionId)
        TransactionType.TRANSFER -> repository.cancelTransfer(transactionId)
        else -> ApiResult.UnknownError(
            IllegalArgumentException("Tipo sem cancelamento: $transactionType")
        )
    }

    private fun fail(previous: TransactionDetailUiState.Success, message: String) {
        _uiState.value = previous.copy(isCancelling = false, error = message)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = TransactionDetailUiState.Loading
            when (val result = repository.getDetail(transactionType, transactionId)) {
                is ApiResult.Success ->
                    _uiState.value = TransactionDetailUiState.Success(result.data)
                is ApiResult.HttpError ->
                    _uiState.value = TransactionDetailUiState.Error(
                        if (result.code == 404) {
                            "Transação não encontrada (404)."
                        } else {
                            "Erro ao carregar transação (${result.code})."
                        }
                    )
                is ApiResult.NetworkError ->
                    _uiState.value = TransactionDetailUiState.Error(
                        "Sem conexão. Verifique sua internet."
                    )
                is ApiResult.UnknownError ->
                    _uiState.value = TransactionDetailUiState.Error(
                        "Detalhe indisponível para este tipo de transação."
                    )
            }
        }
    }
}
