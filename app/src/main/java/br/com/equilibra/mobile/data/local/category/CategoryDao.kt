package br.com.equilibra.mobile.data.local.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // Leitura

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CategoryEntity?>

    /**
     * Filtra por aplicabilidade no estilo do backend:
     * EXPENSE → EXPENSE + BOTH; INCOME → INCOME + BOTH; BOTH → apenas BOTH.
     * Apenas categorias ativas.
     */
    @Query(
        """
        SELECT * FROM categories
        WHERE active = 1 AND (
            :applicability = 'BOTH' AND applicability = 'BOTH'
            OR :applicability = 'EXPENSE' AND applicability IN ('EXPENSE', 'BOTH')
            OR :applicability = 'INCOME' AND applicability IN ('INCOME', 'BOTH')
        )
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    fun observeByApplicability(applicability: String): Flow<List<CategoryEntity>>

    // Sincronização

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryEntity)

    @Query("DELETE FROM categories WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()

    /** Remove fora da lista; vazio → limpa tudo (evita NOT IN () inválido). */
    suspend fun deleteNotInSafe(keepIds: List<String>) {
        if (keepIds.isEmpty()) clearAll() else deleteNotIn(keepIds)
    }
}
