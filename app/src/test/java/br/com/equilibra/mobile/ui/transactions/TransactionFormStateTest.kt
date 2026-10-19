package br.com.equilibra.mobile.ui.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFormStateTest {

    private fun validState() = TransactionFormState(
        description = "Mercado",
        amount = "150,90",
        occurredDate = "01/10/2026",
        accountId = "acc-1",
        categoryId = "cat-1",
        tagIds = listOf("t1", "t2"),
        notes = "compra do mês"
    )

    @Test
    fun estadoValidoPassaSemErros() {
        val validated = validState().validate()

        assertTrue(validated.isValid)
        assertNull(validated.descriptionError)
        assertNull(validated.amountError)
        assertNull(validated.dateError)
        assertNull(validated.accountError)
        assertNull(validated.categoryError)
        assertNull(validated.notesError)
    }

    @Test
    fun camposObrigatoriosAusentesGeramErros() {
        val validated = TransactionFormState().validate()

        assertFalse(validated.isValid)
        assertEquals("Informe a descrição", validated.descriptionError)
        assertEquals("Informe o valor", validated.amountError)
        assertEquals("Data inválida (use dd/MM/aaaa)", validated.dateError)
        assertEquals("Selecione a conta", validated.accountError)
        assertEquals("Selecione a categoria", validated.categoryError)
    }

    @Test
    fun valorNegativoInvalida() {
        val validated = validState().copy(amount = "-5,00").validate()
        assertEquals("Valor deve ser maior que zero", validated.amountError)
        assertFalse(validated.isValid)
    }

    @Test
    fun tagsENotasPreservadosNaValidacao() {
        val validated = validState().validate()
        assertEquals(listOf("t1", "t2"), validated.tagIds)
        assertEquals("compra do mês", validated.notes)
    }

    @Test
    fun normalizedAmountExpoeValorProntoParaEnvio() {
        val validated = validState().copy(amount = " 1.234,56 ").validate()
        assertEquals("1234.56", validated.normalizedAmount())
    }

    @Test
    fun occurredAtIsoExpoeInstantParaApi() {
        val validated = validState().validate()
        assertEquals("2026-10-01T00:00:00Z", validated.occurredAtIso())
    }

    @Test
    fun occurredAtIsoNuloQuandoDataInvalida() {
        val validated = validState().copy(occurredDate = "99/99/9999").validate()
        assertNull(validated.occurredAtIso())
    }

    @Test
    fun limparErroAposCorrecao() {
        var state = validState().copy(description = "").validate()
        assertFalse(state.isValid)

        state = state.copy(description = "Corrigida", descriptionError = null)
        assertTrue(state.isValid)
    }
}
