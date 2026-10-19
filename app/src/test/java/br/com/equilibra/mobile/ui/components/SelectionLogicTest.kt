package br.com.equilibra.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionLogicTest {

    // Seleção única

    @Test
    fun `unica seleciona item vazio`() {
        assertEquals(listOf("a"), SelectionLogic.toggle(emptyList(), "a", multiSelect = false))
    }

    @Test
    fun `unica substitui selecao anterior`() {
        assertEquals(listOf("b"), SelectionLogic.toggle(listOf("a"), "b", multiSelect = false))
    }

    @Test
    fun `unica desseleciona ao tocar novamente`() {
        assertEquals(emptyList<String>(), SelectionLogic.toggle(listOf("a"), "a", multiSelect = false))
    }

    // Seleção múltipla

    @Test
    fun `multi adiciona item`() {
        assertEquals(listOf("a", "b"), SelectionLogic.toggle(listOf("a"), "b", multiSelect = true))
    }

    @Test
    fun `multi remove item existente`() {
        assertEquals(listOf("a"), SelectionLogic.toggle(listOf("a", "b"), "b", multiSelect = true))
    }

    @Test
    fun `multi nao altera ordem de itens restantes`() {
        assertEquals(listOf("a", "c"), SelectionLogic.toggle(listOf("a", "b", "c"), "b", multiSelect = true))
    }

    @Test
    fun `unica e multi respeitam multiSelect como chave`() {
        // Mesma seleção prévia, comportamentos distintos conforme o modo
        assertEquals(listOf("b"), SelectionLogic.toggle(listOf("a"), "b", multiSelect = false))
        assertEquals(listOf("a", "b"), SelectionLogic.toggle(listOf("a"), "b", multiSelect = true))
    }
}
