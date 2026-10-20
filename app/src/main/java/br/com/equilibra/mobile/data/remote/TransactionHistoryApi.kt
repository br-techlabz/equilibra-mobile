package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.transaction.HistoryPageResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Task 4.11 — Histórico unificado (contrato real do TransactionHistoryController).
 *
 * GET /transactions com filtros combinados (todos opcionais):
 * - type: EXPENSE | INCOME | TRANSFER | STATEMENT_PAYMENT
 * - status: ACTIVE | CANCELLED
 * - from/to: Instant ISO-8601 DATE_TIME (BETWEEN inclusivo no backend)
 * - accountId, categoryId: UUIDs
 * - tagIds: lista (parâmetros repetidos na query)
 *
 * Nulo → parâmetro omitido (Retrofit ignora @Query nulo).
 * A tela deste endpoint NÃO usa cache (Task 4.11): sem leitura/escrita Room.
 */
interface TransactionHistoryApi {

    @GET("transactions")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("accountId") accountId: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("tagIds") tagIds: List<String>? = null
    ): Response<HistoryPageResponseDto>
}
