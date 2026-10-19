package br.com.equilibra.mobile.data.remote.dto.tag

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun responseDeserializesFromBackendJson() {
        // Contrato real: TagResponse(id, name, active, createdAt, updatedAt)
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "name": "URGENTE",
              "active": true,
              "createdAt": "2024-01-01T12:00:00Z",
              "updatedAt": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<TagDto>(raw)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id)
        assertEquals("URGENTE", dto.name)
        assertTrue(dto.active)
    }

    @Test
    fun responseIgnoraCamposDesconhecidos() {
        // Backend pode acrescentar campos (ex.: version) sem quebrar o cliente
        val raw = """
            {"id":"1","name":"A","active":false,
             "createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z",
             "version":3,"ownerId":"x"}
        """.trimIndent()

        val dto = json.decodeFromString<TagDto>(raw)
        assertEquals(false, dto.active)
    }

    @Test
    fun listDeserializes() {
        val raw = """[
            {"id":"1","name":"A","active":true,"createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"},
            {"id":"2","name":"B","active":false,"createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}
        ]"""
        val list = json.decodeFromString<List<TagDto>>(raw)
        assertEquals(2, list.size)
        assertFalse(list[1].active)
    }

    @Test
    fun createRequestSerializaApenasName() {
        // CreateTagRequest do backend: apenas {name} (max 100)
        val req = CreateTagRequestDto(name = "URGENTE")
        val out = json.encodeToString(CreateTagRequestDto.serializer(), req)

        assertTrue(out.contains("\"name\":\"URGENTE\""))
        assertEquals(setOf("name"), json.parseToJsonElement(out).jsonObjectKeys())
    }

    @Test
    fun updateRequestSerializaApenasName() {
        val req = UpdateTagRequestDto(name = "RENAMEADA")
        val out = json.encodeToString(UpdateTagRequestDto.serializer(), req)

        assertTrue(out.contains("\"name\":\"RENAMEADA\""))
        assertEquals(setOf("name"), json.parseToJsonElement(out).jsonObjectKeys())
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectKeys(): Set<String> =
        (this as kotlinx.serialization.json.JsonObject).keys
}
