package br.com.equilibra.mobile.ui.purchases

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.equilibra.mobile.data.local.purchase.InstallmentEntity
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Task 6.9 — O que o detalhe mostra.
 * Precisa ser fiel ao backend: valores e ciclos das parcelas como vieram.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PurchaseDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: PurchaseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun purchase(status: String = "ACTIVE") = PurchaseEntity(
        id = "p1",
        creditCardId = "c1",
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = "100.00",
        purchaseDate = "2026-10-15",
        purchaseAt = "2026-10-15T14:30:00Z",
        installmentCount = 3,
        cycleMonth = "2026-11-01",
        status = status,
        createdAt = "2026-10-15T14:30:00Z",
        updatedAt = "2026-10-15T14:30:00Z"
    )

    private fun installments() = listOf(
        InstallmentEntity("p1:1", "p1", 1, 3, "33.34", "2026-11-01"),
        InstallmentEntity("p1:2", "p1", 2, 3, "33.33", "2026-12-01"),
        InstallmentEntity("p1:3", "p1", 3, 3, "33.33", "2027-01-01")
    )

    private fun viewModel() = PurchaseDetailViewModel(repository, "p1")

    private fun stub(status: String = "ACTIVE") {
        coEvery { repository.refreshById("p1") } returns ApiResult.Success(purchase(status))
        coEvery { repository.getInstallmentsCached("p1") } returns installments()
        coEvery { repository.getCached("p1") } returns purchase(status)
    }

    @Test
    fun mostraValorTotalEParcelasDoBackend() {
        stub()
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Notebook").assertIsDisplayed()
        compose.onNodeWithText("100,00", substring = true).assertIsDisplayed()
        compose.onNodeWithText("3 parcelas").assertIsDisplayed()
        compose.onNodeWithText("1/3").assertIsDisplayed()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        compose.onNodeWithText("3/3").assertIsDisplayed()
    }

    @Test
    fun cadaParcelaTrazValorECicloCorretos() {
        stub()
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        // valores distintos provam que o app nao redistribui centavos:
        // a 1a parcela e 33,34 (resto do backend) e as outras 33,33
        compose.onNodeWithText("33,34", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("33,33", substring = true)[0].assertIsDisplayed()
        compose.onNodeWithText("fatura de 12/2026").assertIsDisplayed()
        compose.onNodeWithText("fatura de 01/2027").assertIsDisplayed()
    }

    @Test
    fun compraCanceladaMostraStatusESemAcaoDeCancelar() {
        stub(status = "CANCELLED")
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelada").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Cancelar compra").fetchSemanticsNodes().size)
    }

    @Test
    fun compraAtivaOfereceCancelar() {
        stub()
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelar compra").assertIsDisplayed()
    }

    @Test
    fun naoEncontradaMostraEstadoTerminal() {
        coEvery { repository.refreshById("p1") } returns ApiResult.HttpError(404, "nf", null)
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Compra não encontrada").assertIsDisplayed()
    }

    @Test
    fun semParcelasNaRespostaNaoInventaDados() {
        coEvery { repository.refreshById("p1") } returns ApiResult.Success(purchase().copy(installmentCount = 0))
        coEvery { repository.getInstallmentsCached("p1") } returns emptyList()
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("O backend não retornou parcelas para esta compra.").assertIsDisplayed()
    }

    @Test
    fun cancelamentoNoDetalheExigeConfirmacao() {
        stub()
        var cancelado = false
        coEvery { repository.cancel("p1") } answers {
            cancelado = true
            ApiResult.Success(purchase(status = "CANCELLED"))
        }
        compose.setContent { PurchaseDetailScreen(onBack = {}, viewModel = viewModel()) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelar compra").performClick()
        compose.waitForIdle()
        assertEquals(false, cancelado)

        // o dialogo usa o mesmo rotulo do botao; o ultimo no e o do dialogo
        compose.onAllNodesWithText("Cancelar compra")[1].performClick()
        compose.waitForIdle()
        assertEquals(true, cancelado)
    }
}
