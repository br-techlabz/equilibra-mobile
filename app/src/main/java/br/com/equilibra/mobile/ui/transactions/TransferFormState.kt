package br.com.equilibra.mobile.ui.transactions

/**
 * Task 4.9 — Estado hoisted do formulário de transferência entre contas.
 *
 * Diferente do formulário base (4.4): DUAS contas (origem + destino) e
 * categoria obrigatória (constraint @NotBlank do CreateTransferRequest).
 * A transferência é um payload único (source/destination) — nunca é
 * modelada como despesa + receita.
 */
data class TransferFormState(
    val description: String = "",
    val amount: String = "",
    /** Data digitada pelo usuário em dd/MM/aaaa (exibição pt-BR). */
    val occurredDate: String = "",
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val tagIds: List<String> = emptyList(),
    val notes: String = "",
    val descriptionError: String? = null,
    val amountError: String? = null,
    val dateError: String? = null,
    val sourceAccountError: String? = null,
    val destinationAccountError: String? = null,
    val categoryError: String? = null,
    val notesError: String? = null
) {

    fun validate(): TransferFormState = copy(
        descriptionError = TransactionFormValidator.validateDescription(description),
        amountError = TransactionFormValidator.validateAmount(amount),
        dateError = TransactionFormValidator.validateDate(occurredDate),
        sourceAccountError = if (sourceAccountId == null) "Selecione a conta de origem" else null,
        destinationAccountError = when {
            destinationAccountId == null -> "Selecione a conta de destino"
            destinationAccountId == sourceAccountId ->
                "Conta de origem e destino não podem ser iguais"
            else -> null
        },
        categoryError = if (categoryId == null) "Selecione a categoria" else null,
        notesError = TransactionFormValidator.validateNotes(notes)
    )

    val isValid: Boolean
        get() = descriptionError == null && amountError == null && dateError == null &&
            sourceAccountError == null && destinationAccountError == null &&
            categoryError == null && notesError == null

    fun normalizedAmount(): String = TransactionFormValidator.normalizeAmount(amount)

    fun occurredAtIso(): String? = TransactionFormValidator.toInstantIso(occurredDate)
}
