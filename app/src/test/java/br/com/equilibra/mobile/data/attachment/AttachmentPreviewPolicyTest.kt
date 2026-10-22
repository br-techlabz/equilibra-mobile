package br.com.equilibra.mobile.data.attachment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 5.4 — Regra de pré-visualização: só formatos seguros e suportados,
 * com fallback explícito para o resto.
 */
class AttachmentPreviewPolicyTest {

    @Test
    fun `imagens suportadas viram preview de imagem`() {
        assertEquals(PreviewKind.IMAGE, AttachmentPreviewPolicy.kindFor("image/jpeg"))
        assertEquals(PreviewKind.IMAGE, AttachmentPreviewPolicy.kindFor("image/png"))
        assertEquals(PreviewKind.IMAGE, AttachmentPreviewPolicy.kindFor("image/webp"))
        assertEquals(PreviewKind.IMAGE, AttachmentPreviewPolicy.kindFor("IMAGE/PNG"))
    }

    @Test
    fun `pdf vira preview de pdf`() {
        assertEquals(PreviewKind.PDF, AttachmentPreviewPolicy.kindFor("application/pdf"))
    }

    @Test
    fun `formatos nao suportados nao tem preview`() {
        assertEquals(PreviewKind.None, AttachmentPreviewPolicy.kindFor("text/html"))
        assertEquals(PreviewKind.None, AttachmentPreviewPolicy.kindFor("image/svg+xml"))
        assertEquals(PreviewKind.None, AttachmentPreviewPolicy.kindFor("application/zip"))
        assertEquals(PreviewKind.None, AttachmentPreviewPolicy.kindFor(null))
    }

    @Test
    fun `tamanho zero ou acima do teto nao abre preview`() {
        assertFalse(AttachmentPreviewPolicy.canPreview("image/png", 0))
        assertFalse(AttachmentPreviewPolicy.canPreview("image/png", -5))
        assertTrue(AttachmentPreviewPolicy.canPreview("image/png", 1024))
        assertTrue(
            AttachmentPreviewPolicy.canPreview(
                "application/pdf",
                AttachmentPreviewPolicy.MAX_AUTO_PREVIEW_BYTES
            )
        )
        assertFalse(
            AttachmentPreviewPolicy.canPreview(
                "application/pdf",
                AttachmentPreviewPolicy.MAX_AUTO_PREVIEW_BYTES + 1
            )
        )
    }

    @Test
    fun `formato desconhecido nunca abre preview mesmo pequeno`() {
        assertFalse(AttachmentPreviewPolicy.canPreview("application/octet-stream", 10))
    }

    @Test
    fun `fallback e explicito e orienta o download`() {
        val message = AttachmentPreviewPolicy.fallbackMessage("application/zip")
        assertTrue(message.contains("download", ignoreCase = true))
        assertTrue(message.contains("não tem pré-visualização", ignoreCase = true))
    }
}
