package br.com.equilibra.mobile.data.remote.dto.attachment

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Task 5.1 — serialização do contrato real do AttachmentResponse. */
class AttachmentDtoTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun deserializaRespostaDoBackend() {
        val raw = """
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "originalFileName": "nota fiscal.pdf",
              "contentType": "application/pdf",
              "sizeBytes": 245760,
              "createdAt": "2026-10-20T12:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<AttachmentDto>(raw)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id)
        assertEquals("nota fiscal.pdf", dto.originalFileName)
        assertEquals("application/pdf", dto.contentType)
        assertEquals(245760L, dto.sizeBytes)
        assertEquals("2026-10-20T12:00:00Z", dto.createdAt)
    }

    @Test
    fun deserializaListaDoContrato() {
        val raw = """
            [
              {"id":"a1","originalFileName":"a.png","contentType":"image/png","sizeBytes":10,"createdAt":"2026-10-20T12:00:00Z"},
              {"id":"a2","originalFileName":"b.pdf","contentType":"application/pdf","sizeBytes":20,"createdAt":"2026-10-20T12:00:01Z"}
            ]
        """.trimIndent()

        val list = json.decodeFromString<List<AttachmentDto>>(raw)

        assertEquals(2, list.size)
        assertEquals("a.png", list[0].originalFileName)
        assertEquals("b.pdf", list[1].originalFileName)
    }

    @Test
    fun ignoraCamposPrivadosQueOBackendNaoExpoe() {
        // storageKey/ownerId/checksum são internos do servidor; se aparecerem
        // em versão futura, o app não deve quebrar
        val raw = """
            {"id":"a1","originalFileName":"a.png","contentType":"image/png","sizeBytes":10,
             "createdAt":"2026-10-20T12:00:00Z","storageKey":"owner/tx/uuid","ownerId":"o1",
             "checksumSha256":"abc"}
        """.trimIndent()

        val dto = json.decodeFromString<AttachmentDto>(raw)

        assertEquals("a1", dto.id)
    }
}
