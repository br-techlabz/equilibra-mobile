package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExpensesUiState {
    data object Loading : ExpensesUiState()
    data class Success(
        val items: List<TransactionEntity>,
        val page: Int = 0,
        val totalPages: Int = 1,
        val totalElements: Long = 0,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val includeCancelled: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null
    ) : ExpensesUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : ExpensesUiState()
}

/**
 * Task 4.5 — ViewModel da lista de despesas (API paginada, sem cache).
 *
 * - Paginação real do backend (page 0-based, size 20); loadMore anexa a
 *   próxima página com deduplicação por id; nunca carrega todas de uma vez.
 * - Filtro suportado pela API: includeCancelled (os demais filtros da task
 *   só existem no endpoint de histórico unificado — Task 4.11).
 * - Estados: loading inicial, vazio, erro+retry, refresh, erro em refresh
 *   preserva itens (snackbar).
 * - Expõe mapas de contas/categorias do Room para resolução de nomes.
 */
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val _accountsByName = MutableStateFlow<Map<String, AccountEntity>>(emptyMap())
    val accountsByName: StateFlow<Map<String, AccountEntity>> = _accountsByName.asStateFlow()

    private val _categoriesByName = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())
    val categoriesByName: StateFlow<Map<String, CategoryEntity>> = _categoriesByName.asStateFlow()

    private var includeCancelled = false

    init {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { list ->
                _accountsByName.value = list.associateBy { it.id }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeCategories(null).collect { list ->
                _categoriesByName.value = list.associateBy { it.id }
            }
        }
        load(reset = true)
    }

    fun refresh() = load(reset = true)

    fun retry() = load(reset = true)

    fun loadMore() {
        val current = _uiState.value
        if (current !is ExpensesUiState.Success) return
        if (current.endReached || current.isLoadingMore) return
        load(reset = false)
    }

    fun toggleIncludeCancelled() {
        includeCancelled = !includeCancelled
        load(reset = true)
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is ExpensesUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val nextPage = if (reset) 0 else (current as? ExpensesUiState.Success)?.page?.plus(1) ?: 0

            _uiState.value = when {
                reset && current is ExpensesUiState.Success ->
                    current.copy(isRefreshing = true, error = null)
                !reset && current is ExpensesUiState.Success ->
                    current.copy(isLoadingMore = true)
                else -> ExpensesUiState.Loading
            }

            when (val result = repository.listExpenses(nextPage, includeCancelled)) {
                is ApiResult.Success -> {
                    val data = result.data
                    val previous = (_uiState.value as? ExpensesUiState.Success)?.items.orEmpty()
                    val items = if (reset) {
                        data.items
                    } else {
                        (previous + data.items).distinctBy { it.id }
                    }
                    _uiState.value = ExpensesUiState.Success(
                        items = items,
                        page = data.page,
                        totalPages = data.totalPages,
                        totalElements = data.totalElements,
                        isLoadingMore = false,
                        isRefreshing = false,
                        includeCancelled = includeCancelled,
                        endReached = data.isLastPage
                    )
                }
                is ApiResult.HttpError -> handleError("Erro ao carregar despesas (${result.code}).", reset)
                is ApiResult.NetworkError -> handleError("Sem conexão. Verifique sua internet.", reset)
                is ApiResult.UnknownError -> handleError("Erro inesperado ao carregar despesas.", reset)
            }
        }
    }

    private fun handleError(message: String, reset: Boolean) {
        val current = _uiState.value
        _uiState.value = if (current is ExpensesUiState.Success && current.items.isNotEmpty()) {
            // Erro em refresh/loadMore com itens: preserva lista e mostra snackbar
            current.copy(isRefreshing = false, isLoadingMore = false, error = message)
        } else if (current is ExpensesUiState.Loading || reset) {
            ExpensesUiState.Error(message)
        } else {
            ExpensesUiState.Error(message)
        }
    }
}
