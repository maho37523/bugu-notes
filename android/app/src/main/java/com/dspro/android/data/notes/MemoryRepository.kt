package com.dspro.android.data.notes

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MemoryRepository(private val database: NotesDatabase) {
    private val dao = database.memoryDao()

    fun observe(projectId: String?): Flow<List<MemoryEntity>> = dao.observeForProject(projectId)

    suspend fun active(projectId: String?): List<MemoryEntity> = dao.activeForProject(projectId)

    suspend fun add(projectId: String?, content: String, sourceType: String = "manual", sourceId: String? = null): MemoryEntity {
        val memory = MemoryEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            content = content.trim(),
            sourceType = sourceType,
            sourceId = sourceId,
            createdAt = System.currentTimeMillis(),
        )
        dao.upsert(memory)
        return memory
    }

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun setEnabled(id: String, enabled: Boolean) = dao.setEnabled(id, enabled)
}
