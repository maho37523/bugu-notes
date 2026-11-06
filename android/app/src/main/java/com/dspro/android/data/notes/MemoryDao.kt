package com.dspro.android.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE projectId IS :projectId ORDER BY createdAt DESC")
    fun observeForProject(projectId: String?): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE enabled = 1 AND projectId IS :projectId ORDER BY createdAt DESC")
    suspend fun activeForProject(projectId: String?): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM memories WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)

    @Query("UPDATE memories SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}
