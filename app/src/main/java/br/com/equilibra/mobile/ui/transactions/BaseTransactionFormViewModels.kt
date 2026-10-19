package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Task 4.8 — Bases compartilhadas dos ViewModels de formulário de transação
 * (criar/editar, despesa/receita — lógica idêntica; só o endpoint muda).
 * Os testes de despesa (Task 4.6) validam a refactor; receita (Task 4.8)
 * cobre os mesmos comportamentos.
 */

abstract class BaseCreateTransactionViewModel : ViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _generalError = MutableStateFlow<String?>(null)
    val generalError: StateFlow<String?> = _generalError.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    protected abstract suspend fun create(
        state: TransactionFormState
    ): ApiResult<TransactionEntity>

    fun onDescriptionChange(value: String) = update { it.copy(description = value, descriptionError = null) }

    fun onAmountChange(value: String) = update { it.copy(amount = value, amountError = null) }

    fun onDateChange(value: String) = update { it.copy(occurredDate = value, dateError = null) }

    fun onAccountChange(value: String) = update { it.copy(accountId = value, accountError = null) }

    fun onCategoryChange(value: String) = update { it.copy(categoryId = value, categoryError = null) }

    fun onTagsChange(value: List<String>) = update { it.copy(tagIds = value) }

    fun onNotesChange(value: String) = update { it.copy(notes = value, notesError = null) }

    fun clearGeneralError() {
        _generalError.value = null
    }

    fun consumeSuccess() {
        _isSuccess.value = false
    }

    fun onSaveClick() {
        if (_isSaving.value) return

        val validated = _formState.value.validate()
        _formState.value = validated
        if (!validated.isValid) return

        _isSaving.value = true
        _generalError.value = null

        viewModelScope.launch {
            when (val result = create(validated)) {
                is ApiResult.Success -> {
                    _isSaving.value = false
                    _isSuccess.value = true
                }
                is ApiResult.HttpError -> {
                    _isSaving.value = false
                    _generalError.value = "Erro ao salvar (${result.code}). Verifique os dados."
                }
                is ApiResult.NetworkError -> {
                    _isSaving.value = false
                    _generalError.value = "Sem conexão. Tente novamente."
                }
                is ApiResult.UnknownError -> {
                    _isSaving.value = false
                    _generalError.value = "Erro inesperado ao salvar."
                }
            }
        }
    }

    protected fun update(transform: (TransactionFormState) -> TransactionFormState) {
        _formState.value = transform(_formState.value)
        _generalError.value = null
    }
}

abstract class BaseEditTransactionViewModel(
    private val repository: TransactionRepository,
    private val transactionId: String,
    private val entityType: String
) : ViewModel() {

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

    protected abstract suspend fun submitUpdate(
        id: String,
        state: TransactionFormState,
        tagIds: List<String>?
    ): ApiResult<TransactionEntity>

    init {
        viewModelScope.launch {
            val cached = repository.getByIdCached(transactionId)
            if (cached != null) prefill(cached)

            when (val result = repository.refreshById(entityType, transactionId)) {
                is ApiResult.Success -> prefill(result.data)
                is ApiResult.NetworkError -> {
                    if (cached == null) {
                        _generalError.value = "Sem conexão. Não foi possível carregar a transação."
                    }
                }
                else -> {
                    if (cached == null) {
                        _generalError.value = "Erro ao carregar transação."
                    }
                }
            }
        }
    }

    private fun prefill(entity: TransactionEntity) {
        val cancelled = entity.status == "CANCELLED"
        _editable.value = !cancelled
        if (cancelled) {
            _generalError.value = "Transação cancelada não pode ser editada."
        }
        _formState.value = TransactionFormState(
            description = entity.description,
            amount = entity.amount,
            occurredDate = TransactionFormValidator.toDisplayDate(entity.occurredAt),
            accountId = entity.sourceAccountId ?: entity.destinationAccountId,
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
            when (val result = submitUpdate(
                id = transactionId,
                state = validated,
                tagIds = if (tagsTouched) validated.tagIds else null
            )) {
                is ApiResult.Success -> {
                    _isSaving.value = false
                    _isSuccess.value = true
                }
                is ApiResult.HttpError -> {
                    _isSaving.value = false
                    _generalError.value = "Erro ao salvar (${result.code}). Verifique os dados."
                }
                is ApiResult.NetworkError -> {
                    _isSaving.value = false
                    _generalError.value = "Sem conexão. Tente novamente."
                }
                is ApiResult.UnknownError -> {
                    _isSaving.value = false
                    _generalError.value = "Erro inesperado ao salvar."
                }
            }
        }
    }

    private inline fun update(transform: (TransactionFormState) -> TransactionFormState) {
        _formState.value = transform(_formState.value)
        _generalError.value = null
    }
}
