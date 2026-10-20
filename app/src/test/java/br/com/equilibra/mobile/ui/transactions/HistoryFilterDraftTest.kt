package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.repository.HistoryFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFilterDraftTest {

    @Test
    fun `rascunho vazio equivale a sem filtros`() {
        val draft = HistoryFilterDraft.empty()

        assertTrue(draft.isValid)
        assertEquals(HistoryFilters.empty(), draft.toFilters())
    }

    @Test
    fun `datas viram inicio do dia e inicio do dia seguinte`() {
        val draft = HistoryFilterDraft(fromDate = "01/10/2026", toDate = "31/10/2026")

        val filters = draft.toFilters()

        assertEquals("2026-10-01T00:00:00Z", filters.fromIso)
        // Backend compara occurredAt < :to — limite superior exclusivo
        assertEquals("2026-11-01T00:00:00Z", filters.toIso)
    }

    @Test
    fun `data invalida bloqueia aplicacao`() {
        val draft = HistoryFilterDraft(fromDate = "31/02/2026")

        assertFalse(draft.isValid)
        assertEquals("Data inválida (use dd/MM/aaaa)", draft.fromError)
    }

    @Test
    fun `intervalo invertido bloqueia aplicacao`() {
        val draft = HistoryFilterDraft(fromDate = "31/10/2026", toDate = "01/10/2026")

        assertFalse(draft.isValid)
        assertEquals("Data inicial deve ser anterior à final", draft.rangeError)
    }

    @Test
    fun `mesmo dia e valido e vira um dia de intervalo`() {
        val draft = HistoryFilterDraft(fromDate = "31/10/2026", toDate = "31/10/2026")

        assertTrue(draft.isValid)
        assertNull(draft.rangeError)
        // from < to no backend: 31/10T00:00 < 01/11T00:00
        assertEquals("2026-10-31T00:00:00Z", draft.toFilters().fromIso)
        assertEquals("2026-11-01T00:00:00Z", draft.toFilters().toIso)
    }

    @Test
    fun `exibicao do limite superior recua um dia na meia-noite`() {
        // ida e volta: o painel mostra "31/10", não "01/11"
        val date = TransactionFormValidator.toDisplayDateExclusiveEnd("2026-11-01T00:00:00Z")
        assertEquals("31/10/2026", date)
        // valor no meio do dia é exibido como está
        assertEquals(
            "01/11/2026",
            TransactionFormValidator.toDisplayDateExclusiveEnd("2026-11-01T12:00:00Z")
        )
    }

    @Test
    fun `filtros combinados sao preservados`() {
        val draft = HistoryFilterDraft(
            type = HistoryFilterOptions.TYPE_TRANSFER,
            status = HistoryFilterOptions.STATUS_CANCELLED,
            fromDate = "01/10/2026",
            toDate = "31/10/2026",
            accountId = "acc-1",
            categoryId = "cat-1",
            tagIds = listOf("t1", "t2")
        )

        val filters = draft.toFilters()

        assertEquals("TRANSFER", filters.type)
        assertEquals("CANCELLED", filters.status)
        assertEquals("acc-1", filters.accountId)
        assertEquals("cat-1", filters.categoryId)
        assertEquals(listOf("t1", "t2"), filters.tagIds)
    }

    @Test
    fun `from reconverte filtros aplicados para o rascunho`() {
        val applied = HistoryFilters(
            type = "EXPENSE",
            status = "ACTIVE",
            fromIso = "2026-10-01T00:00:00Z",
            toIso = "2026-11-01T00:00:00Z",
            accountId = "acc-1",
            categoryId = null,
            tagIds = listOf("t1")
        )

        val draft = HistoryFilterDraft.from(applied)

        assertEquals("EXPENSE", draft.type)
        assertEquals("ACTIVE", draft.status)
        assertEquals("01/10/2026", draft.fromDate)
        assertEquals("31/10/2026", draft.toDate)
        assertEquals("acc-1", draft.accountId)
        assertEquals(listOf("t1"), draft.tagIds)
        // ida e volta estável
        assertEquals(applied, draft.toFilters())
    }

    @Test
    fun `limpar o rascunho remove todos os campos`() {
        val cleared = HistoryFilterDraft.empty()

        val filters = cleared.toFilters()

        assertNull(filters.type)
        assertNull(filters.status)
        assertNull(filters.fromIso)
        assertNull(filters.toIso)
        assertNull(filters.accountId)
        assertNull(filters.categoryId)
        assertTrue(filters.tagIds.isEmpty())
    }

    @Test
    fun `rotulos de tipo refletem o contrato`() {
        assertEquals("Despesa", HistoryFilterOptions.typeLabel("EXPENSE"))
        assertEquals("Receita", HistoryFilterOptions.typeLabel("INCOME"))
        assertEquals("Transferência", HistoryFilterOptions.typeLabel("TRANSFER"))
        assertEquals("Pagamento de fatura", HistoryFilterOptions.typeLabel("STATEMENT_PAYMENT"))
    }
}
