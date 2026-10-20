package br.com.equilibra.mobile.ui.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionFormValidatorTest {

    // Descrição

    @Test
    fun descricaoVaziaTemErro() {
        assertEquals("Informe a descrição", TransactionFormValidator.validateDescription("   "))
    }

    @Test
    fun descricaoComMaisDe255TemErro() {
        val longa = "a".repeat(256)
        assertEquals("Descrição deve ter no máximo 255 caracteres", TransactionFormValidator.validateDescription(longa))
    }

    @Test
    fun descricaoValidaSemErro() {
        assertNull(TransactionFormValidator.validateDescription("Mercado do mês"))
        assertNull(TransactionFormValidator.validateDescription("a".repeat(255)))
    }

    // Valor

    @Test
    fun valorVazioTemErro() {
        assertEquals("Informe o valor", TransactionFormValidator.validateAmount("  "))
    }

    @Test
    fun valorNaoNumericoTemErro() {
        assertEquals("Valor inválido", TransactionFormValidator.validateAmount("abc"))
    }

    @Test
    fun valorZeroOuNegativoTemErro() {
        assertEquals("Valor deve ser maior que zero", TransactionFormValidator.validateAmount("0"))
        assertEquals("Valor deve ser maior que zero", TransactionFormValidator.validateAmount("-10,50"))
    }

    @Test
    fun valorAceitaVirgulaPtBR() {
        assertNull(TransactionFormValidator.validateAmount("1234,56"))
    }

    @Test
    fun valorComMaisDeDuasCasasRejeitadoSemArredondar() {
        // Rejeita em vez de arredondar indevidamente
        assertEquals("Máximo 2 casas decimais", TransactionFormValidator.validateAmount("10.999"))
    }

    @Test
    fun valorValidoSemErro() {
        assertNull(TransactionFormValidator.validateAmount("0.01"))
        assertNull(TransactionFormValidator.validateAmount("1234567890.10"))
    }

    @Test
    fun normalizeAmountMantemPrecisaoESemArredondamento() {
        // Só normaliza separador/espaços; NÃO toca na escala
        assertEquals("1234.56", TransactionFormValidator.normalizeAmount(" 1.234,56 "))
        assertEquals("10.5", TransactionFormValidator.normalizeAmount("10,5"))
        assertEquals("1234567890.10", TransactionFormValidator.normalizeAmount("1234567890.10"))
    }

    // Notas

    @Test
    fun notasAcimaDe4000TemErro() {
        assertEquals("Notas devem ter no máximo 4000 caracteres",
            TransactionFormValidator.validateNotes("x".repeat(4001)))
    }

    @Test
    fun notasValidasSemErro() {
        assertNull(TransactionFormValidator.validateNotes(""))
        assertNull(TransactionFormValidator.validateNotes("x".repeat(4000)))
    }

    // Data

    @Test
    fun dataValidaSemErro() {
        assertNull(TransactionFormValidator.validateDate("01/10/2026"))
    }

    @Test
    fun dataInvalidaTemErro() {
        assertEquals("Data inválida (use dd/MM/aaaa)", TransactionFormValidator.validateDate("2026-10-01"))
        assertEquals("Data inválida (use dd/MM/aaaa)", TransactionFormValidator.validateDate("32/01/2026"))
        assertEquals("Data inválida (use dd/MM/aaaa)", TransactionFormValidator.validateDate("31/02/2026"))
        assertEquals("Data inválida (use dd/MM/aaaa)", TransactionFormValidator.validateDate(""))
    }

    @Test
    fun toInstantIsoConverteParaUtc() {
        // 01/10/2026 → início do dia em UTC, ISO-8601
        assertEquals("2026-10-01T00:00:00Z", TransactionFormValidator.toInstantIso("01/10/2026"))
        assertNull(TransactionFormValidator.toInstantIso("inválida"))
    }

    @Test
    fun toInstantExclusiveEndIsoAbreODiaSeguinte() {
        // Backend usa occurredAt < :to (limite exclusivo): "até 31/10" envia 01/11
        assertEquals(
            "2026-11-01T00:00:00Z",
            TransactionFormValidator.toInstantExclusiveEndIso("31/10/2026")
        )
        assertNull(TransactionFormValidator.toInstantExclusiveEndIso("inválida"))
    }
}
