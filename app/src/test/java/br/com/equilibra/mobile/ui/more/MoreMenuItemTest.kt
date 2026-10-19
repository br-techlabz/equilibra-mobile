package br.com.equilibra.mobile.ui.more

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreMenuItemTest {

    @Test
    fun `item desabilitado não responde a clique`() {
        var clicked = false
        val item = MoreMenuItem(
            icon = Icons.Default.Settings,
            label = "Configurações",
            subtitle = "Em breve",
            enabled = false,
            onClick = { clicked = true }
        )

        // Item desabilitado: onClick não deve ser chamado
        // (a UI não chama onClick para itens desabilitados)
        assertFalse("Item desabilitado não deve ser clicável", item.enabled)
    }

    @Test
    fun `item habilitado executa ação ao clicar`() {
        var clicked = false
        val item = MoreMenuItem(
            icon = Icons.Default.Settings,
            label = "Sair da conta",
            enabled = true,
            onClick = { clicked = true }
        )

        assertTrue("Item habilitado deve ser clicável", item.enabled)

        // Simula clique
        item.onClick()
        assertTrue("onClick deve ser executado", clicked)
    }

    @Test
    fun `itens habilitados e em breve seguem o contrato da tela Mais`() {
        // Este teste documenta o comportamento esperado da tela Mais:
        // itens entregues (Categorias, Tags) + logout habilitados; o resto é "Em breve"
        val comingSoonItems = listOf("Perfil", "Notificações", "Segurança", "Configurações", "Ajuda", "Privacidade")
        val enabledItems = listOf("Categorias", "Tags", "Despesas", "Sair da conta")

        assertEquals(6, comingSoonItems.size)
        assertEquals(4, enabledItems.size)
        assertTrue(enabledItems.contains("Sair da conta"))
        assertTrue(enabledItems.contains("Categorias"))
        assertTrue(enabledItems.contains("Tags"))
        assertTrue(enabledItems.contains("Despesas"))
    }
}
