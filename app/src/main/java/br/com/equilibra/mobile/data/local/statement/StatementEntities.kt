package br.com.equilibra.mobile.data.local.statement

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task 7.1 — Fatura de cartão de crédito (cache local).
 *
 * Espelha CreditCardStatementResponse do backend:
 * - cycleStart/cycleEnd/dueDate: LocalDate ISO-8601 ("2026-11-01")
 * - closingAt: Instant ISO-8601
 * - status: OPEN | CLOSED | OVERDUE | PARTIALLY_PAID | PAID | CANCELLED
 * - totalAmount/paidAmount/outstandingAmount: BigDecimal como String, sem
 *   Double/Float. São calculados pelo backend e apenas copiados aqui.
 *
 * Sem FK para credit_cards, pelo mesmo motivo de PurchaseEntity: o cache de
 * faturas é sincronizado por cartão e não deve perder dados se a lista de
 * cartões for atualizada antes; a integridade é garantida pela API (fonte da
 * verdade). O índice cobre a listagem por cartão ordenada por cycleStart.
 */
@Entity(
    tableName = "credit_card_statements",
    indices = [Index(value = ["creditCardId", "cycleStart"])]
)
data class StatementEntity(
    @PrimaryKey val id: String,
    val creditCardId: String,
    /** LocalDate ISO-8601. */
    val cycleStart: String,
    /** LocalDate ISO-8601. */
    val cycleEnd: String,
    /** Instant ISO-8601. */
    val closingAt: String,
    /** LocalDate ISO-8601. */
    val dueDate: String,
    /** OPEN | CLOSED | OVERDUE | PARTIALLY_PAID | PAID | CANCELLED. */
    val status: String,
    /** BigDecimal como String — ex.: "1234.56". */
    val totalAmount: String,
    /** BigDecimal como String. */
    val paidAmount: String,
    /** BigDecimal como String (pendente). */
    val outstandingAmount: String,
    /** Epoch millis da última sincronização bem-sucedida. */
    val syncedAt: Long? = null
) {
    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_CLOSED = "CLOSED"
        const val STATUS_OVERDUE = "OVERDUE"
        const val STATUS_PARTIALLY_PAID = "PARTIALLY_PAID"
        const val STATUS_PAID = "PAID"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

/**
 * Task 7.1 — Item da fatura (parcela de compra que caiu no ciclo).
 *
 * Espelha CreditCardStatementResponse.Item. O `amount` é o principal da
 * parcela distribuído pelo backend; nunca é recalculado aqui (Task 7.5).
 * FK em cascata: remover a fatura remove os itens.
 */
@Entity(
    tableName = "statement_items",
    foreignKeys = [
        ForeignKey(
            entity = StatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["statementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["statementId"])]
)
data class StatementItemEntity(
    /** Id real da parcela no backend (Installment.id). */
    @PrimaryKey val id: String,
    val statementId: String,
    val purchaseId: String,
    val description: String,
    val sequenceNumber: Int,
    val totalInstallments: Int,
    /** BigDecimal como String. */
    val amount: String,
    /** LocalDate ISO-8601. */
    val cycleMonth: String
)
