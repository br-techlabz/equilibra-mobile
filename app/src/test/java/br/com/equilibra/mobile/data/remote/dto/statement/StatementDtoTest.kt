package br.com.equilibra.mobile.data.remote.dto.statement

import br.com.equilibra.mobile.data.remote.dto.BigDecimalStringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 7.1 — Contrato real de faturas de cartão.
 *
 * Backend (CreditCardStatementController):
 * - GET  /credit-cards/{cardId}/current-statement → CreditCardStatementResponse
 * - GET  /credit-cards/{cardId}/statements        → List<CreditCardStatementResponse>
 * - GET  /credit-card-statements/{id}             → CreditCardStatementResponse
 * - POST /credit-cards/{cardId}/statements/close  → CreditCardStatementResponse
 *
 * Shape de CreditCardStatementResponse.java:
 * id, creditCardId, cycleStart (LocalDate), cycleEnd (LocalDate),
 * closingAt (Instant), dueDate (LocalDate), status (OPEN|CLOSED|OVERDUE|
 * PARTIALLY_PAID|PAID|CANCELLED), totalAmount/paidAmount/outstandingAmount
 * (BigDecimal) e items[{id, purchaseId, description, sequenceNumber,
 * totalInstallments, amount (BigDecimal), cycleMonth (LocalDate)}].
 */
class StatementDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Resposta no shape real: valores monetários chegam como number no JSON. */
    private val raw = """
        {
          "id": "s-1",
          "creditCardId": "c-1",
          "cycleStart": "2026-10-03",
          "cycleEnd": "2026-11-02",
          "closingAt": "2026-11-03T03:00:00Z",
          "dueDate": "2026-11-10",
          "status": "PARTIALLY_PAID",
          "totalAmount": 1234.56,
          "paidAmount": 200.00,
          "outstandingAmount": 1034.56,
          "items": [
            {"id": "i-1", "purchaseId": "p-1", "description": "Notebook", "sequenceNumber": 1, "totalInstallments": 3, "amount": 411.52, "cycleMonth": "2026-11-01"},
            {"id": "i-2", "purchaseId": "p-1", "description": "Notebook", "sequenceNumber": 2, "totalInstallments": 3, "amount": 411.52, "cycleMonth": "2026-11-01"},
            {"id": "i-3", "purchaseId": "p-1", "description": "Notebook", "sequenceNumber": 3, "totalInstallments": 3, "amount": 411.52, "cycleMonth": "2026-11-01"}
          ]
        }
    """.trimIndent()

    @Test
    fun responseDeserializaContratoReal() {
        val dto = json.decodeFromString<StatementDto>(raw)

        assertEquals("s-1", dto.id)
        assertEquals("c-1", dto.creditCardId)
        assertEquals("2026-10-03", dto.cycleStart)
        assertEquals("2026-11-02", dto.cycleEnd)
        assertEquals("2026-11-03T03:00:00Z", dto.closingAt)
        assertEquals("2026-11-10", dto.dueDate)
        assertEquals("PARTIALLY_PAID", dto.status)
        assertEquals("1234.56", dto.totalAmount)
        assertEquals("200.00", dto.paidAmount)
        assertEquals("1034.56", dto.outstandingAmount)
        assertEquals(3, dto.items.size)
    }

    @Test
    fun valoresMonetariosPreservamCentavosSemDouble() {
        val dto = json.decodeFromString<StatementDto>(raw)

        // String exata do backend — nenhum Double/Float intermediário.
        assertEquals("1234.56", dto.totalAmount)
        assertEquals("200.00", dto.paidAmount)
        assertEquals("1034.56", dto.outstandingAmount)

        // centavos fecham com o total sem criar/perder 1 centavo
        val somaItens = dto.items.sumOf { it.amount.toBigDecimal() }
        assertEquals(0, somaItens.compareTo(dto.totalAmount.toBigDecimal()))
        assertEquals(0, dto.paidAmount.toBigDecimal().add(dto.outstandingAmount.toBigDecimal())
            .compareTo(dto.totalAmount.toBigDecimal()))
    }

    @Test
    fun itensDaFaturaPreservamValoresEMetadados() {
        val dto = json.decodeFromString<StatementDto>(raw)

        val primeiro = dto.items.first()
        assertEquals("i-1", primeiro.id)
        assertEquals("p-1", primeiro.purchaseId)
        assertEquals("Notebook", primeiro.description)
        assertEquals(1, primeiro.sequenceNumber)
        assertEquals(3, primeiro.totalInstallments)
        assertEquals("411.52", primeiro.amount)
        assertEquals("2026-11-01", primeiro.cycleMonth)
    }

    @Test
    fun aceitaValoresMonetariosComoString() {
        val comStrings = """
            {
              "id": "s-2", "creditCardId": "c-1",
              "cycleStart": "2026-10-03", "cycleEnd": "2026-11-02",
              "closingAt": "2026-11-03T03:00:00Z", "dueDate": "2026-11-10",
              "status": "OPEN",
              "totalAmount": "250.10", "paidAmount": "0.00", "outstandingAmount": "250.10",
              "items": [
                {"id": "i-1", "purchaseId": "p-1", "description": "Cadeira", "sequenceNumber": 1, "totalInstallments": 1, "amount": "250.10", "cycleMonth": "2026-10-01"}
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<StatementDto>(comStrings)

        assertEquals("250.10", dto.totalAmount)
        assertEquals("0.00", dto.paidAmount)
        assertEquals("250.10", dto.outstandingAmount)
        assertEquals("250.10", dto.items.single().amount)
    }

    @Test
    fun statusCobreTodosOsValoresDoBackend() {
        val statuses = listOf(
            "OPEN", "CLOSED", "OVERDUE", "PARTIALLY_PAID", "PAID", "CANCELLED"
        )

        statuses.forEach { status ->
            val dto = json.decodeFromString<StatementDto>(statementJson(status))
            assertEquals(status, dto.status)
        }

        assertEquals(statuses.toSet(), StatementStatusDto.all.toSet())
        statuses.forEach { assertTrue(StatementStatusDto.labelPtBr(it).isNotBlank()) }
        assertEquals("Paga", StatementStatusDto.labelPtBr("PAID"))
        assertEquals("Parcialmente paga", StatementStatusDto.labelPtBr("PARTIALLY_PAID"))
        assertEquals("CANCELLED", StatementStatusDto.CANCELLED)
    }

    @Test
    fun closeRequestSerializaCycleMonth() {
        val body = CloseStatementRequestDto(cycleMonth = "2026-11-01")

        val obj = json.encodeToJsonElement(body)

        assertEquals(setOf("cycleMonth"), obj.jsonObject.keys)
        assertEquals("2026-11-01", obj.jsonObject["cycleMonth"]!!.jsonPrimitive.content)
    }

    @Test
    fun serializadorAceitaNumeroEString() {
        assertEquals("19.99", json.decodeFromString(BigDecimalStringSerializer, "19.99"))
        assertEquals("19.99", json.decodeFromString(BigDecimalStringSerializer, "\"19.99\""))
    }

    private fun statementJson(status: String) = """
        {
          "id": "s-1", "creditCardId": "c-1",
          "cycleStart": "2026-10-03", "cycleEnd": "2026-11-02",
          "closingAt": "2026-11-03T03:00:00Z", "dueDate": "2026-11-10",
          "status": "$status",
          "totalAmount": 10.00, "paidAmount": 0.00, "outstandingAmount": 10.00,
          "items": []
        }
    """.trimIndent()
}
