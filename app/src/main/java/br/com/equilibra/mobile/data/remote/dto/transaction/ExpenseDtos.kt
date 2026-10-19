package br.com.equilibra.mobile.data.remote.dto.transaction

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 4.2 — DTOs de despesa (contrato real do ExpenseController, /expenses).
 *
 * - POST   /expenses                    → 201 ExpenseResponse
 * - GET    /expenses?page=&size=&includeCancelled= → 200 ExpensePageResponse
 * - GET    /expenses/{id}               → 200
 * - PUT    /expenses/{id}               → 200
 * - PATCH  /expenses/{id}/cancel        → 200
 *
 * amount: BigDecimal no backend (JSON number ou string) — serializado como
 * String com [BigDecimalStringSerializer] para preservar precisão.
 * occurredAt: Instant ISO-8601 (Jackson sem WRITE_DATES_AS_TIMESTAMPS).
 */
@Serializable
data class ExpenseDto(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("accountId") val accountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("paymentMethod") val paymentMethod: String? = null,
    @SerialName("creditCardId") val creditCardId: String? = null,
    @SerialName("installmentCount") val installmentCount: Int? = null,
    @SerialName("cycleMonth") val cycleMonth: String? = null,
    @SerialName("purchaseAt") val purchaseAt: String? = null,
    @SerialName("status") val status: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("tags") val tags: List<TagSummaryDto> = emptyList()
)

@Serializable
data class ExpensePageResponseDto(
    @SerialName("content") val content: List<ExpenseDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("totalElements") val totalElements: Long,
    @SerialName("totalPages") val totalPages: Int
)

@Serializable
data class CreateExpenseRequestDto(
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("accountId") val accountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("tagIds") val tagIds: List<String>? = null
)

@Serializable
data class UpdateExpenseRequestDto(
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("accountId") val accountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("tagIds") val tagIds: List<String>? = null
)
