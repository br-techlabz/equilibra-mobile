package br.com.equilibra.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaceholderDao {
    @Query("SELECT * FROM _room_placeholder LIMIT 1")
    suspend fun get(): PlaceholderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaceholderEntity)

    @Query("DELETE FROM _room_placeholder")
    suspend fun clear()
}
