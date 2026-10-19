package br.com.equilibra.mobile.data.remote.dto.transaction

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 4.2 — DTOs de receita (contrato real do IncomeController, /incomes).
 *
 * - POST   /incomes                     → 201 IncomeResponse
 * - GET    /incomes?page=&size=&includeCancelled= → 200 IncomePageResponse
 * - GET    /incomes/{id}                → 200
 * - PUT    /incomes/{id}                → 200
 * - PATCH  /incomes/{id}/cancel         → 200
 *
 * NOTA: response da receita NÃO é o mesmo shape da despesa (sem campos de
 * cartão/pagamento) — DTOs próprios, conforme contrato real.
 */
@Serializable
data class IncomeDto(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("accountId") val accountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("status") val status: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("tags") val tags: List<TagSummaryDto> = emptyList()
)

@Serializable
data class IncomePageResponseDto(
    @SerialName("content") val content: List<IncomeDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("totalElements") val totalElements: Long,
    @SerialName("totalPages") val totalPages: Int
)

@Serializable
data class CreateIncomeRequestDto(
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
data class UpdateIncomeRequestDto(
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("accountId") val accountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("tagIds") val tagIds: List<String>? = null
)
