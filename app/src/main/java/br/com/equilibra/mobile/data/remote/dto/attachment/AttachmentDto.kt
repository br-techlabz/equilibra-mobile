package br.com.equilibra.mobile.data.remote.dto.attachment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 5.1 — DTOs de anexos (contrato real do AttachmentController do backend).
 *
 * - POST   /transactions/{transactionId}/attachments  (multipart, part "file") → 201
 * - GET    /transactions/{transactionId}/attachments  → 200 lista (ordem createdAt asc)
 * - GET    /attachments/{id}/content                  → 200 binário (streaming)
 * - DELETE /attachments/{id}                          → 204
 *
 * A resposta expõe só metadata pública: storageKey/checksum/ownerId ficam no
 * servidor. Monotônico: nenhum binário aqui (Task 5.5).
 */
@Serializable
data class AttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("originalFileName") val originalFileName: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("sizeBytes") val sizeBytes: Long,
    @SerialName("createdAt") val createdAt: String
)
