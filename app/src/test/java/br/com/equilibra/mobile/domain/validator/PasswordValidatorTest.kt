package br.com.equilibra.mobile.domain.validator

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordValidatorTest {

    @Test fun `valido com 8 caracteres`() {
        assertNull(PasswordValidator.validate("12345678"))
    }

    @Test fun `invalido vazio`() {
        assertNotNull(PasswordValidator.validate(""))
    }

    @Test fun `invalido so espacos`() {
        assertNotNull(PasswordValidator.validate("        "))
    }

    @Test fun `invalido menor que 8`() {
        assertNotNull(PasswordValidator.validate("1234567"))
    }

    @Test fun `invalido maior que 128`() {
        assertNotNull(PasswordValidator.validate("a".repeat(129)))
    }

    @Test fun `valido exatamente 128`() {
        assertNull(PasswordValidator.validate("a".repeat(128)))
    }

    @Test fun `valido com unicode e espacos internos`() {
        assertNull(PasswordValidator.validate("senha segura 123"))
    }
}
