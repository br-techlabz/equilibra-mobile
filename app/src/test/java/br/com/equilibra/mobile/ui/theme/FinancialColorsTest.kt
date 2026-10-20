package br.com.equilibra.mobile.ui.theme

import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Task 4.13 — Cores semânticas financeiras por tema.
 *
 * Invariantes: receita e despesa nunca compartilham a mesma cor, e a
 * paleta muda entre claro/escuro (contraste legível nos dois temas).
 */
class FinancialColorsTest {

    @Test
    fun receitaEDespesaSaoDistintasEmCadaTema() {
        assertNotEquals(LightFinancialColors.income, LightFinancialColors.expense)
        assertNotEquals(DarkFinancialColors.income, DarkFinancialColors.expense)
    }

    @Test
    fun paletaEscuroDiferenteDaClara() {
        assertNotEquals(LightFinancialColors, DarkFinancialColors)
    }
}
