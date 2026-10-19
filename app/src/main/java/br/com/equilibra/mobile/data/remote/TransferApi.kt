package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.transaction.CreateTransferRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TransferDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TransferPageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateTransferRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 4.2 — TransferApi (contrato real do TransferController, /transfers).
 *
 * - POST / → criar (201)
 * - GET /?page=&size=&includeCancelled= → lista paginada
 * - GET /{id} → obter
 * - PUT /{id} → atualizar
 * - PATCH /{id}/cancel → cancelar (Task 4.10)
 */
interface TransferApi {

    @POST("transfers")
    suspend fun create(
        @Body body: CreateTransferRequestDto
    ): Response<TransferDto>

    @GET("transfers")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("includeCancelled") includeCancelled: Boolean = false
    ): Response<TransferPageResponseDto>

    @GET("transfers/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<TransferDto>

    @PUT("transfers/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateTransferRequestDto
    ): Response<TransferDto>

    @PATCH("transfers/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String
    ): Response<TransferDto>
}
