package br.com.equilibra.mobile.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchasesUiState(
    val purchases: List<PurchaseEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Compra aguardando confirmação de cancelamento (null = sem diálogo). */
    val confirmCancelId: String? = null,
    val isCancelling: Boolean = false
)

/**
 * Task 6.8 — Lista de compras de um cartão.
 *
 * - Cache-first: o Flow do Room alimenta a lista; a API sincroniza em seguida.
 * - Falha de sincronização NÃO apaga a lista já exibida — só sinaliza o erro.
 * - Cancelar exige confirmação e só altera a lista depois do backend
 *   (repository não faz atualização otimista — Task 6.7).
 * - Nenhuma regra de fatura/ciclo é calculada aqui: valores vêm do backend.
 */
@HiltViewModel
class PurchasesViewModel @Inject constructor(
    private val repository: PurchaseRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val cardId: String = savedStateHandle.get<String>(ARG_CARD_ID).orEmpty()

    constructor(repository: PurchaseRepository, cardId: String) : this(
        repository,
        androidx.lifecycle.SavedStateHandle(mapOf(ARG_CARD_ID to cardId))
    )

    private val _uiState = MutableStateFlow(PurchasesUiState())
    val uiState: StateFlow<PurchasesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeByCard(cardId).collect { cached ->
                _uiState.update {
                    it.copy(purchases = cached, isLoading = false)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        val hadData = _uiState.value.purchases.isNotEmpty()
        _uiState.update { it.copy(isRefreshing = hadData, isLoading = !hadData && it.isLoading, error = null) }

        viewModelScope.launch {
            when (val result = repository.refreshByCard(cardId)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
                is ApiResult.HttpError ->
                    _uiState.update { it.copy(isRefreshing = false, isLoading = false, error = "Erro ao carregar compras (${result.code}).") }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isRefreshing = false, isLoading = false, error = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isRefreshing = false, isLoading = false, error = "Erro inesperado ao carregar compras.") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onCancelClick(purchaseId: String) {
        if (_uiState.value.isCancelling) return
        _uiState.update { it.copy(confirmCancelId = purchaseId) }
    }

    fun onDismissCancel() {
        _uiState.update { it.copy(confirmCancelId = null) }
    }

    fun onConfirmCancel() {
        val purchaseId = _uiState.value.confirmCancelId ?: return
        if (_uiState.value.isCancelling) return

        _uiState.update { it.copy(confirmCancelId = null, isCancelling = true, error = null) }

        viewModelScope.launch {
            when (val result = repository.cancel(purchaseId)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isCancelling = false) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        404 -> "Compra não encontrada."
                        409 -> "Não foi possível cancelar: a compra não está ativa."
                        else -> "Erro ao cancelar (${result.code})."
                    }
                    _uiState.update { it.copy(isCancelling = false, error = msg) }
                }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isCancelling = false, error = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isCancelling = false, error = "Erro inesperado ao cancelar.") }
            }
        }
    }

    private companion object {
        const val ARG_CARD_ID = "cardId"
    }
}
