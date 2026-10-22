package br.com.equilibra.mobile.ui.transactions

import br.com.equilibra.mobile.domain.model.transaction.TransactionDetail
import br.com.equilibra.mobile.domain.model.transaction.TransactionStatus
import br.com.equilibra.mobile.domain.model.transaction.TransactionType
import br.com.equilibra.mobile.domain.util.DateTimeFormat
import br.com.equilibra.mobile.domain.util.MoneyFormatter

/** Campo exibido no detalhe (rótulo pt-BR + valor já formatado). */
data class DetailField(val label: String, val value: String)

/**
 * Task 4.12 — Montagem da apresentação do detalhe (pura, testável fora do
 * Compose). Formatação monetária segue [MoneyFormatter] (pt-BR) e datas em
 * UTC — mesmo fuso do contrato Instant do backend.
 */
object TransactionDetailPresenter {

    /**
     * Campos na ordem de exibição. Nomes resolvidos pelo cache (conta/categoria);
     * UUID desconhecido é omitido em vez de exposto. Opcionais vazios não
     * geram campo.
     */
    fun fields(
        detail: TransactionDetail,
        accountNames: Map<String, String>,
        categoryNames: Map<String, String>
    ): List<DetailField> = buildList {
        add(DetailField("Tipo", TransactionType.label(detail.type)))
        add(DetailField("Status", TransactionStatus.label(detail.status)))
        add(DetailField("Valor", MoneyFormatter.format(detail.amount)))
        add(DetailField("Data", formatDate(detail.occurredAt)))

        detail.sourceAccountId?.let { accountNames[it] }
            ?.let { add(DetailField("Conta de origem", it)) }
        detail.destinationAccountId?.let { accountNames[it] }
            ?.let { add(DetailField("Conta de destino", it)) }
        detail.categoryId?.let { categoryNames[it] }
            ?.let { add(DetailField("Categoria", it)) }
        detail.paymentMethod?.let { add(DetailField("Forma de pagamento", labelPaymentMethod(it))) }

        if (detail.tagNames.isNotEmpty()) {
            // Backend já ordena as tags por nome (TransactionHistoryService)
            add(DetailField("Tags", detail.tagNames.joinToString(", ")))
        }
        detail.notes?.takeIf { it.isNotBlank() }?.let { add(DetailField("Notas", it.trim())) }

        formatDateTime(detail.updatedAt)?.let { add(DetailField("Atualizada em", it)) }
    }

    private fun formatDate(iso: String): String =
        DateTimeFormat.formatDate(iso, fallback = iso)

    private fun formatDateTime(iso: String): String? =
        DateTimeFormat.formatDateTime(iso)

    private fun labelPaymentMethod(value: String): String = when (value) {
        "BANK_ACCOUNT" -> "Conta bancária"
        "CREDIT_CARD" -> "Cartão de crédito"
        else -> value
    }
}
