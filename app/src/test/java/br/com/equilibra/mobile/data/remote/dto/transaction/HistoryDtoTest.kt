package br.com.equilibra.mobile.data.remote.dto.transaction

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun historyResponseDeserializesComTipoStatusTagsEAttachments() {
        // TransactionHistoryResponse real: shape único com type/status/attachmentCount
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "type": "TRANSFER",
              "status": "CANCELLED",
              "description": "Poupança",
              "occurredAt": "2026-10-10T00:00:00Z",
              "amount": 250.75,
              "categoryId": "cat-1",
              "sourceAccountId": "acc-1",
              "destinationAccountId": "acc-2",
              "notes": null,
              "createdAt": "2026-10-10T00:00:00Z",
              "updatedAt": "2026-10-10T00:00:00Z",
              "tags": [{"id": "t1", "name": "Invest", "active": true}],
              "attachmentCount": 2
            }
        """.trimIndent()

        val dto = json.decodeFromString<HistoryItemDto>(raw)

        assertEquals("TRANSFER", dto.type)
        assertEquals("CANCELLED", dto.status)
        assertEquals("250.75", dto.amount)
        assertEquals("acc-1", dto.sourceAccountId)
        assertEquals("acc-2", dto.destinationAccountId)
        assertEquals(1, dto.tags.size)
        assertEquals(2, dto.attachmentCount)
    }

    @Test
    fun historyPageDeserializes() {
        val raw = """
            {"content": [], "page": 1, "size": 20, "totalElements": 57, "totalPages": 3}
        """.trimIndent()

        val page = json.decodeFromString<HistoryPageResponseDto>(raw)

        assertEquals(1, page.page)
        assertEquals(20, page.size)
        assertEquals(57L, page.totalElements)
        assertEquals(3, page.totalPages)
    }

    @Test
    fun historyDeserializaOsQuatroTipos() {
        val base = """
            {"id":"1","type":"%s","status":"ACTIVE","description":"D",
             "occurredAt":"2026-10-01T00:00:00Z","amount":1.00,
             "categoryId":null,"sourceAccountId":null,"destinationAccountId":null,
             "notes":null,"createdAt":"2026-10-01T00:00:00Z",
             "updatedAt":"2026-10-01T00:00:00Z","tags":[],"attachmentCount":0}
        """.trimIndent()

        listOf("EXPENSE", "INCOME", "TRANSFER", "STATEMENT_PAYMENT").forEach { type ->
            assertEquals(type, json.decodeFromString<HistoryItemDto>(base.format(type)).type)
        }
    }

    @Test(expected = SerializationException::class)
    fun semCamposObrigatoriosLancaErro() {
        json.decodeFromString<HistoryItemDto>("""{"id":"1"}""")
    }

    @Test
    fun camposDesconhecidosIgnorados() {
        val raw = """
            {"id":"1","type":"EXPENSE","status":"ACTIVE","description":"D",
             "occurredAt":"2026-10-01T00:00:00Z","amount":1,
             "categoryId":null,"sourceAccountId":null,"destinationAccountId":null,
             "notes":null,"createdAt":"2026-10-01T00:00:00Z",
             "updatedAt":"2026-10-01T00:00:00Z","tags":[],"attachmentCount":0,
             "campoFuturo": {"x": 1}}
        """.trimIndent()
        assertTrue(json.decodeFromString<HistoryItemDto>(raw).description.isNotEmpty())
    }
}
