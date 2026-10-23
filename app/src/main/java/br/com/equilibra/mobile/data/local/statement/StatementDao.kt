package br.com.equilibra.mobile.data.local.statement

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Task 7.1 — DAO de faturas de cartão.
 *
 * Ordenação por cycleStart DESC, igual ao
 * findAllByOwnerIdAndCreditCardIdOrderByCycleStartDesc do backend.
 * A "fatura atual" continua sendo definida pela API
 * (/current-statement); [observeLatestByCard] é apenas um atalho
 * cache-first para o ciclo mais recente já sincronizado.
 */
@Dao
interface StatementDao {

    // Leitura

    /** Lista observável por cartão, do ciclo mais recente para o mais antigo. */
    @Query(
        """
        SELECT * FROM credit_card_statements
        WHERE creditCardId = :cardId
        ORDER BY cycleStart DESC
        """
    )
    fun observeByCard(cardId: String): Flow<List<StatementEntity>>

    @Query(
        """
        SELECT * FROM credit_card_statements
        WHERE creditCardId = :cardId
        ORDER BY cycleStart DESC
        """
    )
    suspend fun getByCard(cardId: String): List<StatementEntity>

    /** Ciclo mais recente em cache — fallback de leitura Room-first (Task 7.3). */
    @Query(
        """
        SELECT * FROM credit_card_statements
        WHERE creditCardId = :cardId
        ORDER BY cycleStart DESC
        LIMIT 1
        """
    )
    fun observeLatestByCard(cardId: String): Flow<StatementEntity?>

    @Query(
        """
        SELECT * FROM credit_card_statements
        WHERE creditCardId = :cardId
        ORDER BY cycleStart DESC
        LIMIT 1
        """
    )
    suspend fun getLatestByCard(cardId: String): StatementEntity?

    @Query("SELECT * FROM credit_card_statements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StatementEntity?

    @Query("SELECT * FROM credit_card_statements WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<StatementEntity?>

    /** Itens da fatura em ordem de sequência (1..n). */
    @Query(
        """
        SELECT * FROM statement_items
        WHERE statementId = :statementId
        ORDER BY sequenceNumber ASC, id ASC
        """
    )
    suspend fun getItems(statementId: String): List<StatementItemEntity>

    @Query(
        """
        SELECT * FROM statement_items
        WHERE statementId = :statementId
        ORDER BY sequenceNumber ASC, id ASC
        """
    )
    fun observeItems(statementId: String): Flow<List<StatementItemEntity>>

    // Escrita

    @Upsert
    suspend fun upsertStatement(entity: StatementEntity)

    @Upsert
    suspend fun upsertStatements(entities: List<StatementEntity>)

    @Upsert
    suspend fun upsertItem(entity: StatementItemEntity)

    @Upsert
    suspend fun upsertItems(entities: List<StatementItemEntity>)

    /**
     * Substitui a fatura e seus itens numa única transação: uma fatura
     * atualizada nunca fica com itens de uma versão anterior. Os itens são
     * apagados e regravados porque a API pode remover parcelas do ciclo.
     */
    @Transaction
    suspend fun upsertStatementWithItems(
        statement: StatementEntity,
        items: List<StatementItemEntity>
    ) {
        upsertStatement(statement)
        deleteItems(statement.id)
        if (items.isNotEmpty()) upsertItems(items)
    }

    @Query("DELETE FROM statement_items WHERE statementId = :statementId")
    suspend fun deleteItems(statementId: String)

    @Query(
        """
        DELETE FROM credit_card_statements
        WHERE creditCardId = :cardId AND id NOT IN (:keepIds)
        """
    )
    suspend fun deleteByCardNotIn(cardId: String, keepIds: List<String>)

    /**
     * Remove as faturas do cartão ausentes em [keepIds]; lista vazia limpa
     * todas as do cartão. Evita `NOT IN ()`, inválido no SQLite.
     * Os itens vão junto (FK CASCADE).
     */
    @Transaction
    suspend fun deleteByCardNotInSafe(cardId: String, keepIds: List<String>) {
        if (keepIds.isEmpty()) {
            deleteByCardOnly(cardId)
        } else {
            deleteByCardNotIn(cardId, keepIds)
        }
    }

    @Query("DELETE FROM credit_card_statements WHERE creditCardId = :cardId")
    suspend fun deleteByCardOnly(cardId: String)

    @Query("DELETE FROM credit_card_statements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_card_statements")
    suspend fun clearAll()
}
