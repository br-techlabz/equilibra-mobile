package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.category.CategoryDto
import br.com.equilibra.mobile.data.remote.dto.category.CreateCategoryRequestDto
import br.com.equilibra.mobile.data.remote.dto.category.UpdateCategoryRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 3.9 — CategoryApi (contrato real do CategoryController, /categories).
 *
 * - POST / → criar (201)
 * - GET /?includeInactive=&applicability= → listar (applicability filtra
 *   EXPENSE→EXPENSE+BOTH, INCOME→INCOME+BOTH no backend)
 * - GET /{id} → obter
 * - PUT /{id} → atualizar (name, applicability)
 * - PATCH /{id}/deactivate → desativar
 * - PATCH /{id}/activate → ativar
 */
interface CategoryApi {

    @POST("categories")
    suspend fun create(
        @Body body: CreateCategoryRequestDto
    ): Response<CategoryDto>

    @GET("categories")
    suspend fun list(
        @Query("includeInactive") includeInactive: Boolean = false,
        @Query("applicability") applicability: String? = null
    ): Response<List<CategoryDto>>

    @GET("categories/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<CategoryDto>

    @PUT("categories/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateCategoryRequestDto
    ): Response<CategoryDto>

    @PATCH("categories/{id}/deactivate")
    suspend fun deactivate(
        @Path("id") id: String
    ): Response<CategoryDto>

    @PATCH("categories/{id}/activate")
    suspend fun activate(
        @Path("id") id: String
    ): Response<CategoryDto>
}
