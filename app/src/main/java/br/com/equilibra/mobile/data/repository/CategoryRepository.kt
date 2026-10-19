package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.category.CategoryDao
import br.com.equilibra.mobile.data.local.category.CategoryEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.CategoryApi
import br.com.equilibra.mobile.data.remote.dto.category.CategoryApplicabilityDto
import br.com.equilibra.mobile.data.remote.dto.category.CreateCategoryRequestDto
import br.com.equilibra.mobile.data.remote.dto.category.UpdateCategoryRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 3.10 — CategoryRepository
 *
 * - API como fonte da verdade; Room como cache.
 * - Leitura: Flow emite cache imediatamente e depois atualiza com API (refresh).
 * - pull-to-refresh consulta API explicitamente.
 * - Escritas (create/update/activate/deactivate) só atualizam Room após sucesso remoto.
 * - Erros não apagam cache válido.
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val api: CategoryApi,
    private val dao: CategoryDao
) {

    // Leitura

    fun observeCategories(applicability: String? = null): Flow<List<CategoryEntity>> {
        return if (applicability == null) dao.observeAll() else dao.observeByApplicability(applicability)
    }

    suspend fun getAllCached(applicability: String? = null): List<CategoryEntity> {
        return if (applicability == null) {
            dao.getAll()
        } else {
            dao.observeByApplicability(applicability).first()
        }
    }

    suspend fun getByIdCached(id: String): CategoryEntity? = dao.getById(id)

    /**
     * Sincroniza categorias da API para o Room.
     * - Sucesso: upsert + deleteNotInSafe (remove locais que não existem mais no servidor).
     * - Erro: retorna ApiResult de erro e NÃO limpa cache.
     */
    suspend fun refresh(
        applicability: String? = null,
        includeInactive: Boolean = false
    ): ApiResult<List<CategoryEntity>> {
        return when (val result = safeApiCall { api.list(includeInactive, applicability) }) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val entities = result.data.map { dto ->
                    CategoryEntity(
                        id = dto.id,
                        name = dto.name,
                        applicability = dto.applicability,
                        active = dto.active,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        syncedAt = now
                    )
                }
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

    suspend fun refreshById(id: String): ApiResult<CategoryEntity> {
        return when (val result = safeApiCall { api.get(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    applicability = dto.applicability,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncedAt = System.currentTimeMillis()
                )
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    // Escritas — só atualizam Room após sucesso remoto

    suspend fun create(name: String, applicability: String): ApiResult<CategoryEntity> {
        val body = CreateCategoryRequestDto(name = name, applicability = applicability)
        return when (val result = safeApiCall { api.create(body) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    applicability = dto.applicability,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncedAt = System.currentTimeMillis()
                )
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun update(id: String, name: String, applicability: String): ApiResult<CategoryEntity> {
        val body = UpdateCategoryRequestDto(name = name, applicability = applicability)
        return when (val result = safeApiCall { api.update(id, body) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    applicability = dto.applicability,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncedAt = System.currentTimeMillis()
                )
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun deactivate(id: String): ApiResult<CategoryEntity> {
        return when (val result = safeApiCall { api.deactivate(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    applicability = dto.applicability,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncedAt = System.currentTimeMillis()
                )
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun activate(id: String): ApiResult<CategoryEntity> {
        return when (val result = safeApiCall { api.activate(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    applicability = dto.applicability,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    syncedAt = System.currentTimeMillis()
                )
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }
}