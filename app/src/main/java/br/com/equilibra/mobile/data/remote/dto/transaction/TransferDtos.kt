package br.com.equilibra.mobile.data.remote.dto.transaction

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 4.2 — DTOs de transferência (contrato real do TransferController, /transfers).
 *
 * - POST   /transfers                   → 201 TransferResponse
 * - GET    /transfers?page=&size=&includeCancelled= → 200 TransferPageResponse
 * - GET    /transfers/{id}              → 200
 * - PUT    /transfers/{id}              → 200
 * - PATCH  /transfers/{id}/cancel       → 200
 *
 * Payload com sourceAccountId + destinationAccountId — a transferência NÃO é
 * modelada como despesa+receita (Task 4.9); categoryId é obrigatória no backend.
 */
@Serializable
data class TransferDto(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("sourceAccountId") val sourceAccountId: String,
    @SerialName("destinationAccountId") val destinationAccountId: String,
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
data class TransferPageResponseDto(
    @SerialName("content") val content: List<TransferDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("totalElements") val totalElements: Long,
    @SerialName("totalPages") val totalPages: Int
)

@Serializable
data class CreateTransferRequestDto(
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("sourceAccountId") val sourceAccountId: String,
    @SerialName("destinationAccountId") val destinationAccountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("tagIds") val tagIds: List<String>? = null
)

@Serializable
data class UpdateTransferRequestDto(
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("sourceAccountId") val sourceAccountId: String,
    @SerialName("destinationAccountId") val destinationAccountId: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("tagIds") val tagIds: List<String>? = null
)
