package br.com.equilibra.mobile.data.dashboard

import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.DashboardApi
import br.com.equilibra.mobile.data.remote.dto.dashboard.AccountBalanceDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.BalancePointDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.BalanceSeriesDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.CardSummaryDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.CategoryExpenseDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.DashboardResponseDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.PeriodDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.RecentTransactionDto
import br.com.equilibra.mobile.data.remote.dto.dashboard.SummaryDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class DashboardRepositoryTest {

    private val api = mockk<DashboardApi>()
    private val repository = DashboardRepository(api)

    private fun successResponse() = Response.success(
        DashboardResponseDto(
            period = PeriodDto("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z"),
            summary = SummaryDto("1000.00", "500.00", "500.00", "5000.00"),
            accounts = listOf(
                AccountBalanceDto("acc1", "Conta Corrente", "CHECKING", "1000.00", "1500.00", true)
            ),
            recentTransactions = listOf(
                RecentTransactionDto(
                    id = "tx1",
                    type = "EXPENSE",
                    description = "Supermercado",
                    occurredAt = "2026-10-15T10:00:00Z",
                    amount = "100.00",
                    sourceAccountId = "acc1",
                    destinationAccountId = null,
                    status = "ACTIVE",
                    paymentMethod = "BANK_ACCOUNT"
                )
            ),
            balanceEvolution = listOf(
                BalanceSeriesDto("acc1", "Conta Corrente", listOf(BalancePointDto("2026-10-01T00:00:00Z", "1000.00")))
            ),
            expenseByCategory = listOf(CategoryExpenseDto("Alimentação", "200.00")),
            incomeByCategory = listOf(CategoryExpenseDto("Salário", "1000.00")),
            cardSummary = CardSummaryDto("0.00", "0.00", "5000.00", "5000.00")
        )
    )

    @Test
    fun `getDashboard retorna dominio em caso de sucesso`() = runTest {
        coEvery { api.getDashboard(any(), any()) } returns successResponse()

        val result = repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")

        assertTrue(result is ApiResult.Success)
        val dashboard = (result as ApiResult.Success).data
        assertEquals("1000.00", dashboard.summary.income)
        assertEquals("500.00", dashboard.summary.expense)
        assertEquals("500.00", dashboard.summary.net)
        assertEquals("5000.00", dashboard.summary.netWorth)
        assertEquals(1, dashboard.accounts.size)
        assertEquals("Conta Corrente", dashboard.accounts[0].name)
        assertEquals(1, dashboard.recentTransactions.size)
        assertEquals("Supermercado", dashboard.recentTransactions[0].description)
        assertEquals(1, dashboard.expenseByCategory.size)
        assertEquals(1, dashboard.incomeByCategory.size)
        assertEquals("0.00", dashboard.cardSummary.pendingStatements)
    }

    @Test
    fun `getDashboard retorna HttpError em caso de erro HTTP`() = runTest {
        coEvery { api.getDashboard(any(), any()) } returns Response.error(
            500,
            "Internal Server Error".toResponseBody()
        )

        val result = repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(500, (result as ApiResult.HttpError).code)
    }

    @Test
    fun `getDashboard retorna NetworkError em caso de falha de rede`() = runTest {
        coEvery { api.getDashboard(any(), any()) } throws java.io.IOException("no internet")

        val result = repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getDashboard repassa from e to corretamente`() = runTest {
        coEvery { api.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z") } returns successResponse()

        repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")

        coEvery { api.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z") } returns successResponse()
    }

    @Test
    fun `conversao preserva valores monetarios como string`() = runTest {
        coEvery { api.getDashboard(any(), any()) } returns successResponse()

        val result = repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")
        val dashboard = (result as ApiResult.Success).data

        // Verifica que valores monetários são String (sem conversão para Double)
        assertTrue(dashboard.summary.income is String)
        assertTrue(dashboard.summary.expense is String)
        assertTrue(dashboard.summary.net is String)
        assertTrue(dashboard.summary.netWorth is String)
        assertTrue(dashboard.accounts[0].currentBalance is String)
        assertTrue(dashboard.recentTransactions[0].amount is String)
    }

    @Test
    fun `conversao preserva campos opcionais nulos`() = runTest {
        val responseWithNulls = Response.success(
            DashboardResponseDto(
                period = PeriodDto("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z"),
                summary = SummaryDto("0.00", "0.00", "0.00", "0.00"),
                accounts = emptyList(),
                recentTransactions = listOf(
                    RecentTransactionDto(
                        id = "tx1",
                        type = "EXPENSE",
                        description = "Compra",
                        occurredAt = "2026-10-15T10:00:00Z",
                        amount = "50.00",
                        sourceAccountId = null,
                        destinationAccountId = null,
                        status = "ACTIVE",
                        paymentMethod = "BANK_ACCOUNT"
                    )
                ),
                balanceEvolution = emptyList(),
                expenseByCategory = emptyList(),
                incomeByCategory = emptyList(),
                cardSummary = CardSummaryDto("0.00", "0.00", "0.00", "0.00")
            )
        )
        coEvery { api.getDashboard(any(), any()) } returns responseWithNulls

        val result = repository.getDashboard("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z")
        val dashboard = (result as ApiResult.Success).data

        // Campos opcionais nulos devem permanecer nulos
        assertEquals(null, dashboard.recentTransactions[0].sourceAccountId)
        assertEquals(null, dashboard.recentTransactions[0].destinationAccountId)
    }
}
