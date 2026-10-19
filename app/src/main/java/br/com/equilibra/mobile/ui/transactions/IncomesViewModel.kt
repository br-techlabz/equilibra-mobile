package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionPage
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.7 — Lista de receitas (mesma base paginada das despesas).
 *
 * - Filtro de canceladas via includeCancelled (contrato real do /incomes).
 * - Conta da receita é o destino — resolvida na tela via destinationAccountId.
 */
@HiltViewModel
class IncomesViewModel @Inject constructor(
    private val repository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : PaginatedTransactionsViewModel(repository, accountRepository, categoryRepository) {

    override val entityLabel: String = "receitas"

    override suspend fun fetchPage(page: Int, includeCancelled: Boolean): ApiResult<TransactionPage> =
        repository.listIncomes(page, includeCancelled)
}
