package br.com.equilibra.mobile.data.attachment

import br.com.equilibra.mobile.data.remote.ApiResult
import okhttp3.ResponseBody
import java.io.Closeable
import java.io.InputStream

/**
 * Task 5.1 — Conteúdo de anexo aberto para streaming.
 *
 * O [ResponseBody] do OkHttp precisa ser fechado; um [InputStream] derivado
 * dele lê em pedaços (nunca carrega o arquivo inteiro em memória). Use
 * [useStream] para garantir o fechamento mesmo em erro.
 */
class AttachmentContent(
    val contentType: String?,
    val contentLength: Long,
    val fileName: String?,
    private val body: ResponseBody
) : Closeable {

    override fun close() = body.close()

    /** Executa [block] com o stream e fecha o corpo ao final (sucesso ou erro). */
    fun <T> useStream(block: (InputStream) -> T): T = body.byteStream().use(block)
}

/**
 * Falhas tipadas do fluxo de anexos. A UI traduz por caso (5.3) em vez de
 * comparar códigos HTTP espalhados.
 */
sealed class AttachmentError {
    /** 400 — tamanho/MIME/assinatura/nome recusados pelo servidor. */
    data object InvalidFile : AttachmentError()

    /** 404 — transação/anexo inexistente ou de outro usuário. */
    data object NotFound : AttachmentError()

    /** 409 — transação cancelada ou limite de anexos por transação. */
    data class Conflict(val reason: ConflictReason) : AttachmentError()

    /** 413 — corpo maior que o limite aceito pelo servidor/proxy. */
    data object TooLarge : AttachmentError()

    /** Outros 4xx/5xx. */
    data class Http(val code: Int) : AttachmentError()

    /** Falha de rede (sem conexão, timeout). */
    data class Network(val throwable: Throwable) : AttachmentError()

    /** Falha inesperada (ex.: IO ao gravar o download). */
    data class Unknown(val throwable: Throwable) : AttachmentError()

    /** Erro genérico para `ApiResult.HttpError` sem corpo conhecido. */
    data class Generic(val message: String?) : AttachmentError()

    enum class ConflictReason { CANCELLED_TRANSACTION, LIMIT_REACHED, UNKNOWN }
}

/** Converte [ApiResult] de anexo em erro tipado (null = sucesso). */
fun attachmentErrorFrom(result: ApiResult<*>): AttachmentError? = when (result) {
    is ApiResult.Success -> null
    is ApiResult.HttpError -> when (result.code) {
        400 -> AttachmentError.InvalidFile
        404 -> AttachmentError.NotFound
        409 -> AttachmentError.Conflict(classifyConflict(result.body))
        413 -> AttachmentError.TooLarge
        else -> AttachmentError.Http(result.code)
    }
    is ApiResult.NetworkError -> AttachmentError.Network(result.throwable)
    is ApiResult.UnknownError -> AttachmentError.Unknown(result.throwable)
}

/** O backend diferencia os 409 apenas pela mensagem do Problem Details. */
internal fun classifyConflict(body: String?): AttachmentError.ConflictReason = when {
    body == null -> AttachmentError.ConflictReason.UNKNOWN
    body.contains("Cancelled transactions", ignoreCase = true) ->
        AttachmentError.ConflictReason.CANCELLED_TRANSACTION
    body.contains("Attachment limit", ignoreCase = true) ->
        AttachmentError.ConflictReason.LIMIT_REACHED
    else -> AttachmentError.ConflictReason.UNKNOWN
}
