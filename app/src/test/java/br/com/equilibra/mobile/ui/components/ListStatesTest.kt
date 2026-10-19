package br.com.equilibra.mobile.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListStatesTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadingExibeMensagem() {
        compose.setContent { ListLoading("Carregando contas...") }

        compose.onNodeWithText("Carregando contas...").assertIsDisplayed()
    }

    @Test
    fun errorExibeMensagemERetryExecutaCallback() {
        var retried = false
        compose.setContent {
            ListError(message = "Erro ao carregar", onRetry = { retried = true })
        }

        compose.onNodeWithText("Erro ao carregar").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performClick()
        assertTrue(retried)
    }

    @Test
    fun errorSemRetryNaoExibeBotao() {
        compose.setContent { ListError(message = "Erro permanente", onRetry = null) }

        compose.onNodeWithText("Erro permanente").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
    }

    @Test
    fun emptyExibeTituloSubtituloEAcao() {
        var clicked = false
        compose.setContent {
            ListEmptyState(
                title = "Nenhuma tag",
                subtitle = "Crie sua primeira tag para começar",
                actionLabel = "Criar primeira tag",
                onAction = { clicked = true }
            )
        }

        compose.onNodeWithText("Nenhuma tag").assertIsDisplayed()
        compose.onNodeWithText("Crie sua primeira tag para começar").assertIsDisplayed()
        compose.onNodeWithText("Criar primeira tag").performClick()
        assertTrue(clicked)
    }

    @Test
    fun emptySemAcaoNaoExibeBotao() {
        compose.setContent {
            ListEmptyState(title = "Nenhuma conta cadastrada")
        }

        compose.onNodeWithText("Nenhuma conta cadastrada").assertIsDisplayed()
        compose.onNodeWithText("Criar conta").assertDoesNotExist()
    }
}
