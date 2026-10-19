package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.tag.TagDao
import br.com.equilibra.mobile.data.local.tag.TagEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.TagApi
import br.com.equilibra.mobile.data.remote.dto.tag.CreateTagRequestDto
import br.com.equilibra.mobile.data.remote.dto.tag.TagDto
import br.com.equilibra.mobile.data.remote.dto.tag.UpdateTagRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 3.12 — TagRepository
 *
 * - API como fonte da verdade; Room como cache.
 * - Leitura: Flow emite cache imediatamente e depois atualiza com API (refresh).
 * - pull-to-refresh consulta API explicitamente.
 * - Escritas (create/update/activate/deactivate) só atualizam Room após sucesso remoto.
 * - Erros não apagam cache válido.
 */
@Singleton
class TagRepository @Inject constructor(
    private val api: TagApi,
    private val dao: TagDao
) {

    // Leitura

    fun observeTags(): Flow<List<TagEntity>> = dao.observeAll()

    suspend fun getAllCached(): List<TagEntity> = dao.getAll()

    suspend fun getByIdCached(id: String): TagEntity? = dao.getById(id)

    /**
     * Sincroniza tags da API para o Room.
     * - Sucesso: upsert + deleteNotInSafe (remove locais que não existem mais no servidor).
     * - Erro: retorna ApiResult de erro e NÃO limpa cache.
     */
    suspend fun refresh(includeInactive: Boolean = false): ApiResult<List<TagEntity>> {
        return when (val result = safeApiCall { api.list(includeInactive) }) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val entities = result.data.map { it.toEntity(now) }
                dao.upsertAll(entities)
                // Remove locais que não existem mais no servidor
                dao.deleteNotInSafe(entities.map { it.id })
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun refreshById(id: String): ApiResult<TagEntity> {
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

    suspend fun create(name: String): ApiResult<TagEntity> {
        val body = CreateTagRequestDto(name = name)
        return writeAndPersist { api.create(body) }
    }

    suspend fun update(id: String, name: String): ApiResult<TagEntity> {
        val body = UpdateTagRequestDto(name = name)
        return writeAndPersist { api.update(id, body) }
    }

    suspend fun deactivate(id: String): ApiResult<TagEntity> =
        writeAndPersist { api.deactivate(id) }

    suspend fun activate(id: String): ApiResult<TagEntity> =
        writeAndPersist { api.activate(id) }

    /** Executa escrita remota; persiste no Room apenas em sucesso. */
    private suspend fun writeAndPersist(
        call: suspend () -> retrofit2.Response<TagDto>
    ): ApiResult<TagEntity> {
        return when (val result = safeApiCall(call)) {
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

    private fun TagDto.toEntity(syncedAt: Long) = TagEntity(
        id = id,
        name = name,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt
    )
}
