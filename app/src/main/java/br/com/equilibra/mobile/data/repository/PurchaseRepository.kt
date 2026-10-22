package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.purchase.InstallmentEntity
import br.com.equilibra.mobile.data.local.purchase.PurchaseDao
import br.com.equilibra.mobile.data.local.purchase.PurchaseEntity
import br.com.equilibra.mobile.data.local.purchase.toEntity
import br.com.equilibra.mobile.data.local.purchase.toInstallmentEntities
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.PurchaseApi
import br.com.equilibra.mobile.data.remote.dto.purchase.CreatePurchaseRequestDto
import br.com.equilibra.mobile.data.remote.dto.purchase.PurchaseDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 6.7 — PurchaseRepository
 *
 * - API como fonte da verdade; Room é só cache de leitura.
 * - Leitura cache-first: os Flows emitem o cache imediatamente e quem
 *   chama decide quando buscar da API.
 * - Escritas (create/cancel) só tocam o cache DEPOIS da confirmação do
 *   backend — nada é aplicado localmente antes, nem como otimismo.
 * - Falha (HTTP ou rede) preserva o cache anterior; erro nunca apaga dado bom.
 * - Sincronização é POR CARTÃO: remove do cache apenas o que sumiu no
 *   servidor naquele cartão, sem tocar nas compras dos demais.
 */
@Singleton
class PurchaseRepository @Inject constructor(
    private val api: PurchaseApi,
    private val dao: PurchaseDao
) {

    // Leitura (cache-first)

    fun observeByCard(cardId: String): Flow<List<PurchaseEntity>> = dao.observeByCard(cardId)

    fun observePurchase(id: String): Flow<PurchaseEntity?> = dao.observePurchaseById(id)

    fun observeInstallments(purchaseId: String): Flow<List<InstallmentEntity>> =
        dao.observeInstallments(purchaseId)

    suspend fun getCached(id: String): PurchaseEntity? = dao.getPurchaseById(id)

    suspend fun getInstallmentsCached(purchaseId: String): List<InstallmentEntity> =
        dao.getInstallments(purchaseId)

    // Sincronização

    /**
     * Traz da API as compras do cartão e substitui a fatia do cache daquele
     * cartão. As compras ausentes na resposta são removidas (o backend lista
     * todas, inclusive canceladas) — as parcelas vão junto pela FK CASCADE.
     * Em erro, retorna o [ApiResult] de falha e não mexe no cache.
     */
    suspend fun refreshByCard(cardId: String): ApiResult<List<PurchaseEntity>> {
        return when (val result = safeApiCall { api.listByCard(cardId) }) {
            is ApiResult.Success -> {
                val now = System.currentTimeMillis()
                val entities = result.data.map { it.toEntity(now) }
                saveAll(entities, result.data)
                dao.deleteByCardNotInSafe(cardId, entities.map { it.id })
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /**
     * Busca uma compra por id na API (com suas parcelas) e atualiza o cache.
     * É o caminho do detalhe (Task 6.9): o cache local pode nem ter a compra
     * ou pode estar desatualizado. Em erro, preserva o cache e propaga.
     */
    suspend fun refreshById(id: String): ApiResult<PurchaseEntity> {
        return when (val result = safeApiCall { api.get(id) }) {
            is ApiResult.Success -> {
                val entity = result.data.toEntity(System.currentTimeMillis())
                saveAll(listOf(entity), listOf(result.data))
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    // Escritas (cache só depois da confirmação)

    suspend fun create(
        description: String,
        purchaseDate: String,
        purchaseAt: String,
        totalAmount: String,
        installmentCount: Int,
        creditCardId: String,
        categoryId: String
    ): ApiResult<PurchaseEntity> {
        val body = CreatePurchaseRequestDto(
            description = description,
            purchaseDate = purchaseDate,
            purchaseAt = purchaseAt,
            totalAmount = totalAmount,
            installmentCount = installmentCount,
            creditCardId = creditCardId,
            categoryId = categoryId
        )
        return when (val result = safeApiCall { api.create(body) }) {
            is ApiResult.Success -> {
                val entity = result.data.toEntity(System.currentTimeMillis())
                saveAll(listOf(entity), listOf(result.data))
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /** Cancela a compra; o status do cache só muda após a confirmação remota. */
    suspend fun cancel(id: String): ApiResult<PurchaseEntity> {
        return when (val result = safeApiCall { api.cancel(id) }) {
            is ApiResult.Success -> {
                val entity = result.data.toEntity(System.currentTimeMillis())
                saveAll(listOf(entity), listOf(result.data))
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    /**
     * Grava compras e parcelas numa transação por compra: [PurchaseDao]
     * substitui as parcelas da compra junto, então re-sincronizar é idempotente.
     */
    private suspend fun saveAll(entities: List<PurchaseEntity>, dtos: List<PurchaseDto>) {
        dtos.forEachIndexed { index, dto ->
            dao.upsertPurchaseWithInstallments(entities[index], dto.toInstallmentEntities())
        }
    }
}
