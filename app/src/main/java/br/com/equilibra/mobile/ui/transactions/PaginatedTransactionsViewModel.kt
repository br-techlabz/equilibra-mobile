package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
import br.com.equilibra.mobile.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Task 4.7 — Estado compartilhado das listas paginadas de transações
 * (despesas e receitas — lógica idêntica, rótulos por tipo).
 */
sealed class TransactionsListUiState {
    data object Loading : TransactionsListUiState()
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
    ) : TransactionsListUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : TransactionsListUiState()
}

/**
 * Base das listas paginadas (Tasks 4.5 e 4.7) — paginação real com
 * deduplicação, refresh, toggle de canceladas, estados e mapas de nomes
 * do Room. Subclasses fornecem o fetch da página e o rótulo do tipo.
 */
/**
 * @repository é `protected val` de propósito: o [init] chama o método
 * abstrato [fetchPage] das subclasses, e em device (Main.immediate) isso
 * roda de forma síncrona DURANTE a construção — campos próprios das
 * subclasses ainda seriam null (NPE real ao abrir Despesas/Receitas).
 * Subclasses devem usar este campo, nunca declarar outro `repository`.
 */
abstract class PaginatedTransactionsViewModel(
    protected val repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    /** Nome plural para mensagens ("despesas", "receitas"). */
    protected open val entityLabel: String = "transações"

    private val _uiState = MutableStateFlow<TransactionsListUiState>(TransactionsListUiState.Loading)
    val uiState: StateFlow<TransactionsListUiState> = _uiState.asStateFlow()

    private val _accountsByName = MutableStateFlow<Map<String, AccountEntity>>(emptyMap())
    val accountsByName: StateFlow<Map<String, AccountEntity>> = _accountsByName.asStateFlow()

    private val _categoriesByName = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())
    val categoriesByName: StateFlow<Map<String, CategoryEntity>> = _categoriesByName.asStateFlow()

    private var includeCancelled = false

    protected abstract suspend fun fetchPage(
        page: Int,
        includeCancelled: Boolean
    ): ApiResult<TransactionPage>

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
        if (current !is TransactionsListUiState.Success) return
        if (current.endReached || current.isLoadingMore) return
        load(reset = false)
    }

    fun toggleIncludeCancelled() {
        includeCancelled = !includeCancelled
        load(reset = true)
    }

    fun dismissError() {
        val current = _uiState.value
        if (current is TransactionsListUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            val nextPage = if (reset) 0 else (current as? TransactionsListUiState.Success)?.page?.plus(1) ?: 0

            _uiState.value = when {
                reset && current is TransactionsListUiState.Success ->
                    current.copy(isRefreshing = true, error = null)
                !reset && current is TransactionsListUiState.Success ->
                    current.copy(isLoadingMore = true)
                else -> TransactionsListUiState.Loading
            }

            when (val result = fetchPage(nextPage, includeCancelled)) {
                is ApiResult.Success -> {
                    val data = result.data
                    val previous = (_uiState.value as? TransactionsListUiState.Success)?.items.orEmpty()
                    val items = if (reset) {
                        data.items
                    } else {
                        (previous + data.items).distinctBy { it.id }
                    }
                    _uiState.value = TransactionsListUiState.Success(
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
                is ApiResult.HttpError -> handleError("Erro ao carregar $entityLabel (${result.code}).", reset)
                is ApiResult.NetworkError -> handleError("Sem conexão. Verifique sua internet.", reset)
                is ApiResult.UnknownError -> handleError("Erro inesperado ao carregar $entityLabel.", reset)
            }
        }
    }

    private fun handleError(message: String, reset: Boolean) {
        val current = _uiState.value
        _uiState.value = if (current is TransactionsListUiState.Success && current.items.isNotEmpty()) {
            // Erro em refresh/loadMore com itens: preserva lista e mostra snackbar
            current.copy(isRefreshing = false, isLoadingMore = false, error = message)
        } else {
            TransactionsListUiState.Error(message)
        }
    }
}
