package br.com.equilibra.mobile.data.remote

/**
 * Resultado tipado para chamadas HTTP. Converte erros HTTP em casos tratáveis
 * pela UI sem espalhar Retrofit/OkHttp pelos ViewModels.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String?, val body: String?) : ApiResult<Nothing>()
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>()
    data class UnknownError(val throwable: Throwable) : ApiResult<Nothing>()
}

/**
 * Converte Retrofit Response em ApiResult.
 */
fun <T> retrofit2.Response<T>.toApiResult(): ApiResult<T> {
    return if (isSuccessful) {
        val body = body()
        if (body != null) ApiResult.Success(body)
        else ApiResult.HttpError(code(), message(), null)
    } else {
        val errorBody = try { errorBody()?.string() } catch (_: Exception) { null }
        ApiResult.HttpError(code(), message(), errorBody)
    }
}

suspend fun <T> safeApiCall(block: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        block().toApiResult()
    } catch (e: java.io.IOException) {
        ApiResult.NetworkError(e)
    } catch (e: Exception) {
        ApiResult.UnknownError(e)
    }
}
