package br.com.equilibra.mobile.ui.statements

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.local.statement.StatementEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import br.com.equilibra.mobile.data.repository.StatementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Task 7.3 — Tela de faturas (estado do ViewModel).
 *
 * Regras travadas aqui:
 * - Room-first: cache alimenta a tela antes da API responder
 * - fatura atual vem do /current-statement; histórico é o resto, ordenado
 *   por ciclo DESC (desempate pelo vencimento)
 * - seleção de cartão troca a origem dos dados e ressincroniza
 * - falha de sync preserva o cache; sem cache vira erro recuperável
 * - o cliente não infere status/agregados: apenas copia o que veio do backend
 * - 404 do current significa "sem fatura atual", não erro fatal
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatementsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var statementRepository: StatementRepository
    private lateinit var cardRepository: CreditCardRepository
    private lateinit var cardsFlow: MutableStateFlow<List<CreditCardEntity>>
    private lateinit var statementFlows: MutableMap<String, MutableStateFlow<List<StatementEntity>>>
    private lateinit var currentByCard: MutableMap<String, StatementEntity>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        statementRepository = mockk(relaxed = true)
        cardRepository = mockk(relaxed = true)
        cardsFlow = MutableStateFlow(emptyList())
        statementFlows = mutableMapOf()
        currentByCard = mutableMapOf()

        every { cardRepository.observeCards() } returns cardsFlow
        every { statementRepository.observeByCard(any()) } answers { flowFor(firstArg()) }
        coEvery { statementRepository.refreshByCard(any()) } answers {
            val cardId = firstArg<String>()
            ApiResult.Success(flowFor(cardId).value)
        }
        coEvery { statementRepository.refreshCurrent(any()) } answers {
            val cardId = firstArg<String>()
            val current = currentByCard[cardId]
            if (current == null) {
                ApiResult.HttpError(404, "no current", null)
            } else {
                saveIntoFlow(cardId, current)
                ApiResult.Success(current)
            }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun flowFor(cardId: String): MutableStateFlow<List<StatementEntity>> =
        statementFlows.getOrPut(cardId) { MutableStateFlow(emptyList()) }

    private fun saveIntoFlow(cardId: String, statement: StatementEntity) {
        flowFor(cardId).value = (flowFor(cardId).value.filterNot { it.id == statement.id } + statement)
            .sortedByDescending { it.cycleStart }
    }

    private fun card(
        id: String = "c1",
        name: String = "Cartão 1",
        status: String = "ACTIVE"
    ) = CreditCardEntity(
        id = id,
        name = name,
        institution = "Banco",
        nickname = null,
        totalLimit = "1000.00",
        committedLimit = "0.00",
        reservedLimit = "0.00",
        availableLimit = "1000.00",
        closingDay = 20,
        dueDay = 10,
        timezone = "America/Sao_Paulo",
        status = status,
        createdAt = "2026-10-01T00:00:00Z",
        updatedAt = "2026-10-01T00:00:00Z"
    )

    private fun statement(
        id: String,
        cardId: String = "c1",
        cycleStart: String = "2026-10-03",
        dueDate: String = "2026-11-10",
        status: String = "OPEN",
        total: String = "100.00",
        paid: String = "0.00",
        outstanding: String = "100.00"
    ) = StatementEntity(
        id = id,
        creditCardId = cardId,
        cycleStart = cycleStart,
        cycleEnd = "2026-11-02",
        closingAt = "2026-11-03T03:00:00Z",
        dueDate = dueDate,
        status = status,
        totalAmount = total,
        paidAmount = paid,
        outstandingAmount = outstanding,
        syncedAt = null
    )

    private fun viewModel() = StatementsViewModel(statementRepository, cardRepository)

    @Test
    fun `cache alimenta atual e historico antes da api responder`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual")
        val antiga = statement(id = "antiga", cycleStart = "2026-09-03", dueDate = "2026-10-10")
        flowFor("c1").value = listOf(antiga, atual)
        currentByCard["c1"] = atual

        val gate = CompletableDeferred<Unit>()
        coEvery { statementRepository.refreshByCard("c1") } coAnswers {
            gate.await()
            ApiResult.Success(flowFor("c1").value)
        }
        coEvery { statementRepository.refreshCurrent("c1") } coAnswers {
            gate.await()
            ApiResult.Success(atual)
        }

        val vm = viewModel()
        runCurrent()

        val durante = vm.uiState.value
        assertEquals("atual", durante.current?.id)
        assertEquals(listOf("antiga"), durante.history.map { it.id })
        assertFalse(durante.isLoading)
        assertTrue(durante.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `sincroniza api e ordena historico por ciclo desc`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual", cycleStart = "2026-10-03")
        val setembro = statement(id = "setembro", cycleStart = "2026-09-03", dueDate = "2026-10-10")
        val agosto = statement(id = "agosto", cycleStart = "2026-08-03", dueDate = "2026-09-10")
        // cache pode chegar fora de ordem; o ViewModel garante a ordem de exibição
        flowFor("c1").value = listOf(setembro, agosto, atual)
        currentByCard["c1"] = atual

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("atual", state.current?.id)
        assertEquals(listOf("setembro", "agosto"), state.history.map { it.id })
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify(exactly = 1) { statementRepository.refreshByCard("c1") }
        coVerify(exactly = 1) { statementRepository.refreshCurrent("c1") }
    }

    @Test
    fun `falha de rede preserva cache e mostra dados locais`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual")
        val antiga = statement(id = "antiga", cycleStart = "2026-09-03", dueDate = "2026-10-10")
        flowFor("c1").value = listOf(antiga, atual)
        coEvery { statementRepository.refreshByCard("c1") } returns
            ApiResult.NetworkError(IOException("offline"))
        coEvery { statementRepository.refreshCurrent("c1") } returns
            ApiResult.NetworkError(IOException("offline"))

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("atual", state.current?.id)
        assertEquals(listOf("antiga"), state.history.map { it.id })
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.error!!.contains("Sem conexão. Mostrando dados locais."))
    }

    @Test
    fun `falha sem cache vira erro recuperavel`() = runTest {
        cardsFlow.value = listOf(card())
        coEvery { statementRepository.refreshByCard("c1") } returns
            ApiResult.NetworkError(IOException("offline"))
        coEvery { statementRepository.refreshCurrent("c1") } returns
            ApiResult.NetworkError(IOException("offline"))

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.current)
        assertTrue(state.history.isEmpty())
        assertFalse(state.isLoading)
        assertTrue(state.error!!.contains("Sem conexão. Tente novamente."))
    }

    @Test
    fun `sem cartoes nao chama api de faturas`() = runTest {
        cardsFlow.value = emptyList()

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.cards.isEmpty())
        assertNull(vm.uiState.value.selectedCardId)
        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 0) { statementRepository.refreshByCard(any()) }
        coVerify(exactly = 0) { statementRepository.refreshCurrent(any()) }
    }

    @Test
    fun `seleciona primeiro cartao ativo por padrao`() = runTest {
        cardsFlow.value = listOf(
            card(id = "inativo", status = "INACTIVE"),
            card(id = "c2", name = "Cartão 2"),
            card(id = "c3", name = "Cartão 3")
        )
        val s2 = statement(id = "s2", cardId = "c2")
        flowFor("c2").value = listOf(s2)
        currentByCard["c2"] = s2

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("c2", vm.uiState.value.selectedCardId)
        assertEquals("s2", vm.uiState.value.current?.id)
        coVerify(exactly = 1) { statementRepository.refreshByCard("c2") }
        coVerify(exactly = 0) { statementRepository.refreshByCard("inativo") }
    }

    @Test
    fun `troca de cartao recarrega a fatura do cartao escolhido`() = runTest {
        cardsFlow.value = listOf(card(id = "c1", name = "Cartão 1"), card(id = "c2", name = "Cartão 2"))
        val s1 = statement(id = "s1", cardId = "c1", total = "100.00")
        val s2 = statement(id = "s2", cardId = "c2", total = "200.00", outstanding = "200.00")
        flowFor("c1").value = listOf(s1)
        flowFor("c2").value = listOf(s2)
        currentByCard["c1"] = s1
        currentByCard["c2"] = s2

        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("c1", vm.uiState.value.selectedCardId)
        assertEquals("100.00", vm.uiState.value.current?.totalAmount)

        vm.onCardSelected("c2")
        advanceUntilIdle()

        assertEquals("c2", vm.uiState.value.selectedCardId)
        assertEquals("200.00", vm.uiState.value.current?.totalAmount)
        assertTrue(vm.uiState.value.history.isEmpty())
        coVerify(exactly = 1) { statementRepository.refreshByCard("c2") }
    }

    @Test
    fun `refresh manual dispara nova sincronizacao`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual")
        flowFor("c1").value = listOf(atual)
        currentByCard["c1"] = atual

        val vm = viewModel()
        advanceUntilIdle()
        coVerify(exactly = 1) { statementRepository.refreshByCard("c1") }

        vm.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { statementRepository.refreshByCard("c1") }
        coVerify(exactly = 2) { statementRepository.refreshCurrent("c1") }
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `erro de conteudo pode ser dispensado sem perder dados`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual")
        flowFor("c1").value = listOf(atual)
        coEvery { statementRepository.refreshByCard("c1") } returns
            ApiResult.HttpError(500, "boom", null)
        coEvery { statementRepository.refreshCurrent("c1") } returns
            ApiResult.HttpError(500, "boom", null)

        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.error != null)
        assertEquals("atual", vm.uiState.value.current?.id)

        vm.dismissError()

        assertNull(vm.uiState.value.error)
        assertEquals("atual", vm.uiState.value.current?.id)
    }

    @Test
    fun `status e agregados sao copiados do backend`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(
            id = "atual",
            dueDate = "2020-01-01",
            status = "PARTIALLY_PAID",
            total = "100.00",
            paid = "40.00",
            outstanding = "60.00"
        )
        flowFor("c1").value = listOf(atual)
        currentByCard["c1"] = atual

        val vm = viewModel()
        advanceUntilIdle()

        val current = vm.uiState.value.current!!
        assertEquals("PARTIALLY_PAID", current.status)
        assertEquals("100.00", current.totalAmount)
        assertEquals("40.00", current.paidAmount)
        assertEquals("60.00", current.outstandingAmount)
    }

    @Test
    fun `current 404 sem fatura atual move cache para o historico`() = runTest {
        cardsFlow.value = listOf(card())
        val fechada = statement(
            id = "fechada",
            status = "CLOSED",
            cycleStart = "2026-09-03",
            dueDate = "2026-10-10"
        )
        flowFor("c1").value = listOf(fechada)
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(fechada))
        coEvery { statementRepository.refreshCurrent("c1") } returns
            ApiResult.HttpError(404, "no current", null)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.current)
        assertEquals(listOf("fechada"), state.history.map { it.id })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `fatura atual retornada pela api entra no cache e no estado`() = runTest {
        cardsFlow.value = listOf(card())
        val antiga = statement(
            id = "antiga",
            status = "CLOSED",
            cycleStart = "2026-09-03",
            dueDate = "2026-10-10"
        )
        val nova = statement(id = "nova", cycleStart = "2026-10-03")
        flowFor("c1").value = listOf(antiga)
        currentByCard["c1"] = nova

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("nova", state.current?.id)
        assertEquals(listOf("antiga"), state.history.map { it.id })
        assertEquals(listOf("nova", "antiga"), flowFor("c1").value.map { it.id })
    }

    @Test
    fun `atualizacao do cache re-emite estado sem refazer api`() = runTest {
        cardsFlow.value = listOf(card())
        val atual = statement(id = "atual")
        flowFor("c1").value = listOf(atual)
        currentByCard["c1"] = atual

        val vm = viewModel()
        advanceUntilIdle()

        val antiga = statement(id = "antiga", cycleStart = "2026-09-03", dueDate = "2026-10-10")
        flowFor("c1").value = listOf(atual, antiga)
        advanceUntilIdle()

        assertEquals(listOf("antiga"), vm.uiState.value.history.map { it.id })
        coVerify(exactly = 1) { statementRepository.refreshByCard("c1") }
    }
}
