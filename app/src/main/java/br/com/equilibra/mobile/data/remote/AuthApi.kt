package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.remote.dto.CurrentUserDto
import br.com.equilibra.mobile.data.remote.dto.LoginRequestDto
import br.com.equilibra.mobile.data.remote.dto.LoginResponseDto
import br.com.equilibra.mobile.data.remote.dto.RegisterRequestDto
import br.com.equilibra.mobile.data.remote.dto.RegisteredUserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * API de autenticação — contrato real do backend Equilibra.
 *
 * Base: /api (Retrofit baseUrl já inclui /api/)
 * - POST auth/login  → 200 LoginResponseDto + Set-Cookie equilibra_refresh
 * - POST auth/refresh → 200 LoginResponseDto (cookie httpOnly)
 * - POST auth/logout → 204 + Set-Cookie clear
 * - POST auth/register → 201 RegisteredUserDto
 * - GET  users/me / api/users/me → 200 CurrentUserDto (autenticado)
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<RegisteredUserDto>

    @POST("auth/refresh")
    suspend fun refresh(): Response<LoginResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<CurrentUserDto>
}
