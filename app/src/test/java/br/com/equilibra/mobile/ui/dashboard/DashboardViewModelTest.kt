package br.com.equilibra.mobile.ui.dashboard

import br.com.equilibra.mobile.data.dashboard.DashboardRepository
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.domain.model.dashboard.AccountBalance
import br.com.equilibra.mobile.domain.model.dashboard.BalancePoint
import br.com.equilibra.mobile.domain.model.dashboard.BalanceSeries
import br.com.equilibra.mobile.domain.model.dashboard.CardSummary
import br.com.equilibra.mobile.domain.model.dashboard.CategoryExpense
import br.com.equilibra.mobile.domain.model.dashboard.Dashboard
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriod
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodSelection
import br.com.equilibra.mobile.domain.model.dashboard.DashboardPeriodType
import br.com.equilibra.mobile.domain.model.dashboard.DashboardSummary
import br.com.equilibra.mobile.domain.model.dashboard.RecentTransaction
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DashboardRepository

    private fun successDashboard(
        accounts: List<AccountBalance> = emptyList(),
        recentTransactions: List<RecentTransaction> = emptyList(),
        balanceEvolution: List<BalanceSeries> = emptyList(),
        expenseByCategory: List<CategoryExpense> = emptyList(),
        incomeByCategory: List<CategoryExpense> = emptyList(),
        cardSummary: CardSummary = CardSummary("0.00", "0.00", "5000.00", "5000.00")
    ) = Dashboard(
        period = DashboardPeriod("2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z"),
        summary = DashboardSummary("1000.00", "500.00", "500.00", "5000.00"),
        accounts = accounts,
        recentTransactions = recentTransactions,
        balanceEvolution = balanceEvolution,
        expenseByCategory = expenseByCategory,
        incomeByCategory = incomeByCategory,
        cardSummary = cardSummary
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

        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 30)
        val period = DashboardPeriodSelection.custom(from, to)
        val (fromIso, toIso) = period.toIso() // 2026-09-01T00:00:00Z, 2026-10-01T00:00:00Z

        val customDashboard = successDashboard().copy(
            period = DashboardPeriod(fromIso, toIso)
        )
        coEvery { repository.getDashboard(fromIso, toIso) } returns ApiResult.Success(customDashboard)

        vm.selectCustomPeriod(from, to)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Success)
        assertEquals(fromIso, (state as DashboardUiState.Success).dashboard.period.from)
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

    // Task 2.4 — Saldos por conta

    @Test
    fun `sucesso com contas exibe lista correta`() = runTest {
        val accounts = listOf(
            AccountBalance("id1", "Conta Corrente", "CHECKING", "1000.00", "1500.00", true),
            AccountBalance("id2", "Poupança", "SAVINGS", "500.00", "600.00", true)
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(accounts))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(2, state.dashboard.accounts.size)
        assertEquals("Conta Corrente", state.dashboard.accounts[0].name)
        assertEquals("1500.00", state.dashboard.accounts[0].currentBalance)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso com lista vazia não quebra`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(emptyList()))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.dashboard.accounts.isEmpty())
        Dispatchers.resetMain()
    }

    @Test
    fun `ordem das contas respeita API (não reordena no ViewModel)`() = runTest {
        // O backend retorna ordenado por nome (ORDER BY name ASC)
        // O ViewModel não deve reordenar; a UI faz ordenação estável
        val accounts = listOf(
            AccountBalance("id2", "Conta B", "CHECKING", "0.00", "100.00", true),
            AccountBalance("id1", "Conta A", "CHECKING", "0.00", "200.00", true)
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(accounts))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        // ViewModel preserva ordem da API
        assertEquals("Conta B", state.dashboard.accounts[0].name)
        assertEquals("Conta A", state.dashboard.accounts[1].name)
        Dispatchers.resetMain()
    }

    @Test
    fun `não dispara chamadas duplicadas desnecessárias`() = runTest {
        var calls = 0
        coEvery { repository.getDashboard(any(), any()) } coAnswers {
            calls++
            ApiResult.Success(successDashboard())
        }

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        // Apenas 1 chamada no init
        assertEquals(1, calls)

        // Retry causa 1 chamada adicional
        vm.retry()
        advanceUntilIdle()
        assertEquals(2, calls)
        Dispatchers.resetMain()
    }

    // Task 2.5 — Transações recentes

    @Test
    fun `sucesso com transações exibe lista correta`() = runTest {
        val transactions = listOf(
            RecentTransaction("tx1", "EXPENSE", "Supermercado", "2026-10-15T10:00:00Z", "100.00", "acc1", null, "ACTIVE", "BANK_ACCOUNT"),
            RecentTransaction("tx2", "INCOME", "Salário", "2026-10-14T09:00:00Z", "1000.00", null, "acc1", "ACTIVE", "BANK_ACCOUNT")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(recentTransactions = transactions))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(2, state.dashboard.recentTransactions.size)
        assertEquals("Supermercado", state.dashboard.recentTransactions[0].description)
        assertEquals("100.00", state.dashboard.recentTransactions[0].amount)
        assertEquals("Salário", state.dashboard.recentTransactions[1].description)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso com transações vazias não quebra`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(recentTransactions = emptyList()))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.dashboard.recentTransactions.isEmpty())
        Dispatchers.resetMain()
    }

    @Test
    fun `ordem das transações respeita backend`() = runTest {
        // Backend retorna ordenada por occurredAt DESC
        val transactions = listOf(
            RecentTransaction("tx1", "EXPENSE", "Mais recente", "2026-10-15T10:00:00Z", "50.00", "acc1", null, "ACTIVE", "BANK_ACCOUNT"),
            RecentTransaction("tx2", "EXPENSE", "Mais antiga", "2026-10-14T10:00:00Z", "30.00", "acc1", null, "ACTIVE", "BANK_ACCOUNT")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(recentTransactions = transactions))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals("Mais recente", state.dashboard.recentTransactions[0].description)
        assertEquals("Mais antiga", state.dashboard.recentTransactions[1].description)
        Dispatchers.resetMain()
    }

    @Test
    fun `transação cancelada tem status CANCELLED`() = runTest {
        val transactions = listOf(
            RecentTransaction("tx1", "EXPENSE", "Cancelada", "2026-10-15T10:00:00Z", "50.00", "acc1", null, "CANCELLED", "BANK_ACCOUNT")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(recentTransactions = transactions))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals("CANCELLED", state.dashboard.recentTransactions[0].status)
        Dispatchers.resetMain()
    }

    // Task 2.6 — Evolução de saldo

    @Test
    fun `sucesso com evolução exibe series corretas`() = runTest {
        val series = listOf(
            BalanceSeries("acc1", "Conta Corrente", listOf(
                BalancePoint("2026-10-01T00:00:00Z", "1000.00"),
                BalancePoint("2026-10-02T00:00:00Z", "1500.00")
            ))
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(balanceEvolution = series))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(1, state.dashboard.balanceEvolution.size)
        assertEquals("acc1", state.dashboard.balanceEvolution[0].accountId)
        assertEquals(2, state.dashboard.balanceEvolution[0].points.size)
        assertEquals("1000.00", state.dashboard.balanceEvolution[0].points[0].balance)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso com evolução vazia não quebra`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(balanceEvolution = emptyList()))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.dashboard.balanceEvolution.isEmpty())
        Dispatchers.resetMain()
    }

    @Test
    fun `evolução com pontos vazios não quebra`() = runTest {
        val series = listOf(
            BalanceSeries("acc1", "Conta Corrente", emptyList())
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(balanceEvolution = series))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.dashboard.balanceEvolution[0].points.isEmpty())
        Dispatchers.resetMain()
    }

    // Task 2.7 — Gráficos por categoria

    @Test
    fun `sucesso com categorias de despesa exibe lista correta`() = runTest {
        val expenses = listOf(
            CategoryExpense("Alimentação", "200.00"),
            CategoryExpense("Transporte", "100.00")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(expenseByCategory = expenses))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(2, state.dashboard.expenseByCategory.size)
        assertEquals("Alimentação", state.dashboard.expenseByCategory[0].categoryName)
        assertEquals("200.00", state.dashboard.expenseByCategory[0].amount)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso com categorias de receita exibe lista correta`() = runTest {
        val incomes = listOf(
            CategoryExpense("Salário", "1000.00"),
            CategoryExpense("Investimentos", "200.00")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(incomeByCategory = incomes))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(2, state.dashboard.incomeByCategory.size)
        assertEquals("Salário", state.dashboard.incomeByCategory[0].categoryName)
        Dispatchers.resetMain()
    }

    @Test
    fun `sucesso com categorias vazias não quebra`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(expenseByCategory = emptyList(), incomeByCategory = emptyList()))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertTrue(state.dashboard.expenseByCategory.isEmpty())
        assertTrue(state.dashboard.incomeByCategory.isEmpty())
        Dispatchers.resetMain()
    }

    @Test
    fun `totais de categorias batem com valores da API`() = runTest {
        // Verifica que a soma dos valores corresponde (via BigDecimal)
        val expenses = listOf(
            CategoryExpense("A", "100.00"),
            CategoryExpense("B", "200.00"),
            CategoryExpense("C", "300.00")
        )
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(expenseByCategory = expenses))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        val total = state.dashboard.expenseByCategory.fold(java.math.BigDecimal.ZERO) { acc, cat ->
            acc + java.math.BigDecimal(cat.amount)
        }
        assertEquals(java.math.BigDecimal("600.00"), total)
        Dispatchers.resetMain()
    }

    // Task 2.8 — Resumo de cartão

    @Test
    fun `sucesso com resumo de cartão exibe valores corretos`() = runTest {
        val cardSummary = CardSummary("150.00", "100.00", "2000.00", "3000.00")
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(cardSummary = cardSummary))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals("150.00", state.dashboard.cardSummary.pendingStatements)
        assertEquals("100.00", state.dashboard.cardSummary.nextDueAmount)
        assertEquals("2000.00", state.dashboard.cardSummary.committedLimit)
        assertEquals("3000.00", state.dashboard.cardSummary.availableLimit)
        Dispatchers.resetMain()
    }

    @Test
    fun `resumo de cartão com todos zero indica sem cartões`() = runTest {
        val cardSummary = CardSummary("0.00", "0.00", "0.00", "0.00")
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(cardSummary = cardSummary))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals("0.00", state.dashboard.cardSummary.pendingStatements)
        assertEquals("0.00", state.dashboard.cardSummary.availableLimit)
        Dispatchers.resetMain()
    }

    @Test
    fun `resumo de cartão com valor não zero indica cartões existentes`() = runTest {
        val cardSummary = CardSummary("0.00", "0.00", "500.00", "4500.00")
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard(cardSummary = cardSummary))

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals("500.00", state.dashboard.cardSummary.committedLimit)
        assertEquals("4500.00", state.dashboard.cardSummary.availableLimit)
        Dispatchers.resetMain()
    }

    // Task 2.9 — Filtro de período

    @Test
    fun `mudar para mês anterior recarrega dados`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is DashboardUiState.Success)

        val prevMonth = DashboardPeriodSelection.previousMonth()
        val (from, to) = prevMonth.toIso()
        coEvery { repository.getDashboard(from, to) } returns ApiResult.Success(successDashboard())

        vm.selectPeriod(DashboardPeriodType.PREVIOUS_MONTH)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(DashboardPeriodType.PREVIOUS_MONTH, state.selectedPeriod.type)
        Dispatchers.resetMain()
    }

    @Test
    fun `mudar para mês atual recarrega dados`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())
        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        // Muda para mês anterior primeiro
        val prevMonth = DashboardPeriodSelection.previousMonth()
        val (from1, to1) = prevMonth.toIso()
        coEvery { repository.getDashboard(from1, to1) } returns ApiResult.Success(successDashboard())
        vm.selectPeriod(DashboardPeriodType.PREVIOUS_MONTH)
        advanceUntilIdle()

        // Volta para mês atual
        val currMonth = DashboardPeriodSelection.currentMonth()
        val (from2, to2) = currMonth.toIso()
        coEvery { repository.getDashboard(from2, to2) } returns ApiResult.Success(successDashboard())
        vm.selectPeriod(DashboardPeriodType.CURRENT_MONTH)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(DashboardPeriodType.CURRENT_MONTH, state.selectedPeriod.type)
        Dispatchers.resetMain()
    }

    @Test
    fun `período customizado válido recarrega dados`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())
        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val customFrom = LocalDate.of(2026, 1, 1)
        val customTo = LocalDate.of(2026, 3, 31)
        val period = DashboardPeriodSelection.custom(customFrom, customTo)
        val (from, to) = period.toIso()
        coEvery { repository.getDashboard(from, to) } returns ApiResult.Success(successDashboard())

        vm.selectCustomPeriod(customFrom, customTo)
        advanceUntilIdle()

        val state = vm.uiState.value as DashboardUiState.Success
        assertEquals(DashboardPeriodType.CUSTOM, state.selectedPeriod.type)
        Dispatchers.resetMain()
    }

    @Test
    fun `período customizado inválido não chama API e exibe erro`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())
        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val invalidFrom = LocalDate.of(2026, 10, 31)
        val invalidTo = LocalDate.of(2026, 10, 1)
        vm.selectCustomPeriod(invalidFrom, invalidTo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertTrue((state as DashboardUiState.Error).message.contains("inválido", ignoreCase = true))
        Dispatchers.resetMain()
    }

    @Test
    fun `período igual (from == to) é inválido`() = runTest {
        coEvery { repository.getDashboard(any(), any()) } returns ApiResult.Success(successDashboard())
        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        val sameDay = LocalDate.of(2026, 10, 15)
        vm.selectCustomPeriod(sameDay, sameDay)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is DashboardUiState.Error)
        Dispatchers.resetMain()
    }

    // Task 2.10 — Sem cache Room (dashboard sempre consulta API)

    @Test
    fun `cada load consulta API (sem cache)`() = runTest {
        var calls = 0
        coEvery { repository.getDashboard(any(), any()) } coAnswers {
            calls++
            ApiResult.Success(successDashboard())
        }

        val vm = DashboardViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, calls)

        vm.retry()
        advanceUntilIdle()
        assertEquals(2, calls)

        vm.load()
        advanceUntilIdle()
        assertEquals(3, calls)
        Dispatchers.resetMain()
    }
}
