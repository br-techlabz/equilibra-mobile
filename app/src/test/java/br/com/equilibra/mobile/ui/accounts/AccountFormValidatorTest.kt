package br.com.equilibra.mobile.ui.accounts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccountFormValidatorTest {

    // Nome

    @Test
    fun nomeVazioRetornaErro() {
        assertNotNull(AccountFormValidator.validateName(""))
        assertNotNull(AccountFormValidator.validateName("   "))
    }

    @Test
    fun nomeValidoRetornaNull() {
        assertNull(AccountFormValidator.validateName("Conta X"))
        assertNull(AccountFormValidator.validateName("  Conta X  "))
    }

    @Test
    fun nomeCom101CaracteresRetornaErro() {
        assertNotNull(AccountFormValidator.validateName("a".repeat(101)))
    }

    @Test
    fun nomeCom100CaracteresEValido() {
        assertNull(AccountFormValidator.validateName("a".repeat(100)))
    }

    // Saldo

    @Test
    fun saldoVazioRetornaErro() {
        assertNotNull(AccountFormValidator.validateMoney(""))
        assertNotNull(AccountFormValidator.validateMoney("  "))
    }

    @Test
    fun saldoInvalidoRetornaErro() {
        assertNotNull(AccountFormValidator.validateMoney("abc"))
    }

    @Test
    fun saldoComMaisDeDuasCasasDecimaisRetornaErro() {
        assertNotNull(AccountFormValidator.validateMoney("10.123"))
    }

    @Test
    fun saldoValidoRetornaNull() {
        assertNull(AccountFormValidator.validateMoney("0"))
        assertNull(AccountFormValidator.validateMoney("100.00"))
        assertNull(AccountFormValidator.validateMoney("100,50"))
        assertNull(AccountFormValidator.validateMoney("-50.10"))
    }

    @Test
    fun normalizeMoneyConverteVirgulaParaPonto() {
        assertEquals("100.50", AccountFormValidator.normalizeMoney("100,50"))
    }

    @Test
    fun normalizeMoneySempreDuasCasasDecimais() {
        assertEquals("100.00", AccountFormValidator.normalizeMoney("100"))
        assertEquals("100.10", AccountFormValidator.normalizeMoney("100,1"))
    }

    @Test
    fun normalizeMoneyPreservaPrecisao() {
        assertEquals("123456789012345.67", AccountFormValidator.normalizeMoney("123456789012345,67"))
    }
}
