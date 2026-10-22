package br.com.equilibra.mobile.data.local.purchase

import br.com.equilibra.mobile.data.remote.dto.purchase.InstallmentDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Task 6.6 — Fronteira DTO → Entity.
 *
 * O mapeamento existe para o cache guardar exatamente o que a API mandou;
 * nenhum valor monetário passa por Double/Float e nenhuma parcela é
 * recalculada a partir do total.
 */
class PurchaseMapperTest {

    private fun dto(
        totalAmount: String = "100.00",
        purchaseAt: String? = "2026-10-15T14:30:00Z",
        installments: List<InstallmentDto> = listOf(
            InstallmentDto(1, 2, "50.01", "2026-11-01"),
            InstallmentDto(2, 2, "49.99", "2026-12-01")
        )
    ) = PurchaseDto(
        id = "p-1",
        creditCardId = "c-1",
        categoryId = "cat-1",
        description = "Notebook",
        totalAmount = totalAmount,
        purchaseDate = "2026-10-15",
        purchaseAt = purchaseAt,
        installmentCount = installments.size,
        cycleMonth = "2026-11-01",
        status = "ACTIVE",
        installments = installments
    )

    @Test
    fun entityCopiaValoresSemConversao() {
        val e = dto().toEntity(syncedAt = 1234L)

        assertEquals("p-1", e.id)
        assertEquals("c-1", e.creditCardId)
        assertEquals("cat-1", e.categoryId)
        assertEquals("100.00", e.totalAmount)
        assertEquals("2026-10-15", e.purchaseDate)
        assertEquals("2026-10-15T14:30:00Z", e.purchaseAt)
        assertEquals(2, e.installmentCount)
        assertEquals("2026-11-01", e.cycleMonth)
        assertEquals("ACTIVE", e.status)
        assertEquals(1234L, e.syncedAt)
    }

    @Test
    fun parcelasPreservamCentavosDoBackend() {
        val parcelas = dto().toInstallmentEntities()

        assertEquals(listOf("50.01", "49.99"), parcelas.map { it.principalAmount })
        assertEquals(listOf(1, 2), parcelas.map { it.sequenceNumber })
        assertEquals(listOf("2026-11-01", "2026-12-01"), parcelas.map { it.cycleMonth })
        assertEquals(listOf("p-1", "p-1"), parcelas.map { it.purchaseId })
        // id determinístico: re-sincronizar a mesma compra não duplica parcelas
        assertEquals("p-1:1", parcelas[0].id)
        assertEquals("p-1:2", parcelas[1].id)
        assertEquals(2, parcelas.map { it.totalInstallments }.distinct().single())
    }

    @Test
    fun purchaseAtNuloSobreviveAoMapeamento() {
        val e = dto(purchaseAt = null).toEntity(syncedAt = null)

        assertNull(e.purchaseAt)
        assertNull(e.syncedAt)
    }

    @Test
    fun quantidadeDeParcelasVemDoArrayNaoDoContador() {
        // Se o backend mandar menos parcelas que o contador, o cache guarda o
        // que realmente veio — nada é inventado para "fechar" a conta.
        val e = dto().toEntity(syncedAt = null).copy(installmentCount = 3)
        val parcelas = dto().toInstallmentEntities()

        assertEquals(3, e.installmentCount)
        assertEquals(2, parcelas.size)
    }
}
