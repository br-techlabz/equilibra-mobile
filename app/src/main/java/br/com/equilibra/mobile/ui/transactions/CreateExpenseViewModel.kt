package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.6 — Criação de despesa sobre a base compartilhada (Task 4.8).
 * Estado hoisted, validação antes do envio, Room só após sucesso remoto.
 */
@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    private val repository: TransactionRepository
) : BaseCreateTransactionViewModel() {

    override suspend fun create(state: TransactionFormState): ApiResult<TransactionEntity> =
        repository.createExpense(
            description = state.description.trim(),
            occurredAt = state.occurredAtIso()!!,
            accountId = state.accountId!!,
            amount = state.normalizedAmount(),
            categoryId = state.categoryId!!,
            notes = state.notes.ifBlank { null },
            tagIds = state.tagIds
        )
}
