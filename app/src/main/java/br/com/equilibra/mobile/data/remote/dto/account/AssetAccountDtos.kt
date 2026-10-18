package br.com.equilibra.mobile.data.remote.dto.account

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Task 3.3 — DTOs de conta de ativo
 *
 * Contrato real do backend (AssetAccountController):
 * - POST   /asset-accounts              → 201 AssetAccountResponse
 * - GET    /asset-accounts?includeInactive → 200 List<AssetAccountResponse>
 * - GET    /asset-accounts/{id}         → 200 AssetAccountResponse
 * - PUT    /asset-accounts/{id}         → 200 AssetAccountResponse
 * - PATCH  /asset-accounts/{id}/deactivate → 200 AssetAccountResponse
 * - PATCH  /asset-accounts/{id}/activate   → 200 AssetAccountResponse
 * - POST   /asset-accounts/{id}/reconcile  → 200 ReconciliationResultResponse
 *
 * Backend serializa BigDecimal como JSON number (BigDecimal, precision 19, scale 2)
 * e Instant como ISO-8601. No mobile, BigDecimal é exposto como String para
 * evitar perda de precisão via Double/Float (ver MoneyFormatter/BigDecimal).
 *
 * Usa [BigDecimalStringSerializer] para aceitar tanto "123.45" quanto 123.45.
 */
@Serializable
data class AssetAccountDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("initialBalance") val initialBalance: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("currentBalance") val currentBalance: String,
    @SerialName("active") val active: Boolean,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateAssetAccountRequestDto(
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("initialBalance") val initialBalance: String
)

@Serializable
data class UpdateAssetAccountRequestDto(
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("initialBalance") val initialBalance: String
)

@Serializable
data class ReconcileAccountRequestDto(
    /** LocalDate YYYY-MM-DD */
    @SerialName("reconciliationDate") val reconciliationDate: String,
    /** HH:mm — pattern ^([01]\d|2[0-3]):([0-5]\d)$ */
    @SerialName("reconciliationTime") val reconciliationTime: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("informedBalance") val informedBalance: String
)

@Serializable
data class ReconciliationResultDto(
    @SerialName("accountId") val accountId: String,
    /** LocalDate YYYY-MM-DD */
    @SerialName("reconciliationDate") val reconciliationDate: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("calculatedBalance") val calculatedBalance: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("informedBalance") val informedBalance: String,
    @Serializable(with = BigDecimalStringSerializer::class)
    @SerialName("difference") val difference: String,
    @SerialName("adjustmentType") val adjustmentType: String? = null,
    @SerialName("createdTransactionId") val createdTransactionId: String? = null
)

/**
 * Tipos de conta suportados pelo backend (AssetAccountType).
 * Serializados como String no JSON.
 */
object AssetAccountTypeDto {
    const val CHECKING = "CHECKING"
    const val SAVINGS = "SAVINGS"
    const val CASH = "CASH"
    const val INVESTMENT = "INVESTMENT"
    const val DIGITAL = "DIGITAL"
    const val OTHER = "OTHER"

    val all = listOf(CHECKING, SAVINGS, CASH, INVESTMENT, DIGITAL, OTHER)

    /**
     * Labels pt-BR para exibição (Task 3.6).
     */
    fun labelPtBr(type: String): String = when (type) {
        CHECKING -> "Conta corrente"
        SAVINGS -> "Poupança"
        CASH -> "Dinheiro"
        INVESTMENT -> "Investimento"
        DIGITAL -> "Digital"
        OTHER -> "Outro"
        else -> type
    }
}
