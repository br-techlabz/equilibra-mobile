package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.statement.StatementDao
import br.com.equilibra.mobile.data.local.statement.StatementEntity
import br.com.equilibra.mobile.data.local.statement.StatementItemEntity
import br.com.equilibra.mobile.data.local.statement.toEntity
import br.com.equilibra.mobile.data.local.statement.toItemEntities
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.StatementApi
import br.com.equilibra.mobile.data.remote.dto.statement.CloseStatementRequestDto
import br.com.equilibra.mobile.data.remote.dto.statement.StatementDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 7.2 — StatementRepository
 *
 * - API é a fonte da verdade; Room é apenas cache de leitura.
 * - Leitura cache-first: os Flows emitem o cache imediatamente e quem chama
 *   decide quando buscar da API.
 * - Toda escrita (close, refresh) só toca o cache DEPOIS da confirmação do
 *   backend — nada é aplicado localmente antes, nem como otimismo.
 * - Falha (HTTP ou rede) preserva o cache anterior; erro nunca apaga dado bom.
 * - Sincronização por cartão: remove do cache apenas as faturas que sumiram
 *   no servidor naquele cartão, sem tocar nas faturas dos demais.
 * - Status (OPEN/CLOSED/OVERDUE/PARTIALLY_PAID/PAID/CANCELLED) e agregados
 *   (total/pago/pendente) são copiados do backend. O app NÃO infere OVERDUE
 *   por data, NÃO muda status ao pagar e NÃO recalcula valores.
 *
 * O pagamento pertence à Task 7.6; depois de pagar, o chamador deve usar
 * [refreshCurrent] e/ou [refreshByCard] para sincronizar a resposta do
 * servidor (a fatura pode virar PARTIALLY_PAID/PAID).
 */
@Singleton
class StatementRepository @Inject constructor(
    private val api: StatementApi,
    private val dao: StatementDao
) {

    // Leitura (cache-first)

    fun observeByCard(cardId: String): Flow<List<StatementEntity>> = dao.observeByCard(cardId)

    /** Ciclo mais recente em cache — fallback até o /current-statement responder. */
    fun observeLatestByCard(cardId: String): Flow<StatementEntity?> =
        dao.observeLatestByCard(cardId)

    fun observeStatement(id: String): Flow<StatementEntity?> = dao.observeById(id)

    fun observeItems(statementId: String): Flow<List<StatementItemEntity>> =
        dao.observeItems(statementId)

    suspend fun getCached(id: String): StatementEntity? = dao.getById(id)

    suspend fun getCachedByCard(cardId: String): List<StatementEntity> = dao.getByCard(cardId)

    suspend fun getItemsCached(statementId: String): List<StatementItemEntity> =
        dao.getItems(statementId)

    // Sincronização

    /**
     * Traz da API o histórico do cartão e substitui a fatia do cache daquele
     * cartão. As faturas ausentes na resposta são removidas (o backend lista
     * todas) — os itens vão junto pela FK CASCADE.
     * Em erro, retorna o [ApiResult] de falha e não mexe no cache.
     */
    suspend fun refreshByCard(cardId: String): ApiResult<List<StatementEntity>> {
        return when (val result = safeApiCall { api.listByCard(cardId) }) {
            is ApiResult.Success -> {
                val entities = saveAll(result.data)
                dao.deleteByCardNotInSafe(cardId, entities.map { it.id })
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /**
     * Busca a fatura do ciclo atual na API e atualiza o cache sem apagar o
     * histórico já sincronizado. É o caminho da tela de faturas (Task 7.3)
     * para abrir com dados frescos.
     */
    suspend fun refreshCurrent(cardId: String): ApiResult<StatementEntity> {
        return when (val result = safeApiCall { api.current(cardId) }) {
            is ApiResult.Success -> ApiResult.Success(saveOne(result.data))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /**
     * Busca uma fatura por id (com itens) e atualiza o cache. É o caminho do
     * detalhe (Task 7.5): o cache local pode nem ter a fatura ou pode estar
     * desatualizado. Em erro, preserva o cache e propaga.
     */
    suspend fun refreshById(id: String): ApiResult<StatementEntity> {
        return when (val result = safeApiCall { api.get(id) }) {
            is ApiResult.Success -> ApiResult.Success(saveOne(result.data))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    // Escrita (cache só depois da confirmação)

    /**
     * Fecha a fatura do ciclo informado. O backend devolve a fatura já
     * fechada (status CLOSED/PAID e itens), então o cache é atualizado com a
     * resposta — nunca com um status "previsto" pelo cliente.
     * Para atualizar também a lista/current, o chamador pode usar
     * [refreshByCard]/[refreshCurrent] depois desta chamada.
     */
    suspend fun close(cardId: String, cycleMonth: String): ApiResult<StatementEntity> {
        val body = CloseStatementRequestDto(cycleMonth = cycleMonth)
        return when (val result = safeApiCall { api.close(cardId, body) }) {
            is ApiResult.Success -> ApiResult.Success(saveOne(result.data))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /**
     * Grava faturas e itens numa transação por fatura: [StatementDao]
     * substitui os itens da fatura junto, então re-sincronizar é idempotente.
     */
    private suspend fun saveAll(dtos: List<StatementDto>): List<StatementEntity> {
        val now = System.currentTimeMillis()
        val entities = dtos.map { it.toEntity(now) }
        dtos.forEachIndexed { index, dto ->
            dao.upsertStatementWithItems(entities[index], dto.toItemEntities())
        }
        return entities
    }

    private suspend fun saveOne(dto: StatementDto): StatementEntity {
        val entity = dto.toEntity(System.currentTimeMillis())
        dao.upsertStatementWithItems(entity, dto.toItemEntities())
        return entity
    }
}
