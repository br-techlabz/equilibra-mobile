package br.com.equilibra.mobile.ui.statements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

/**
 * Task 7.3 — Comportamento visível da tela de faturas.
 *
 * Cobre o critério de aceite: fatura atual e histórico corretos, seleção de
 * cartão, status visíveis e estados completos (loading, vazio, erro, conteúdo).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StatementsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var statementRepository: StatementRepository
    private lateinit var cardRepository: CreditCardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        statementRepository = mockk(relaxed = true)
        cardRepository = mockk(relaxed = true)

        every { cardRepository.observeCards() } returns flowOf(listOf(card("c1", "Cartão 1")))
        every { statementRepository.observeByCard(any()) } returns flowOf(emptyList())
        coEvery { statementRepository.refreshByCard(any()) } returns ApiResult.Success(emptyList())
        coEvery { statementRepository.refreshCurrent(any()) } returns
            ApiResult.HttpError(404, "no current", null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun card(id: String, name: String) = CreditCardEntity(
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
        status = "ACTIVE",
        createdAt = "2026-10-01T00:00:00Z",
        updatedAt = "2026-10-01T00:00:00Z"
    )

    private fun statement(
        id: String,
        cycleStart: String = "2026-10-03",
        dueDate: String = "2026-11-10",
        status: String = "OPEN",
        total: String = "100.00",
        paid: String = "0.00",
        outstanding: String = "100.00"
    ) = StatementEntity(
        id = id,
        creditCardId = "c1",
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
    fun `exibe fatura atual e historico com status e valores`() {
        val atual = statement(id = "atual", status = "OPEN")
        val antiga = statement(
            id = "antiga",
            cycleStart = "2026-09-03",
            dueDate = "2026-10-10",
            status = "PAID",
            total = "50.00",
            paid = "50.00",
            outstanding = "0.00"
        )
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(antiga, atual))
        coEvery { statementRepository.refreshByCard("c1") } returns
            ApiResult.Success(listOf(antiga, atual))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(atual)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fatura atual").assertIsDisplayed()
        compose.onNodeWithText("Aberta").assertIsDisplayed()
        compose.onNodeWithText("03/10/2026 a 02/11/2026", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Vence em 10/11/2026").assertIsDisplayed()
        compose.onNodeWithText("Total").assertIsDisplayed()
        compose.onAllNodesWithText("100,00", substring = true).onFirst().assertIsDisplayed()
        compose.onNodeWithText("Histórico").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Paga").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("50,00", substring = true).onFirst()
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `exibe loading enquanto o primeiro sync nao termina`() {
        every { statementRepository.observeByCard("c1") } returns flowOf(emptyList())
        val gate = CompletableDeferred<Unit>()
        coEvery { statementRepository.refreshByCard("c1") } coAnswers {
            gate.await()
            ApiResult.Success(emptyList())
        }
        coEvery { statementRepository.refreshCurrent("c1") } coAnswers {
            gate.await()
            ApiResult.HttpError(404, "no current", null)
        }

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Carregando faturas...").assertIsDisplayed()

        gate.complete(Unit)
        compose.waitForIdle()

        compose.onNodeWithText("Nenhuma fatura para este cartão").assertIsDisplayed()
    }

    @Test
    fun `sem faturas exibe estado vazio do cartao`() {
        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Nenhuma fatura para este cartão").assertIsDisplayed()
    }

    @Test
    fun `sem cartoes exibe estado vazio proprio`() {
        every { cardRepository.observeCards() } returns flowOf(emptyList())

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Nenhum cartão cadastrado").assertIsDisplayed()
    }

    @Test
    fun `erro sem cache mostra mensagem e retry`() {
        coEvery { statementRepository.refreshByCard("c1") } returns
            ApiResult.NetworkError(IOException("offline"))
        coEvery { statementRepository.refreshCurrent("c1") } returns
            ApiResult.NetworkError(IOException("offline"))

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Sem conexão. Tente novamente.").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").assertIsDisplayed()
    }

    @Test
    fun `seletor troca o cartao exibido`() {
        val s1 = statement(id = "s1", total = "100.00", outstanding = "100.00")
        val s2 = statement(id = "s2", total = "200.00", outstanding = "200.00")
        every { cardRepository.observeCards() } returns
            flowOf(listOf(card("c1", "Cartão 1"), card("c2", "Cartão 2")))
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(s1))
        every { statementRepository.observeByCard("c2") } returns flowOf(listOf(s2))
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(s1))
        coEvery { statementRepository.refreshByCard("c2") } returns ApiResult.Success(listOf(s2))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(s1)
        coEvery { statementRepository.refreshCurrent("c2") } returns ApiResult.Success(s2)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onAllNodesWithText("100,00", substring = true).onFirst().assertIsDisplayed()

        compose.onNodeWithText("Cartão 2").performClick()
        compose.waitForIdle()

        compose.onAllNodesWithText("200,00", substring = true).onFirst().assertIsDisplayed()
        compose.onNodeWithText("100,00", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("Cartão 1").onFirst().assertIsDisplayed()
    }

    @Test
    fun `botao voltar aciona navegacao`() {
        var voltou = 0

        compose.setContent {
            StatementsScreen(onBack = { voltou++ }, viewModel = viewModel())
        }
        compose.waitForIdle()

        compose.onNodeWithText("Voltar").performClick()

        assertEquals(1, voltou)
    }

    @Test
    fun `fatura aberta exibe acao de fechar`() {
        val atual = statement(id = "atual", status = "OPEN")
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(atual))
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(atual))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(atual)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura").assertIsDisplayed()
    }

    @Test
    fun `fatura fechada nao exibe acao de fechar`() {
        val atual = statement(id = "atual", status = "CLOSED")
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(atual))
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(atual))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(atual)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura").assertDoesNotExist()
    }

    @Test
    fun `fechar fatura pede confirmacao e cancelar nao chama api`() {
        val atual = statement(id = "atual", status = "OPEN")
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(atual))
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(atual))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(atual)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura").performClick()
        compose.onNodeWithText("Fechar fatura?").assertIsDisplayed()
        compose.onNodeWithText("A fatura do ciclo 10/2026", substring = true).assertIsDisplayed()

        compose.onNodeWithText("Cancelar").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura?").assertDoesNotExist()
        coVerify(exactly = 0) { statementRepository.close(any(), any()) }
    }

    @Test
    fun `confirmar fechamento reflete status fechado na lista`() {
        val atual = statement(id = "atual", status = "OPEN")
        val fechada = atual.copy(status = "CLOSED")
        val flow = MutableStateFlow(listOf(atual))
        var current: StatementEntity? = atual

        every { statementRepository.observeByCard("c1") } returns flow
        coEvery { statementRepository.refreshByCard("c1") } answers {
            ApiResult.Success(flow.value)
        }
        coEvery { statementRepository.refreshCurrent("c1") } answers {
            current?.let { ApiResult.Success(it) }
                ?: ApiResult.HttpError(404, "no current", null)
        }
        coEvery { statementRepository.close("c1", "2026-10-03") } coAnswers {
            flow.value = listOf(fechada)
            current = null
            ApiResult.Success(fechada)
        }

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura").performClick()
        compose.onNodeWithText("Confirmar fechamento").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Fechada").assertIsDisplayed()
        compose.onNodeWithText("Fechar fatura").assertDoesNotExist()
        coVerify(exactly = 1) {
            statementRepository.close("c1", "2026-10-03")
        }
    }

    @Test
    fun `erro de fatura ja fechada aparece para o usuario`() {
        val atual = statement(id = "atual", status = "OPEN")
        every { statementRepository.observeByCard("c1") } returns flowOf(listOf(atual))
        coEvery { statementRepository.refreshByCard("c1") } returns ApiResult.Success(listOf(atual))
        coEvery { statementRepository.refreshCurrent("c1") } returns ApiResult.Success(atual)
        coEvery { statementRepository.close("c1", "2026-10-03") } returns
            ApiResult.HttpError(409, "fechada", null)

        compose.setContent { StatementsScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Fechar fatura").performClick()
        compose.onNodeWithText("Confirmar fechamento").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Não foi possível fechar: a fatura já está fechada.")
            .assertIsDisplayed()
        compose.onNodeWithText("Aberta").assertIsDisplayed()
        compose.onNodeWithText("Fechar fatura").assertIsDisplayed()
    }
}
