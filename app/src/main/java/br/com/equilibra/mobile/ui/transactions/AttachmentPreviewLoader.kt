package br.com.equilibra.mobile.ui.transactions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import br.com.equilibra.mobile.data.attachment.AttachmentPreviewPolicy
import br.com.equilibra.mobile.data.attachment.PreviewKind
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import java.io.File

/**
 * Task 5.4 — Carrega o conteúdo de um anexo para preview.
 *
 * - Imagem: bytes decodificados em [Bitmap] (com teto de pixels para não
 *   estourar memória em arquivos grandes).
 * - PDF: `PdfRenderer` nativo, renderizando a primeira página em [Bitmap].
 *   Nunca executa conteúdo; PDF é tratado como dado, não como programa.
 * - Qualquer falha (rede, arquivo corrompido, formato inesperado) retorna
 *   null e a tela mostra o fallback com download.
 *
 * O arquivo temporário do PDF é criado no cache do app apenas durante a
 * renderização (`PdfRenderer` exige um descritor de arquivo) e é apagado
 * sempre em `finally` — nenhum binário de anexo é cacheado.
 */
class AttachmentPreviewLoader(
    private val repository: AttachmentRepository,
    /**
     * Diretório para o arquivo temporário que o `PdfRenderer` exige.
     * Em produção é o `cacheDir` do app (limpo pelo sistema); nunca o
     * armazenamento externo. O arquivo é apagado em `finally` sempre.
     */
    private val tempDir: File? = null
) {

    /** Resultado do preview pronto para desenhar. */
    data class Preview(val kind: PreviewKind, val image: ImageBitmap)

    suspend fun load(
        attachmentId: String,
        contentType: String?,
        sizeBytes: Long
    ): Preview? {
        if (!AttachmentPreviewPolicy.canPreview(contentType, sizeBytes)) return null
        return when (AttachmentPreviewPolicy.kindFor(contentType)) {
            PreviewKind.IMAGE -> decodeImage(attachmentId)
            PreviewKind.PDF -> renderPdfFirstPage(attachmentId)
            PreviewKind.None -> null
        }
    }

    private suspend fun decodeImage(attachmentId: String): Preview? {
        val bytes = downloadBytes(attachmentId) ?: return null
        return try {
            val bitmap = decodeScaled(bytes) ?: return null
            Preview(PreviewKind.IMAGE, bitmap.asImageBitmap())
        } catch (_: Throwable) {
            null
        }
    }

    /** Decodifica limitando a dimensão para não estourar memória. */
    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val sample = calculateSampleSize(options.outWidth, options.outHeight)
        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
        }
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_PREVIEW_DIMENSION || height / sample > MAX_PREVIEW_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private suspend fun renderPdfFirstPage(attachmentId: String): Preview? {
        val bytes = downloadBytes(attachmentId) ?: return null
        var temp: File? = null
        return try {
            // Dir explícito: File.createTempFile(String, String) usaria o
            // diretório temporário do sistema (fora do escopo do app).
            temp = File.createTempFile("preview", ".pdf", tempDir).apply { writeBytes(bytes) }
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(0).use { page ->
                        val scale = MAX_PREVIEW_DIMENSION.toFloat() / page.width.toFloat()
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Fundo branco: páginas de PDF não têm alfa
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        Preview(PreviewKind.PDF, bitmap.asImageBitmap())
                    }
                }
            }
        } catch (e: Throwable) {
            // PdfRenderer é nativo: pode lançar Error (NoSuchMethodError etc.)
            // em implementações incompletas — preview é opcional, nunca propaga.
            null
        } finally {
            temp?.delete()
        }
    }

    /**
     * Baixa os bytes com teto de tamanho. O conteúdo é consumido em stream
     * para um buffer limitado; acima do teto, abandona para não pressionar a
     * memória (e a tela cai no fallback).
     */
    private suspend fun downloadBytes(attachmentId: String): ByteArray? {
        var bytes: ByteArray? = null
        val result = repository.downloadTo(attachmentId) { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            var total = 0
            while (total <= AttachmentPreviewPolicy.MAX_AUTO_PREVIEW_BYTES) {
                val read = input.read(chunk)
                if (read < 0) break
                buffer.write(chunk, 0, read)
                total += read
            }
            bytes = if (total > AttachmentPreviewPolicy.MAX_AUTO_PREVIEW_BYTES) null
                    else buffer.toByteArray()
        }
        return if (result is ApiResult.Success) bytes else null
    }

    private companion object {
        /** Lado máximo da imagem de preview (evita OOM). */
        const val MAX_PREVIEW_DIMENSION = 1600
    }
}
