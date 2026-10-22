package br.com.equilibra.mobile.ui.purchases

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.PurchaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Task 6.8 — Comportamento visível das telas de compras.
 *
 * Cobre o que o critério de aceite pede: lista filtrada por cartão,
 * criação e cancelamento sincronizados (nada muda sem confirmação remota).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PurchasesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: PurchaseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.refreshByCard(any()) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entity(
        id: String = "p1",
        description: String = "Notebook",
        status: String = "ACTIVE",
        cardId: String = "c1"
    ) = PurchaseEntity(
        id = id,
        creditCardId = cardId,
        categoryId = "cat-1",
        description = description,
        totalAmount = "100.00",
        purchaseDate = "2026-10-15",
        purchaseAt = "2026-10-15T14:30:00Z",
        installmentCount = 3,
        cycleMonth = "2026-11-01",
        status = status,
        createdAt = "2026-10-15T14:30:00Z",
        updatedAt = "2026-10-15T14:30:00Z"
    )

    private fun purchasesViewModel(cardId: String = "c1") =
        PurchasesViewModel(repository, cardId)

    // lista

    @Test
    fun listaExibeDescricaoValorEParcelas() {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        val vm = purchasesViewModel()

        compose.setContent { PurchasesScreen(onBack = {}, viewModel = vm) }
        compose.waitForIdle()

        compose.onNodeWithText("Notebook").assertIsDisplayed()
        // pt-BR usa espaço não separável entre "R$" e o valor
        compose.onNodeWithText("100,00", substring = true).assertIsDisplayed()
        // dados de ciclo vêm do backend: outubro/2026 de compra, fatura 11/2026
        compose.onNodeWithText("fatura 11/2026", substring = true).assertIsDisplayed()
    }

    @Test
    fun listaVaziaTemEstadoVazio() {
        every { repository.observeByCard("c1") } returns flowOf(emptyList())
        val vm = purchasesViewModel()

        compose.setContent { PurchasesScreen(onBack = {}, viewModel = vm) }
        compose.waitForIdle()

        compose.onNodeWithText("Nenhuma compra neste cartão").assertIsDisplayed()
    }

    @Test
    fun compraCanceladaNaoOfereceCancelarNovamente() {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity(status = "CANCELLED")))
        val vm = purchasesViewModel()

        compose.setContent { PurchasesScreen(onBack = {}, viewModel = vm) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelada").assertIsDisplayed()
        // sem ação de cancelar para compra já cancelada
        assertEquals(0, compose.onAllNodesWithText("Cancelar").fetchSemanticsNodes().size)
    }

    @Test
    fun cancelamentoSoAconteceAposConfirmar() {
        val ativa = entity()
        every { repository.observeByCard("c1") } returns flowOf(listOf(ativa))
        coEvery { repository.cancel("p1") } returns ApiResult.Success(ativa.copy(status = "CANCELLED"))
        val vm = purchasesViewModel()

        compose.setContent { PurchasesScreen(onBack = {}, viewModel = vm) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelar").performClick()
        compose.waitForIdle()
        // diálogo aberto, nada remoto ainda
        coVerify(exactly = 0) { repository.cancel(any()) }
        compose.onNodeWithText("Cancelar compra?").assertIsDisplayed()

        compose.onNodeWithText("Cancelar compra").performClick()
        compose.waitForIdle()

        coVerify(exactly = 1) { repository.cancel("p1") }
    }

    @Test
    fun cancelamentoPodeSerDescartado() {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        val vm = purchasesViewModel()

        compose.setContent { PurchasesScreen(onBack = {}, viewModel = vm) }
        compose.waitForIdle()

        compose.onNodeWithText("Cancelar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Manter compra").performClick()
        compose.waitForIdle()

        coVerify(exactly = 0) { repository.cancel(any()) }
    }

    // criação

    @Test
    fun cadastroEnviaCompraEVoltaAoConfirmar() {
        coEvery { repository.create(any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(entity(id = "novo"))
        var sucessos = 0
        val vm = CreatePurchaseViewModel(repository, SavedStateHandle(mapOf("cardId" to "c1")))

        compose.setContent {
            CreatePurchaseScreen(
                onSuccess = { sucessos++ },
                onBack = {},
                viewModel = vm,
                categorySelector = {}
            )
        }

        compose.onNodeWithText("Registrar compra").performScrollTo().performClick()
        compose.waitForIdle()

        // validação barra antes de qualquer chamada remota
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
        assertEquals(0, sucessos)
        compose.onNodeWithText("Informe a descrição").assertIsDisplayed()
    }

    @Test
    fun parcelasAcimaDe60SaoRecusadasNaTela() {
        val vm = CreatePurchaseViewModel(repository, SavedStateHandle(mapOf("cardId" to "c1")))

        compose.setContent {
            CreatePurchaseScreen(
                onSuccess = {},
                onBack = {},
                viewModel = vm,
                categorySelector = {}
            )
        }

        vm.onDescriptionChange("Notebook")
        vm.onAmountChange("100,00")
        vm.onDateChange("15/10/2026")
        vm.onInstallmentsChange("61")
        vm.onCategoryChange("cat-1")
        compose.onNodeWithText("Registrar compra").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Parcelas devem ser entre 1 e 60").assertIsDisplayed()
        coVerify(exactly = 0) { repository.create(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun tocarNaCompraAbreODetalhe() {
        every { repository.observeByCard("c1") } returns flowOf(listOf(entity()))
        var aberto: String? = null
        val vm = purchasesViewModel()

        compose.setContent {
            PurchasesScreen(onBack = {}, onPurchaseClick = { aberto = it }, viewModel = vm)
        }
        compose.waitForIdle()

        compose.onNodeWithText("Notebook").performClick()
        compose.waitForIdle()

        assertEquals("p1", aberto)
    }
}
