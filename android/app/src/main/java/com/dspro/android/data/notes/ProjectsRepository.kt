package com.dspro.android.data.notes

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectsRepository(private val database: NotesDatabase) {
    private val dao = database.projectsDao()

    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()

    suspend fun get(id: String): ProjectEntity? = dao.getProject(id)

    suspend fun create(name: String): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "新项目" },
            icon = "📁",
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(project)
        return project
    }

    suspend fun update(project: ProjectEntity) {
        dao.upsert(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setArchived(id: String, archived: Boolean) =
        dao.setArchived(id, if (archived) System.currentTimeMillis() else null)

    suspend fun delete(id: String) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            database.notesDao().moveAllToProject(id, null, now)
            database.chatDao().moveAllToProject(id, null, now)
            database.memoryDao().deleteForProject(id)
            dao.delete(id)
        }
    }
}
