package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.creditcard.CreateCreditCardRequestDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.UpdateCreditCardRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 6.1 — CreditCardApi
 *
 * Endpoints reais do backend (CreditCardController, /credit-cards):
 * - POST   /                       → criar (CreateCreditCardRequest, 201)
 * - GET    /?includeInactive       → listar
 * - GET    /{id}                   → obter
 * - PUT    /{id}                   → atualizar (UpdateCreditCardRequest)
 * - PATCH  /{id}/deactivate        → desativar
 * - PATCH  /{id}/activate          → reativar
 *
 * committedLimit/availableLimit/reservedLimit chegam calculados pelo backend;
 * esta API não calcula nada (backend é autoridade para limites).
 */
interface CreditCardApi {

    @POST("credit-cards")
    suspend fun create(
        @Body body: CreateCreditCardRequestDto
    ): Response<CreditCardDto>

    @GET("credit-cards")
    suspend fun list(
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<CreditCardDto>>

    @GET("credit-cards/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<CreditCardDto>

    @PUT("credit-cards/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateCreditCardRequestDto
    ): Response<CreditCardDto>

    @PATCH("credit-cards/{id}/deactivate")
    suspend fun deactivate(
        @Path("id") id: String
    ): Response<CreditCardDto>

    @PATCH("credit-cards/{id}/activate")
    suspend fun activate(
        @Path("id") id: String
    ): Response<CreditCardDto>
}
