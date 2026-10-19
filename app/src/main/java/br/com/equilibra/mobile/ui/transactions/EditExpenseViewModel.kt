package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.6 — Edição de despesa sobre a base compartilhada (Task 4.8).
 * Cancelada não é editável (regra 409 do backend); tags intocadas → null.
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : BaseEditTransactionViewModel(
    repository = repository,
    transactionId = checkNotNull(savedStateHandle["expenseId"]),
    entityType = "EXPENSE"
) {

    override suspend fun submitUpdate(
        id: String,
        state: TransactionFormState,
        tagIds: List<String>?
    ): ApiResult<TransactionEntity> = repository.updateExpense(
        id = id,
        description = state.description.trim(),
        occurredAt = state.occurredAtIso()!!,
        accountId = state.accountId!!,
        amount = state.normalizedAmount(),
        categoryId = state.categoryId!!,
        notes = state.notes.ifBlank { null },
        tagIds = tagIds
    )
}
