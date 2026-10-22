package br.com.equilibra.mobile.data.local.creditcard

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Task 6.1 — CreditCardEntity
 *
 * Espelha o contrato real do backend (CreditCardController, /credit-cards):
 * - UUID como String (PK)
 * - Limites como BigDecimal → String no Room (precision 19, scale 2, sem perda)
 *   O backend calcula committedLimit/availableLimit; o app armazena apenas o
 *   que a resposta trouxe (fonte da verdade é a API — Task 6.2).
 * - closingDay/dueDay: 1..31 (validação @Min(1) @Max(31) no backend)
 * - timezone: IANA zone ID (backend default "America/Sao_Paulo" quando ausente)
 * - status: ACTIVE | INACTIVE (CreditCardStatus, serializado como String)
 * - createdAt/updatedAt: Instant ISO-8601 (Instant.toString)
 * - syncedAt: epoch millis da última sincronização bem sucedida
 */
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String,
    /** Apelido opcional — null quando o backend não envia. */
    val nickname: String?,
    /** Limite total — BigDecimal como String (sem perda via Double/Float). */
    val totalLimit: String,
    /** Limite comprometido, calculado pelo backend — String. */
    val committedLimit: String,
    /** Limite reservado, sempre "0.00" no contrato atual — String. */
    val reservedLimit: String,
    /** Limite disponível, calculado pelo backend — String. */
    val availableLimit: String,
    /** Dia de fechamento da fatura (1..31). */
    val closingDay: Int,
    /** Dia de vencimento da fatura (1..31). */
    val dueDay: Int,
    /** IANA zone ID (ex: "America/Sao_Paulo"). */
    val timezone: String,
    /** ACTIVE | INACTIVE (CreditCardStatus). */
    val status: String,
    /** Instant ISO-8601 (ex: 2024-01-01T12:00:00Z). */
    val createdAt: String,
    /** Instant ISO-8601. */
    val updatedAt: String,
    /** Epoch millis da última sincronização bem sucedida — null se nunca sincronizado. */
    val syncedAt: Long? = null
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_INACTIVE = "INACTIVE"
    }
}
