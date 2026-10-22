package br.com.equilibra.mobile.data.attachment

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import br.com.equilibra.mobile.data.repository.AttachmentRepository
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * Task 5.2 — Leitura e envio de documentos escolhidos pelo SAF.
 *
 * O SAF entrega uma `content://` URI com permissão temporária. Este picker:
 * - resolve nome e tamanho pelos metadados reais do provedor
 *   ([OpenableColumns]) — nunca confia em extensão nem no nome exibido;
 * - valida tipo/tamanho com a [AttachmentPolicy] antes de ler bytes;
 * - lê o conteúdo em stream e envia direto ao [AttachmentRepository],
 *   sem copiar para o cache do app e sem guardar binário localmente.
 *
 * Nenhuma permissão de armazenamento é necessária: o acesso vem do grant
 * da URI escolhida pelo usuário.
 */
@HiltViewModel
class DocumentAttachmentPicker @Inject constructor(
    /** Exposto para o preview (5.4) reaproveitar a mesma instância/repositório. */
    internal val repository: AttachmentRepository
) : ViewModel() {

    /**
     * Upload direto da URI escolhida.
     *
     * A URI só é válida enquanto o grant existir; o envio acontece dentro do
     * fluxo que a recebeu (Activity Result), por isso não é persistida.
     */
    suspend fun upload(
        contentResolver: ContentResolver,
        transactionId: String,
        uri: Uri
    ): ApiResult<AttachmentDto> {
        val meta = readMetadata(contentResolver, uri) ?: return ApiResult.HttpError(
            400, "Não foi possível ler o arquivo selecionado", null
        )
        when {
            !AttachmentPolicy.isValidFileName(meta.fileName) ->
                return ApiResult.HttpError(400, "Nome de arquivo inválido", null)
            !AttachmentPolicy.isAllowedMimeType(meta.contentType) ->
                return ApiResult.HttpError(400, "Tipo de arquivo não suportado", null)
            // Provedor pode não declarar SIZE: nesse caso só a leitura limitada abaixo decide
            meta.sizeBytes >= 0 && !AttachmentPolicy.isWithinSizeLimit(meta.sizeBytes) ->
                return ApiResult.HttpError(400, "Arquivo vazio ou acima do limite", null)
        }
        val bytes = readBytes(contentResolver, uri)
            ?: return ApiResult.HttpError(400, "Arquivo vazio ou acima do limite", null)
        return repository.upload(
            transactionId = transactionId,
            fileName = meta.fileName,
            contentType = meta.contentType,
            bytes = bytes
        )
    }

    /**
     * Abre o stream do documento para operações de streaming (ex.: enviar
     * direto como corpo sem materializar bytes). O chamador fecha o stream.
     */
    suspend fun openStream(contentResolver: ContentResolver, uri: Uri): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Task 5.3 — Baixa o anexo direto para o destino escolhido no SAF
     * (`ActivityResultContracts.CreateDocument`). Os bytes são copiados em
     * stream do backend para o `OutputStream` do destino: nada é gravado no
     * armazenamento do app (Task 5.5).
     *
     * Em falha, tenta apagar o documento parcialmente criado para não deixar
     * um arquivo truncado no destino do usuário.
     */
    suspend fun saveDownload(
        contentResolver: ContentResolver,
        attachmentId: String,
        destination: Uri
    ): ApiResult<Long> = withContext(Dispatchers.IO) {
        val output = try {
            contentResolver.openOutputStream(destination)
        } catch (_: Exception) {
            null
        } ?: return@withContext ApiResult.UnknownError(
            IllegalStateException("Destino indisponível")
        )
        var written = 0L
        val result = try {
            repository.downloadTo(attachmentId) { input ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    written += read
                }
                output.flush()
            }
        } finally {
            try {
                output.close()
            } catch (_: Exception) {
                // ignorado: o resultado do download já é a informação relevante
            }
        }
        if (result !is ApiResult.Success) {
            // Remove o documento incompleto criado pelo SAF
            try {
                contentResolver.delete(destination, null, null)
            } catch (_: Exception) {
                // melhor esforço
            }
            return@withContext result.withNoData()
        }
        ApiResult.Success(written)
    }

    /** Metadados resolvidos do provedor. `fileName` cai para o último segmento
     * da URI quando o provedor não declara DISPLAY_NAME; `contentType` cai
     * para `application/octet-stream`, que a política rejeita — o usuário é
     * avisado em vez de subir conteúdo de tipo desconhecido.
     */
    suspend fun readMetadata(contentResolver: ContentResolver, uri: Uri): DocumentMetadata? =
        withContext(Dispatchers.IO) {
            var name: String? = null
            var size = -1L
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            try {
                contentResolver.query(uri, projection, null, null, null)?.use { cursor: Cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {
                return@withContext null
            }
            val resolvedName = (name ?: uri.lastPathSegment?.substringAfterLast('/'))
                ?.takeIf { it.isNotBlank() }
                ?: return@withContext null
            DocumentMetadata(
                uri = uri,
                fileName = resolvedName,
                contentType = contentResolver.getType(uri) ?: UNKNOWN_TYPE,
                sizeBytes = size
            )
        }

    /** Lê os bytes validando o limite durante a leitura (defesa extra). */
    private suspend fun readBytes(contentResolver: ContentResolver, uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(MAX_READ_BYTES + 1)
                    var total = 0
                    while (total < buffer.size) {
                        val read = input.read(buffer, total, buffer.size - total)
                        if (read < 0) break
                        total += read
                    }
                    if (total > MAX_READ_BYTES) null else buffer.copyOf(total)
                }
            } catch (_: Exception) {
                null
            }
        }

    private companion object {
        const val UNKNOWN_TYPE = "application/octet-stream"
        const val DOWNLOAD_BUFFER_BYTES = 8 * 1024
        val MAX_READ_BYTES: Int = AttachmentPolicy.MAX_FILE_BYTES.toInt()
    }
}

/** Reaproveita a falha descartando o tipo de dado original. */
private fun <T> ApiResult<T>.withNoData(): ApiResult<Long> = when (this) {
    is ApiResult.Success -> ApiResult.Success(0L)
    is ApiResult.HttpError -> this
    is ApiResult.NetworkError -> this
    is ApiResult.UnknownError -> this
}

/** Metadados de um documento escolhido no SAF. */
data class DocumentMetadata(
    val uri: Uri,
    val fileName: String,
    val contentType: String,
    /** -1 quando o provedor não informa o tamanho. */
    val sizeBytes: Long
)
