package br.com.equilibra.mobile.data.local.transaction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Leitura

    /** Cache das últimas [limit] transações (padrão ~100), mais recentes primeiro. */
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY occurredAt DESC")
    fun observeByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY occurredAt DESC")
    suspend fun getByType(type: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TransactionEntity?>

    // Sincronização

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM transactions WHERE type = :type AND id NOT IN (:keepIds)")
    suspend fun deleteByTypeNotIn(type: String, keepIds: List<String>)

    /** Remove de um tipo fora da lista; vazio → limpa só aquele tipo. */
    suspend fun replaceByTypeSafe(type: String, keepIds: List<String>) {
        if (keepIds.isEmpty()) deleteByType(type) else deleteByTypeNotIn(type, keepIds)
    }

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    /** Remove fora da lista; vazio → limpa tudo (evita NOT IN () inválido). */
    suspend fun deleteNotInSafe(keepIds: List<String>) {
        if (keepIds.isEmpty()) clearAll() else deleteNotIn(keepIds)
    }

    /**
     * Mantém no cache apenas as [limit] transações mais recentes por occurredAt
     * (ISO-8601 ordena lexicograficamente). O cache não armazena páginas
     * ilimitadas — históricos usam paginação da API sem cache (Task 4.11).
     */
    @Query(
        """
        DELETE FROM transactions WHERE id NOT IN (
            SELECT id FROM transactions ORDER BY occurredAt DESC LIMIT :limit
        )
        """
    )
    suspend fun trimToRecent(limit: Int = 100)
}
