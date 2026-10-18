package br.com.equilibra.mobile.data.dashboard

import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.DashboardApi
import br.com.equilibra.mobile.data.remote.safeApiCall
import br.com.equilibra.mobile.data.remote.dto.dashboard.DashboardResponseDto
import br.com.equilibra.mobile.domain.model.dashboard.Dashboard
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriod
import br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary
import br.com.equilibra.mobile.domain.model.dashboard.AccountBalance
import br.com.equilibra.mobile.domain.model.dashboard.RecentTransaction
import br.com.equilibra.mobile.domain.model.dashboard.BalanceSeries
import br.com.equilibra.mobile.domain.model.dashboard.BalancePoint
import br.com.equilibra.mobile.domain.model.dashboard.CategoryExpense
import br.com.equilibra.mobile.domain.model.dashboard.CardSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório do Dashboard.
 *
 * Responsável por:
 * - Chamar a API do dashboard com o período solicitado
 * - Converter DTOs em modelos de domínio
 - Tratamento de erros padronizado via [ApiResult]
 *
 * Task 2.10 (sem cache Room): este repositório SEMPRE consulta a API.
 * Não há persistência local de agregados.
 */
@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi
) {
    suspend fun getDashboard(from: String, to: String): ApiResult<Dashboard> {
        return when (val result = safeApiCall { dashboardApi.getDashboard(from, to) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }
}

/**
 * Conversão de DTO para modelo de domínio.
 *
 * Valores monetários são mantidos como String para evitar problemas de
 * precisão com Double/Float. A formatação é feita na camada de UI.
 */
private fun DashboardResponseDto.toDomain(): Dashboard {
    return Dashboard(
        period = period.toDomain(),
        summary = summary.toDomain(),
        accounts = accounts.map { it.toDomain() },
        recentTransactions = recentTransactions.map { it.toDomain() },
        balanceEvolution = balanceEvolution.map { it.toDomain() },
        expenseByCategory = expenseByCategory.map { it.toDomain() },
        incomeByCategory = incomeByCategory.map { it.toDomain() },
        cardSummary = cardSummary.toDomain()
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.PeriodDto.toDomain(): DashboardPeriod {
    return DashboardPeriod(
        from = from,
        to = to
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.SummaryDto.toDomain(): DashboardSummary {
    return DashboardSummary(
        income = income,
        expense = expense,
        net = net,
        netWorth = netWorth
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.AccountBalanceDto.toDomain(): AccountBalance {
    return AccountBalance(
        accountId = accountId,
        name = name,
        type = type,
        initialBalance = initialBalance,
        currentBalance = currentBalance,
        active = active
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.RecentTransactionDto.toDomain(): RecentTransaction {
    return RecentTransaction(
        id = id,
        type = type,
        description = description,
        occurredAt = occurredAt,
        amount = amount,
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        status = status,
        paymentMethod = paymentMethod
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.BalanceSeriesDto.toDomain(): BalanceSeries {
    return BalanceSeries(
        accountId = accountId,
        accountName = accountName,
        points = points.map { it.toDomain() }
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.BalancePointDto.toDomain(): BalancePoint {
    return BalancePoint(
        at = at,
        balance = balance
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.CategoryExpenseDto.toDomain(): CategoryExpense {
    return CategoryExpense(
        categoryName = categoryName,
        amount = amount
    )
}

private fun br.com.equilibra.mobile.data.remote.dto.dashboard.CardSummaryDto.toDomain(): CardSummary {
    return CardSummary(
        pendingStatements = pendingStatements,
        nextDueAmount = nextDueAmount,
        committedLimit = committedLimit,
        availableLimit = availableLimit
    )
}
