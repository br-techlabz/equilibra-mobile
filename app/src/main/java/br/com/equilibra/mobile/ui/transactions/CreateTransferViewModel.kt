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
 * Task 4.9 — ViewModel de criação de transferência entre contas.
 *
 * - Origem ≠ destino validado no cliente (bloqueio antes do envio).
 * - Payload único de transferência via TransactionRepository.createTransfer
 *   (nunca despesa + receita); Room só atualiza após sucesso remoto.
 * - Saldos ficam sob responsabilidade do backend — nada é calculado aqui.
 */
@HiltViewModel
class CreateTransferViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TransferFormState())
    val formState: StateFlow<TransferFormState> = _formState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _generalError = MutableStateFlow<String?>(null)
    val generalError: StateFlow<String?> = _generalError.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    fun onDescriptionChange(value: String) = update {
        it.copy(description = value, descriptionError = null)
    }

    fun onAmountChange(value: String) = update {
        it.copy(amount = value, amountError = null)
    }

    fun onDateChange(value: String) = update {
        it.copy(occurredDate = value, dateError = null)
    }

    fun onSourceAccountChange(value: String) = update {
        // A igualdade origem/destino envolve as duas contas
        it.copy(sourceAccountId = value, sourceAccountError = null, destinationAccountError = null)
    }

    fun onDestinationAccountChange(value: String) = update {
        it.copy(destinationAccountId = value, destinationAccountError = null)
    }

    fun onCategoryChange(value: String) = update {
        it.copy(categoryId = value, categoryError = null)
    }

    fun onTagsChange(value: List<String>) = update { it.copy(tagIds = value) }

    fun onNotesChange(value: String) = update {
        it.copy(notes = value, notesError = null)
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
            val result = repository.createTransfer(
                description = validated.description.trim(),
                occurredAt = validated.occurredAtIso()!!,
                sourceAccountId = validated.sourceAccountId!!,
                destinationAccountId = validated.destinationAccountId!!,
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

    private inline fun update(transform: (TransferFormState) -> TransferFormState) {
        _formState.value = transform(_formState.value)
        _generalError.value = null
    }
}
