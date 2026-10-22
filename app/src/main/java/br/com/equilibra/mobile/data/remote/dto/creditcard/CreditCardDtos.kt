package br.com.equilibra.mobile.data.remote.dto.creditcard

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 6.1 — DTOs de cartão de crédito
 *
 * Contrato real do backend (CreditCardController, Spring — ver CreditCardResponse.java):
 * - POST   /credit-cards                   → 201 CreditCardResponse
 * - GET    /credit-cards?includeInactive   → 200 List<CreditCardResponse>
 * - GET    /credit-cards/{id}              → 200 CreditCardResponse
 * - PUT    /credit-cards/{id}              → 200 CreditCardResponse
 * - PATCH  /credit-cards/{id}/deactivate   → 200 CreditCardResponse
 * - PATCH  /credit-cards/{id}/activate     → 200 CreditCardResponse
 *
 * Campos da resposta (backend camelCase): id, name, institution, nickname,
 * totalLimit, committedLimit, reservedLimit, availableLimit, closingDay,
 * dueDay, timezone, status, createdAt, updatedAt.
 *
 * - committed/available são calculados PELO BACKEND (compras ativas − pagamentos
 *   de faturas / totalLimit): o app nunca recalcula (instrução global: backend é
 *   autoridade para limites; evita fórmula divergente).
 * - reservedLimit é sempre 0.00 no contrato atual.
 * - BigDecimal chega como JSON number e é exposto como String
 *   ([BigDecimalStringSerializer] aceita tanto number quanto string).
 */
@Serializable
data class CreditCardDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("institution") val institution: String,
    @SerialName("nickname") val nickname: String? = null,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalLimit") val totalLimit: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("committedLimit") val committedLimit: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("reservedLimit") val reservedLimit: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("availableLimit") val availableLimit: String,
    @SerialName("closingDay") val closingDay: Int,
    @SerialName("dueDay") val dueDay: Int,
    @SerialName("timezone") val timezone: String,
    @SerialName("status") val status: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateCreditCardRequestDto(
    @SerialName("name") val name: String,
    @SerialName("institution") val institution: String,
    @SerialName("nickname") val nickname: String? = null,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalLimit") val totalLimit: String,
    @SerialName("closingDay") val closingDay: Int,
    @SerialName("dueDay") val dueDay: Int,
    @SerialName("timezone") val timezone: String? = null
)

@Serializable
data class UpdateCreditCardRequestDto(
    @SerialName("name") val name: String,
    @SerialName("institution") val institution: String,
    @SerialName("nickname") val nickname: String? = null,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("totalLimit") val totalLimit: String,
    @SerialName("closingDay") val closingDay: Int,
    @SerialName("dueDay") val dueDay: Int,
    @SerialName("timezone") val timezone: String? = null
)

/**
 * Status do cartão (backend CreditCardStatus, serializado como String no JSON).
 */
object CreditCardStatusDto {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"

    val all = listOf(ACTIVE, INACTIVE)

    /** Label pt-BR para exibição em telas (usado a partir da Task 6.3). */
    fun labelPtBr(status: String): String = when (status) {
        ACTIVE -> "Ativo"
        INACTIVE -> "Inativo"
        else -> status
    }
}
