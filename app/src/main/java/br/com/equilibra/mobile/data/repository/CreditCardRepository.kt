package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.creditcard.CreditCardDao
import br.com.equilibra.mobile.data.local.creditcard.CreditCardEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.CreditCardApi
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreateCreditCardRequestDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.CreditCardDto
import br.com.equilibra.mobile.data.remote.dto.creditcard.UpdateCreditCardRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 6.2 — CreditCardRepository
 *
 * - API como fonte da verdade; Room como cache.
 * - Leitura cache-first: Flow emite o cache imediatamente; refresh() traz da API.
 * - Escritas (create/update/activate/deactivate) só atualizam Room após sucesso remoto.
 * - Erros não apagam cache válido.
 * - Limites (committed/available/reserved) vêm calculados do backend: o repository
 *   nunca calcula nem infere (backend é autoridade para limites — ver Task 6.1).
 */
@Singleton
class CreditCardRepository @Inject constructor(
    private val api: CreditCardApi,
    private val dao: CreditCardDao
) {

    // Leitura

    fun observeCards(): Flow<List<CreditCardEntity>> = dao.observeAll()

    fun observeActiveCards(): Flow<List<CreditCardEntity>> = dao.observeActive()

    fun observeById(id: String): Flow<CreditCardEntity?> = dao.observeById(id)

    suspend fun getAllCached(): List<CreditCardEntity> = dao.getAll()

    suspend fun getByIdCached(id: String): CreditCardEntity? = dao.getById(id)

    /**
     * Sincroniza cartões da API para o Room.
     * - Sucesso: upsert + deleteNotInSafe (remove locais que não existem mais no servidor).
     *   Como a lista sempre contempla ativos e inativas, o cache fica igual ao servidor.
     * - Erro: retorna ApiResult de erro e NÃO limpa cache.
     */
    suspend fun refresh(): ApiResult<List<CreditCardEntity>> {
        return when (val result = safeApiCall { api.list(includeInactive = true) }) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val entities = result.data.map { it.toEntity(now) }
                dao.upsertAll(entities)
                dao.deleteNotInSafe(entities.map { it.id })
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun refreshById(id: String): ApiResult<CreditCardEntity> {
        return when (val result = safeApiCall { api.get(id) }) {
            is ApiResult.Success -> {
                val entity = result.data.toEntity(System.currentTimeMillis())
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    // Escritas — só atualizam Room após sucesso remoto

    suspend fun create(
        name: String,
        institution: String,
        nickname: String?,
        totalLimit: String,
        closingDay: Int,
        dueDay: Int,
        /** null deixa o backend aplicar o default ("America/Sao_Paulo"). */
        timezone: String?
    ): ApiResult<CreditCardEntity> {
        val body = CreateCreditCardRequestDto(
            name = name,
            institution = institution,
            nickname = nickname,
            totalLimit = totalLimit,
            closingDay = closingDay,
            dueDay = dueDay,
            timezone = timezone
        )
        return when (val result = safeApiCall { api.create(body) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toEntity(System.currentTimeMillis()).also { dao.upsert(it) })
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun update(
        id: String,
        name: String,
        institution: String,
        nickname: String?,
        totalLimit: String,
        closingDay: Int,
        dueDay: Int,
        timezone: String?
    ): ApiResult<CreditCardEntity> {
        val body = UpdateCreditCardRequestDto(
            name = name,
            institution = institution,
            nickname = nickname,
            totalLimit = totalLimit,
            closingDay = closingDay,
            dueDay = dueDay,
            timezone = timezone
        )
        return when (val result = safeApiCall { api.update(id, body) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toEntity(System.currentTimeMillis()).also { dao.upsert(it) })
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /** PATCH /{id}/deactivate — preserva compras/faturas (não há delete local). */
    suspend fun deactivate(id: String): ApiResult<CreditCardEntity> {
        return when (val result = safeApiCall { api.deactivate(id) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toEntity(System.currentTimeMillis()).also { dao.upsert(it) })
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /** PATCH /{id}/activate — desativar/reativar nunca apagam dados locais. */
    suspend fun activate(id: String): ApiResult<CreditCardEntity> {
        return when (val result = safeApiCall { api.activate(id) }) {
            is ApiResult.Success -> ApiResult.Success(result.data.toEntity(System.currentTimeMillis()).also { dao.upsert(it) })
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /** Mapeia DTO → Entity com syncedAt=now; limites copiados, nunca recalculados. */
    private fun CreditCardDto.toEntity(syncedAt: Long): CreditCardEntity = CreditCardEntity(
        id = id,
        name = name,
        institution = institution,
        nickname = nickname,
        totalLimit = totalLimit,
        committedLimit = committedLimit,
        reservedLimit = reservedLimit,
        availableLimit = availableLimit,
        closingDay = closingDay,
        dueDay = dueDay,
        timezone = timezone,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt
    )
}
