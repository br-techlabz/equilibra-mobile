package br.com.equilibra.mobile.data.remote.dto.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 3.9 — DTOs de categoria (contrato real do CategoryController).
 *
 * - POST   /categories                    → 201 CategoryResponse
 * - GET    /categories?includeInactive=&applicability= → 200 List
 * - GET    /categories/{id}               → 200
 * - PUT    /categories/{id}               → 200 (name, applicability)
 * - PATCH  /categories/{id}/deactivate    → 200
 * - PATCH  /categories/{id}/activate      → 200
 *
 * applicability: EXPENSE | INCOME | BOTH
 */
@Serializable
data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("applicability") val applicability: String,
    @SerialName("active") val active: Boolean,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateCategoryRequestDto(
    @SerialName("name") val name: String,
    @SerialName("applicability") val applicability: String
)

@Serializable
data class UpdateCategoryRequestDto(
    @SerialName("name") val name: String,
    @SerialName("applicability") val applicability: String
)

/**
 * Aplicabilidade da categoria + labels pt-BR.
 * Query do backend: EXPENSE inclui EXPENSE+BOTH; INCOME inclui INCOME+BOTH.
 */
object CategoryApplicabilityDto {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
    const val BOTH = "BOTH"

    val all = listOf(EXPENSE, INCOME, BOTH)

    fun labelPtBr(value: String): String = when (value) {
        EXPENSE -> "Despesa"
        INCOME -> "Receita"
        BOTH -> "Despesa e Receita"
        else -> value
    }
}
