package br.com.equilibra.mobile.data.remote.dto.transaction

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 4.11 — DTOs do histórico unificado (contrato real do
 * TransactionHistoryController, /transactions).
 *
 * Shape único com type/status/attachmentCount — NÃO é o mesmo DTO de
 * despesa/receita/transferência (que não têm type nem attachmentCount).
 */
@Serializable
data class HistoryItemDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("status") val status: String,
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("amount") val amount: String,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("sourceAccountId") val sourceAccountId: String? = null,
    @SerialName("destinationAccountId") val destinationAccountId: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("tags") val tags: List<TagSummaryDto> = emptyList(),
    @SerialName("attachmentCount") val attachmentCount: Long = 0
)

@Serializable
data class HistoryPageResponseDto(
    @SerialName("content") val content: List<HistoryItemDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("totalElements") val totalElements: Long,
    @SerialName("totalPages") val totalPages: Int
)
