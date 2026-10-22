package br.com.equilibra.mobile.data.remote.dto.creditcard

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditCardDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun responseDeserializesFromBackendJson() {
        // Shape exato do CreditCardResponse.java (camelCase, BigDecimal como number)
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "name": "Nubank Roxinho",
              "institution": "Nubank",
              "nickname": "Roxinho",
              "totalLimit": 10000.00,
              "committedLimit": 2500.50,
              "reservedLimit": 0.00,
              "availableLimit": 7499.50,
              "closingDay": 25,
              "dueDay": 2,
              "timezone": "America/Sao_Paulo",
              "status": "ACTIVE",
              "createdAt": "2026-10-22T12:00:00Z",
              "updatedAt": "2026-10-22T12:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<CreditCardDto>(raw)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id)
        assertEquals("Nubank Roxinho", dto.name)
        assertEquals("Nubank", dto.institution)
        assertEquals("Roxinho", dto.nickname)
        // BigDecimal number → String sem perda
        assertEquals("10000.00", dto.totalLimit)
        assertEquals("2500.50", dto.committedLimit)
        assertEquals("0.00", dto.reservedLimit)
        assertEquals("7499.50", dto.availableLimit)
        assertEquals(25, dto.closingDay)
        assertEquals(2, dto.dueDay)
        assertEquals("America/Sao_Paulo", dto.timezone)
        assertEquals("ACTIVE", dto.status)
        assertEquals("2026-10-22T12:00:00Z", dto.createdAt)
    }

    @Test
    fun responseDeserializesNullNickname() {
        // Backend: nickname opcional (@Column(length=100), nullable)
        val raw = """
            {
              "id": "id-1",
              "name": "Cartão Inter",
              "institution": "Inter",
              "nickname": null,
              "totalLimit": 5000,
              "committedLimit": 0,
              "reservedLimit": 0,
              "availableLimit": 5000,
              "closingDay": 20,
              "dueDay": 25,
              "timezone": "UTC",
              "status": "INACTIVE",
              "createdAt": "2026-10-22T00:00:00Z",
              "updatedAt": "2026-10-22T00:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<CreditCardDto>(raw)

        assertNull(dto.nickname)
        assertEquals("5000", dto.totalLimit)
        assertEquals("0", dto.committedLimit)
        assertEquals("INACTIVE", dto.status)
    }

    @Test
    fun responseDeserializesMissingNicknameField() {
        // Campo ausente no JSON é aceito (nickname sem valor enviado)
        val raw = """
            {
              "id": "id-2",
              "name": "Livelo",
              "institution": "Bradesco",
              "totalLimit": "1500.75",
              "committedLimit": "1500.75",
              "reservedLimit": "0.00",
              "availableLimit": "0.00",
              "closingDay": 28,
              "dueDay": 5,
              "timezone": "Europe/Lisbon",
              "status": "ACTIVE",
              "createdAt": "2026-10-22T00:00:00Z",
              "updatedAt": "2026-10-22T00:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<CreditCardDto>(raw)

        assertNull(dto.nickname)
        assertEquals("1500.75", dto.totalLimit)
        assertEquals("Europe/Lisbon", dto.timezone)
    }

    @Test
    fun listDeserializes() {
        val raw = """
            [{"id":"1","name":"A","institution":"Bank A","totalLimit":1000.00,"committedLimit":0.00,
              "reservedLimit":0.00,"availableLimit":1000.00,"closingDay":1,"dueDay":10,
              "timezone":"UTC","status":"ACTIVE","createdAt":"2026-10-22T00:00:00Z",
              "updatedAt":"2026-10-22T00:00:00Z"}]
        """.trimIndent()
        val list = json.decodeFromString<List<CreditCardDto>>(raw)
        assertEquals(1, list.size)
        assertEquals("A", list[0].name)
        assertEquals("1000.00", list[0].totalLimit)
    }

    @Test
    fun createRequestSerializes() {
        val req = CreateCreditCardRequestDto(
            name = "Cartão X",
            institution = "Banco X",
            nickname = "X",
            totalLimit = "99.99",
            closingDay = 15,
            dueDay = 20,
            timezone = "America/Sao_Paulo"
        )
        val out = json.encodeToString(CreateCreditCardRequestDto.serializer(), req)
        assertTrue(out.contains("\"name\":\"Cartão X\""))
        assertTrue(out.contains("\"institution\":\"Banco X\""))
        assertTrue(out.contains("\"totalLimit\":\"99.99\""))
        assertTrue(out.contains("\"closingDay\":15"))
        assertTrue(out.contains("\"dueDay\":20"))
        assertTrue(out.contains("\"timezone\":\"America/Sao_Paulo\""))
    }

    @Test
    fun createRequestWithoutOptionalFieldsOmitsDefaults() {
        // encodeDefaults=false (default do kotlinx/serialization): campos com valor
        // default (null) são OMITIDOS no JSON — e o backend aplica seus próprios
        // defaults (nickname → null; timezone ausente → "America/Sao_Paulo",
        // ver CreditCard.validTimezone). Campos obrigatórios seguem presentes.
        val req = CreateCreditCardRequestDto(
            name = "Cartão Y",
            institution = "Banco Y",
            nickname = null,
            totalLimit = "10.00",
            closingDay = 1,
            dueDay = 31,
            timezone = null
        )
        val out = json.encodeToString(CreateCreditCardRequestDto.serializer(), req)
        assertTrue(!out.contains("nickname"))
        assertTrue(!out.contains("timezone"))
        assertTrue(out.contains("\"closingDay\":1"))
        assertTrue(out.contains("\"dueDay\":31"))
    }

    @Test
    fun createRequestWithOptionalFieldsSerializesThem() {
        val req = CreateCreditCardRequestDto(
            name = "Cartão Z",
            institution = "Banco Z",
            nickname = "Z",
            totalLimit = "1.00",
            closingDay = 2,
            dueDay = 3,
            timezone = "UTC"
        )
        val out = json.encodeToString(CreateCreditCardRequestDto.serializer(), req)
        assertTrue(out.contains("\"nickname\":\"Z\""))
        assertTrue(out.contains("\"timezone\":\"UTC\""))
    }

    @Test
    fun updateRequestSerializes() {
        val req = UpdateCreditCardRequestDto(
            name = "Atualizado",
            institution = "Bank B",
            nickname = null,
            totalLimit = "2500.00",
            closingDay = 5,
            dueDay = 12,
            timezone = "UTC"
        )
        val out = json.encodeToString(UpdateCreditCardRequestDto.serializer(), req)
        assertTrue(out.contains("\"name\":\"Atualizado\""))
        assertTrue(out.contains("\"totalLimit\":\"2500.00\""))
        assertTrue(out.contains("\"closingDay\":5"))
    }

    @Test
    fun statusesHavePtBrLabels() {
        assertEquals("Ativo", CreditCardStatusDto.labelPtBr("ACTIVE"))
        assertEquals("Inativo", CreditCardStatusDto.labelPtBr("INACTIVE"))
    }

    @Test
    fun allStatusesContainsTwo() {
        assertEquals(2, CreditCardStatusDto.all.size)
    }
}
