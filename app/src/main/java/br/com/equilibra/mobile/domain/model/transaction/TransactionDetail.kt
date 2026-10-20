package br.com.equilibra.mobile.domain.model.transaction

/**
 * Task 4.12 — Detalhe da transação.
 *
 * Shape mínimo comum às respostas por tipo do backend:
 * - GET /expenses/{id}  → ExpenseResponse  (accountId = origem)
 * - GET /incomes/{id}   → IncomeResponse   (accountId = destino)
 * - GET /transfers/{id} → TransferResponse (origem + destino)
 *
 * Não existe GET /transactions/{id} no backend; o tipo é conhecido pela rota
 * de origem e os campos opcionais (notas, categoria, contas) variam por tipo.
 * Valores monetários permanecem String (sem perda de precisão).
 */
data class TransactionDetail(
    val id: String,
    val type: String,
    val status: String,
    val description: String,
    val occurredAt: String,
    val amount: String,
    val sourceAccountId: String? = null,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    /** Nomes das tags associadas (tags não ficam em Room — Task 4.1). */
    val tagNames: List<String> = emptyList(),
    /** Forma de pagamento (só despesa; backend devolve BANK_ACCOUNT). */
    val paymentMethod: String? = null
) {

    /** Edição só para despesa/receita ativas — telas das Tasks 4.6/4.8. */
    val canEdit: Boolean
        get() = status == TransactionStatus.ACTIVE && TransactionType.isEditable(type)

    /**
     * Cancelamento existe só nos controllers de despesa, receita e
     * transferência; pagamento de fatura não tem endpoint de cancelamento
     * e transação cancelada é imutável no backend.
     */
    val canCancel: Boolean
        get() = status == TransactionStatus.ACTIVE && TransactionType.isCancellable(type)
}

/** Tipos do contrato real (TransactionType do backend). */
object TransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val TRANSFER = "TRANSFER"
    const val STATEMENT_PAYMENT = "STATEMENT_PAYMENT"

    /**
     * Rotas de edição existentes no app para cada tipo (Tasks 4.6/4.8).
     * O backend aceita PUT /transfers/{id}, mas o app ainda não tem tela de
     * edição de transferência — não expomos uma ação sem destino.
     */
    fun isEditable(type: String): Boolean = when (type) {
        EXPENSE, INCOME -> true
        else -> false
    }

    /** Tipos com PATCH /{id}/cancel no backend (despesa, receita, transferência). */
    fun isCancellable(type: String): Boolean = when (type) {
        EXPENSE, INCOME, TRANSFER -> true
        else -> false
    }

    fun label(type: String): String = when (type) {
        EXPENSE -> "Despesa"
        INCOME -> "Receita"
        TRANSFER -> "Transferência"
        STATEMENT_PAYMENT -> "Pagamento de fatura"
        else -> type
    }
}

/** Status do contrato real (TransactionStatus do backend). */
object TransactionStatus {
    const val ACTIVE = "ACTIVE"
    const val CANCELLED = "CANCELLED"

    fun label(status: String): String = when (status) {
        ACTIVE -> "Ativa"
        CANCELLED -> "Cancelada"
        else -> status
    }
}
