package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.purchase.CreatePurchaseRequestDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Task 6.6 — PurchaseApi
 *
 * Rotas reais do backend (CreditCardPurchaseController, /credit-card-purchases):
 * - POST /                       → criar (CreateCreditCardPurchaseRequest, 201)
 * - GET  /card/{cardId}          → listar compras do cartão
 * - GET  /{id}                   → obter compra (com parcelas)
 * - POST /{id}/cancel            → cancelar
 *
 * Não há PUT/DELETE no contrato: compra é imutável, só cancela (status).
 */
interface PurchaseApi {

    @POST("credit-card-purchases")
    suspend fun create(
        @Body body: CreatePurchaseRequestDto
    ): Response<PurchaseDto>

    @GET("credit-card-purchases/card/{cardId}")
    suspend fun listByCard(
        @Path("cardId") cardId: String
    ): Response<List<PurchaseDto>>

    @GET("credit-card-purchases/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<PurchaseDto>

    @POST("credit-card-purchases/{id}/cancel")
    suspend fun cancel(
        @Path("id") id: String
    ): Response<PurchaseDto>
}
