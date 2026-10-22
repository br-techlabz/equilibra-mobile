package br.com.equilibra.mobile.data.local.creditcard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    // Leitura — observável para lista cache-first
    // Ordem por name collate NOCASE, igual ao backend (OrderByNameAsc).

    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE status = 'ACTIVE' ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE status = 'ACTIVE' ORDER BY name COLLATE NOCASE ASC")
    suspend fun getActive(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CreditCardEntity?>

    // Sincronização

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CreditCardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM credit_cards")
    suspend fun clearAll()

    /**
     * Remove cartões que não estão em [keepIds]; se a lista for vazia, limpa tudo.
     * Evita `DELETE ... NOT IN ()` que é sintaxe SQLite inválida.
     */
    suspend fun deleteNotInSafe(keepIds: List<String>) {
        if (keepIds.isEmpty()) clearAll() else deleteNotIn(keepIds)
    }
}
