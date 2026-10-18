package br.com.equilibra.mobile.ui.dashboard

import br.com.equilibra.mobile.data.dashboard.DashboardRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.model.dashboard.CardSummary
import br.com.equilibra.mobile.domain.model.dashboard.Dashboard
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriod
import br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DashboardRepository

    private fun successDashboard() = Dashboard(
        period = DashboardPeriod("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z"),
        summary = DashboardSummary("1000.00", "500.00", "500.00", "5000.00"),
        accounts = emptyList(),
        recentTransactions = emptyList(),
        balanceEvolution = emptyList(),
        expenseByCategory = emptyList(),
        incomeByCategory = emptyList(),
        cardSummary = CardSummary("0.00", "0.00", "5000.00", "5000.00")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial é Loading`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        val vm = DashboardViewModel(repository)

        // Antes do primeiro avanço, o estado deve ser Loading
        assertTrue(vm.uiState.value is DashboardUiState.Loading)
        advanceUntilIdle()
        Dispatchers.setMain(testDispatcher)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso exibe Success com valores da API`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals("1000.00", success.dashboard.summary.income)
        assertEquals("500.00", success.dashboard.summary.expense)
        assertEquals("500.00", success.dashboard.summary.net)
        assertEquals("5000.00", success.dashboard.summary.netWorth)
        Dispatchers.resetMain()
    }

    @Test
    fun `erro HTTP exibe Error com retry`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.HttpError(500, "Internal", null)

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertTrue((state as DashboardUiState.Error).canRetry)
        Dispatchers.resetMain()
    }

    @Test
    fun `falha de rede exibe erro de conexão com retry`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.NetworkError(java.io.IOException())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Error)
        val error = state as DashboardUiState.Error
        assertTrue(error.message.contains("conexão", ignoreCase = true))
        assertTrue(error.canRetry)
        Dispatchers.resetMain()
    }

    @Test
    fun `retry recarrega dados com sucesso`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.NetworkError(java.io.IOException())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is DashboardUiState.Error)

        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is DashboardUiState.Success)
        Dispatchers.resetMain()
    }

    @Test
    fun `load com período customizado recarrega dados`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is DashboardUiState.Success)

        val customDashboard = successDashboard().copy(
            period = DashboardPeriod("2026-09-01T00:00:00Z", "2026-09-30T23:59:59Z")
        )
        coEvery { repository.getDashboard("2026-09-01T00:00:00Z", "2026-09-30T23:59:59Z") } returns ApiResult.Success(customDashboard)

        vm.load("2026-09-01T00:00:00Z", "2026-09-30T23:59:59Z")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Success)
        assertEquals("2026-09-01T00:00:00Z", (state as DashboardUiState.Success).dashboard.period.from)
        Dispatchers.resetMain()
    }

    @Test
    fun `período exibido é formatado corretamente`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.periodLabel.contains("2026"))
        Dispatchers.resetMain()
    }
}
