package br.com.equilibra.mobile.data.remote.dto.tag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 3.11 — DTOs de tag (contrato real do TagController).
 *
 * - POST   /tags                     → 201 TagResponse
 * - GET    /tags?includeInactive=    → 200 List
 * - GET    /tags/{id}                → 200
 * - PUT    /tags/{id}                → 200 (name)
 * - PATCH  /tags/{id}/deactivate     → 200
 * - PATCH  /tags/{id}/activate       → 200
 *
 * name: @NotBlank @Size(max = 100) no backend.
 */
@Serializable
data class TagDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("active") val active: Boolean,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateTagRequestDto(
    @SerialName("name") val name: String
)

@Serializable
data class UpdateTagRequestDto(
    @SerialName("name") val name: String
)
