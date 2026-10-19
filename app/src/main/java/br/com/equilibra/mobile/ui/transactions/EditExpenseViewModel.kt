package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 4.6 — ViewModel de edição de despesa.
 *
 * - Pré-carrega do cache (Room) e sincroniza com a API; API prevalece.
 * - Despesa CANCELLED não é editável (regra do backend — 409); a tela
 *   bloqueia antes de enviar.
 * - Atualização só persiste no Room após sucesso remoto (repository).
 */
@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val expenseId: String = checkNotNull(savedStateHandle["expenseId"])

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _generalError = MutableStateFlow<String?>(null)
    val generalError: StateFlow<String?> = _generalError.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _editable = MutableStateFlow(true)
    val editable: StateFlow<Boolean> = _editable.asStateFlow()

    /**
     * O cache local não guarda tags (Task 4.1) e o backend substitui as
     * tags quando tagIds != null. Enviar null preserva as existentes;
     * a lista é enviada apenas quando o usuário toca no seletor.
     */
    private var tagsTouched = false

    init {
        viewModelScope.launch {
            val cached = repository.getByIdCached(expenseId)
            if (cached != null) prefill(cached)

            when (val result = repository.refreshById("EXPENSE", expenseId)) {
                is ApiResult.Success -> prefill(result.data)
                is ApiResult.NetworkError -> {
                    if (cached == null) {
                        _generalError.value = "Sem conexão. Não foi possível carregar a despesa."
                    }
                }
                else -> {
                    if (cached == null) {
                        _generalError.value = "Erro ao carregar despesa."
                    }
                }
            }
        }
    }

    private fun prefill(entity: TransactionEntity) {
        val cancelled = entity.status == "CANCELLED"
        _editable.value = !cancelled
        if (cancelled) {
            _generalError.value = "Despesa cancelada não pode ser editada."
        }
        _formState.value = TransactionFormState(
            description = entity.description,
            amount = entity.amount,
            occurredDate = TransactionFormValidator.toDisplayDate(entity.occurredAt),
            accountId = entity.sourceAccountId,
            categoryId = entity.categoryId,
            tagIds = emptyList(),
            notes = entity.notes.orEmpty()
        )
        tagsTouched = false
    }

    fun onDescriptionChange(value: String) = update { it.copy(description = value, descriptionError = null) }

    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = null) }

    fun onDateChange(value: String) = update { it.copy(occurredDate = value, dateError = null) }

    fun onAccountChange(value: String) = update { it.copy(accountId = value, accountError = null) }

    fun onCategoryChange(value: String) = update { it.copy(categoryId = value, categoryError = null) }

    fun onTagsChange(value: List<String>) {
        tagsTouched = true
        update { it.copy(tagIds = value) }
    }

    fun onNotesChange(value: String) = update { it.copy(notes = value, notesError = null) }

    fun clearGeneralError() {
        _generalError.value = null
    }

    fun consumeSuccess() {
        _isSuccess.value = false
    }

    fun onSaveClick() {
        if (_isSaving.value) return
        if (!_editable.value) return

        val validated = _formState.value.validate()
        _formState.value = validated
        if (!validated.isValid) return

        _isSaving.value = true
        _generalError.value = null

        viewModelScope.launch {
            val result = repository.updateExpense(
                id = expenseId,
                description = validated.description.trim(),
                occurredAt = validated.occurredAtIso()!!,
                accountId = validated.accountId!!,
                amount = validated.normalizedAmount(),
                categoryId = validated.categoryId!!,
                notes = validated.notes.ifBlank { null },
                tagIds = if (tagsTouched) validated.tagIds else null
            )
            _isSaving.value = false
            when (result) {
                is ApiResult.Success -> _isSuccess.value = true
                is ApiResult.HttpError ->
                    _generalError.value = "Erro ao salvar (${result.code}). Verifique os dados."
                is ApiResult.NetworkError ->
                    _generalError.value = "Sem conexão. Tente novamente."
                is ApiResult.UnknownError ->
                    _generalError.value = "Erro inesperado ao salvar."
            }
        }
    }

    private inline fun update(transform: (TransactionFormState) -> TransactionFormState) {
        _formState.value = transform(_formState.value)
        _generalError.value = null
    }
}
