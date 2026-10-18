package br.com.equilibra.mobile.data.remote.dto.category

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun responseDeserializesFromBackendJson() {
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "name": "Alimentação",
              "applicability": "EXPENSE",
              "active": true,
              "createdAt": "2024-01-01T12:00:00Z",
              "updatedAt": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<CategoryDto>(raw)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id)
        assertEquals("Alimentação", dto.name)
        assertEquals("EXPENSE", dto.applicability)
        assertTrue(dto.active)
    }

    @Test
    fun listDeserializes() {
        val raw = """[
            {"id":"1","name":"A","applicability":"EXPENSE","active":true,"createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"},
            {"id":"2","name":"B","applicability":"BOTH","active":false,"createdAt":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z"}
        ]"""
        val list = json.decodeFromString<List<CategoryDto>>(raw)
        assertEquals(2, list.size)
        assertEquals("BOTH", list[1].applicability)
        assertEquals(false, list[1].active)
    }

    @Test
    fun createRequestSerializes() {
        val req = CreateCategoryRequestDto(name = "Lazer", applicability = "BOTH")
        val out = json.encodeToString(CreateCategoryRequestDto.serializer(), req)
        assertTrue(out.contains("\"name\":\"Lazer\""))
        assertTrue(out.contains("\"applicability\":\"BOTH\""))
    }

    @Test
    fun updateRequestSerializes() {
        val req = UpdateCategoryRequestDto(name = "Renomeada", applicability = "INCOME")
        val out = json.encodeToString(UpdateCategoryRequestDto.serializer(), req)
        assertTrue(out.contains("\"applicability\":\"INCOME\""))
    }

    @Test
    fun applicabilityLabelsPtBr() {
        assertEquals("Despesa", CategoryApplicabilityDto.labelPtBr("EXPENSE"))
        assertEquals("Receita", CategoryApplicabilityDto.labelPtBr("INCOME"))
        assertEquals("Despesa e Receita", CategoryApplicabilityDto.labelPtBr("BOTH"))
    }

    @Test
    fun applicabilityTemTresValores() {
        assertEquals(3, CategoryApplicabilityDto.all.size)
        assertEquals(listOf("EXPENSE", "INCOME", "BOTH"), CategoryApplicabilityDto.all)
    }
}
