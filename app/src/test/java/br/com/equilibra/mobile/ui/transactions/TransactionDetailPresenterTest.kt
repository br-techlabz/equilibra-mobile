package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.domain.model.transaction.TransactionDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 4.12 — Montagem dos campos exibidos no detalhe.
 *
 * Nomes de conta/categoria vêm do cache (catálogos, não da transação); quando
 * o label ainda não sincronizou, o campo é omitido em vez de mostrar UUID.
 */
class TransactionDetailPresenterTest {

    private fun detail(
        type: String = "EXPENSE",
        sourceAccountId: String? = "acc-1",
        destinationAccountId: String? = null,
        categoryId: String? = "cat-1",
        notes: String? = "obs",
        tagNames: List<String> = listOf("mercado")
    ) = TransactionDetail(
        id = "t1",
        type = type,
        status = "ACTIVE",
        description = "Mercado",
        occurredAt = "2026-10-01T12:00:00Z",
        amount = "42.90",
        sourceAccountId = sourceAccountId,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        notes = notes,
        createdAt = "2026-10-01T12:00:00Z",
        updatedAt = "2026-10-02T09:30:00Z",
        tagNames = tagNames
    )

    private val accounts = mapOf("acc-1" to "Conta Corrente", "acc-2" to "Poupança")
    private val categories = mapOf("cat-1" to "Alimentação")

    private fun fields(d: TransactionDetail) =
        TransactionDetailPresenter.fields(
            detail = d,
            accountNames = accounts,
            categoryNames = categories
        ).associate { it.label to it.value }

    @Test
    fun `despesa mostra conta de origem categoria tags e notas`() {
        val f = fields(detail())

        assertEquals("Despesa", f["Tipo"])
        assertEquals("Ativa", f["Status"])
        // MoneyFormatter usa NBSP após "R$"
        assertEquals("R$ 42,90", f["Valor"])
        assertEquals("01/10/2026", f["Data"])
        assertEquals("Conta Corrente", f["Conta de origem"])
        assertEquals(null, f["Conta de destino"])
        assertEquals("Alimentação", f["Categoria"])
        assertEquals("mercado", f["Tags"])
        assertEquals("obs", f["Notas"])
    }

    @Test
    fun `transferencia mostra origem e destino`() {
        val f = fields(
            detail(type = "TRANSFER", sourceAccountId = "acc-1", destinationAccountId = "acc-2")
        )

        assertEquals("Transferência", f["Tipo"])
        assertEquals("Conta Corrente", f["Conta de origem"])
        assertEquals("Poupança", f["Conta de destino"])
    }

    @Test
    fun `receita mostra conta de destino`() {
        val f = fields(detail(type = "INCOME", sourceAccountId = null, destinationAccountId = "acc-2"))

        assertEquals("Receita", f["Tipo"])
        assertEquals("Poupança", f["Conta de destino"])
        assertEquals(null, f["Conta de origem"])
    }

    @Test
    fun `nome de conta desconhecido e omitido em vez de exibir uuid`() {
        val f = fields(detail(sourceAccountId = "acc-desconhecida"))

        assertEquals(null, f["Conta de origem"])
    }

    @Test
    fun `categoria desconhecida e omitida`() {
        val f = fields(detail(categoryId = "cat-desconhecida"))

        assertEquals(null, f["Categoria"])
    }

    @Test
    fun `sem tags e sem notas nao criam campos vazios`() {
        val f = fields(detail(notes = null, tagNames = emptyList()))

        assertEquals(null, f["Tags"])
        assertEquals(null, f["Notas"])
    }

    @Test
    fun `notas em branco nao criam campo`() {
        val f = fields(detail(notes = "   "))

        assertEquals(null, f["Notas"])
    }

    @Test
    fun `cancelada exibe status cancelada`() {
        val f = fields(detail().copy(status = "CANCELLED"))

        assertEquals("Cancelada", f["Status"])
    }

    @Test
    fun `tags aparecem separadas por virgula na ordem recebida`() {
        // O backend já devolve ordenadas por nome; a tela não reordena.
        val f = fields(detail(tagNames = listOf("mercado", "cartão")))

        assertEquals("mercado, cartão", f["Tags"])
    }

    @Test
    fun `data e hora de atualizacao sao exibidas`() {
        val f = fields(detail())

        assertEquals("02/10/2026 09:30", f["Atualizada em"])
        assertFalse(f.containsKey("Criada em"))
    }

    @Test
    fun `valor invalido nao quebra a montagem`() {
        val f = fields(detail().copy(amount = "abc"))

        assertEquals("abc", f["Valor"])
    }

    @Test
    fun `anexos ficam em estado provisorio ate a sprint 5`() {
        val section = TransactionDetailPresenter.attachmentsState(attachmentCount = null)

        assertTrue(section.isProvisional)
        assertTrue(section.message!!.contains("próxima versão", ignoreCase = true))
    }
}
