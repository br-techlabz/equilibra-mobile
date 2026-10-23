package br.com.equilibra.mobile.data.local.statement

import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Task 7.1 — Fronteira DTO → Entity das faturas.
 *
 * O cache guarda exatamente o que a API mandou: nenhum valor monetário passa
 * por Double/Float e nenhum item é recalculado a partir do total.
 */
class StatementMapperTest {

    private fun dto(
        totalAmount: String = "1234.56",
        paidAmount: String = "200.00",
        outstandingAmount: String = "1034.56",
        items: List<StatementItemDto> = listOf(
            StatementItemDto(
                id = "i-1",
                purchaseId = "p-1",
                description = "Notebook",
                sequenceNumber = 1,
                totalInstallments = 3,
                amount = "411.52",
                cycleMonth = "2026-11-01"
            ),
            StatementItemDto(
                id = "i-2",
                purchaseId = "p-1",
                description = "Notebook",
                sequenceNumber = 2,
                totalInstallments = 3,
                amount = "411.52",
                cycleMonth = "2026-12-01"
            )
        )
    ) = StatementDto(
        id = "s-1",
        creditCardId = "c-1",
        cycleStart = "2026-10-03",
        cycleEnd = "2026-11-02",
        closingAt = "2026-11-03T03:00:00Z",
        dueDate = "2026-11-10",
        status = "PARTIALLY_PAID",
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        items = items
    )

    @Test
    fun entityCopiaCamposSemConversao() {
        val e = dto().toEntity(syncedAt = 1234L)

        assertEquals("s-1", e.id)
        assertEquals("c-1", e.creditCardId)
        assertEquals("2026-10-03", e.cycleStart)
        assertEquals("2026-11-02", e.cycleEnd)
        assertEquals("2026-11-03T03:00:00Z", e.closingAt)
        assertEquals("2026-11-10", e.dueDate)
        assertEquals("PARTIALLY_PAID", e.status)
        assertEquals("1234.56", e.totalAmount)
        assertEquals("200.00", e.paidAmount)
        assertEquals("1034.56", e.outstandingAmount)
        assertEquals(1234L, e.syncedAt)
    }

    @Test
    fun itensPreservamValoresERecebemStatementId() {
        val itens = dto().toItemEntities()

        assertEquals(2, itens.size)
        assertEquals(listOf("s-1", "s-1"), itens.map { it.statementId })
        assertEquals(listOf("i-1", "i-2"), itens.map { it.id })
        assertEquals(listOf("p-1", "p-1"), itens.map { it.purchaseId })
        assertEquals(listOf("411.52", "411.52"), itens.map { it.amount })
        assertEquals(listOf(1, 2), itens.map { it.sequenceNumber })
        assertEquals(listOf(3, 3), itens.map { it.totalInstallments })
        assertEquals(listOf("2026-11-01", "2026-12-01"), itens.map { it.cycleMonth })
    }

    @Test
    fun faturasSemItensContinuamMapeando() {
        val e = dto(items = emptyList()).toEntity(syncedAt = null)

        assertNull(e.syncedAt)
        assertEquals(0, dto(items = emptyList()).toItemEntities().size)
    }

    @Test
    fun totalPagoEPendenteNaoSaoRecalculados() {
        // Mesmo que os números não "fechem", o mapper copia a resposta do
        // backend — a autoridade dos agregados é do servidor (Task 7.2).
        val e = dto(
            totalAmount = "100.00",
            paidAmount = "10.00",
            outstandingAmount = "89.99"
        ).toEntity(syncedAt = null)

        assertEquals("100.00", e.totalAmount)
        assertEquals("10.00", e.paidAmount)
        assertEquals("89.99", e.outstandingAmount)
    }
}
