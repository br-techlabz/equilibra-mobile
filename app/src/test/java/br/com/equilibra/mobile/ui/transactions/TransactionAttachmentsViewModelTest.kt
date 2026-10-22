package br.com.equilibra.mobile.ui.transactions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Task 5.3 — Anexos da transação: carga, upload (anti duplo envio), exclusão
 * com confirmação e estados de erro. Só metadata; nada persistido.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionAttachmentsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AttachmentRepository
    private lateinit var viewModel: TransactionAttachmentsViewModel

    private fun attachment(id: String, name: String = "nota.pdf") = AttachmentDto(
        id = id,
        originalFileName = name,
        contentType = "application/pdf",
        sizeBytes = 1024,
        createdAt = "2026-10-20T12:00:00Z"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.list(any()) } returns ApiResult.Success(emptyList())
        viewModel = TransactionAttachmentsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bindCarregaListaDaApi() = runTest {
        coEvery { repository.list("tx1") } returns ApiResult.Success(listOf(attachment("a1")))
        viewModel.bind("tx1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(attachment("a1")), state.items)
        assertFalse(state.isLoading)
    }

    @Test
    fun bindRepetidoNaoRecarrega() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()
        viewModel.bind("tx1")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.list("tx1") }
    }

    @Test
    fun erroDeCargaViraLoadErrorComRetry() = runTest {
        coEvery { repository.list("tx1") } returns ApiResult.NetworkError(IOException("offline"))
        viewModel.bind("tx1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadError!!.contains("conexão", ignoreCase = true))

        coEvery { repository.list("tx1") } returns ApiResult.Success(listOf(attachment("a1")))
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(1, viewModel.uiState.value.items.size)
    }

    @Test
    fun uploadComSucessoRecarregaMetadataEAvisa() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()
        coEvery { repository.list("tx1") } returns ApiResult.Success(listOf(attachment("a1")))

        viewModel.onUploadStarted("nota.pdf")
        assertTrue(viewModel.uiState.value.isUploading)
        viewModel.onUploadResult(ApiResult.Success(attachment("a1")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUploading)
        assertEquals("Anexo \"nota.pdf\" enviado.", state.notice)
        assertEquals(1, state.items.size) // metadata recarregada do servidor
    }

    @Test
    fun uploadComFalhaViraActionErrorMantendoLista() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()

        viewModel.onUploadResult(ApiResult.HttpError(409, null, """{"detail":"Attachment limit reached."}"""))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.actionError!!.contains("Limite de 10"))
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun segundoUploadEnquantoEnviandoEhIgnorado() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()

        viewModel.onUploadStarted("a.pdf")
        viewModel.onUploadStarted("b.pdf") // anti duplo envio

        assertEquals("a.pdf", viewModel.uiState.value.uploadingFileName)
    }

    @Test
    fun cancelarUploadLimpaEstadoSemErro() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()
        viewModel.onUploadStarted("a.pdf")

        viewModel.cancelUpload()

        assertFalse(viewModel.uiState.value.isUploading)
        assertNull(viewModel.uiState.value.actionError)
    }

    @Test
    fun deleteRemoveItemLocalEAvisa() = runTest {
        coEvery { repository.list("tx1") } returns ApiResult.Success(listOf(attachment("a1"), attachment("a2")))
        coEvery { repository.delete("a1") } returns ApiResult.Success(Unit)
        viewModel.bind("tx1")
        advanceUntilIdle()

        viewModel.delete(attachment("a1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("a2"), state.items.map { it.id })
        assertEquals("Anexo excluído.", state.notice)
        assertNull(state.deletingId)
    }

    @Test
    fun deleteComFalhaPreservaItemEInformaErro() = runTest {
        coEvery { repository.list("tx1") } returns ApiResult.Success(listOf(attachment("a1")))
        coEvery { repository.delete("a1") } returns ApiResult.HttpError(404, null, null)
        viewModel.bind("tx1")
        advanceUntilIdle()

        viewModel.delete(attachment("a1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertTrue(state.actionError!!.contains("não encontrado", ignoreCase = true))
    }

    @Test
    fun limiteDeDezAnexosBloqueiaUpload() = runTest {
        val ten = (1..10).map { attachment("a$it") }
        coEvery { repository.list("tx1") } returns ApiResult.Success(ten)
        viewModel.bind("tx1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFull)
        assertFalse(viewModel.uiState.value.canUpload)
    }

    @Test
    fun limparAvisoEErro() = runTest {
        viewModel.bind("tx1")
        advanceUntilIdle()
        viewModel.onUploadResult(ApiResult.Success(attachment("a1")))
        advanceUntilIdle()

        viewModel.clearNotice()
        viewModel.clearActionError()

        assertNull(viewModel.uiState.value.notice)
        assertNull(viewModel.uiState.value.actionError)
    }
}
