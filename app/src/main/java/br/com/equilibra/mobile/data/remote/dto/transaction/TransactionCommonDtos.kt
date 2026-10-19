package br.com.equilibra.mobile.data.remote.dto.transaction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 4.2 — DTOs compartilhados de transação.
 *
 * TagSummaryDto espelha o record homônimo do backend
 * (id, name, active) e é comum a despesa, receita e transferência.
 */
@Serializable
data class TagSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("active") val active: Boolean
)
