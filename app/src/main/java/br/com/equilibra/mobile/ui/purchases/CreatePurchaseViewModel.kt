package br.com.equilibra.mobile.ui.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePurchaseFormState(
    val creditCardId: String = "",
    val description: String = "",
    val descriptionError: String? = null,
    val amount: String = "",
    val amountError: String? = null,
    val date: String = "",
    val dateError: String? = null,
    val installments: String = "1",
    val installmentsError: String? = null,
    val categoryId: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val generalError: String? = null
)

/**
 * Task 6.8 — Cadastro de compra parcelada (1..60).
 *
 * - Valida tudo antes de chamar a API (nada de requisição que o backend
 *   certamente recusaria).
 * - Envia datas no formato do contrato: purchaseDate como LocalDate ISO e
 *   purchaseAt como Instant no início do dia UTC.
 * - Não calcula ciclo nem valores de parcela — o backend devolve e a
 *   listagem/detalhe exibem (Tasks 6.7/6.9).
 * - Falha preserva o formulário; sucesso só é sinalizado após a confirmação.
 */
@HiltViewModel
class CreatePurchaseViewModel @Inject constructor(
    private val repository: PurchaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    constructor(repository: PurchaseRepository, cardId: String) : this(
        repository,
        SavedStateHandle(mapOf(ARG_CARD_ID to cardId))
    )

    private val _uiState = MutableStateFlow(
        CreatePurchaseFormState(
            creditCardId = savedStateHandle.get<String>(ARG_CARD_ID).orEmpty()
        )
    )
    val uiState: StateFlow<CreatePurchaseFormState> = _uiState.asStateFlow()

    fun onDescriptionChange(value: String) =
        _uiState.update { it.copy(description = value, descriptionError = null, generalError = null) }

    fun onAmountChange(value: String) =
        _uiState.update { it.copy(amount = value, amountError = null, generalError = null) }

    fun onDateChange(value: String) =
        _uiState.update { it.copy(date = value, dateError = null, generalError = null) }

    fun onInstallmentsChange(value: String) =
        _uiState.update { it.copy(installments = value, installmentsError = null, generalError = null) }

    fun onCategoryChange(value: String?) =
        _uiState.update { it.copy(categoryId = value, categoryError = null, generalError = null) }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return

        val descriptionError = PurchaseFormValidator.validateDescription(state.description)
        val amountError = PurchaseFormValidator.validateAmount(state.amount)
        val dateError = PurchaseFormValidator.validateDate(state.date)
        val installmentsError = PurchaseFormValidator.validateInstallments(state.installments)
        val categoryError = PurchaseFormValidator.validateCategory(state.categoryId)
        val cardError = PurchaseFormValidator.validateCard(state.creditCardId)

        if (listOfNotNull(
                descriptionError, amountError, dateError,
                installmentsError, categoryError, cardError
            ).isNotEmpty()
        ) {
            _uiState.update {
                it.copy(
                    descriptionError = descriptionError,
                    amountError = amountError,
                    dateError = dateError,
                    installmentsError = installmentsError,
                    categoryError = categoryError,
                    generalError = cardError
                )
            }
            return
        }

        val purchaseDate = PurchaseFormValidator.toLocalDateIso(state.date)
        val purchaseAt = PurchaseFormValidator.toPurchaseAtIso(state.date)
        val installments = state.installments.trim().toIntOrNull()
        if (purchaseDate == null || purchaseAt == null || installments == null) return

        _uiState.update { it.copy(isSaving = true, generalError = null) }

        viewModelScope.launch {
            val result = repository.create(
                description = state.description.trim(),
                purchaseDate = purchaseDate,
                purchaseAt = purchaseAt,
                totalAmount = PurchaseFormValidator.normalizeAmount(state.amount),
                installmentCount = installments,
                creditCardId = state.creditCardId,
                categoryId = state.categoryId!!
            )
            when (result) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                is ApiResult.HttpError -> {
                    val msg = when (result.code) {
                        400, 422 -> parseDetail(result.body) ?: "Dados inválidos. Verifique os campos."
                        404 -> "Cartão ou categoria não encontrados."
                        409 -> parseDetail(result.body) ?: "Não foi possível registrar a compra."
                        else -> "Erro ao salvar (${result.code})."
                    }
                    _uiState.update { it.copy(isSaving = false, generalError = msg) }
                }
                is ApiResult.NetworkError ->
                    _uiState.update { it.copy(isSaving = false, generalError = "Sem conexão. Tente novamente.") }
                is ApiResult.UnknownError ->
                    _uiState.update { it.copy(isSaving = false, generalError = "Erro inesperado ao salvar.") }
            }
        }
    }

    private fun parseDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
            ?: Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    }

    private companion object {
        const val ARG_CARD_ID = "cardId"
    }
}
