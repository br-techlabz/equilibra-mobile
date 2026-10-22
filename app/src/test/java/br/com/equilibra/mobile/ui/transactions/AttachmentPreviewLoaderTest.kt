package br.com.equilibra.mobile.ui.transactions

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.attachment.PreviewKind
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Task 5.4 — Carregamento do preview: decodifica imagem, renderiza PDF e
 * nunca propaga exceção (falha → null, tela cai no fallback).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentPreviewLoaderTest {

    private val repository: AttachmentRepository = mockk(relaxed = true)

    private fun pngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    private fun stubDownload(bytes: ByteArray) {
        coEvery { repository.downloadTo(any(), any()) } answers {
            val sink = secondArg<(java.io.InputStream) -> Unit>()
            sink(bytes.inputStream())
            ApiResult.Success(Unit)
        }
    }

    @Test
    fun carregaImagemDecodificada() = runTest {
        stubDownload(pngBytes())
        val loader = AttachmentPreviewLoader(repository)

        val preview = loader.load("a1", "image/png", 4096)

        assertNotNull(preview)
        assertEquals(PreviewKind.IMAGE, preview!!.kind)
    }

    @Test
    fun formatoNaoSuportadoNaoBaixaNemTentaPreview() = runTest {
        val loader = AttachmentPreviewLoader(repository)

        val preview = loader.load("a1", "application/zip", 4096)

        assertNull(preview)
        io.mockk.coVerify(exactly = 0) { repository.downloadTo(any(), any()) }
    }

    @Test
    fun arquivoAcimaDoTetoNaoAbrePreview() = runTest {
        val loader = AttachmentPreviewLoader(repository)

        val preview = loader.load("a1", "image/png", 50L * 1024 * 1024)

        assertNull(preview)
        io.mockk.coVerify(exactly = 0) { repository.downloadTo(any(), any()) }
    }

    @Test
    fun imagemCorrompidaRetornaNullSemLancar() = runTest {
        stubDownload(byteArrayOf(1, 2, 3, 4)) // não é imagem válida
        val loader = AttachmentPreviewLoader(repository)

        assertNull(loader.load("a1", "image/png", 4))
    }

    @Test
    fun pdfIndisponivelNoAmbienteRetornaNullSemLancar() = runTest {
        // Robolectric não implementa o PdfRenderer nativo: o loader deve
        // absorver o erro (inclusive Error) e devolver null para o fallback.
        stubDownload("%PDF-inválido".toByteArray())
        val loader = AttachmentPreviewLoader(repository)

        assertNull(loader.load("a1", "application/pdf", 15))
    }

    @Test
    fun falhaDeRedeNoDownloadRetornaNull() = runTest {
        coEvery { repository.downloadTo(any(), any()) } returns ApiResult.NetworkError(IOException("offline"))
        val loader = AttachmentPreviewLoader(repository)

        assertNull(loader.load("a1", "image/png", 100))
    }

    @Test
    fun pdfNaoDeixaArquivoTemporarioNoCache() = runTest {
        stubDownload("%PDF-inválido".toByteArray())
        val loader = AttachmentPreviewLoader(repository)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        loader.load("a1", "application/pdf", 15)

        val leftover = context.cacheDir.walkTopDown().filter { it.isFile }.toList()
        assertEquals(emptyList<Any>(), leftover.filter { it.name.startsWith("preview") })
    }
}
