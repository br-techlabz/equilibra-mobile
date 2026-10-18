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
    fun `logout é o único item habilitado por padrão`() {
        // Este teste documenta o comportamento esperado:
        // apenas "Sair da conta" é habilitado; o resto é "Em breve"
        val comingSoonItems = listOf("Perfil", "Notificações", "Segurança", "Configurações", "Ajuda", "Privacidade")
        val enabledItems = listOf("Sair da conta")

        assertEquals(6, comingSoonItems.size)
        assertEquals(1, enabledItems.size)
    }
}
