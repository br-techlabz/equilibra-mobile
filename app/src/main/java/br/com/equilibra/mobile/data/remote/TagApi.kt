package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.tag.CreateTagRequestDto
import br.com.equilibra.mobile.data.remote.dto.tag.TagDto
import br.com.equilibra.mobile.data.remote.dto.tag.UpdateTagRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Task 3.11 — TagApi (contrato real do TagController, /tags).
 *
 * - POST / → criar (201, body: name max 100)
 * - GET /?includeInactive= → listar (default includeInactive=false)
 * - GET /{id} → obter
 * - PUT /{id} → atualizar (name)
 * - PATCH /{id}/deactivate → desativar
 * - PATCH /{id}/activate → ativar
 */
interface TagApi {

    @POST("tags")
    suspend fun create(
        @Body body: CreateTagRequestDto
    ): Response<TagDto>

    @GET("tags")
    suspend fun list(
        @Query("includeInactive") includeInactive: Boolean = false
    ): Response<List<TagDto>>

    @GET("tags/{id}")
    suspend fun get(
        @Path("id") id: String
    ): Response<TagDto>

    @PUT("tags/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateTagRequestDto
    ): Response<TagDto>

    @PATCH("tags/{id}/deactivate")
    suspend fun deactivate(
        @Path("id") id: String
    ): Response<TagDto>

    @PATCH("tags/{id}/activate")
    suspend fun activate(
        @Path("id") id: String
    ): Response<TagDto>
}
