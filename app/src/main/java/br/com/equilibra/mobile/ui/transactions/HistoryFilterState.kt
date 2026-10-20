package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.data.repository.HistoryFilters

/**
 * Task 4.11 — Estado do painel de filtros do histórico unificado.
 *
 * Separado do [HistoryViewModel] porque é estado de rascunho da UI: o
 * usuário combina os filtros e só então chama `applyFilters`. A conversão
 * para [HistoryFilters] normaliza as datas para o contrato real
 * (Instant ISO-8601): "de" no início do dia e "até" no início do dia
 * SEGUINTE, porque o backend compara `occurredAt < :to` (limite superior
 * exclusivo).
 *
 * Campos vazios/nulos significam "sem filtro" — o parâmetro é omitido da
 * query. Datas continuam em dd/MM/aaaa (exibição pt-BR).
 */
data class HistoryFilterDraft(
    val type: String? = null,
    val status: String? = null,
    val fromDate: String = "",
    val toDate: String = "",
    val accountId: String? = null,
    val categoryId: String? = null,
    val tagIds: List<String> = emptyList()
) {

    val fromError: String?
        get() = if (fromDate.isBlank()) null else TransactionFormValidator.validateDate(fromDate)

    val toError: String?
        get() = if (toDate.isBlank()) null else TransactionFormValidator.validateDate(toDate)

    /**
     * Intervalo invertido só é erro quando as duas datas são válidas.
     * Mesmo dia é permitido: "até" vira o início do dia seguinte e o
     * backend recebe from < to.
     */
    val rangeError: String?
        get() {
            if (fromDate.isBlank() || toDate.isBlank()) return null
            if (fromError != null || toError != null) return null
            val from = TransactionFormValidator.parseDate(fromDate)
            val to = TransactionFormValidator.parseDate(toDate)
            return if (from.isAfter(to)) "Data inicial deve ser anterior à final" else null
        }

    val isValid: Boolean
        get() = fromError == null && toError == null && rangeError == null

    /** Converte para o contrato da API; use apenas quando [isValid]. */
    fun toFilters(): HistoryFilters = HistoryFilters(
        type = type,
        status = status,
        fromIso = fromDate.takeIf { it.isNotBlank() }
            ?.let { TransactionFormValidator.toInstantIso(it) },
        toIso = toDate.takeIf { it.isNotBlank() }
            ?.let { TransactionFormValidator.toInstantExclusiveEndIso(it) },
        accountId = accountId,
        categoryId = categoryId,
        tagIds = tagIds
    )

    companion object {
        fun empty() = HistoryFilterDraft()

        /** Rascunho a partir dos filtros já aplicados (o painel reflete o estado atual). */
        fun from(filters: HistoryFilters): HistoryFilterDraft = HistoryFilterDraft(
            type = filters.type,
            status = filters.status,
            fromDate = filters.fromIso?.let { TransactionFormValidator.toDisplayDate(it) }.orEmpty(),
            // "to" é exclusivo: volta a ser exibido como o último dia incluído
            toDate = filters.toIso
                ?.let { TransactionFormValidator.toDisplayDateExclusiveEnd(it) }
                .orEmpty(),
            accountId = filters.accountId,
            categoryId = filters.categoryId,
            tagIds = filters.tagIds
        )
    }
}

/**
 * Opções do painel — valores do contrato real de /transactions.
 * Rótulos em pt-BR; os valores enviados são sempre os enums do backend.
 */
object HistoryFilterOptions {

    const val TYPE_EXPENSE = "EXPENSE"
    const val TYPE_INCOME = "INCOME"
    const val TYPE_TRANSFER = "TRANSFER"
    const val TYPE_STATEMENT_PAYMENT = "STATEMENT_PAYMENT"

    const val STATUS_ACTIVE = "ACTIVE"
    const val STATUS_CANCELLED = "CANCELLED"

    val types: List<Pair<String, String>> = listOf(
        TYPE_EXPENSE to "Despesa",
        TYPE_INCOME to "Receita",
        TYPE_TRANSFER to "Transferência",
        TYPE_STATEMENT_PAYMENT to "Pagamento de fatura"
    )

    val statuses: List<Pair<String, String>> = listOf(
        STATUS_ACTIVE to "Ativas",
        STATUS_CANCELLED to "Canceladas"
    )

    /** Rótulo pt-BR do tipo exibido em cada item do histórico. */
    fun typeLabel(type: String): String = when (type) {
        TYPE_EXPENSE -> "Despesa"
        TYPE_INCOME -> "Receita"
        TYPE_TRANSFER -> "Transferência"
        TYPE_STATEMENT_PAYMENT -> "Pagamento de fatura"
        else -> type
    }
}
