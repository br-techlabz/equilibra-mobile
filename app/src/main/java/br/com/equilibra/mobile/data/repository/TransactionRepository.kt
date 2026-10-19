package br.com.equilibra.mobile.data.repository

import br.com.equilibra.mobile.data.local.transaction.TransactionDao
import br.com.equilibra.mobile.data.local.transaction.TransactionEntity
import br.com.equilibra.mobile.data.remote.ApiResult
import br.com.equilibra.mobile.data.remote.ExpenseApi
import br.com.equilibra.mobile.data.remote.IncomeApi
import br.com.equilibra.mobile.data.remote.TransferApi
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateExpenseRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateIncomeRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.CreateTransferRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.ExpenseDto
import br.com.equilibra.mobile.data.remote.dto.transaction.IncomeDto
import br.com.equilibra.mobile.data.remote.dto.transaction.TransferDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateExpenseRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateIncomeRequestDto
import br.com.equilibra.mobile.data.remote.dto.transaction.UpdateTransferRequestDto
import br.com.equilibra.mobile.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 4.3 — TransactionRepository (despesa, receita e transferência).
 *
 * - API é a fonte da verdade; Room guarda cache de até ~100 recentes.
 * - refresh(): sincronização determinística — por tipo, a primeira página
 *   (ordenada por occurredAt DESC no backend) substitui o cache daquele
 *   tipo (upsert + replaceByTypeSafe) e o trim global mantém 100 linhas.
 * - Escritas (create/update/cancel) só atualizam Room após sucesso remoto.
 * - Erros não apagam cache válido; em refresh parcial, o que sincronizou
 *   permanece e o primeiro erro é reportado.
 * - O histórico unificado (Task 4.11) NÃO usa este cache — consulta API
 *   direto, sem leitura/escrita Room.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val expenseApi: ExpenseApi,
    private val incomeApi: IncomeApi,
    private val transferApi: TransferApi,
    private val dao: TransactionDao
) {

    // Leitura do cache

    fun observeRecent(limit: Int = CACHE_LIMIT): Flow<List<TransactionEntity>> =
        dao.observeRecent(limit)

    fun observeByType(type: String): Flow<List<TransactionEntity>> = dao.observeByType(type)

    suspend fun getByIdCached(id: String): TransactionEntity? = dao.getById(id)

    // Paginação (listas — Task 4.5) — API direto, sem escrever no cache

    /**
     * Lista paginada de despesas conforme o contrato real (page 0-based,
     * size default 20, includeCancelled). Mapeia para [TransactionEntity]
     * sem persistir — o cache só é populado por [refresh].
     */
    suspend fun listExpenses(
        page: Int,
        includeCancelled: Boolean,
        size: Int = PAGE_SIZE_LIST
    ): ApiResult<TransactionPage> = when (val result = safeApiCall { expenseApi.list(page, size, includeCancelled) }) {
        is ApiResult.Success -> ApiResult.Success(
            TransactionPage(
                items = result.data.content.map { it.like().toEntity(TYPE_EXPENSE, syncedAt = null) },
                page = result.data.page,
                totalPages = result.data.totalPages,
                totalElements = result.data.totalElements
            )
        )
        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
        is ApiResult.UnknownError -> result
    }

    // Sincronização

    /**
     * Sincroniza o cache com a API. [type] nulo sincroniza os três tipos;
     * com tipo, sincroniza só aquele e não toca nos demais.
     */
    suspend fun refresh(type: String? = null): ApiResult<List<TransactionEntity>> {
        val types = if (type == null) ALL_TYPES else listOf(type)
        var firstError: ApiResult<Nothing>? = null
        val synced = mutableListOf<TransactionEntity>()

        for (t in types) {
            when (val result = syncType(t)) {
                is ApiResult.Success -> synced += result.data
                is ApiResult.HttpError -> if (firstError == null) firstError = result
                is ApiResult.NetworkError -> if (firstError == null) firstError = result
                is ApiResult.UnknownError -> if (firstError == null) firstError = result
            }
        }
        dao.trimToRecent(CACHE_LIMIT)

        @Suppress("UNCHECKED_CAST")
        return firstError as? ApiResult<List<TransactionEntity>> ?: ApiResult.Success(synced)
    }

    /** Sincroniza um tipo: primeira página (size 100) substitui o cache do tipo. */
    private suspend fun syncType(type: String): ApiResult<List<TransactionEntity>> {
        val now = System.currentTimeMillis()
        val result: ApiResult<List<TransactionDtoLike>> = when (type) {
            TYPE_EXPENSE ->
                mapped({ expenseApi.list(page = 0, size = SYNC_PAGE_SIZE) }) { it.content.map { d -> d.like() } }
            TYPE_INCOME ->
                mapped({ incomeApi.list(page = 0, size = SYNC_PAGE_SIZE) }) { it.content.map { d -> d.like() } }
            TYPE_TRANSFER ->
                mapped({ transferApi.list(page = 0, size = SYNC_PAGE_SIZE) }) { it.content.map { d -> d.like() } }
            else -> return ApiResult.UnknownError(IllegalArgumentException("Tipo desconhecido: $type"))
        }
        return when (result) {
            is ApiResult.Success -> {
                val entities = result.data.map { it.toEntity(type, now) }
                dao.upsertAll(entities)
                dao.replaceByTypeSafe(type, entities.map { it.id })
                ApiResult.Success(entities)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    private suspend fun <P> mapped(
        call: suspend () -> Response<P>,
        extract: (P) -> List<TransactionDtoLike>
    ): ApiResult<List<TransactionDtoLike>> = when (val result = safeApiCall(call)) {
        is ApiResult.Success -> ApiResult.Success(extract(result.data))
        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
        is ApiResult.UnknownError -> result
    }

    /** Atualiza um item do cache via GET /{id} do tipo correspondente. */
    suspend fun refreshById(type: String, id: String): ApiResult<TransactionEntity> {
        val result: ApiResult<TransactionEntity> = when (type) {
            TYPE_EXPENSE -> toEntityResult(safeApiCall { expenseApi.get(id) }, TYPE_EXPENSE) { it.like() }
            TYPE_INCOME -> toEntityResult(safeApiCall { incomeApi.get(id) }, TYPE_INCOME) { it.like() }
            TYPE_TRANSFER -> toEntityResult(safeApiCall { transferApi.get(id) }, TYPE_TRANSFER) { it.like() }
            else -> ApiResult.UnknownError(IllegalArgumentException("Tipo desconhecido: $type"))
        }
        if (result is ApiResult.Success) dao.upsert(result.data)
        return result
    }

    // Escritas — Room só após sucesso remoto

    suspend fun createExpense(
        description: String,
        occurredAt: String,
        accountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = CreateExpenseRequestDto(description, occurredAt, accountId, amount, categoryId, notes, tagIds)
        return writeAndPersist({ expenseApi.create(body) }, TYPE_EXPENSE) { it.like() }
    }

    suspend fun updateExpense(
        id: String,
        description: String,
        occurredAt: String,
        accountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = UpdateExpenseRequestDto(description, occurredAt, accountId, amount, categoryId, notes, tagIds)
        return writeAndPersist({ expenseApi.update(id, body) }, TYPE_EXPENSE) { it.like() }
    }

    suspend fun cancelExpense(id: String): ApiResult<TransactionEntity> =
        writeAndPersist({ expenseApi.cancel(id) }, TYPE_EXPENSE) { it.like() }

    suspend fun createIncome(
        description: String,
        occurredAt: String,
        accountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = CreateIncomeRequestDto(description, occurredAt, accountId, amount, categoryId, notes, tagIds)
        return writeAndPersist({ incomeApi.create(body) }, TYPE_INCOME) { it.like() }
    }

    suspend fun updateIncome(
        id: String,
        description: String,
        occurredAt: String,
        accountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = UpdateIncomeRequestDto(description, occurredAt, accountId, amount, categoryId, notes, tagIds)
        return writeAndPersist({ incomeApi.update(id, body) }, TYPE_INCOME) { it.like() }
    }

    suspend fun cancelIncome(id: String): ApiResult<TransactionEntity> =
        writeAndPersist({ incomeApi.cancel(id) }, TYPE_INCOME) { it.like() }

    suspend fun createTransfer(
        description: String,
        occurredAt: String,
        sourceAccountId: String,
        destinationAccountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = CreateTransferRequestDto(
            description, occurredAt, sourceAccountId, destinationAccountId, amount, categoryId, notes, tagIds
        )
        return writeAndPersist({ transferApi.create(body) }, TYPE_TRANSFER) { it.like() }
    }

    suspend fun updateTransfer(
        id: String,
        description: String,
        occurredAt: String,
        sourceAccountId: String,
        destinationAccountId: String,
        amount: String,
        categoryId: String,
        notes: String? = null,
        tagIds: List<String>? = null
    ): ApiResult<TransactionEntity> {
        val body = UpdateTransferRequestDto(
            description, occurredAt, sourceAccountId, destinationAccountId, amount, categoryId, notes, tagIds
        )
        return writeAndPersist({ transferApi.update(id, body) }, TYPE_TRANSFER) { it.like() }
    }

    suspend fun cancelTransfer(id: String): ApiResult<TransactionEntity> =
        writeAndPersist({ transferApi.cancel(id) }, TYPE_TRANSFER) { it.like() }

    // Mapeamento DTO → Entity

    private suspend fun <D> writeAndPersist(
        call: suspend () -> Response<D>,
        type: String,
        map: (D) -> TransactionDtoLike
    ): ApiResult<TransactionEntity> {
        return when (val result = safeApiCall(call)) {
            is ApiResult.Success -> {
                val entity = map(result.data).toEntity(type, System.currentTimeMillis())
                dao.upsert(entity)
                ApiResult.Success(entity)
            }
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    private suspend fun <D> toEntityResult(
        result: ApiResult<D>,
        type: String,
        map: (D) -> TransactionDtoLike
    ): ApiResult<TransactionEntity> = when (result) {
        is ApiResult.Success ->
            ApiResult.Success(map(result.data).toEntity(type, System.currentTimeMillis()))
        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
        is ApiResult.UnknownError -> result
    }

    /** Contrato mínimo comum aos DTOs de resposta de transação. */
    private interface TransactionDtoLike {
        val id: String
        val description: String
        val occurredAt: String
        val amount: String
        val categoryId: String?
        val notes: String?
        val status: String
        val createdAt: String
        val updatedAt: String
        fun type(): String
        fun sourceAccountId(): String?
        fun destinationAccountId(): String?
    }

    private fun ExpenseDto.like() = object : TransactionDtoLike {
        override val id = this@like.id
        override val description = this@like.description
        override val occurredAt = this@like.occurredAt
        override val amount = this@like.amount
        override val categoryId = this@like.categoryId
        override val notes = this@like.notes
        override val status = this@like.status
        override val createdAt = this@like.createdAt
        override val updatedAt = this@like.updatedAt
        override fun type() = TYPE_EXPENSE
        override fun sourceAccountId() = this@like.accountId // despesa sai da conta
        override fun destinationAccountId(): String? = null
    }

    private fun IncomeDto.like() = object : TransactionDtoLike {
        override val id = this@like.id
        override val description = this@like.description
        override val occurredAt = this@like.occurredAt
        override val amount = this@like.amount
        override val categoryId = this@like.categoryId
        override val notes = this@like.notes
        override val status = this@like.status
        override val createdAt = this@like.createdAt
        override val updatedAt = this@like.updatedAt
        override fun type() = TYPE_INCOME
        override fun sourceAccountId(): String? = null
        override fun destinationAccountId() = this@like.accountId // receita entra na conta
    }

    private fun TransferDto.like() = object : TransactionDtoLike {
        override val id = this@like.id
        override val description = this@like.description
        override val occurredAt = this@like.occurredAt
        override val amount = this@like.amount
        override val categoryId = this@like.categoryId
        override val notes = this@like.notes
        override val status = this@like.status
        override val createdAt = this@like.createdAt
        override val updatedAt = this@like.updatedAt
        override fun type() = TYPE_TRANSFER
        override fun sourceAccountId() = this@like.sourceAccountId
        override fun destinationAccountId() = this@like.destinationAccountId
    }

    private fun TransactionDtoLike.toEntity(type: String, syncedAt: Long?) = TransactionEntity(
        id = id,
        type = type,
        status = status,
        description = description,
        occurredAt = occurredAt,
        amount = amount,
        categoryId = categoryId,
        sourceAccountId = sourceAccountId(),
        destinationAccountId = destinationAccountId(),
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt
    )

    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_INCOME = "INCOME"
        const val TYPE_TRANSFER = "TRANSFER"
        const val CACHE_LIMIT = 100
        const val SYNC_PAGE_SIZE = 100
        const val PAGE_SIZE_LIST = 20
        private val ALL_TYPES = listOf(TYPE_EXPENSE, TYPE_INCOME, TYPE_TRANSFER)
    }
}

/**
 * Página de transações mapeadas para exibição (não persiste no cache).
 * totalPages 0-based: última página = totalPages - 1 (página vazia → 0).
 */
data class TransactionPage(
    val items: List<TransactionEntity>,
    val page: Int,
    val totalPages: Int,
    val totalElements: Long
) {
    val isLastPage: Boolean get() = page >= totalPages - 1 || items.isEmpty()
}
