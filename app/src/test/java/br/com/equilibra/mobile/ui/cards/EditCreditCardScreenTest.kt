package br.com.equilibra.mobile.ui.cards

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Task 6.5 — Feedback de sucesso da edição/status: ao salvar ou ao
 * ativar/desativar, a tela precisa avisar a navegação (`onSuccess`) — sem
 * isso o usuário fica parado na tela depois da operação concluída.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditCreditCardScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CreditCardRepository

    private fun entity(status: String = "ACTIVE") = CreditCardEntity(
        id = "c1", name = "Nubank Roxinho", institution = "Nubank", nickname = "Roxinho",
        totalLimit = "10000.00", committedLimit = "250.00", reservedLimit = "0.00", availableLimit = "9750.00",
        closingDay = 25, dueDay = 2, timezone = "America/Sao_Paulo", status = status,
        createdAt = "2026-10-22T00:00:00Z", updatedAt = "2026-10-22T00:00:00Z"
    )

    private fun viewModel(card: CreditCardEntity) = EditCreditCardViewModel(
        repository,
        SavedStateHandle(mapOf("cardId" to card.id))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getByIdCached(any()) } answers { entity() }
        coEvery { repository.refreshById(any()) } answers { ApiResult.Success(entity()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `salvar com sucesso avisa navegacao`() {
        coEvery { repository.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.Success(entity())
        var successes = 0
        val vm = viewModel(entity())

        compose.setContent {
            EditCreditCardScreen(onSuccess = { successes++ }, onBack = {}, viewModel = vm)
        }

        compose.onNodeWithText("Salvar").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(1, successes)
    }

    @Test
    fun `ativar com sucesso avisa navegacao`() {
        coEvery { repository.getByIdCached(any()) } answers { entity(status = "INACTIVE") }
        coEvery { repository.refreshById(any()) } answers { ApiResult.Success(entity(status = "INACTIVE")) }
        coEvery { repository.activate(any()) } answers { ApiResult.Success(entity(status = "ACTIVE")) }
        var successes = 0
        val vm = viewModel(entity(status = "INACTIVE"))

        compose.setContent {
            EditCreditCardScreen(onSuccess = { successes++ }, onBack = {}, viewModel = vm)
        }

        compose.onNodeWithText("Ativar cartão").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(1, successes)
    }

    @Test
    fun `desativar confirmado avisa navegacao`() {
        coEvery { repository.deactivate(any()) } answers { ApiResult.Success(entity(status = "INACTIVE")) }
        var successes = 0
        val vm = viewModel(entity())

        compose.setContent {
            EditCreditCardScreen(onSuccess = { successes++ }, onBack = {}, viewModel = vm)
        }

        compose.onNodeWithText("Desativar cartão").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(0, successes)  // confirmação ainda pendente

        compose.onNodeWithText("Desativar").performClick()
        compose.waitForIdle()

        assertEquals(1, successes)
    }

    @Test
    fun `falha ao salvar nao avisa navegacao`() {
        coEvery { repository.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            ApiResult.HttpError(409, "conf", null)
        var successes = 0
        val vm = viewModel(entity())

        compose.setContent {
            EditCreditCardScreen(onSuccess = { successes++ }, onBack = {}, viewModel = vm)
        }

        compose.onNodeWithText("Salvar").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(0, successes)
        assertFalse(vm.uiState.value.isSuccess)
        assertTrue(vm.uiState.value.generalError != null)
    }
}
