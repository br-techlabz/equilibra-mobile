package br.com.equilibra.mobile.ui.purchases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Task 6.8 — Validação do formulário de compra.
 *
 * Regras vêm do contrato real (CreateCreditCardPurchaseRequest.java):
 * - description @NotBlank @Size(max = 255)
 * - purchaseDate @NotNull
 * - totalAmount @NotNull @DecimalMin("0.01") (2 casas, como todo money do backend)
 * - installmentCount @NotNull @Min(1) @Max(60)
 * - creditCardId/categoryId @NotBlank
 */
class PurchaseFormValidatorTest {

    @Test
    fun descricaoObrigatoriaEAte255() {
        assertEquals("Informe a descrição", PurchaseFormValidator.validateDescription("   "))
        assertNull(PurchaseFormValidator.validateDescription("Notebook"))
        assertNull(PurchaseFormValidator.validateDescription("a".repeat(255)))
        assertNotNull(PurchaseFormValidator.validateDescription("a".repeat(256)))
    }

    @Test
    fun valorPrecisaSerPositivoComAteDuasCasas() {
        assertEquals("Informe o valor", PurchaseFormValidator.validateAmount(""))
        assertEquals("Valor deve ser maior que zero", PurchaseFormValidator.validateAmount("0,00"))
        assertEquals("Valor deve ser maior que zero", PurchaseFormValidator.validateAmount("-10,00"))
        assertEquals("Valor inválido", PurchaseFormValidator.validateAmount("abc"))
        assertNotNull(PurchaseFormValidator.validateAmount("10,999"))
        assertNull(PurchaseFormValidator.validateAmount("0,01"))
        assertNull(PurchaseFormValidator.validateAmount("1.234,56"))
    }

    @Test
    fun normalizaValorPtBrPreservandoCentavos() {
        assertEquals("1234.56", PurchaseFormValidator.normalizeAmount("1.234,56"))
        assertEquals("10.5", PurchaseFormValidator.normalizeAmount("10,5"))
        assertEquals("10.00", PurchaseFormValidator.normalizeAmount("10.00"))
    }

    @Test
    fun parcelasVaoDe1a60() {
        assertEquals("Informe o número de parcelas", PurchaseFormValidator.validateInstallments(""))
        assertEquals("Parcelas devem ser entre 1 e 60", PurchaseFormValidator.validateInstallments("0"))
        assertEquals("Parcelas devem ser entre 1 e 60", PurchaseFormValidator.validateInstallments("61"))
        assertEquals("Número de parcelas inválido", PurchaseFormValidator.validateInstallments("abc"))
        assertNull(PurchaseFormValidator.validateInstallments("1"))
        assertNull(PurchaseFormValidator.validateInstallments("60"))
    }

    @Test
    fun dataUsaFormatoDdMMAaaaEEhReal() {
        assertEquals("Informe a data", PurchaseFormValidator.validateDate(""))
        assertEquals("Data inválida (use dd/MM/aaaa)", PurchaseFormValidator.validateDate("31/02/2026"))
        assertEquals("Data inválida (use dd/MM/aaaa)", PurchaseFormValidator.validateDate("2026-10-15"))
        assertNull(PurchaseFormValidator.validateDate("15/10/2026"))
    }

    @Test
    fun categoriaECartaoSaoObrigatorios() {
        assertEquals("Selecione a categoria", PurchaseFormValidator.validateCategory(null))
        assertEquals("Selecione a categoria", PurchaseFormValidator.validateCategory(""))
        assertNull(PurchaseFormValidator.validateCategory("cat-1"))
        assertEquals("Cartão não informado", PurchaseFormValidator.validateCard(null))
        assertNull(PurchaseFormValidator.validateCard("c-1"))
    }

    @Test
    fun dataViraLocalDateIsoEInstantNoInicioDoDiaUtc() {
        val localDate = PurchaseFormValidator.toLocalDateIso("15/10/2026")
        val instant = PurchaseFormValidator.toPurchaseAtIso("15/10/2026")

        assertEquals("2026-10-15", localDate)
        assertEquals("2026-10-15T00:00:00Z", instant)
        assertNull(PurchaseFormValidator.toLocalDateIso("31/02/2026"))
        assertNull(PurchaseFormValidator.toPurchaseAtIso("31/02/2026"))
    }
}
