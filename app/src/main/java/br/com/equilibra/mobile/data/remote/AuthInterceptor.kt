package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adiciona Authorization: Bearer <token> em chamadas protegidas.
 * Endpoints públicos (login, register) não recebem o header.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    companion object {
        private val PUBLIC_PATHS = setOf(
            "/auth/login",
            "/auth/register"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Não anexa token em endpoints públicos
        val isPublic = PUBLIC_PATHS.any { path.endsWith(it) }
        if (isPublic) return chain.proceed(request)

        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) return chain.proceed(request)

        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authenticated)
    }
}
