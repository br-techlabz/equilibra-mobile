package br.com.equilibra.mobile.data.local.tag

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // Leitura

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TagEntity?>

    /** Apenas tags ativas — padrão do backend (GET /tags?includeInactive=false). */
    @Query("SELECT * FROM tags WHERE active = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<TagEntity>>

    // Sincronização

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tags")
    suspend fun clearAll()

    /** Remove fora da lista; vazio → limpa tudo (evita NOT IN () inválido). */
    suspend fun deleteNotInSafe(keepIds: List<String>) {
        if (keepIds.isEmpty()) clearAll() else deleteNotIn(keepIds)
    }
}
