package br.com.equilibra.mobile.ui.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AccountRepository
import br.com.equilibra.mobile.data.repository.CategoryRepository
import br.com.equilibra.mobile.data.repository.TransactionRepository
import br.com.equilibra.mobile.domain.model.transaction.TransactionDetail
import br.com.equilibra.mobile.domain.util.MoneyFormatter
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Task 4.12 — Apresentação do detalhe. O dispatcher principal é o de teste, o
 * que deixa a carga do ViewModel determinística dentro do `setContent`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransactionDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private val detailId = "e1"
    private lateinit var repository: TransactionRepository

    /** Rótulos vazios: os testes aqui cobrem o detalhe, não os catálogos. */
    private fun labelsViewModel() = TransactionDetailLabelsViewModel(
        accountRepository = mockk(relaxed = true),
        categoryRepository = mockk(relaxed = true)
    )

    private fun detail(
        type: String = "EXPENSE",
        status: String = "ACTIVE"
    ) = TransactionDetail(
        id = detailId,
        type = type,
        status = status,
        description = "Mercado",
        occurredAt = "2026-10-01T12:00:00Z",
        amount = "42.90",
        sourceAccountId = "acc-1",
        destinationAccountId = null,
        categoryId = "cat-1",
        notes = "obs",
        createdAt = "2026-10-01T12:00:00Z",
        updatedAt = "2026-10-01T12:00:00Z",
        tagNames = listOf("mercado")
    )

    private fun viewModel(type: String = "EXPENSE"): TransactionDetailViewModel =
        TransactionDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf("transactionType" to type, "transactionId" to detailId)
            )
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.Success(detail())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Avança a fila do Dispatchers.Main de teste e sincroniza o Compose. */
    private fun settle() {
        testDispatcher.scheduler.advanceUntilIdle()
        compose.waitForIdle()
    }

    @Test
    fun telaExibeDescricaoEValor() {
        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        compose.onNodeWithText("Mercado").assertIsDisplayed()
        // pt-BR usa espaço não-quebrável (R$ 42,90): compara via próprio formatador
        val valor = MoneyFormatter.format("42.90")
        // Valor aparece no cabeçalho e no campo "Valor" — basta um exibido
        compose.onAllNodesWithText(valor).onFirst().assertIsDisplayed()
    }

    @Test
    fun erroExibeMensagemERetryRecarrega() {
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.HttpError(404, null, null)

        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        compose.onNodeWithText("Transação não encontrada (404).").assertIsDisplayed()

        // Retry interno recarrega via próprio ViewModel (padrão do app)
        coEvery { repository.getDetail(any(), any()) } returns ApiResult.Success(detail())
        compose.onNodeWithText("Tentar novamente").performClick()
        settle()

        compose.onNodeWithText("Mercado").assertIsDisplayed()
    }

    @Test
    fun botaoVoltarChamaCallback() {
        var backed = false

        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = { backed = true }
            )
        }
        settle()

        compose.onNodeWithText("Voltar").performClick()
        assertTrue(backed)
    }

    @Test
    fun acoesExibidasParaDespesaAtiva() {
        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        // Ações ficam abaixo da dobra: rola até elas antes de afirmar
        compose.onNodeWithText("Editar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cancelar transação").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun canceladaOcultaEditarECancelar() {
        coEvery { repository.getDetail(any(), any()) } returns
            ApiResult.Success(detail(status = "CANCELLED"))

        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Cancelar transação").assertDoesNotExist()
    }

    @Test
    fun cancelamentoAposConfirmacaoMarcaCancelada() {
        coEvery { repository.cancelExpense(any()) } returns ApiResult.Success(mockk(relaxed = true))

        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        compose.onNodeWithText("Cancelar transação").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar").performClick()
        settle()

        // "Cancelada" aparece no badge e no campo Status — basta um exibido
        compose.onAllNodesWithText("Cancelada").onFirst().assertIsDisplayed()
    }

    @Test
    fun secaoDeAnexosExibeEstadoProvisorio() {
        compose.setContent {
            TransactionDetailScreen(
                viewModel = viewModel(),
                labelsViewModel = labelsViewModel(),
                onEdit = {},
                onBack = {}
            )
        }
        settle()

        compose.onNodeWithText("Anexos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Anexos estarão disponíveis na próxima versão do app.")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
