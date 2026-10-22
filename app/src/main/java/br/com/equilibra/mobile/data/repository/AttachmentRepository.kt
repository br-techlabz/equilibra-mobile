package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.attachment.AttachmentContent
import br.com.equilibra.mobile.data.attachment.AttachmentPolicy
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AttachmentApi
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Task 5.1 — Anexos de transação (API only, sem Room — binários nunca são cache).
 *
 * - Lista e escrita vêm sempre do backend (fonte da verdade).
 * - Upload multipart do part `file`, validando tamanho/MIME/nome antes de
 *   enviar (o servidor revalida e é a autoridade final).
 * - Download em streaming: o conteúdo exposto é um [AttachmentContent]
 *   fechável — nada de carregar o arquivo inteiro em memória.
 * - Erros viram [ApiResult] tipado; a tradução para casos de UI fica em
 *   `AttachmentError.from`.
 */
class AttachmentRepository @Inject constructor(
    private val api: AttachmentApi
) {

    /**
     * Envia um arquivo local como anexo.
     *
     * Validação antecipada (policy): nome, tamanho e MIME declarado. O
     * `Content-Type` do part é o MIME declarado — o backend valida a
     * assinatura do conteúdo de qualquer forma.
     */
    suspend fun upload(
        transactionId: String,
        file: File,
        contentType: String
    ): ApiResult<AttachmentDto> {
        when {
            !AttachmentPolicy.isValidFileName(file.name) ->
                return ApiResult.HttpError(400, "Nome de arquivo inválido", null)
            !AttachmentPolicy.isAllowedMimeType(contentType) ->
                return ApiResult.HttpError(400, "Tipo de arquivo não suportado", null)
            !AttachmentPolicy.isWithinSizeLimit(file.length()) ->
                return ApiResult.HttpError(400, "Arquivo vazio ou acima do limite", null)
        }
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = file.asRequestBody(contentType.toLowerCase().toMediaTypeOrNull())
        )
        return safeApiCall { api.upload(transactionId, part) }
    }

    /**
     * Upload a partir de bytes já lidos (ex.: stream de `ContentResolver`).
     * Evita materializar um arquivo temporário no cache do app; limite-se ao
     * que o SAF entregar.
     */
    suspend fun upload(
        transactionId: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray
    ): ApiResult<AttachmentDto> {
        when {
            !AttachmentPolicy.isValidFileName(fileName) ->
                return ApiResult.HttpError(400, "Nome de arquivo inválido", null)
            !AttachmentPolicy.isAllowedMimeType(contentType) ->
                return ApiResult.HttpError(400, "Tipo de arquivo não suportado", null)
            !AttachmentPolicy.isWithinSizeLimit(bytes.size.toLong()) ->
                return ApiResult.HttpError(400, "Arquivo vazio ou acima do limite", null)
        }
        val body = bytes.toRequestBody(contentType.toLowerCase().toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        return safeApiCall { api.upload(transactionId, part) }
    }

    /** Lista a metadata dos anexos da transação (ordem do backend: createdAt asc). */
    suspend fun list(transactionId: String): ApiResult<List<AttachmentDto>> =
        safeApiCall { api.list(transactionId) }

    /**
     * Abre o conteúdo para streaming. Em sucesso o chamador DEVE fechar
     * [AttachmentContent] (use `useStream`/`use`). Em erro o corpo é
     * fechado aqui e nunca vaza.
     */
    suspend fun openContent(attachmentId: String): ApiResult<AttachmentContent> =
        when (val result = safeApiCall { api.content(attachmentId) }) {
            is ApiResult.Success -> {
                val body = result.data
                val length = body.contentLength()
                if (length == 0L) {
                    // Corpo vazio/ausente não é um conteúdo válido de anexo
                    body.close()
                    ApiResult.HttpError(404, "Conteúdo do anexo indisponível", null)
                } else {
                    ApiResult.Success(
                        AttachmentContent(
                            contentType = body.contentType()?.toString(),
                            contentLength = length,
                            fileName = null, // exposto no header Content-Disposition pelo backend
                            body = body
                        )
                    )
                }
            }
            is ApiResult.HttpError -> {
                // corpo de erro já consumido por toApiResult; nada a fechar aqui
                result
            }
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }

    /**
     * Copia o conteúdo do anexo para [sink] em blocos (streaming). Não
     * guarda bytes localmente: o destino é responsabilidade do chamador
     * (ex.: `OutputStream` do SAF na Task 5.3).
     */
    suspend fun downloadTo(
        attachmentId: String,
        sink: (java.io.InputStream) -> Unit
    ): ApiResult<Unit> {
        val opened = openContent(attachmentId)
        val content = (opened as? ApiResult.Success)?.data
            ?: return opened as ApiResult<Unit>
        return try {
            content.useStream(sink)
            ApiResult.Success(Unit)
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        } catch (e: Exception) {
            ApiResult.UnknownError(e)
        } finally {
            content.close()
        }
    }

    /** Exclui o anexo (204 no sucesso); metadata e bytes saem no servidor. */
    suspend fun delete(attachmentId: String): ApiResult<Unit> =
        when (val result = safeApiCall { api.delete(attachmentId) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
}
