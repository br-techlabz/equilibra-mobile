package br.com.equilibra.mobile.data.remote.dto.statement

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 7.1 — DTOs de faturas de cartão de crédito.
 *
 * Contrato real do backend (CreditCardStatementController):
 * - GET  /credit-cards/{cardId}/current-statement → 200 CreditCardStatementResponse
 * - GET  /credit-cards/{cardId}/statements        → 200 List<CreditCardStatementResponse>
 * - GET  /credit-card-statements/{id}             → 200 CreditCardStatementResponse
 * - POST /credit-cards/{cardId}/statements/close  → 200 CreditCardStatementResponse
 *
 * Shape de CreditCardStatementResponse.java:
 * id, creditCardId, cycleStart/cycleEnd (LocalDate), closingAt (Instant),
 * dueDate (LocalDate), status (CreditCardStatementStatus), totalAmount,
 * paidAmount e outstandingAmount (BigDecimal), além de items:
 * id, purchaseId, description, sequenceNumber, totalInstallments,
 * amount (BigDecimal) e cycleMonth (LocalDate).
 *
 * - BigDecimal chega como JSON number e é exposto como String
 *   ([BigDecimalStringSerializer] aceita number e string) — sem Double/Float.
 * - Os agregados (total/pago/pendente) são calculados PELO BACKEND. O app
 *   apenas armazena/exibe o que veio na resposta; nunca recalcula a fatura.
 * - O pagamento da fatura é outro controller (/credit-card-statements/{id}/payments)
 *   e será tratado na Task 7.6.
 */
@Serializable
data class StatementDto(
    @SerialName("id") val id: String,
    @SerialName("creditCardId") val creditCardId: String,
    /** LocalDate ISO-8601 — início do ciclo. */
    @SerialName("cycleStart") val cycleStart: String,
    /** LocalDate ISO-8601 — fim do ciclo. */
    @SerialName("cycleEnd") val cycleEnd: String,
    /** Instant ISO-8601 — timestamp de fechamento do ciclo. */
    @SerialName("closingAt") val closingAt: String,
    /** LocalDate ISO-8601 — vencimento. */
    @SerialName("dueDate") val dueDate: String,
    /** OPEN | CLOSED | OVERDUE | PARTIALLY_PAID | PAID | CANCELLED. */
    @SerialName("status") val status: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalAmount") val totalAmount: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("paidAmount") val paidAmount: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("outstandingAmount") val outstandingAmount: String,
    @SerialName("items") val items: List<StatementItemDto> = emptyList()
)

/**
 * Item da fatura (CreditCardStatementResponse.Item): uma parcela de compra
 * que caiu neste ciclo. `amount` é o principal da parcela, calculado pelo
 * backend na criação da compra.
 */
@Serializable
data class StatementItemDto(
    @SerialName("id") val id: String,
    @SerialName("purchaseId") val purchaseId: String,
    @SerialName("description") val description: String,
    @SerialName("sequenceNumber") val sequenceNumber: Int,
    @SerialName("totalInstallments") val totalInstallments: Int,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    /** LocalDate ISO-8601 — mês do ciclo da parcela. */
    @SerialName("cycleMonth") val cycleMonth: String
)

/**
 * Body de CloseStatementRequest.java: o backend deriva o ciclo a partir de
 * `cycleMonth` (qualquer dia do mês; ele normaliza para o dia 1).
 */
@Serializable
data class CloseStatementRequestDto(
    @SerialName("cycleMonth") val cycleMonth: String
)

/**
 * Status real da fatura (backend CreditCardStatementStatus, String no JSON).
 * A migration V24 do backend estendeu os status para PARTIALLY_PAID e PAID.
 */
object StatementStatusDto {
    const val OPEN = "OPEN"
    const val CLOSED = "CLOSED"
    const val OVERDUE = "OVERDUE"
    const val PARTIALLY_PAID = "PARTIALLY_PAID"
    const val PAID = "PAID"
    const val CANCELLED = "CANCELLED"

    val all = listOf(OPEN, CLOSED, OVERDUE, PARTIALLY_PAID, PAID, CANCELLED)

    /** Label pt-BR para exibição em telas (Task 7.3/7.5). */
    fun labelPtBr(status: String): String = when (status) {
        OPEN -> "Aberta"
        CLOSED -> "Fechada"
        OVERDUE -> "Vencida"
        PARTIALLY_PAID -> "Parcialmente paga"
        PAID -> "Paga"
        CANCELLED -> "Cancelada"
        else -> status
    }
}
