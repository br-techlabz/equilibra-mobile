package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Task 4.8 — Edição de receita sobre a base compartilhada.
 * Receita cancelada não é editável; tags intocadas → null (preserva).
 */
@HiltViewModel
class EditIncomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : BaseEditTransactionViewModel(
    repository = repository,
    transactionId = checkNotNull(savedStateHandle["incomeId"]),
    entityType = "INCOME"
) {

    override suspend fun submitUpdate(
        id: String,
        state: TransactionFormState,
        tagIds: List<String>?
    ): ApiResult<TransactionEntity> = repository.updateIncome(
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
