package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.statement.CloseStatementRequestDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Task 7.1 — StatementApi
 *
 * Rotas reais do backend (CreditCardStatementController):
 * - GET  /credit-cards/{cardId}/current-statement → fatura do ciclo atual
 * - GET  /credit-cards/{cardId}/statements        → histórico (cycleStart DESC)
 * - GET  /credit-card-statements/{id}             → fatura por id
 * - POST /credit-cards/{cardId}/statements/close  → fechar fatura do ciclo
 *
 * Não há PUT/DELETE: a fatura é gerada pelo backend e só muda por
 * fechamento/pagamento. Pagamento fica em /credit-card-statements/{id}/payments
 * e será tratado na Task 7.6.
 */
interface StatementApi {

    @GET("credit-cards/{cardId}/current-statement")
    suspend fun current(
        @Path("cardId") cardId: String
    ): Response<StatementDto>

    @GET("credit-cards/{cardId}/statements")
    suspend fun listByCard(
        @Path("cardId") cardId: String
    ): Response<List<StatementDto>>

    @GET("credit-card-statements/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<StatementDto>

    @POST("credit-cards/{cardId}/statements/close")
    suspend fun close(
        @Path("cardId") cardId: String,
        @Body body: CloseStatementRequestDto
    ): Response<StatementDto>
}
