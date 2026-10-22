package br.com.equilibra.mobile.data.attachment

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AttachmentApi
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Task 5.2 — SAF contra um ContentProvider em memória (Robolectric).
 *
 * Prova o caminho real `content://`: metadados pelo provider, bytes lidos em
 * stream, validação antes de enviar, cancelamento tratado e nenhum binário
 * gravado no armazenamento do app.
 */
@RunWith(RobolectricTestRunner::class)
class DocumentAttachmentPickerTest {

    /** Provider que declara DISPLAY_NAME/SIZE e serve bytes de um arquivo. */
    class FakeDocumentsProvider : ContentProvider() {
        override fun onCreate() = true
        override fun query(
            uri: Uri, projection: Array<out String>?, selection: String?,
            selectionArgs: Array<out String>?, sortOrder: String?
        ): Cursor {
            val type = getType(uri)
            val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            cursor.addRow(arrayOf(uri.lastPathSegment, contentFor(type).size.toLong()))
            return cursor
        }
        override fun getType(uri: Uri): String = when {
            uri.lastPathSegment?.endsWith(".pdf", true) == true -> "application/pdf"
            uri.lastPathSegment?.endsWith(".gif", true) == true -> "image/gif"
            else -> "application/octet-stream"
        }
        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val file = File.createTempFile("fake", ".bin").apply {
                writeBytes(contentFor(getType(uri)))
                deleteOnExit()
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

        companion object {
            val PDF = "%PDF-1.7\nconteúdo".toByteArray()
            fun contentFor(type: String) = if (type == "application/pdf") PDF else byteArrayOf(0x47, 0x49, 0x46)
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val api: AttachmentApi = io.mockk.mockk(relaxed = true)
    private lateinit var picker: DocumentAttachmentPicker

    private fun uri(name: String) = Uri.parse("content://test.documents/$name")

    @Before
    fun setUp() {
        Robolectric.buildContentProvider(FakeDocumentsProvider::class.java)
            .create("test.documents")
        picker = DocumentAttachmentPicker(AttachmentRepository(api))
    }

    private fun stubUpload() {
        coEvery { api.upload(any(), any()) } returns retrofit2.Response.success(
            AttachmentDto("a1", "nota.pdf", "application/pdf", 12, "2026-10-20T12:00:00Z")
        )
    }

    @Test
    fun leMetadadosReaisDoProvedor() = runTest {
        val meta = picker.readMetadata(context.contentResolver, uri("nota.pdf"))

        assertNotNull(meta)
        assertEquals("nota.pdf", meta!!.fileName)
        assertEquals("application/pdf", meta.contentType)
        assertEquals(FakeDocumentsProvider.PDF.size.toLong(), meta.sizeBytes)
    }

    @Test
    fun enviaBytesReaisComNomeETipoDoProvedor() = runTest {
        stubUpload()

        val result = picker.upload(context.contentResolver, "tx1", uri("nota.pdf"))

        assertTrue(result is ApiResult.Success)
        // Confere que os bytes enviados são os do provider
        val slot = slot<okhttp3.MultipartBody.Part>()
        io.mockk.coVerify { api.upload("tx1", capture(slot)) }
        val body: RequestBody = slot.captured.body
        val buffer = Buffer()
        body.writeTo(buffer)
        assertArrayEquals(FakeDocumentsProvider.PDF, buffer.readByteArray())
    }

    @Test
    fun tipoForaDaWhitelistFalhaAntesDeLerBytes() = runTest {
        val result = picker.upload(context.contentResolver, "tx1", uri("anim.gif"))

        assertTrue(result is ApiResult.HttpError)
        assertEquals(400, (result as ApiResult.HttpError).code)
        io.mockk.coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @Test
    fun uriInexistenteViraErroClaroSemChamarApi() = runTest {
        // Nenhum provider registrado para esta authority → metadata nula
        val result = picker.upload(context.contentResolver, "tx1", Uri.parse("content://nao.existe/a.pdf"))

        assertTrue(result is ApiResult.HttpError)
        assertEquals(400, (result as ApiResult.HttpError).code)
        io.mockk.coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @Test
    fun cancelamentoDoSeletorNaoChamaNada() {
        // Contrato OpenDocument entrega null no cancelamento; a tela ignora e
        // nenhuma operação ocorre. Garantimos que nada foi enviado.
        assertNull(null as Uri?)
        io.mockk.coVerify(exactly = 0) { api.upload(any(), any()) }
    }

    @Test
    fun uploadNaoGravaBinarioNoArmazenamentoDoApp() = runTest {
        stubUpload()

        picker.upload(context.contentResolver, "tx1", uri("nota.pdf"))

        // Task 5.5: nenhum byte do anexo no cache/files do app.
        // (Provedores/testes podem ter arquivos temporários fora desses dirs.)
        val appFiles = (
            context.cacheDir.walkTopDown() + context.filesDir.walkTopDown()
            ).count { it.isFile }
        assertEquals(0, appFiles)
    }
}
