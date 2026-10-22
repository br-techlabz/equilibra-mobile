package br.com.equilibra.mobile.ui.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditCardFormValidatorTest {

    // name

    @Test
    fun nameVazioRejeitado() {
        assertEquals("Informe o nome", CreditCardFormValidator.validateName(""))
        assertEquals("Informe o nome", CreditCardFormValidator.validateName("   "))
    }

    @Test
    fun nameMaiorQue100Rejeitado() {
        assertEquals("Nome deve ter no máximo 100 caracteres", CreditCardFormValidator.validateName("a".repeat(101)))
    }

    @Test
    fun nameValidoAceito() {
        assertNull(CreditCardFormValidator.validateName("Nubank Roxinho"))
    }

    // institution

    @Test
    fun institutionVaziaRejeitada() {
        assertEquals("Informe a instituição", CreditCardFormValidator.validateInstitution(""))
    }

    @Test
    fun institutionMaiorQue100Rejeitada() {
        assertEquals("Instituição deve ter no máximo 100 caracteres", CreditCardFormValidator.validateInstitution("b".repeat(101)))
    }

    @Test
    fun institutionValidaAceita() {
        assertNull(CreditCardFormValidator.validateInstitution("Nubank"))
    }

    // nickname (opcional)

    @Test
    fun nicknameVazioAceito() {
        assertNull(CreditCardFormValidator.validateNickname(""))
        assertNull(CreditCardFormValidator.validateNickname("   "))
    }

    @Test
    fun nicknameMaiorQue100Rejeitado() {
        assertEquals("Apelido deve ter no máximo 100 caracteres", CreditCardFormValidator.validateNickname("c".repeat(101)))
    }

    // limite

    @Test
    fun limiteVazioRejeitado() {
        assertEquals("Informe o limite", CreditCardFormValidator.validateLimit(""))
    }

    @Test
    fun limiteZeroOuNegativoRejeitado() {
        // Backend @DecimalMin("0.01") + money(): signum() <= 0 rejeita
        assertEquals("Limite deve ser maior que zero", CreditCardFormValidator.validateLimit("0"))
        assertEquals("Limite deve ser maior que zero", CreditCardFormValidator.validateLimit("0.00"))
        assertEquals("Limite deve ser maior que zero", CreditCardFormValidator.validateLimit("-10"))
    }

    @Test
    fun limiteCom3CasasRejeitado() {
        assertEquals("Máximo 2 casas decimais", CreditCardFormValidator.validateLimit("10.123"))
    }

    @Test
    fun limiteInvalidoRejeitado() {
        assertEquals("Valor inválido", CreditCardFormValidator.validateLimit("abc"))
    }

    @Test
    fun limiteComVirgulaDecimalAceito() {
        // Mesma regra do AccountFormValidator: vírgula = decimal ("1234,56");
        // ponto só é usado como decimal, não como separador de milhar
        assertNull(CreditCardFormValidator.validateLimit("1234,56"))
        assertEquals("1234.56", CreditCardFormValidator.normalizeLimit("1234,56"))
    }

    @Test
    fun limiteValidoAceito() {
        assertNull(CreditCardFormValidator.validateLimit("10000.00"))
        assertNull(CreditCardFormValidator.validateLimit("0.01"))
    }

    // closing/due day

    @Test
    fun diaVazioRejeitado() {
        assertEquals("Informe o dia de fechamento", CreditCardFormValidator.validateClosingDay(""))
    }

    @Test
    fun diaForaDaFaixaRejeitado() {
        // Backend @Min(1) @Max(31)
        assertEquals("Dia deve ser entre 1 e 31", CreditCardFormValidator.validateClosingDay("0"))
        assertEquals("Dia deve ser entre 1 e 31", CreditCardFormValidator.validateClosingDay("32"))
        assertEquals("Dia deve ser entre 1 e 31", CreditCardFormValidator.validateDueDay("-5"))
    }

    @Test
    fun diaNaoNumericoRejeitado() {
        assertEquals("Dia inválido", CreditCardFormValidator.validateDueDay("abc"))
    }

    @Test
    fun diaValidoAceito() {
        assertNull(CreditCardFormValidator.validateClosingDay("1"))
        assertNull(CreditCardFormValidator.validateDueDay("31"))
    }

    @Test
    fun normalizeDayConverte() {
        assertEquals(25, CreditCardFormValidator.normalizeDay("25"))
        assertEquals(31, CreditCardFormValidator.normalizeDay(" 31 "))
        assertEquals(null, CreditCardFormValidator.normalizeDay("abc"))
    }

    // timezone

    @Test
    fun timezoneVaziaEValida() {
        // null → backend aplica "America/Sao_Paulo"
        assertNull(CreditCardFormValidator.validateTimezone(null))
        assertNull(CreditCardFormValidator.validateTimezone(""))
    }

    @Test
    fun timezoneIanaValida() {
        assertNull(CreditCardFormValidator.validateTimezone("America/Sao_Paulo"))
        assertNull(CreditCardFormValidator.validateTimezone("UTC"))
        assertNull(CreditCardFormValidator.validateTimezone("Europe/Lisbon"))
    }

    @Test
    fun timezoneInvalidaRejeitada() {
        assertEquals("Fuso horário inválido", CreditCardFormValidator.validateTimezone("Mars/Olympus"))
        assertEquals("Fuso horário inválido", CreditCardFormValidator.validateTimezone("not a zone"))
    }

    // Regra explícita do prompt: NÃO assumir fechamento antes do vencimento

    @Test
    fun fechamentoDepoisDoVencimentoEAceito() {
        // O backend só valida 1..31 individualmente — sem contrato de ordem,
        // o formulário não impõe (registrar comportamento: ambos válidos)
        assertNull(CreditCardFormValidator.validateClosingDay("28"))
        assertNull(CreditCardFormValidator.validateDueDay("2"))
    }
}
