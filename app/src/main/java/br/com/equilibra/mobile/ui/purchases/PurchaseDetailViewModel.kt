package br.com.equilibra.mobile.ui.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.purchase.InstallmentEntity
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseStatusDto
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PurchaseDetailUiState {
    data object Loading : PurchaseDetailUiState()

    data class Success(
        val purchase: PurchaseEntity,
        val installments: List<InstallmentEntity>,
        /** Aviso não-erro (ex.: falha de sync exibindo o cache). */
        val warning: String? = null,
        val error: String? = null,
        val isCancelling: Boolean = false,
        val confirmCancelVisible: Boolean = false
    ) : PurchaseDetailUiState() {
        /** Só compra ATIVA pode ser cancelada — regra do backend. */
        val canCancel: Boolean get() = purchase.status == PurchaseStatusDto.ACTIVE
    }

    /** Compra não existe (404): estado terminal, sem formulário nem retry. */
    data object NotFound : PurchaseDetailUiState()

    data class Error(val message: String, val canRetry: Boolean = true) : PurchaseDetailUiState()
}

/**
 * Task 6.9 — Detalhe da compra.
 *
 * - Sempre busca da API (o cache pode nem ter a compra e pode estar velho);
 *   em falha cai para o cache, com aviso, quando ele existe.
 * - Parcelas, valores e ciclos são os do backend — o detalhe não infere
 *   nem recalcula campo algum.
 * - Compra cancelada é exibida normalmente (histórico preservado), apenas
 *   sem ação de cancelar.
 */
@HiltViewModel
class PurchaseDetailViewModel @Inject constructor(
    private val repository: PurchaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val purchaseId: String = savedStateHandle.get<String>(ARG_PURCHASE_ID).orEmpty()

    constructor(repository: PurchaseRepository, purchaseId: String) : this(
        repository,
        SavedStateHandle(mapOf(ARG_PURCHASE_ID to purchaseId))
    )

    private val _uiState = MutableStateFlow<PurchaseDetailUiState>(PurchaseDetailUiState.Loading)
    val uiState: StateFlow<PurchaseDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = PurchaseDetailUiState.Loading
        viewModelScope.launch {
            when (val result = repository.refreshById(purchaseId)) {
                is ApiResult.Success -> {
                    val parcelas = repository.getInstallmentsCached(purchaseId)
                    _uiState.value = PurchaseDetailUiState.Success(result.data, parcelas)
                }
                is ApiResult.HttpError -> {
                    if (result.code == 404) {
                        _uiState.value = PurchaseDetailUiState.NotFound
                    } else {
                        loadFromCacheOrError("Erro ao carregar compra (${result.code}).")
                    }
                }
                is ApiResult.NetworkError -> loadFromCacheOrError("Sem conexão. Tente novamente.")
                is ApiResult.UnknownError -> loadFromCacheOrError("Erro inesperado ao carregar compra.")
            }
        }
    }

    private suspend fun loadFromCacheOrError(message: String) {
        val cached = repository.getCached(purchaseId)
        _uiState.value = if (cached != null) {
            PurchaseDetailUiState.Success(
                purchase = cached,
                installments = repository.getInstallmentsCached(purchaseId),
                warning = "Mostrando dados salvos. $message"
            )
        } else {
            PurchaseDetailUiState.Error(message)
        }
    }

    fun onCancelClick() {
        val current = _uiState.value as? PurchaseDetailUiState.Success ?: return
        if (!current.canCancel || current.isCancelling) return
        _uiState.value = current.copy(confirmCancelVisible = true)
    }

    fun onDismissCancel() {
        val current = _uiState.value as? PurchaseDetailUiState.Success ?: return
        _uiState.value = current.copy(confirmCancelVisible = false)
    }

    fun onConfirmCancel() {
        val current = _uiState.value as? PurchaseDetailUiState.Success ?: return
        if (current.isCancelling) return
        _uiState.value = current.copy(confirmCancelVisible = false, isCancelling = true, error = null)

        viewModelScope.launch {
            when (val result = repository.cancel(purchaseId)) {
                is ApiResult.Success -> {
                    val parcelas = repository.getInstallmentsCached(purchaseId)
                    _uiState.value = PurchaseDetailUiState.Success(result.data, parcelas)
                }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        404 -> "Compra não encontrada."
                        409 -> "Não foi possível cancelar: a compra não está ativa."
                        else -> "Erro ao cancelar (${result.code})."
                    }
                    val s = _uiState.value as? PurchaseDetailUiState.Success ?: return@launch
                    _uiState.value = s.copy(isCancelling = false, error = msg)
                }
                is ApiResult.NetworkError -> {
                    val s = _uiState.value as? PurchaseDetailUiState.Success ?: return@launch
                    _uiState.value = s.copy(isCancelling = false, error = "Sem conexão. Tente novamente.")
                }
                is ApiResult.UnknownError -> {
                    val s = _uiState.value as? PurchaseDetailUiState.Success ?: return@launch
                    _uiState.value = s.copy(isCancelling = false, error = "Erro inesperado ao cancelar.")
                }
            }
        }
    }

    fun clearError() {
        val current = _uiState.value as? PurchaseDetailUiState.Success ?: return
        _uiState.value = current.copy(error = null)
    }

    fun clearWarning() {
        val current = _uiState.value as? PurchaseDetailUiState.Success ?: return
        _uiState.value = current.copy(warning = null)
    }

    private companion object {
        const val ARG_PURCHASE_ID = "purchaseId"
    }
}
