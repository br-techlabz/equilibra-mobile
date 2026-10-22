package br.com.equilibra.mobile.data

import br.com.equilibra.mobile.data.local.EquilibraDatabase
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AuthApi
import br.com.equilibra.mobile.data.remote.PersistentCookieJar
import br.com.equilibra.mobile.data.remote.dto.LoginRequestDto
import br.com.equilibra.mobile.data.remote.dto.RegisterRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import br.com.equilibra.mobile.data.session.SessionManager
import br.com.equilibra.mobile.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val cookieJar: PersistentCookieJar,
    private val database: EquilibraDatabase
) {
    suspend fun login(email: String, password: String): ApiResult<Unit> {
        val result = safeApiCall { authApi.login(LoginRequestDto(email, password)) }
        return when (result) {
            is ApiResult.Success -> {
                sessionManager.saveAccessToken(result.data.accessToken)
                // Busca dados do usuário após login para persistir userId/email
                when (val me = safeApiCall { authApi.getCurrentUser() }) {
                    is ApiResult.Success -> {
                        sessionManager.saveUserSession(me.data.id, me.data.email, result.data.accessToken)
                    }
                    else -> {
                        // Token já salvo; me falhou mas login foi bem-sucedido
                    }
                }
                ApiResult.Success(Unit)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun register(email: String, password: String): ApiResult<User> {
        val result = safeApiCall { authApi.register(RegisterRequestDto(email, password)) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(User(result.data.id, result.data.email, result.data.createdAt))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun refresh(): ApiResult<Unit> {
        val result = safeApiCall { authApi.refresh() }
        return when (result) {
            is ApiResult.Success -> {
                sessionManager.saveAccessToken(result.data.accessToken)
                ApiResult.Success(Unit)
            }
            is ApiResult.HttpError -> ApiResult.HttpError(result.code, result.message, result.body)
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        // Tenta revogar no backend, mas sempre limpa local mesmo se falhar
        try { authApi.logout() } catch (_: Exception) { /* ignora */ }
        sessionManager.clearSession()
        cookieJar.clear()
        // O cache Room é por conta: sem isso, os dados de quem saiu aparecem
        // para o próximo usuário (bug real: categorias de outra conta no
        // seletor de um usuário recém-criado).
        database.clearAllTables()
        return ApiResult.Success(Unit)
    }

    suspend fun getCurrentUser(): ApiResult<User> {
        val result = safeApiCall { authApi.getCurrentUser() }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(User(result.data.id, result.data.email, result.data.createdAt))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }
}
