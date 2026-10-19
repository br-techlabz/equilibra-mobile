package br.com.equilibra.mobile.ui.transactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferFormStateTest {

    private fun validState() = TransferFormState(
        description = "Poupança",
        amount = "250,75",
        occurredDate = "10/10/2026",
        sourceAccountId = "acc-1",
        destinationAccountId = "acc-2",
        categoryId = "cat-1",
        tagIds = listOf("t1"),
        notes = "reserva"
    )

    @Test
    fun estadoValidoPassaSemErros() {
        val validated = validState().validate()

        assertTrue(validated.isValid)
        assertNull(validated.sourceAccountError)
        assertNull(validated.destinationAccountError)
        assertNull(validated.categoryError)
    }

    @Test
    fun origemIgualAoDestinoTemErro() {
        val validated = validState().copy(destinationAccountId = "acc-1").validate()

        assertFalse(validated.isValid)
        assertEquals(
            "Conta de origem e destino não podem ser iguais",
            validated.destinationAccountError
        )
    }

    @Test
    fun origemObrigatoria() {
        val validated = validState().copy(sourceAccountId = null).validate()
        assertEquals("Selecione a conta de origem", validated.sourceAccountError)
    }

    @Test
    fun destinoObrigatorio() {
        val validated = validState().copy(destinationAccountId = null).validate()
        assertEquals("Selecione a conta de destino", validated.destinationAccountError)
    }

    @Test
    fun categoriaObrigatoriaNoContrato() {
        val validated = validState().copy(categoryId = null).validate()
        assertEquals("Selecione a categoria", validated.categoryError)
    }

    @Test
    fun camposBaseValidadosComoNosOutrosFormularios() {
        val validated = TransferFormState().validate()

        assertFalse(validated.isValid)
        assertEquals("Informe a descrição", validated.descriptionError)
        assertEquals("Informe o valor", validated.amountError)
        assertEquals("Data inválida (use dd/MM/aaaa)", validated.dateError)
    }

    @Test
    fun valorNegativoInvalida() {
        val validated = validState().copy(amount = "-1").validate()
        assertEquals("Valor deve ser maior que zero", validated.amountError)
    }

    @Test
    fun conversoesParaEnvio() {
        val validated = validState().validate()
        assertEquals("250.75", validated.normalizedAmount())
        assertEquals("2026-10-10T00:00:00Z", validated.occurredAtIso())
        assertEquals(listOf("t1"), validated.tagIds)
    }
}
