package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.account.AssetAccountDto
import br.com.equilibra.mobile.data.remote.dto.account.CreateAssetAccountRequestDto
import br.com.equilibra.mobile.data.remote.dto.account.ReconcileAccountRequestDto
import br.com.equilibra.mobile.data.remote.dto.account.ReconciliationResultDto
import br.com.equilibra.mobile.data.remote.dto.account.UpdateAssetAccountRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 3.3 — AssetAccountApi
 *
 * Endpoints reais do backend (AssetAccountController, /asset-accounts):
 * - POST   /  → criar (CreateAssetAccountRequest, 201)
 * - GET    /?includeInactive → listar
 * - GET    /{id} → obter
 * - PUT    /{id} → atualizar (UpdateAssetAccountRequest)
 * - PATCH  /{id}/deactivate → desativar
 * - PATCH  /{id}/activate   → ativar
 * - POST   /{id}/reconcile → reconciliar (ReconcileAccountRequest)
 */
interface AssetAccountApi {

    @POST("asset-accounts")
    suspend fun create(
        @Body body: CreateAssetAccountRequestDto
    ): Response<AssetAccountDto>

    @GET("asset-accounts")
    suspend fun list(
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<AssetAccountDto>>

    @GET("asset-accounts/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<AssetAccountDto>

    @PUT("asset-accounts/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateAssetAccountRequestDto
    ): Response<AssetAccountDto>

    @PATCH("asset-accounts/{id}/deactivate")
    suspend fun deactivate(
        @Path("id") id: String
    ): Response<AssetAccountDto>

    @PATCH("asset-accounts/{id}/activate")
    suspend fun activate(
        @Path("id") id: String
    ): Response<AssetAccountDto>

    @POST("asset-accounts/{id}/reconcile")
    suspend fun reconcile(
        @Path("id") id: String,
        @Body body: ReconcileAccountRequestDto
    ): Response<ReconciliationResultDto>
}
