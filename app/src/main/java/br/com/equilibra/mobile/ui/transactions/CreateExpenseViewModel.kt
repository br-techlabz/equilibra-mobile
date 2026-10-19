package br.com.equilibra.mobile.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Task 4.6 — ViewModel de criação de despesa.
 *
 * - Estado hoisted do formulário ([TransactionFormState]); validação antes
 *   do envio (valor positivo, conta/categoria obrigatórias).
 * - Escrita remota via TransactionRepository; Room só atualiza após
 *   sucesso (responsabilidade do repository); falha não altera cache.
 * - Proteção contra duplo toque (isSaving).
 */
@HiltViewModel
class CreateExpenseViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _generalError = MutableStateFlow<String?>(null)
    val generalError: StateFlow<String?> = _generalError.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    fun onDescriptionChange(value: String) {
        _formState.value = _formState.value.copy(description = value, descriptionError = null)
        _generalError.value = null
    }

    fun onAmountChange(value: String) {
        _formState.value = _formState.value.copy(amount = value, amountError = null)
        _generalError.value = null
    }

    fun onDateChange(value: String) {
        _formState.value = _formState.value.copy(occurredDate = value, dateError = null)
        _generalError.value = null
    }

    fun onAccountChange(value: String) {
        _formState.value = _formState.value.copy(accountId = value, accountError = null)
        _generalError.value = null
    }

    fun onCategoryChange(value: String) {
        _formState.value = _formState.value.copy(categoryId = value, categoryError = null)
        _generalError.value = null
    }

    fun onTagsChange(value: List<String>) {
        _formState.value = _formState.value.copy(tagIds = value)
        _generalError.value = null
    }

    fun onNotesChange(value: String) {
        _formState.value = _formState.value.copy(notes = value, notesError = null)
        _generalError.value = null
    }

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
            val result = repository.createExpense(
                description = validated.description.trim(),
                occurredAt = validated.occurredAtIso()!!,
                accountId = validated.accountId!!,
                amount = validated.normalizedAmount(),
                categoryId = validated.categoryId!!,
                notes = validated.notes.ifBlank { null },
                tagIds = validated.tagIds
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
}
