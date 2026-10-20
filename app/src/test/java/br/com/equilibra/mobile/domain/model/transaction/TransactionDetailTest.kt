package br.com.equilibra.mobile.domain.model.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 4.12 — Ações disponíveis por tipo/status do detalhe da transação.
 *
 * Regras reais do backend:
 * - cancelamento existe só para EXPENSE, INCOME e TRANSFER
 *   (PATCH /{id}/cancel); STATEMENT_PAYMENT não tem endpoint de cancelamento.
 * - transação CANCELLED é imutável (edição e novo cancelamento não existem).
 */
class TransactionDetailTest {

    private fun detail(
        type: String = TransactionType.EXPENSE,
        status: String = TransactionStatus.ACTIVE
    ) = TransactionDetail(
        id = "t1",
        type = type,
        status = status,
        description = "Mercado",
        occurredAt = "2026-10-01T12:00:00Z",
        amount = "10.00",
        sourceAccountId = "acc-1",
        destinationAccountId = null,
        categoryId = "cat-1",
        notes = null,
        createdAt = "2026-10-01T12:00:00Z",
        updatedAt = "2026-10-01T12:00:00Z"
    )

    @Test
    fun `despesa ativa pode editar e cancelar`() {
        val d = detail(TransactionType.EXPENSE, TransactionStatus.ACTIVE)

        assertTrue(d.canEdit)
        assertTrue(d.canCancel)
    }

    @Test
    fun `receita ativa pode editar e cancelar`() {
        val d = detail(TransactionType.INCOME, TransactionStatus.ACTIVE)

        assertTrue(d.canEdit)
        assertTrue(d.canCancel)
    }

    @Test
    fun `transferencia ativa cancela mas nao edita (sem tela de edicao no app)`() {
        val d = detail(TransactionType.TRANSFER, TransactionStatus.ACTIVE)

        assertFalse(d.canEdit)
        assertTrue(d.canCancel)
    }

    @Test
    fun `transacao cancelada nao edita nem cancela`() {
        val d = detail(TransactionType.EXPENSE, TransactionStatus.CANCELLED)

        assertFalse(d.canEdit)
        assertFalse(d.canCancel)
    }

    @Test
    fun `pagamento de fatura nao edita nem cancela`() {
        val d = detail(TransactionType.STATEMENT_PAYMENT, TransactionStatus.ACTIVE)

        assertFalse(d.canEdit)
        assertFalse(d.canCancel)
    }

    @Test
    fun `tipo desconhecido nao expoe acoes`() {
        val d = detail("SOMETHING_ELSE", TransactionStatus.ACTIVE)

        assertFalse(d.canEdit)
        assertFalse(d.canCancel)
    }

    @Test
    fun `tipo de dados expoe labels pt-br`() {
        assertEquals("Despesa", TransactionType.label(TransactionType.EXPENSE))
        assertEquals("Receita", TransactionType.label(TransactionType.INCOME))
        assertEquals("Transferência", TransactionType.label(TransactionType.TRANSFER))
        assertEquals("Pagamento de fatura", TransactionType.label(TransactionType.STATEMENT_PAYMENT))
        assertEquals("SOMETHING_ELSE", TransactionType.label("SOMETHING_ELSE"))
    }

    @Test
    fun `status expoe labels pt-br`() {
        assertEquals("Ativa", TransactionStatus.label(TransactionStatus.ACTIVE))
        assertEquals("Cancelada", TransactionStatus.label(TransactionStatus.CANCELLED))
        assertEquals("DESCONHECIDO", TransactionStatus.label("DESCONHECIDO"))
    }
}
