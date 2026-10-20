package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.TransactionHistoryApi
import br.com.equilibra.mobile.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 4.11 — Filtros do histórico unificado.
 *
 * Valores já normalizados para o contrato: from/to em Instant ISO-8601
 * (início/fim do dia em UTC); nulo/ vazio = filtro ausente (parâmetro
 * omitido na query).
 */
data class HistoryFilters(
    val type: String? = null,
    val status: String? = null,
    val fromIso: String? = null,
    val toIso: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val tagIds: List<String> = emptyList()
) {
    companion object {
        fun empty() = HistoryFilters()
    }
}

/**
 * Task 4.11 — Histórico unificado SEM cache.
 *
 * A API é sempre a fonte da tela: este repositório não tem DAO e não
 * lê/escreve Room (diferente do TransactionRepository, das listas 4.5/4.7).
 * Mapeia o shape único do TransactionHistoryResponse para
 * [TransactionEntity] (apenas exibição; syncedAt nulo, nunca persistido).
 */
@Singleton
class TransactionHistoryRepository @Inject constructor(
    private val api: TransactionHistoryApi
) {

    suspend fun list(
        page: Int,
        filters: HistoryFilters,
        size: Int = TransactionRepository.PAGE_SIZE_LIST
    ): ApiResult<TransactionPage> {
        return when (val result = safeApiCall {
            api.list(
                page = page,
                size = size,
                type = filters.type,
                status = filters.status,
                from = filters.fromIso,
                to = filters.toIso,
                accountId = filters.accountId,
                categoryId = filters.categoryId,
                tagIds = filters.tagIds.ifEmpty { null }
            )
        }) {
            is ApiResult.Success -> ApiResult.Success(
                TransactionPage(
                    items = result.data.content.map { dto ->
                        TransactionEntity(
                            id = dto.id,
                            type = dto.type,
                            status = dto.status,
                            description = dto.description,
                            occurredAt = dto.occurredAt,
                            amount = dto.amount,
                            categoryId = dto.categoryId,
                            sourceAccountId = dto.sourceAccountId,
                            destinationAccountId = dto.destinationAccountId,
                            notes = dto.notes,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt,
                            syncedAt = null // nunca persiste
                        )
                    },
                    page = result.data.page,
                    totalPages = result.data.totalPages,
                    totalElements = result.data.totalElements
                )
            )
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }
}
