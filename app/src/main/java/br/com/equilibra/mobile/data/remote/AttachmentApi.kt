package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.attachment.AttachmentDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Task 5.1 — AttachmentApi (contrato real do AttachmentController do backend).
 *
 * - POST   /transactions/{transactionId}/attachments (multipart, part "file") → 201
 * - GET    /transactions/{transactionId}/attachments → 200 lista satisfeita por
 *          transactions/{id} do DONO (cross-owner → 404)
 * - GET    /attachments/{id}/content → 200 binário, streaming
 * - DELETE /attachments/{id} → 204
 *
 * Erros do backend (Problem Details): 400 arquivo inválido (tamanho/MIME/
 * assinatura/nome), 404 transação/anexo inexistente ou de outro owner,
 * 409 transação cancelada ou limite de 10 anexos atingido.
 */
interface AttachmentApi {

    @Multipart
    @POST("transactions/{transactionId}/attachments")
    suspend fun upload(
        @Path("transactionId") transactionId: String,
        @Part file: MultipartBody.Part
    ): Response<AttachmentDto>

    @GET("transactions/{transactionId}/attachments")
    suspend fun list(
        @Path("transactionId") transactionId: String
    ): Response<List<AttachmentDto>>

    /**
     * @Streaming evita carregar o arquivo inteiro em memória — o corpo precisa
     * ser consumido e fechado pelo chamador (ver [AttachmentRepository]).
     */
    @Streaming
    @GET("attachments/{id}/content")
    suspend fun content(
        @Path("id") id: String
    ): Response<ResponseBody>

    @DELETE("attachments/{id}")
    suspend fun delete(
        @Path("id") id: String
    ): Response<Unit>
}
