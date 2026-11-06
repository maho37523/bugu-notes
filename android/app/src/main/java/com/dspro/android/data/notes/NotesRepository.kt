package com.dspro.android.data.notes

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NotesRepository(private val database: NotesDatabase) {

    private val dao = database.notesDao()

    val notes: Flow<List<NoteEntity>> = dao.observeNotes()
    val folders: Flow<List<FolderEntity>> = dao.observeFolders()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val noteTags: Flow<List<NoteTagCrossRef>> = dao.observeNoteTags()
    val archivedNotes: Flow<List<NoteEntity>> = dao.observeArchived()

    fun observeNote(id: String): Flow<NoteEntity?> = dao.observeNote(id)
    fun search(query: String): Flow<List<NoteEntity>> = dao.search(query)
    fun searchWorkspace(projectId: String?, query: String): Flow<List<NoteEntity>> = dao.searchWorkspace(projectId, query)
    fun searchWorkspaceTag(projectId: String?, query: String, tagId: String, folderId: String?): Flow<List<NoteEntity>> = dao.searchWorkspaceTag(projectId, query, tagId, folderId)
    fun searchWorkspaceFolder(projectId: String?, query: String, folderId: String?): Flow<List<NoteEntity>> = dao.searchWorkspaceFolder(projectId, query, folderId)

    suspend fun importMarkdown(title: String, content: String, projectId: String?, folderId: String?): NoteEntity = database.withTransaction {
        val now = System.currentTimeMillis()
        val note = NoteEntity(UUID.randomUUID().toString(), title.trim().ifBlank { "无标题笔记" }, content,
            folderId?.takeIf { dao.existingFolder(it) != null },
            projectId?.takeIf { database.projectsDao().getProject(it) != null }, createdAt = now, updatedAt = now)
        dao.upsert(note)
        refreshLinks(note)
        note
    }
    fun observeRevisions(noteId: String): Flow<List<NoteRevisionEntity>> = dao.observeRevisions(noteId)
    fun observeBacklinks(noteId: String): Flow<List<NoteLinkEntity>> = dao.observeBacklinks(noteId)
    fun observeLinksFrom(noteId: String): Flow<List<NoteLinkEntity>> = dao.observeLinksFrom(noteId)

    suspend fun getNote(id: String): NoteEntity? = dao.getNote(id)

    suspend fun upsertNewNoteDraft(id: String, title: String, content: String, folderId: String?, projectId: String?, pinned: Boolean, titleEdited: Boolean): NoteEntity? = database.withTransaction {
        val existing = dao.getNote(id)
        val cleanTitle = title.trim()
        if (content.isBlank() && !(titleEdited && cleanTitle.isNotBlank())) {
            dao.deleteRevisions(id); dao.deleteTagRefs(id); dao.deleteLinks(id); dao.deleteDraftNote(id)
            return@withTransaction null
        }
        val now = System.currentTimeMillis()
        val note = NoteEntity(id, cleanTitle.ifBlank { "无标题笔记" }, content,
            folderId?.takeIf { dao.existingFolder(it) != null },
            projectId?.takeIf { database.projectsDao().getProject(it) != null }, pinned,
            existing?.createdAt ?: now, now, existing?.deletedAt, existing?.archivedAt)
        dao.upsert(note)
        dao.resolveLinks(note.title, note.id)
        refreshLinks(note)
        note
    }

    suspend fun createNote(
        folderId: String? = null,
        title: String = "无标题笔记",
        content: String = "",
        projectId: String? = null,
    ): NoteEntity = database.withTransaction {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            folderId = folderId?.let { dao.existingFolder(it) },
            projectId = projectId?.takeIf { database.projectsDao().getProject(it) != null },
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(note)
        dao.resolveLinks(note.title, note.id)
        refreshLinks(note)
        note
    }

    suspend fun updateNote(
        id: String,
        title: String,
        content: String,
        folderId: String?,
        pinned: Boolean,
    ) {
        database.withTransaction {
            val existing = dao.getNote(id) ?: return@withTransaction
            val note = existing.copy(
                title = title.ifBlank { "无标题笔记" },
                content = content,
                folderId = folderId?.let { dao.existingFolder(it) },
                pinned = pinned,
            )
            if (note == existing) return@withTransaction
            dao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
            if (content != existing.content) refreshLinks(note)
        }
    }

    suspend fun deleteNote(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restoreNote(id: String) {
        dao.restore(id, System.currentTimeMillis())
    }

    suspend fun permanentlyDelete(id: String) {
        dao.deleteRevisions(id)
        dao.deleteTagRefs(id)
        dao.deleteLinks(id)
        dao.hardDelete(id)
    }

    fun observeTrash(): Flow<List<NoteEntity>> = dao.observeTrash()

    suspend fun moveToProject(id: String, projectId: String?) {
        dao.moveToProject(id, projectId, System.currentTimeMillis())
    }

    suspend fun saveVersion(noteId: String, content: String, name: String, kind: String = "manual"): NoteRevisionEntity {
        val next = (dao.maxVersion(noteId) ?: 0) + 1
        val revision = NoteRevisionEntity(
            id = UUID.randomUUID().toString(),
            noteId = noteId,
            content = content,
            createdAt = System.currentTimeMillis(),
            reason = if (kind == "manual") "手动保存" else "自动检查点",
            name = name,
            version = next,
            kind = kind,
        )
        dao.insertRevision(revision)
        return revision
    }

    suspend fun revisions(noteId: String): List<NoteRevisionEntity> = dao.revisions(noteId)

    suspend fun cleanupTrash(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        dao.trashBefore(cutoff).forEach { permanentlyDelete(it.id) }
    }

    suspend fun setTags(noteId: String, tagIds: List<String>) {
        database.withTransaction {
            if (dao.getNote(noteId) == null) return@withTransaction
            dao.clearTags(noteId)
            tagIds.forEach { dao.insertTagRef(noteId, it) }
        }
    }

    suspend fun setFolderArchived(id: String, archived: Boolean) =
        dao.archiveFolder(id, if (archived) System.currentTimeMillis() else null)

    suspend fun setTagArchived(id: String, archived: Boolean) =
        dao.archiveTag(id, if (archived) System.currentTimeMillis() else null)

    suspend fun deleteFolder(id: String) {
        database.withTransaction {
            dao.clearFolderFromNotes(id)
            database.chatDao().clearFolder(id)
            database.chatDao().clearFolderFromTemporaryDrafts(id)
            dao.deleteFolder(id)
        }
    }

    suspend fun renameFolder(id: String, name: String) { dao.renameFolder(id, name.trim().ifBlank { "新文件夹" }) }
    suspend fun moveToFolder(id: String, folderId: String?) { dao.moveToFolder(id, folderId?.takeIf { dao.existingFolder(it) != null }, System.currentTimeMillis()) }
    fun observeArchived(): Flow<List<NoteEntity>> = dao.observeArchived()
    suspend fun archive(id: String) {
        val now = System.currentTimeMillis()
        dao.setArchived(id, now, now)
    }
    suspend fun unarchive(id: String) = dao.setArchived(id, null, System.currentTimeMillis())

    suspend fun deleteTag(id: String) {
        database.withTransaction {
            dao.clearTagFromNotes(id)
            dao.deleteTag(id)
        }
    }

    suspend fun createFolder(name: String): FolderEntity {
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "新文件夹" },
            sort = (System.currentTimeMillis() % 100000).toInt(),
        )
        dao.upsertFolder(folder)
        return folder
    }

    suspend fun createTag(name: String): TagEntity {
        val tag = TagEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "新标签" },
            color = (0..5).random(),
        )
        dao.upsertTag(tag)
        return tag
    }

    suspend fun addRevision(noteId: String, content: String, reason: String) {
        dao.insertRevision(
            NoteRevisionEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                content = content,
                createdAt = System.currentTimeMillis(),
                reason = reason,
            )
        )
    }

    suspend fun findInProject(title: String, projectId: String?): NoteEntity? =
        dao.findInProject(title.trim(), projectId)

    suspend fun findOrCreateByTitle(title: String, projectId: String? = null): NoteEntity {
        val clean = title.trim()
        findInProject(clean, projectId)?.let { return it }
        return createNote(title = clean, content = "# $clean\n\n", projectId = projectId)
    }

    suspend fun backlinks(noteId: String): List<NoteLinkEntity> = dao.backlinks(noteId)

    suspend fun tagIds(noteId: String): List<String> = dao.tagRefs(noteId).map { it.tagId }

    suspend fun backlinkNotes(noteId: String): List<NoteEntity> =
        dao.backlinks(noteId).mapNotNull { dao.getNote(it.fromNoteId) }

    private suspend fun refreshLinks(note: NoteEntity) {
        dao.clearLinksFrom(note.id)
        extractWikiLinks(note.content).distinct().forEach { title ->
            val target = dao.findByTitle(title)
            dao.insertLink(
                NoteLinkEntity(
                    fromNoteId = note.id,
                    toTitle = title,
                    toNoteId = target?.id,
                )
            )
        }
    }

    private fun extractWikiLinks(content: String): List<String> =
        Regex("\\[\\[([^\\]]+)]](?!\\()").findAll(content).map { it.groupValues[1].trim() }.toList()
}
