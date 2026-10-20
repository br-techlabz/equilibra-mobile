package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.HistoryFilters
import br.com.equilibra.mobile.data.repository.TransactionHistoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(
        val items: List<TransactionEntity>,
        val page: Int = 0,
        val totalPages: Int = 1,
        val totalElements: Long = 0,
        val filters: HistoryFilters = HistoryFilters.empty(),
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null
    ) : HistoryUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : HistoryUiState()
}

/**
 * Task 4.11 — Histórico unificado (API sempre é a fonte da tela).
 *
 * - Filtros combinados (tipo/status/período/conta/categoria/tags) via
 *   GET /transactions; paginação real com dedupe; sem nenhum acesso
 *   a Room (repositório dedicado, sem DAO).
 * - applyFilters recarrega da página 0; clearFilters limpa tudo.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TransactionHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var appliedFilters: HistoryFilters = HistoryFilters.empty()

    init {
        load(reset = true)
    }

    fun applyFilters(filters: HistoryFilters) {
        appliedFilters = filters
        load(reset = true)
    }

    fun clearFilters() {
        appliedFilters = HistoryFilters.empty()
        load(reset = true)
    }

    fun refresh() = load(reset = true)

    fun retry() = load(reset = true)

    fun loadMore() {
        val current = _uiState.value
        if (current !is HistoryUiState.Success) return
        if (current.endReached || current.isLoadingMore) return
        load(reset = false)
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is HistoryUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val nextPage = if (reset) 0 else (current as? HistoryUiState.Success)?.page?.plus(1) ?: 0

            _uiState.value = when {
                reset && current is HistoryUiState.Success ->
                    current.copy(isRefreshing = true, error = null)
                !reset && current is HistoryUiState.Success ->
                    current.copy(isLoadingMore = true)
                else -> HistoryUiState.Loading
            }

            when (val result = repository.list(nextPage, appliedFilters)) {
                is ApiResult.Success -> {
                    val data: TransactionPage = result.data
                    val previous = (_uiState.value as? HistoryUiState.Success)?.items.orEmpty()
                    val items = if (reset) {
                        data.items
                    } else {
                        (previous + data.items).distinctBy { it.id }
                    }
                    _uiState.value = HistoryUiState.Success(
                        items = items,
                        page = data.page,
                        totalPages = data.totalPages,
                        totalElements = data.totalElements,
                        filters = appliedFilters,
                        isLoadingMore = false,
                        isRefreshing = false,
                        endReached = data.isLastPage
                    )
                }
                is ApiResult.HttpError -> handleError("Erro ao carregar histórico (${result.code}).", current)
                is ApiResult.NetworkError -> handleError("Sem conexão. Verifique sua internet.", current)
                is ApiResult.UnknownError -> handleError("Erro inesperado ao carregar histórico.", current)
            }
        }
    }

    private fun handleError(message: String, previous: HistoryUiState) {
        _uiState.value = if (previous is HistoryUiState.Success && previous.items.isNotEmpty()) {
            previous.copy(isRefreshing = false, isLoadingMore = false, error = message)
        } else {
            HistoryUiState.Error(message)
        }
    }
}
