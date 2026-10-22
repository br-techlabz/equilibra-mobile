package br.com.equilibra.mobile.data.local.purchase

import br.com.equilibra.mobile.data.remote.dto.purchase.InstallmentDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto

/**
 * Task 6.6 — DTO → Entity.
 *
 * Cópia literal de valores: nada de conversão monetária (a String já vem
 * precisa do [br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer])
 * e nada de recálculo de parcelas.
 */

fun PurchaseDto.toEntity(syncedAt: Long?): PurchaseEntity = PurchaseEntity(
    id = id,
    creditCardId = creditCardId,
    categoryId = categoryId,
    description = description,
    totalAmount = totalAmount,
    purchaseDate = purchaseDate,
    purchaseAt = purchaseAt,
    installmentCount = installmentCount,
    cycleMonth = cycleMonth,
    status = status,
    createdAt = purchaseAt ?: purchaseDate,
    updatedAt = purchaseAt ?: purchaseDate,
    syncedAt = syncedAt
)

/**
 * Id determinístico `<purchaseId>:<sequenceNumber>`: re-sincronizar a mesma
 * compra sobrescreve as parcelas em vez de duplicá-las.
 */
fun PurchaseDto.toInstallmentEntities(): List<InstallmentEntity> =
    installments.map { it.toEntity(purchaseId = id) }

fun InstallmentDto.toEntity(purchaseId: String): InstallmentEntity = InstallmentEntity(
    id = "$purchaseId:$sequenceNumber",
    purchaseId = purchaseId,
    sequenceNumber = sequenceNumber,
    totalInstallments = totalInstallments,
    principalAmount = principalAmount,
    cycleMonth = cycleMonth
)
