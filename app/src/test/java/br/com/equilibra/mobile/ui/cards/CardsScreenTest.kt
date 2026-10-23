package br.com.equilibra.mobile.ui.cards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.CreditCardRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Task 7.3 — entrada da tela de faturas a partir da aba Cartões.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CreditCardRepository
    private lateinit var cardsFlow: MutableStateFlow<List<CreditCardEntity>>

    private fun card() = CreditCardEntity(
        id = "c1",
        name = "Nubank",
        institution = "Nubank",
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        cardsFlow = MutableStateFlow(listOf(card()))
        every { repository.observeCards() } returns cardsFlow
        coEvery { repository.getAllCached() } returns listOf(card())
        coEvery { repository.refresh() } returns ApiResult.Success(listOf(card()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `botao faturas aciona a navegacao`() {
        var aberturas = 0
        val viewModel = CardsViewModel(repository)

        compose.setContent {
            CardsScreen(
                onStatementsClick = { aberturas++ },
                viewModel = viewModel
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Faturas").assertIsDisplayed()
        compose.onNodeWithText("Faturas").performClick()

        assertEquals(1, aberturas)
    }
}
