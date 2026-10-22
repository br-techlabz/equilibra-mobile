package br.com.equilibra.mobile.data.remote.dto.purchase

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 6.6 — Contrato real de compras de cartão.
 *
 * Backend (CreditCardPurchaseController, /credit-card-purchases):
 * - POST /                          → 201 CreditCardPurchaseResponse
 * - GET  /card/{cardId}             → 200 List<CreditCardPurchaseResponse>
 * - GET  /{id}                      → 200 CreditCardPurchaseResponse
 * - POST /{id}/cancel               → 200 CreditCardPurchaseResponse
 *
 * Shape de CreditCardPurchaseResponse.java:
 * id, creditCardId, categoryId, description, totalAmount (BigDecimal),
 * purchaseDate (LocalDate), purchaseAt (Instant, nullable), installmentCount,
 * cycleMonth (LocalDate), status (ACTIVE|REVERSED|CANCELLED),
 * installments[{sequenceNumber, totalInstallments, principalAmount, cycleMonth}].
 */
class PurchaseDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Resposta real do backend: 100,00 em 3x → 33,34 + 33,33 + 33,33 (resto nas primeiras). */
    private val raw = """
        {
          "id": "p-1",
          "creditCardId": "c-1",
          "categoryId": "cat-1",
          "description": "Notebook",
          "totalAmount": 100.00,
          "purchaseDate": "2026-10-15",
          "purchaseAt": "2026-10-15T14:30:00Z",
          "installmentCount": 3,
          "cycleMonth": "2026-11-01",
          "status": "ACTIVE",
          "installments": [
            {"sequenceNumber": 1, "totalInstallments": 3, "principalAmount": 33.34, "cycleMonth": "2026-11-01"},
            {"sequenceNumber": 2, "totalInstallments": 3, "principalAmount": 33.33, "cycleMonth": "2026-12-01"},
            {"sequenceNumber": 3, "totalInstallments": 3, "principalAmount": 33.33, "cycleMonth": "2027-01-01"}
          ]
        }
    """.trimIndent()

    @Test
    fun responseDeserializesFromBackendJson() {
        val dto = json.decodeFromString<PurchaseDto>(raw)

        assertEquals("p-1", dto.id)
        assertEquals("c-1", dto.creditCardId)
        assertEquals("cat-1", dto.categoryId)
        assertEquals("Notebook", dto.description)
        assertEquals("100.00", dto.totalAmount)
        assertEquals("2026-10-15", dto.purchaseDate)
        assertEquals("2026-10-15T14:30:00Z", dto.purchaseAt)
        assertEquals(3, dto.installmentCount)
        assertEquals("2026-11-01", dto.cycleMonth)
        assertEquals("ACTIVE", dto.status)
    }

    @Test
    fun installmentsPreservamCentavosSemPerda() {
        val dto = json.decodeFromString<PurchaseDto>(raw)

        assertEquals(3, dto.installments.size)
        assertEquals("33.34", dto.installments[0].principalAmount)
        assertEquals("33.33", dto.installments[1].principalAmount)
        assertEquals("33.33", dto.installments[2].principalAmount)
        // soma exata fecha o total (nenhum centavo criado ou perdido)
        val soma = dto.installments.sumOf { it.principalAmount.toBigDecimal() }
        assertEquals(0, soma.compareTo(dto.totalAmount.toBigDecimal()))
    }

    @Test
    fun installmentCarregaSequenciaTotalECiclo() {
        val dto = json.decodeFromString<PurchaseDto>(raw)

        val segunda = dto.installments[1]
        assertEquals(2, segunda.sequenceNumber)
        assertEquals(3, segunda.totalInstallments)
        assertEquals("2026-12-01", segunda.cycleMonth)
    }

    @Test
    fun aceitaValoresMonetariosComoString() {
        val comStrings = """
            {
              "id": "p-2", "creditCardId": "c-1", "categoryId": "cat-1",
              "description": "Cadeira", "totalAmount": "250.10",
              "purchaseDate": "2026-10-20", "purchaseAt": null,
              "installmentCount": 1, "cycleMonth": "2026-10-01",
              "status": "ACTIVE",
              "installments": [
                {"sequenceNumber": 1, "totalInstallments": 1, "principalAmount": "250.10", "cycleMonth": "2026-10-01"}
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<PurchaseDto>(comStrings)

        assertEquals("250.10", dto.totalAmount)
        assertEquals("250.10", dto.installments.single().principalAmount)
        assertNull(dto.purchaseAt)
    }

    @Test
    fun statusCobreTodosOsValoresDoBackend() {
        val statuses = json.decodeFromString<List<PurchaseDto>>(
            """[
              {"id":"1","creditCardId":"c","categoryId":"k","description":"a","totalAmount":10.00,
               "purchaseDate":"2026-10-15","purchaseAt":"2026-10-15T10:00:00Z","installmentCount":1,
               "cycleMonth":"2026-10-01","status":"ACTIVE","installments":[]},
              {"id":"2","creditCardId":"c","categoryId":"k","description":"b","totalAmount":10.00,
               "purchaseDate":"2026-10-15","purchaseAt":"2026-10-15T10:00:00Z","installmentCount":1,
               "cycleMonth":"2026-10-01","status":"REVERSED","installments":[]},
              {"id":"3","creditCardId":"c","categoryId":"k","description":"c","totalAmount":10.00,
               "purchaseDate":"2026-10-15","purchaseAt":"2026-10-15T10:00:00Z","installmentCount":1,
               "cycleMonth":"2026-10-01","status":"CANCELLED","installments":[]}
            ]"""
        )

        assertEquals(listOf("ACTIVE", "REVERSED", "CANCELLED"), statuses.map { it.status })
        assertEquals(setOf("ACTIVE", "REVERSED", "CANCELLED"), PurchaseStatusDto.all.toSet())
        assertTrue(PurchaseStatusDto.labelPtBr("CANCELLED").isNotBlank())
        assertEquals("CANCELLED", PurchaseStatusDto.CANCELLED)
    }

    @Test
    fun requestSerializaShapeDoBackend() {
        val body = CreatePurchaseRequestDto(
            description = "Notebook",
            purchaseDate = "2026-10-15",
            purchaseAt = "2026-10-15T14:30:00Z",
            totalAmount = "100.00",
            installmentCount = 3,
            creditCardId = "c-1",
            categoryId = "cat-1"
        )

        val obj = json.encodeToJsonElement(body)

        assertEquals(
            setOf("description", "purchaseDate", "purchaseAt", "totalAmount", "installmentCount", "creditCardId", "categoryId"),
            obj.jsonObject.keys
        )
        assertEquals("c-1", obj.jsonObject["creditCardId"]!!.jsonPrimitive.content)
        assertEquals("3", obj.jsonObject["installmentCount"]!!.jsonPrimitive.content)
    }

    @Test
    fun serializadorAceitaNumeroEString() {
        assertEquals("19.99", json.decodeFromString(BigDecimalStringSerializer, "19.99"))
        assertEquals("19.99", json.decodeFromString(BigDecimalStringSerializer, "\"19.99\""))
    }
}
