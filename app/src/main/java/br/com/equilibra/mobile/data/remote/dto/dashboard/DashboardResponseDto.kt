package br.com.equilibra.mobile.data.remote.dto.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resposta do endpoint GET /dashboard (contrato real do backend).
 *
 * Campos opcionais são tratados com `ignoreUnknownKeys = true` no Json
 * e com valores padrão nos DTOs.
 *
 * Valores monetários usamos String para evitar problemas de precisão
 * com Double/Float. A formatação para exibição é feita na camada de UI.
 */
@Serializable
data class DashboardResponseDto(
    @SerialName("period") val period: PeriodDto,
    @SerialName("summary") val summary: SummaryDto,
    @SerialName("accounts") val accounts: List<AccountBalanceDto>,
    @SerialName("recentTransactions") val recentTransactions: List<RecentTransactionDto>,
    @SerialName("balanceEvolution") val balanceEvolution: List<BalanceSeriesDto>,
    @SerialName("expenseByCategory") val expenseByCategory: List<CategoryExpenseDto>,
    @SerialName("incomeByCategory") val incomeByCategory: List<CategoryExpenseDto>,
    @SerialName("cardSummary") val cardSummary: CardSummaryDto
)

@Serializable
data class PeriodDto(
    @SerialName("from") val from: String,
    @SerialName("to") val to: String
)

@Serializable
data class SummaryDto(
    @SerialName("income") val income: String,
    @SerialName("expense") val expense: String,
    @SerialName("net") val net: String,
    @SerialName("netWorth") val netWorth: String
)

@Serializable
data class AccountBalanceDto(
    @SerialName("accountId") val accountId: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String,
    @SerialName("initialBalance") val initialBalance: String,
    @SerialName("currentBalance") val currentBalance: String,
    @SerialName("active") val active: Boolean
)

@Serializable
data class RecentTransactionDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("description") val description: String,
    @SerialName("occurredAt") val occurredAt: String,
    @SerialName("amount") val amount: String,
    @SerialName("sourceAccountId") val sourceAccountId: String? = null,
    @SerialName("destinationAccountId") val destinationAccountId: String? = null,
    @SerialName("status") val status: String,
    @SerialName("paymentMethod") val paymentMethod: String
)

@Serializable
data class BalanceSeriesDto(
    @SerialName("accountId") val accountId: String,
    @SerialName("accountName") val accountName: String,
    @SerialName("points") val points: List<BalancePointDto>
)

@Serializable
data class BalancePointDto(
    @SerialName("at") val at: String,
    @SerialName("balance") val balance: String
)

@Serializable
data class CategoryExpenseDto(
    @SerialName("categoryName") val categoryName: String,
    @SerialName("amount") val amount: String
)

@Serializable
data class CardSummaryDto(
    @SerialName("pendingStatements") val pendingStatements: String,
    @SerialName("nextDueAmount") val nextDueAmount: String,
    @SerialName("committedLimit") val committedLimit: String,
    @SerialName("availableLimit") val availableLimit: String
)
