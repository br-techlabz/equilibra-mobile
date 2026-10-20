package br.com.equilibra.mobile.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatterTest {

    @Test
    fun format_formatsValueInPtBr() {
        val formatted = MoneyFormatter.format("1234.56")
        // pt-BR format: R$ 1.234,56 (com espaço não-quebrável)
        assertTrue(formatted.contains("R$"))
        assertTrue(formatted.contains("1.234"))
        assertTrue(formatted.contains("56"))
    }

    @Test
    fun format_handlesZero() {
        val formatted = MoneyFormatter.format("0.00")
        assertTrue(formatted.contains("R$"))
        assertTrue(formatted.contains("0,00"))
    }

    @Test
    fun format_handlesNegativeValue() {
        val formatted = MoneyFormatter.format("-100.00")
        assertTrue(formatted.contains("-"))
        assertTrue(formatted.contains("R$"))
    }

    @Test
    fun format_handlesLargeValue() {
        val formatted = MoneyFormatter.format("1234567.89")
        assertTrue(formatted.contains("R$"))
        assertTrue(formatted.contains("1.234.567"))
    }

    @Test
    fun format_returnsOriginalOnInvalidInput() {
        assertEquals("invalid", MoneyFormatter.format("invalid"))
        assertEquals("", MoneyFormatter.format(""))
    }

    @Test
    fun format_nullUsaPlaceholderDeAusencia() {
        // ausência de valor não pode parecer zero
        assertEquals("-", MoneyFormatter.format(null))
        assertEquals(MoneyFormatter.NULL_PLACEHOLDER, MoneyFormatter.format(null))
    }

    @Test
    fun format_negativoMantemSinalAnteriorAoSimbolo() {
        val formatted = MoneyFormatter.format("-100.00")
        assertTrue(formatted.startsWith("-"))
        assertTrue(formatted.contains("100,00"))
    }

    @Test
    fun parse_convertsStringToBigDecimal() {
        assertEquals(BigDecimal("1234.56"), MoneyFormatter.parse("1234.56"))
        assertEquals(BigDecimal("0.00"), MoneyFormatter.parse("0.00"))
        assertEquals(BigDecimal("-100.00"), MoneyFormatter.parse("-100.00"))
    }

    @Test
    fun parse_returnsZeroOnInvalidInput() {
        assertEquals(BigDecimal.ZERO, MoneyFormatter.parse("invalid"))
        assertEquals(BigDecimal.ZERO, MoneyFormatter.parse(""))
    }

    @Test
    fun parse_preservesPrecision() {
        // BigDecimal deve preservar precisão (sem conversão via Double)
        val value = "123456789012345.67"
        assertEquals(BigDecimal(value), MoneyFormatter.parse(value))
    }
}
