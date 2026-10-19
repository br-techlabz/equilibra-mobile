package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.transaction.CreateExpenseRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.ExpenseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.ExpensePageResponseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateExpenseRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 4.2 — ExpenseApi (contrato real do ExpenseController, /expenses).
 *
 * - POST / → criar (201)
 * - GET /?page=&size=&includeCancelled= → lista paginada
 *   (page default 0, size default 20, máx 100 no backend)
 * - GET /{id} → obter
 * - PUT /{id} → atualizar
 * - PATCH /{id}/cancel → cancelar (Task 4.10)
 */
interface ExpenseApi {

    @POST("expenses")
    suspend fun create(
        @Body body: CreateExpenseRequestDto
    ): Response<ExpenseDto>

    @GET("expenses")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("includeCancelled") includeCancelled: Boolean = false
    ): Response<ExpensePageResponseDto>

    @GET("expenses/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<ExpenseDto>

    @PUT("expenses/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateExpenseRequestDto
    ): Response<ExpenseDto>

    @PATCH("expenses/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String
    ): Response<ExpenseDto>
}
