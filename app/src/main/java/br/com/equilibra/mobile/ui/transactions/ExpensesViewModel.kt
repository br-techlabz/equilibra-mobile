package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
import br.com.equilibra.mobile.data.repository.TransactionRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.5 — Lista de despesas. A lógica de paginação/refresh/erros vive em
 * [PaginatedTransactionsViewModel] (extraída na Task 4.7 para reuso).
 */
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    // sem `private val`: usa o repository protected da base (ver nota lá —
    // campo próprio da subclasse seria null no init da superclasse em device)
    repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : PaginatedTransactionsViewModel(repository, accountRepository, categoryRepository) {

    override val entityLabel: String = "despesas"

    override suspend fun fetchPage(page: Int, includeCancelled: Boolean): ApiResult<TransactionPage> =
        repository.listExpenses(page, includeCancelled)
}
