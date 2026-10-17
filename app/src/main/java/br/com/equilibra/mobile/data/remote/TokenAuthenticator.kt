package br.com.equilibra.mobile.data.remote

import br.com.equilibra.mobile.BuildConfig
import br.com.equilibra.mobile.data.session.SessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticator que tenta renovar o access token ao receber 401.
 *
 * - Usa OkHttp Authenticator (não Interceptor) para evitar loops.
 * - Sincroniza refresh com lock para evitar concorrência de múltiplos refreshes.
 * - Faz POST /auth/refresh enviando o cookie httpOnly via CookieJar.
 * - Em caso de falha (refresh expirado/revogado), limpa sessão e não retenta.
 * - Retenta a requisição original apenas uma vez com o novo token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val cookieJar: PersistentCookieJar,
    private val okHttpClientProvider: dagger.Lazy<okhttp3.OkHttpClient>
) : Authenticator {

    private val refreshLock = ReentrantLock()
    @Volatile private var refreshInProgress = false

    override fun authenticate(route: Route?, response: Response): Request? {
        // Evita retry infinito: só tenta uma vez por cadeia de 401
        if (responseCount(response) >= 2) return null

        // Não tenta refresh para o próprio endpoint de refresh/login/logout
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/refresh") || path.endsWith("/auth/login") || path.endsWith("/auth/logout")) {
            return null
        }

        // Se já existe um token diferente do que causou o 401, usa-o
        val currentToken = sessionManager.getAccessToken()
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
        if (!failedToken.isNullOrBlank() && failedToken != currentToken && !currentToken.isNullOrBlank()) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        // Tenta refresh sincronizado
        val newToken = runBlocking { refreshAccessToken() } ?: run {
            // Refresh falhou → limpa sessão (logout seguro local)
            runBlocking { sessionManager.clearSession() }
            cookieJar.clear()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private suspend fun refreshAccessToken(): String? {
        // Se outro thread já está fazendo refresh, aguarda
        if (refreshLock.isLocked) {
            refreshLock.lock()
            try {
                // Após aguardar, verifica se o token já foi renovado
                return sessionManager.getAccessToken()?.takeIf { it.isNotBlank() }
            } finally {
                refreshLock.unlock()
            }
        }

        refreshLock.lock()
        try {
            if (refreshInProgress) return sessionManager.getAccessToken()
            refreshInProgress = true

            // Cliente sem Authenticator para evitar recursão
            val plainClient = okHttpClientProvider.get().newBuilder()
                .authenticator(Authenticator.NONE)
                .build()

            val refreshRequest = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}auth/refresh")
                .post(ByteArray(0).toRequestBody(null))
                .header("Content-Type", "application/json")
                .build()

            val refreshResponse = try {
                plainClient.newCall(refreshRequest).execute()
            } catch (_: Exception) {
                return null
            } finally {
                refreshInProgress = false
            }

            if (!refreshResponse.isSuccessful) {
                refreshResponse.close()
                return null
            }

            val body = refreshResponse.body?.string() ?: return null
            refreshResponse.close()

            // Extrai accessToken do JSON (sem logar o valor)
            val newToken = try {
                val json = Json.parseToJsonElement(body).jsonObject
                json["accessToken"]?.jsonPrimitive?.content
            } catch (_: Exception) { null }

            if (newToken.isNullOrBlank()) return null

            sessionManager.saveAccessToken(newToken)
            return newToken
        } finally {
            refreshInProgress = false
            if (refreshLock.isHeldByCurrentThread) refreshLock.unlock()
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
