package br.com.equilibra.mobile.data.attachment

import br.com.equilibra.mobile.data.remote.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Task 5.1 — tradução de erros HTTP do backend (Problem Details) para casos
 * de UI e validação de política espelhada do servidor.
 */
class AttachmentErrorMappingTest {

    @Test
    fun `400 vira arquivo invalido`() {
        val error = attachmentErrorFrom(ApiResult.HttpError(400, null, null))
        assertEquals(AttachmentError.InvalidFile, error)
    }

    @Test
    fun `404 vira nao encontrado (inclui cross-owner)`() {
        val error = attachmentErrorFrom(ApiResult.HttpError(404, null, null))
        assertEquals(AttachmentError.NotFound, error)
    }

    @Test
    fun `409 de transacao cancelada e distinguido`() {
        val body = """{"detail":"Cancelled transactions cannot receive attachments."}"""
        val error = attachmentErrorFrom(ApiResult.HttpError(409, null, body))
        assertEquals(
            AttachmentError.Conflict(AttachmentError.ConflictReason.CANCELLED_TRANSACTION),
            error
        )
    }

    @Test
    fun `409 de limite de anexos e distinguido`() {
        val body = """{"detail":"Attachment limit reached."}"""
        val error = attachmentErrorFrom(ApiResult.HttpError(409, null, body))
        assertEquals(
            AttachmentError.Conflict(AttachmentError.ConflictReason.LIMIT_REACHED),
            error
        )
    }

    @Test
    fun `413 vira arquivo grande demais`() {
        assertEquals(AttachmentError.TooLarge, attachmentErrorFrom(ApiResult.HttpError(413, null, null)))
    }

    @Test
    fun `rede e erro desconhecido sao tipados`() {
        val io = IOException("offline")
        assertEquals(AttachmentError.Network(io), attachmentErrorFrom(ApiResult.NetworkError(io)))
        assertEquals(AttachmentError.Unknown(io), attachmentErrorFrom(ApiResult.UnknownError(io)))
    }

    @Test
    fun `sucesso nao e erro`() {
        assertNull(attachmentErrorFrom(ApiResult.Success(Unit)))
    }

    @Test
    fun `politica de mime aceita a whitelist do backend`() {
        assertTrue(AttachmentPolicy.isAllowedMimeType("application/pdf"))
        assertTrue(AttachmentPolicy.isAllowedMimeType("image/jpeg"))
        assertTrue(AttachmentPolicy.isAllowedMimeType("image/png"))
        assertTrue(AttachmentPolicy.isAllowedMimeType("image/webp"))
        assertTrue(AttachmentPolicy.isAllowedMimeType("IMAGE/PNG"))
        assertFalse(AttachmentPolicy.isAllowedMimeType("image/gif"))
        assertFalse(AttachmentPolicy.isAllowedMimeType("image/svg+xml"))
        assertFalse(AttachmentPolicy.isAllowedMimeType("text/html"))
        assertFalse(AttachmentPolicy.isAllowedMimeType(null))
    }

    @Test
    fun `politica de tamanho rejeita vazio e acima de 10MB`() {
        assertFalse(AttachmentPolicy.isWithinSizeLimit(0))
        assertFalse(AttachmentPolicy.isWithinSizeLimit(-1))
        assertTrue(AttachmentPolicy.isWithinSizeLimit(1))
        assertTrue(AttachmentPolicy.isWithinSizeLimit(AttachmentPolicy.MAX_FILE_BYTES))
        assertFalse(AttachmentPolicy.isWithinSizeLimit(AttachmentPolicy.MAX_FILE_BYTES + 1))
    }

    @Test
    fun `politica de nome espelha a validacao do backend`() {
        assertTrue(AttachmentPolicy.isValidFileName("nota fiscal.pdf"))
        assertFalse(AttachmentPolicy.isValidFileName(null))
        assertFalse(AttachmentPolicy.isValidFileName("  "))
        assertFalse(AttachmentPolicy.isValidFileName("a".repeat(256)))
        assertTrue(AttachmentPolicy.isValidFileName("a".repeat(255)))
        assertFalse(AttachmentPolicy.isValidFileName("linha\nquebrada.pdf"))
        assertFalse(AttachmentPolicy.isValidFileName("../../etc/passwd"))
        assertFalse(AttachmentPolicy.isValidFileName("a\u0000b.pdf"))
    }
}
