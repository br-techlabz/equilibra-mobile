package br.com.equilibra.mobile.domain.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EmailValidatorTest {

    @Test fun `valido aceita email normal`() {
        assertNull(EmailValidator.validate("user@example.com"))
    }

    @Test fun `invalido quando vazio`() {
        assertNotNull(EmailValidator.validate(""))
        assertNotNull(EmailValidator.validate("   "))
    }

    @Test fun `invalido sem arroba`() {
        assertNotNull(EmailValidator.validate("userexample.com"))
    }

    @Test fun `invalido sem dominio`() {
        assertNotNull(EmailValidator.validate("user@"))
    }

    @Test fun `invalido quando muito longo`() {
        val long = "a".repeat(250) + "@example.com"
        assertNotNull(EmailValidator.validate(long))
    }

    @Test fun `normalize faz trim e lowercase`() {
        assertEquals("user@example.com", EmailValidator.normalize("  User@Example.COM  "))
    }
}
