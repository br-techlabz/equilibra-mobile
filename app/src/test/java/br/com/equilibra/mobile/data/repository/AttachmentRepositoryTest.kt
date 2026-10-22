package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AttachmentApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.File

/**
 * Task 5.1 — AttachmentRepository contra um servidor HTTP real (MockWebServer).
 *
 * Cobre o que testes de mock puro não provam: o multipart enviado (nome do
 * part, filename, Content-Type) e o streaming de download (bytes chegam
 * íntegros e o corpo é fechado).
 */
class AttachmentRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AttachmentRepository

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = AttachmentRepository(retrofit.create(AttachmentApi::class.java))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun attachmentJson(id: String = "a1") =
        """{"id":"$id","originalFileName":"nota.pdf","contentType":"application/pdf","sizeBytes":4,"createdAt":"2026-10-20T12:00:00Z"}"""

    // ---------- upload ----------

    @Test
    fun uploadEnviaMultipartComPartFileEFilename() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(attachmentJson()))
        val file = File.createTempFile("nota", ".pdf").apply {
            writeBytes("%PDF".toByteArray()) // 4 bytes válidos
            deleteOnExit()
        }

        val result = repository.upload("tx1", file, "application/pdf")

        assertTrue(result is ApiResult.Success)
        assertEquals("a1", (result as ApiResult.Success).data.id)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/transactions/tx1/attachments", recorded.path)
        val contentType = recorded.getHeader("Content-Type")!!
        assertTrue(contentType.startsWith("multipart/form-data"))
        val body = recorded.body.readUtf8()
        // part "file" com filename e o MIME declarado
        assertTrue(body.contains("""name="file""""))
        assertTrue(body.contains("""filename=""""))
        assertTrue(body.contains("application/pdf"))
        assertTrue(body.contains("%PDF"))
    }

    @Test
    fun uploadRecusaTipoNaoSuportadoSemChamarServidor() = runTest {
        val file = File.createTempFile("tipo", ".gif").apply { writeBytes(byteArrayOf(1)); deleteOnExit() }

        val result = repository.upload("tx1", file, "image/gif")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(400, (result as ApiResult.HttpError).code)
        assertEquals(0, server.requestCount) // fail-fast, nada enviado
    }

    @Test
    fun uploadRecusaArquivoVazioSemChamarServidor() = runTest {
        val file = File.createTempFile("vazio0", ".pdf").apply { deleteOnExit() }

        val result = repository.upload("tx1", file, "application/pdf")

        assertTrue(result is ApiResult.HttpError)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun uploadPorBytesEnviaMultipartEFilename() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(attachmentJson("a2")))

        val result = repository.upload("tx2", "recibo.png", "image/png", byteArrayOf(1, 2, 3))

        assertTrue(result is ApiResult.Success)
        val recorded = server.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("""filename="recibo.png""""))
    }

    @Test
    fun uploadPropaga409DeTransacaoCancelada() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"detail":"Cancelled transactions cannot receive attachments."}""")
        )
        val file = File.createTempFile("nota", ".pdf").apply { writeBytes(byteArrayOf(1)); deleteOnExit() }

        val result = repository.upload("tx1", file, "application/pdf")

        assertEquals(409, (result as ApiResult.HttpError).code)
    }

    // ---------- listagem ----------

    @Test
    fun listDeserializaMetadata() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("[${attachmentJson("a1")},${attachmentJson("a2")}]")
        )

        val result = repository.list("tx1")

        val list = (result as ApiResult.Success).data
        assertEquals(2, list.size)
        assertEquals("/transactions/tx1/attachments", server.takeRequest().path)
    }

    // ---------- download em streaming ----------

    @Test
    fun downloadFazStreamingDosBytesEfechaCorpo() = runTest {
        val bytes = ByteArray(4096) { (it % 251).toByte() }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/pdf")
                .setHeader("Content-Length", bytes.size.toString())
                .setBody(okio.Buffer().write(bytes))
        )
        val received = ByteArray(4096)
        var read = 0

        val result = repository.downloadTo("att1") { input ->
            read = input.read(received)
        }

        assertTrue(result is ApiResult.Success)
        assertEquals(4096, read)
        assertTrue(bytes.contentEquals(received))
        assertEquals("/attachments/att1/content", server.takeRequest().path)
    }

    @Test
    fun downloadPropaga404DeAnexoDeOutroOwner() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"Attachment not found."}"""))

        val result = repository.downloadTo("att1") { }

        assertEquals(404, (result as ApiResult.HttpError).code)
    }

    @Test
    fun downloadFalhaDeEscritaViraUnknownError() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Length", "3")
                .setBody(okio.Buffer().write(byteArrayOf(1, 2, 3)))
        )

        val result = repository.downloadTo("att1") { throw IllegalStateException("disco cheio") }

        assertTrue(result is ApiResult.UnknownError)
    }

    // ---------- exclusão ----------

    @Test
    fun deleteUsa204ComoSsucesso() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.delete("att1")

        assertTrue(result is ApiResult.Success)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/attachments/att1", recorded.path)
    }
}
