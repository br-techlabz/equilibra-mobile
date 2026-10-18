package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.dashboard.DashboardResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API do Dashboard — contrato real do backend.
 *
 * Endpoint: GET /dashboard?from=<ISO8601>&to=<ISO8601>
 *
 * O backend espera datas em formato ISO 8601 (ex: 2026-10-01T00:00:00Z).
 * A resposta inclui: período, resumo, saldos por conta, transações recentes,
 * evolução de saldo, despesas/receitas por categoria e resumo de cartão.
 */
interface DashboardApi {

    @GET("dashboard")
    suspend fun getDashboard(
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<DashboardResponseDto>
}
