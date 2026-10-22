package br.com.equilibra.mobile.data.local.purchase

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task 6.6 — Compra de cartão de crédito (cache local).
 *
 * Espelha CreditCardPurchaseResponse do backend:
 * - totalAmount: decimal como String (padrão do projeto — sem Double/Float)
 * - purchaseDate/cycleMonth: LocalDate ISO-8601 ("2026-11-01")
 * - purchaseAt: Instant ISO-8601, nullable (migration V23 do backend)
 * - status: ACTIVE | REVERSED | CANCELLED ([PurchaseStatusDto])
 * - installmentCount: 1..60 (validado pelo backend e pelo formulário)
 *
 * cycleMonth é calculado PELO BACKEND (regra de fechamento do cartão) e só
 * é exibido aqui — o app nunca recalcula a fatura (Task 6.8).
 *
 * Sem FK para credit_cards de propósito: o cache de compras é sincronizado
 * por cartão e sobrevive a um refresh da lista de cartões que ainda não
 * tenha trazido o cartão — a integridade é garantida pela API (fonte da
 * verdade), não pelo SQLite. Os índices cobrem as consultas reais.
 */
@Entity(
    tableName = "credit_card_purchases",
    indices = [
        Index(value = ["creditCardId", "purchaseDate"]),
        Index(value = ["purchaseDate"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val creditCardId: String,
    val categoryId: String,
    val description: String,
    /** BigDecimal como String — ex.: "1234.56". */
    val totalAmount: String,
    /** LocalDate ISO-8601. */
    val purchaseDate: String,
    /** Instant ISO-8601; null em compras anteriores à migration V23. */
    val purchaseAt: String?,
    /** 1..60. */
    val installmentCount: Int,
    /** LocalDate ISO-8601 — mês do ciclo, calculado pelo backend. */
    val cycleMonth: String,
    /** ACTIVE | REVERSED | CANCELLED. */
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    /** Epoch millis da última sincronização bem-sucedida. */
    val syncedAt: Long? = null
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_REVERSED = "REVERSED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}

/**
 * Task 6.6 — Parcela de uma compra.
 *
 * O backend distribui os centavos do total entre as parcelas (resto nas
 * primeiras), então cada principalAmount é armazenado como veio — nunca
 * recalculado aqui. FK em cascata: remover a compra remove as parcelas.
 */
@Entity(
    tableName = "purchase_installments",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["purchaseId"])]
)
data class InstallmentEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val sequenceNumber: Int,
    val totalInstallments: Int,
    /** BigDecimal como String. */
    val principalAmount: String,
    /** LocalDate ISO-8601. */
    val cycleMonth: String
)
