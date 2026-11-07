package com.dspro.android.data.notes

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
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
            requireNotNull(dao.getProject(id)) { "项目不存在" }
            require(!database.chatDao().projectHasContentDrafts(id)) {
                "项目中还有未提交的对话草稿，请先保存、清空或发送后再删除项目"
            }
            val temporaryDraft = database.chatDao().getTemporaryDraftForProject(id)
            if (temporaryDraft != null) {
                val hasContent = temporaryDraft.text.isNotBlank() || temporaryDraft.quoteText.isNullOrBlank().not() ||
                    temporaryDraft.quoteMessageId != null || JSONArray(temporaryDraft.selectedNoteIdsJson).length() > 0 ||
                    JSONArray(temporaryDraft.pendingAttachmentsJson).length() > 0
                require(!hasContent) { "项目中还有未提交的草稿，请先保存、清空或发送后再删除项目" }
                database.chatDao().deleteTemporaryDraftForProject(id)
            }
            val now = System.currentTimeMillis()
            database.notesDao().moveAllToProject(id, null, now)
            database.chatDao().moveAllToProject(id, null, now)
            database.notesDao().moveFoldersToProject(id, null)
            database.memoryDao().deleteForProject(id)
            dao.delete(id)
        }
    }
}
