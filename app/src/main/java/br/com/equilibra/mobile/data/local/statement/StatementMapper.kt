package br.com.equilibra.mobile.data.local.statement

import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementItemDto

/**
 * Task 7.1 — DTO → Entity das faturas.
 *
 * Cópia literal de valores: nada de conversão monetária (a String já vem
 * precisa do BigDecimalStringSerializer) e nada de recálculo de agregados
 * ou de itens — o backend é a autoridade da fatura.
 */

fun StatementDto.toEntity(syncedAt: Long?): StatementEntity = StatementEntity(
    id = id,
    creditCardId = creditCardId,
    cycleStart = cycleStart,
    cycleEnd = cycleEnd,
    closingAt = closingAt,
    dueDate = dueDate,
    status = status,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    outstandingAmount = outstandingAmount,
    syncedAt = syncedAt
)

/** Id do item é o id real da parcela no backend; re-sincronizar sobrescreve. */
fun StatementDto.toItemEntities(): List<StatementItemEntity> =
    items.map { it.toEntity(statementId = id) }

fun StatementItemDto.toEntity(statementId: String): StatementItemEntity = StatementItemEntity(
    id = id,
    statementId = statementId,
    purchaseId = purchaseId,
    description = description,
    sequenceNumber = sequenceNumber,
    totalInstallments = totalInstallments,
    amount = amount,
    cycleMonth = cycleMonth
)
