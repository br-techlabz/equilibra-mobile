package br.com.equilibra.mobile.domain.model.dashboard

/**
 * Modelo de domínio do Dashboard.
 *
 * Representa o resumo financeiro completo retornado pelo backend.
 * Valores monetários são String para evitar problemas de precisão.
 */
data class Dashboard(
    val period: DashboardPeriod,
    val summary: DashboardSummary,
    val accounts: List<AccountBalance>,
    val recentTransactions: List<RecentTransaction>,
    val balanceEvolution: List<BalanceSeries>,
    val expenseByCategory: List<CategoryExpense>,
    val incomeByCategory: List<CategoryExpense>,
    val cardSummary: CardSummary
)

data class DashboardPeriod(
    val from: String,
    val to: String
)

data class DashboardSummary(
    val income: String,
    val expense: String,
    val net: String,
    val netWorth: String
)

data class AccountBalance(
    val accountId: String,
    val name: String,
    val type: String,
    val initialBalance: String,
    val currentBalance: String,
    val active: Boolean
)

data class RecentTransaction(
    val id: String,
    val type: String,
    val description: String,
    val occurredAt: String,
    val amount: String,
    val sourceAccountId: String?,
    val destinationAccountId: String?,
    val status: String,
    val paymentMethod: String
)

data class BalanceSeries(
    val accountId: String,
    val accountName: String,
    val points: List<BalancePoint>
)

data class BalancePoint(
    val at: String,
    val balance: String
)

data class CategoryExpense(
    val categoryName: String,
    val amount: String
)

data class CardSummary(
    val pendingStatements: String,
    val nextDueAmount: String,
    val committedLimit: String,
    val availableLimit: String
)
