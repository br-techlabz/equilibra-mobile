package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.account.AccountDao
import br.com.equilibra.mobile.data.local.account.AccountEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.AssetAccountApi
import br.com.equilibra.mobile.data.remote.dto.account.CreateAssetAccountRequestDto
import br.com.equilibra.mobile.data.remote.dto.account.UpdateAssetAccountRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 3.4 — AccountRepository
 *
 * - API como fonte da verdade; Room como cache.
 * - Leitura: Flow emite cache imediatamente e depois atualiza com API (refresh).
 * - pull-to-refresh consulta API explicitamente.
 * - Escritas (create/update/activate/deactivate/reconcile) só atualizam Room após sucesso remoto.
 * - Erros não apagam cache válido.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val api: AssetAccountApi,
    private val dao: AccountDao
) {

    // Leitura

    fun observeAccounts(): Flow<List<AccountEntity>> = dao.observeAll()

    suspend fun getAllCached(): List<AccountEntity> = dao.getAll()

    suspend fun getByIdCached(id: String): AccountEntity? = dao.getById(id)

    fun observeById(id: String): Flow<AccountEntity?> = dao.observeById(id)

    /**
     * Sincroniza contas da API para o Room.
     * - Sucesso: upsert + deleteNotInSafe (remove locais que não existem mais no servidor).
     * - Erro: retorna ApiResult de erro e NÃO limpa cache.
     */
    suspend fun refresh(includeInactive: Boolean = true): ApiResult<List<AccountEntity>> {
        return when (val result = safeApiCall { api.list(includeInactive) }) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val entities = result.data.map { dto ->
                    AccountEntity(
                        id = dto.id,
                        name = dto.name,
                        type = dto.type,
                        initialBalance = dto.initialBalance,
                        currentBalance = dto.currentBalance,
                        active = dto.active,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        syncedAt = now
                    )
                }
                dao.upsertAll(entities)
                // Remove locais que não existem mais no servidor (apenas quando inclui inativas)
                if (includeInactive) {
                    dao.deleteNotInSafe(entities.map { it.id })
                }
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    suspend fun refreshById(id: String): ApiResult<AccountEntity> {
        return when (val result = safeApiCall { api.get(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    initialBalance = dto.initialBalance,
                    currentBalance = dto.currentBalance,
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

    suspend fun create(name: String, type: String, initialBalance: String): ApiResult<AccountEntity> {
        val body = CreateAssetAccountRequestDto(name = name, type = type, initialBalance = initialBalance)
        return when (val result = safeApiCall { api.create(body) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    initialBalance = dto.initialBalance,
                    currentBalance = dto.currentBalance,
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

    suspend fun update(id: String, name: String, type: String, initialBalance: String): ApiResult<AccountEntity> {
        val body = UpdateAssetAccountRequestDto(name = name, type = type, initialBalance = initialBalance)
        return when (val result = safeApiCall { api.update(id, body) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    initialBalance = dto.initialBalance,
                    currentBalance = dto.currentBalance,
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

    suspend fun deactivate(id: String): ApiResult<AccountEntity> {
        return when (val result = safeApiCall { api.deactivate(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    initialBalance = dto.initialBalance,
                    currentBalance = dto.currentBalance,
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

    suspend fun activate(id: String): ApiResult<AccountEntity> {
        return when (val result = safeApiCall { api.activate(id) }) {
            is ApiResult.Success -> {
                val dto = result.data
                val entity = AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    type = dto.type,
                    initialBalance = dto.initialBalance,
                    currentBalance = dto.currentBalance,
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

    /**
     * Reconciliação — POST /asset-accounts/{id}/reconcile
     * Backend cria ajuste se houver diferença e retorna ReconciliationResultDto.
     * Após sucesso, recarrega a conta do servidor para atualizar currentBalance.
     */
    suspend fun reconcile(
        id: String,
        reconciliationDate: String,
        reconciliationTime: String,
        informedBalance: String
    ): ApiResult<br.com.equilibra.mobile.data.remote.dto.account.ReconciliationResultDto> {
        val body = br.com.equilibra.mobile.data.remote.dto.account.ReconcileAccountRequestDto(
            reconciliationDate = reconciliationDate,
            reconciliationTime = reconciliationTime,
            informedBalance = informedBalance
        )
        return when (val result = safeApiCall { api.reconcile(id, body) }) {
            is ApiResult.Success -> {
                // Recarrega conta após reconciliação para atualizar saldos
                // (não falha o reconcile se o refresh falhar)
                try { refreshById(id) } catch (_: Exception) { }
                ApiResult.Success(result.data)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }
}
