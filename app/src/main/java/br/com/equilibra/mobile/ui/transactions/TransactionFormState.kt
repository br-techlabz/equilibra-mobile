package br.com.equilibra.mobile.ui.transactions

/**
 * Task 4.4 — Estado hoisted do formulário reutilizável de transação.
 *
 * Despesas e receitas compartilham este estado; o consumidor (ViewModel da
 * tela) é dono do estado e aplica [validate] antes de enviar. Erros por
 * campo ficam no próprio estado para exibição inline.
 */
data class TransactionFormState(
    val description: String = "",
    val amount: String = "",
    /** Data digitada pelo usuário em dd/MM/aaaa (exibição pt-BR). */
    val occurredDate: String = "",
    val accountId: String? = null,
    val categoryId: String? = null,
    val tagIds: List<String> = emptyList(),
    val notes: String = "",
    val descriptionError: String? = null,
    val amountError: String? = null,
    val dateError: String? = null,
    val accountError: String? = null,
    val categoryError: String? = null,
    val notesError: String? = null
) {

    /** Valida todos os campos e retorna o estado com os erros atualizados. */
    fun validate(): TransactionFormState = copy(
        descriptionError = TransactionFormValidator.validateDescription(description),
        amountError = TransactionFormValidator.validateAmount(amount),
        dateError = TransactionFormValidator.validateDate(occurredDate),
        accountError = if (accountId == null) "Selecione a conta" else null,
        categoryError = if (categoryId == null) "Selecione a categoria" else null,
        notesError = TransactionFormValidator.validateNotes(notes)
    )

    /** Válido após [validate] — nenhum campo com erro. */
    val isValid: Boolean
        get() = descriptionError == null && amountError == null && dateError == null &&
            accountError == null && categoryError == null && notesError == null

    /** Valor pronto para a API (pt-BR → ponto decimal, sem arredondar). */
    fun normalizedAmount(): String = TransactionFormValidator.normalizeAmount(amount)

    /** Data como Instant ISO-8601 para a API; nula se a data for inválida. */
    fun occurredAtIso(): String? = TransactionFormValidator.toInstantIso(occurredDate)
}
