package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.8 — Criação de receita sobre a base compartilhada.
 * Mesmo comportamento da despesa; categorias compatíveis com INCOME vêm
 * do CategorySelector filtrado pela config do formulário.
 */
@HiltViewModel
class CreateIncomeViewModel @Inject constructor(
    private val repository: TransactionRepository
) : BaseCreateTransactionViewModel() {

    override suspend fun create(state: TransactionFormState): ApiResult<TransactionEntity> =
        repository.createIncome(
            description = state.description.trim(),
            occurredAt = state.occurredAtIso()!!,
            accountId = state.accountId!!,
            amount = state.normalizedAmount(),
            categoryId = state.categoryId!!,
            notes = state.notes.ifBlank { null },
            tagIds = state.tagIds
        )
}
