package br.com.equilibra.mobile.data.remote.dto.purchase

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 6.6 — DTOs de compras de cartão de crédito.
 *
 * Contrato real do backend (CreditCardPurchaseController, /credit-card-purchases):
 * - POST /                       → 201 CreditCardPurchaseResponse
 * - GET  /card/{cardId}          → 200 List<CreditCardPurchaseResponse>
 * - GET  /{id}                   → 200 CreditCardPurchaseResponse
 * - POST /{id}/cancel            → 200 CreditCardPurchaseResponse
 *
 * Formato real das parcelas (CreditCardPurchaseService.create): o backend
 * divide o total em centavos — `base = centavos / n`, `resto = centavos % n`
 * — e distribui 1 centavo extra nas PRIMEIRAS `resto` parcelas. Assim
 * 100,00 em 3x vira 33,34 + 33,33 + 33,33. O app nunca recalcula isso:
 * só exibe o que veio na resposta (Task 6.9).
 *
 * - totalAmount/principalAmount: BigDecimal no backend → String aqui, sem
 *   passar por Double/Float ([BigDecimalStringSerializer] aceita number e string).
 * - purchaseDate/cycleMonth: LocalDate ISO-8601 ("2026-11-01").
 * - purchaseAt: Instant ISO-8601, nullable (migration V23 permitiu NULL).
 * - status: ACTIVE | REVERSED | CANCELLED ([PurchaseStatusDto]).
 */
@Serializable
data class PurchaseDto(
    @SerialName("id") val id: String,
    @SerialName("creditCardId") val creditCardId: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("description") val description: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalAmount") val totalAmount: String,
    /** LocalDate ISO-8601. */
    @SerialName("purchaseDate") val purchaseDate: String,
    /** Instant ISO-8601; pode vir null em compras antigas (V23). */
    @SerialName("purchaseAt") val purchaseAt: String? = null,
    @SerialName("installmentCount") val installmentCount: Int,
    /** LocalDate ISO-8601 — mês do ciclo da fatura. */
    @SerialName("cycleMonth") val cycleMonth: String,
    @SerialName("status") val status: String,
    @SerialName("installments") val installments: List<InstallmentDto> = emptyList()
)

@Serializable
data class InstallmentDto(
    @SerialName("sequenceNumber") val sequenceNumber: Int,
    @SerialName("totalInstallments") val totalInstallments: Int,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("principalAmount") val principalAmount: String,
    /** LocalDate ISO-8601. */
    @SerialName("cycleMonth") val cycleMonth: String
)

/**
 * Body de CreateCreditCardPurchaseRequest.java.
 * Validações do backend: description @NotBlank @Size(max=255); purchaseDate,
 * purchaseAt, totalAmount e installmentCount @NotNull; totalAmount
 * @DecimalMin("0.01"); installmentCount @Min(1) @Max(60).
 */
@Serializable
data class CreatePurchaseRequestDto(
    @SerialName("description") val description: String,
    @SerialName("purchaseDate") val purchaseDate: String,
    @SerialName("purchaseAt") val purchaseAt: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalAmount") val totalAmount: String,
    @SerialName("installmentCount") val installmentCount: Int,
    @SerialName("creditCardId") val creditCardId: String,
    @SerialName("categoryId") val categoryId: String
)

/**
 * Status da compra (backend CreditCardPurchaseStatus, String no JSON).
 */
object PurchaseStatusDto {
    const val ACTIVE = "ACTIVE"
    const val REVERSED = "REVERSED"
    const val CANCELLED = "CANCELLED"

    val all = listOf(ACTIVE, REVERSED, CANCELLED)

    fun labelPtBr(status: String): String = when (status) {
        ACTIVE -> "Ativa"
        REVERSED -> "Estornada"
        CANCELLED -> "Cancelada"
        else -> status
    }
}

/** Limites do backend para parcelas (@Min(1) @Max(60)). */
object InstallmentRules {
    const val MIN = 1
    const val MAX = 60

    fun isValid(count: Int): Boolean = count in MIN..MAX
}
