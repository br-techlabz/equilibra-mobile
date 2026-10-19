package br.com.equilibra.mobile.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectorChipListTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun chipsExibemOpcoesComTagEstavelPorId() {
        compose.setContent {
            SelectorChipList(
                title = "Tags",
                options = listOf("id-1" to "URGENTE", "id-2" to "Lazer"),
                selectedIds = emptyList(),
                multiSelect = true,
                isLoading = false,
                loadingMessage = "Carregando...",
                emptyMessage = "Vazio",
                optionTagPrefix = "tag",
                onToggle = {}
            )
        }

        compose.onNodeWithText("URGENTE").assertIsDisplayed()
        compose.onNodeWithText("Lazer").assertIsDisplayed()
        compose.onNodeWithTag("tag_id-1").assertIsDisplayed()
        compose.onNodeWithTag("tag_id-2").assertIsDisplayed()
    }

    @Test
    fun cliqueRetornaIdEstavelViaSelectionLogic() {
        var selected = listOf("id-1")
        compose.setContent {
            SelectorChipList(
                title = "Tags",
                options = listOf("id-1" to "URGENTE", "id-2" to "Lazer"),
                selectedIds = selected,
                multiSelect = true,
                isLoading = false,
                loadingMessage = "Carregando...",
                emptyMessage = "Vazio",
                optionTagPrefix = "tag",
                onToggle = { id -> selected = SelectionLogic.toggle(selected, id, multiSelect = true) }
            )
        }

        // Multiselect: tocar em item selecionado remove; tocar em não selecionado adiciona
        compose.onNodeWithText("URGENTE").performClick()
        assertEquals(emptyList<String>(), selected)

        compose.onNodeWithText("Lazer").performClick()
        assertEquals(listOf("id-2"), selected)
    }

    @Test
    fun estadoVazioExibeMensagem() {
        compose.setContent {
            SelectorChipList(
                title = "Categorias",
                options = emptyList(),
                selectedIds = emptyList(),
                multiSelect = false,
                isLoading = false,
                loadingMessage = "Carregando...",
                emptyMessage = "Nenhuma categoria disponível",
                optionTagPrefix = "category",
                onToggle = {}
            )
        }

        compose.onNodeWithText("Nenhuma categoria disponível").assertIsDisplayed()
    }

    @Test
    fun estadoLoadingExibeMensagemENaoExibeOpcoes() {
        compose.setContent {
            SelectorChipList(
                title = "Categorias",
                options = emptyList(),
                selectedIds = emptyList(),
                multiSelect = false,
                isLoading = true,
                loadingMessage = "Carregando categorias...",
                emptyMessage = "Nenhuma categoria disponível",
                optionTagPrefix = "category",
                onToggle = {}
            )
        }

        compose.onNodeWithText("Carregando categorias...").assertIsDisplayed()
        compose.onNodeWithText("Nenhuma categoria disponível").assertDoesNotExist()
    }
}
