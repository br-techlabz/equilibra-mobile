package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.transaction.CreateIncomeRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.IncomeDto
import br.com.equilibra.mobile.data.remote.dto.transaction.IncomePageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateIncomeRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 4.2 — IncomeApi (contrato real do IncomeController, /incomes).
 *
 * - POST / → criar (201)
 * - GET /?page=&size=&includeCancelled= → lista paginada
 * - GET /{id} → obter
 * - PUT /{id} → atualizar
 * - PATCH /{id}/cancel → cancelar (Task 4.10)
 */
interface IncomeApi {

    @POST("incomes")
    suspend fun create(
        @Body body: CreateIncomeRequestDto
    ): Response<IncomeDto>

    @GET("incomes")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("includeCancelled") includeCancelled: Boolean = false
    ): Response<IncomePageResponseDto>

    @GET("incomes/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<IncomeDto>

    @PUT("incomes/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateIncomeRequestDto
    ): Response<IncomeDto>

    @PATCH("incomes/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String
    ): Response<IncomeDto>
}
