package br.com.equilibra.mobile.data.remote.dto.transaction

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    // ---------- Expense ----------

    @Test
    fun expenseResponseDeserializesFromBackendJson() {
        // ExpenseResponse real: amount como JSON number, campos de cartão null
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "description": "Mercado",
              "occurredAt": "2026-10-01T12:00:00Z",
              "accountId": "acc-1",
              "amount": 150.90,
              "paymentMethod": "BANK_ACCOUNT",
              "creditCardId": null,
              "installmentCount": null,
              "cycleMonth": null,
              "purchaseAt": null,
              "categoryId": "cat-1",
              "notes": "compra do mês",
              "status": "ACTIVE",
              "createdAt": "2026-10-01T12:00:01Z",
              "updatedAt": "2026-10-01T12:00:01Z",
              "tags": [{"id": "tag-1", "name": "URGENTE", "active": true}]
            }
        """.trimIndent()

        val dto = json.decodeFromString<ExpenseDto>(raw)

        assertEquals("Mercado", dto.description)
        assertEquals("acc-1", dto.accountId)
        assertEquals("150.90", dto.amount)
        assertEquals("cat-1", dto.categoryId)
        assertEquals("ACTIVE", dto.status)
        assertNull(dto.creditCardId)
        assertEquals(1, dto.tags.size)
        assertEquals("URGENTE", dto.tags[0].name)
    }

    @Test
    fun expenseAmountAceitaTantoNumberQuantoString() {
        val base = """
            {"id":"1","description":"D","occurredAt":"2026-10-01T00:00:00Z",
             "accountId":"a","amount":%s,"categoryId":"c","status":"ACTIVE",
             "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z","tags":[]}
        """

        assertEquals("1234567890.123456789",
            json.decodeFromString<ExpenseDto>(base.format("\"1234567890.123456789\"")).amount)
        assertEquals("0.1",
            json.decodeFromString<ExpenseDto>(base.format("0.1")).amount)
    }

    @Test
    fun expensePageDeserializes() {
        val raw = """
            {
              "content": [
                {"id":"1","description":"A","occurredAt":"2026-10-01T00:00:00Z",
                 "accountId":"a","amount":10.5,"categoryId":"c","status":"ACTIVE",
                 "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z","tags":[]}
              ],
              "page": 0, "size": 20, "totalElements": 42, "totalPages": 3
            }
        """.trimIndent()

        val page = json.decodeFromString<ExpensePageResponseDto>(raw)

        assertEquals(1, page.content.size)
        assertEquals(0, page.page)
        assertEquals(20, page.size)
        assertEquals(42L, page.totalElements)
        assertEquals(3, page.totalPages)
    }

    @Test
    fun createExpenseRequestSerializaContrato() {
        val req = CreateExpenseRequestDto(
            description = "Mercado",
            occurredAt = "2026-10-01T12:00:00Z",
            accountId = "acc-1",
            amount = "150.90",
            categoryId = "cat-1",
            notes = "obs",
            tagIds = listOf("t1", "t2")
        )
        val out = json.encodeToString(CreateExpenseRequestDto.serializer(), req)

        assertTrue(out.contains("\"description\":\"Mercado\""))
        assertTrue(out.contains("\"occurredAt\":\"2026-10-01T12:00:00Z\""))
        assertTrue(out.contains("\"accountId\":\"acc-1\""))
        assertTrue(out.contains("\"categoryId\":\"cat-1\""))
        assertTrue(out.contains("\"tagIds\":[\"t1\",\"t2\"]"))
        // BigDecimal serializado como string escala pelo Spring para BigDecimal
        assertTrue(out.contains("\"amount\":\"150.90\""))
    }

    @Test
    fun updateExpenseRequestTemMesmoShapeDoCreate() {
        val req = UpdateExpenseRequestDto(
            description = "Mercado", occurredAt = "2026-10-01T12:00:00Z",
            accountId = "acc-1", amount = "10.00", categoryId = "cat-1"
        )
        val out = json.encodeToString(UpdateExpenseRequestDto.serializer(), req)
        assertTrue(out.contains("\"amount\":\"10.00\""))
        assertTrue(out.contains("\"accountId\":\"acc-1\""))
    }

    // ---------- Income ----------

    @Test
    fun incomeResponseDeserializesFromBackendJson() {
        val raw = """
            {
              "id": "i1", "description": "Salário", "occurredAt": "2026-10-05T00:00:00Z",
              "accountId": "acc-2", "amount": 5000, "categoryId": "cat-in",
              "notes": null, "status": "CANCELLED",
              "createdAt": "2026-10-05T00:00:00Z", "updatedAt": "2026-10-05T00:00:00Z",
              "tags": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<IncomeDto>(raw)

        assertEquals("5000", dto.amount)
        assertEquals("CANCELLED", dto.status)
        assertNull(dto.notes)
    }

    @Test
    fun createIncomeRequestSerializaContrato() {
        val req = CreateIncomeRequestDto(
            description = "Salário", occurredAt = "2026-10-05T00:00:00Z",
            accountId = "acc-2", amount = "5000.00", categoryId = "cat-in"
        )
        val out = json.encodeToString(CreateIncomeRequestDto.serializer(), req)
        assertTrue(out.contains("\"accountId\":\"acc-2\""))
        assertTrue(out.contains("\"amount\":\"5000.00\""))
    }

    @Test
    fun incomePageDeserializes() {
        val raw = """{"content":[],"page":2,"size":10,"totalElements":0,"totalPages":0}"""
        val page = json.decodeFromString<IncomePageResponseDto>(raw)
        assertEquals(0, page.content.size)
        assertEquals(2, page.page)
        assertEquals(0L, page.totalElements)
    }

    // ---------- Transfer ----------

    @Test
    fun transferResponseDeserializesFromBackendJson() {
        val raw = """
            {
              "id": "tr1", "description": "Poupança", "occurredAt": "2026-10-10T00:00:00Z",
              "sourceAccountId": "acc-1", "destinationAccountId": "acc-2",
              "amount": 250.75, "categoryId": "cat-t", "notes": null,
              "status": "ACTIVE",
              "createdAt": "2026-10-10T00:00:00Z", "updatedAt": "2026-10-10T00:00:00Z",
              "tags": [{"id":"t1","name":"Invest","active":true}]
            }
        """.trimIndent()

        val dto = json.decodeFromString<TransferDto>(raw)

        assertEquals("acc-1", dto.sourceAccountId)
        assertEquals("acc-2", dto.destinationAccountId)
        assertEquals("250.75", dto.amount)
        assertEquals(1, dto.tags.size)
    }

    @Test
    fun createTransferRequestSerializaOrigemEDestino() {
        val req = CreateTransferRequestDto(
            description = "Poupança", occurredAt = "2026-10-10T00:00:00Z",
            sourceAccountId = "acc-1", destinationAccountId = "acc-2",
            amount = "250.75", categoryId = "cat-t"
        )
        val out = json.encodeToString(CreateTransferRequestDto.serializer(), req)

        assertTrue(out.contains("\"sourceAccountId\":\"acc-1\""))
        assertTrue(out.contains("\"destinationAccountId\":\"acc-2\""))
        // Transferência não vira despesa+receita: um único payload
        assertTrue(!out.contains("\"accountId\""))
    }

    @Test
    fun transferPageDeserializes() {
        val raw = """{"content":[],"page":0,"size":20,"totalElements":7,"totalPages":1}"""
        val page = json.decodeFromString<TransferPageResponseDto>(raw)
        assertEquals(7L, page.totalElements)
        assertEquals(1, page.totalPages)
    }

    // ---------- Erros ----------

    @Test(expected = SerializationException::class)
    fun expenseSemCamposObrigatoriosLancaErro() {
        json.decodeFromString<ExpenseDto>("""{"id":"1"}""")
    }

    @Test
    fun camposDesconhecidosSaoIgnorados() {
        // Backend pode evoluir o response (ex.: novos campos de cartão)
        val raw = """
            {"id":"1","description":"D","occurredAt":"2026-10-01T00:00:00Z",
             "accountId":"a","amount":10,"categoryId":"c","status":"ACTIVE",
             "createdAt":"2026-10-01T00:00:00Z","updatedAt":"2026-10-01T00:00:00Z","tags":[],
             "novoCampoFuturo": true}
        """.trimIndent()
        val dto = json.decodeFromString<ExpenseDto>(raw)
        assertEquals("D", dto.description)
    }

    @Test
    fun tagSummaryECompartilhadoEntreTipos() {
        val raw = """[{"id":"1","name":"A","active":true},{"id":"2","name":"B","active":false}]"""
        val tags = json.decodeFromString<List<TagSummaryDto>>(raw)
        assertEquals(2, tags.size)
        assertTrue(!tags[1].active)
    }
}
