package com.dspro.android.data.notes

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class FolderDeletionSummary(val noteCount: Int, val chatCount: Int, val childFolderCount: Int, val hiddenCount: Int)

class NotesRepository(private val database: NotesDatabase) {

    private val dao = database.notesDao()

    val notes: Flow<List<NoteEntity>> = dao.observeNotes()
    val folders: Flow<List<FolderEntity>> = dao.observeFolders()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val noteTags: Flow<List<NoteTagCrossRef>> = dao.observeNoteTags()
    val archivedNotes: Flow<List<NoteEntity>> = dao.observeArchived()
    fun observeFolders(projectId: String?): Flow<List<FolderEntity>> = dao.observeFolders(projectId)
    suspend fun getFolder(id: String): FolderEntity? = dao.getFolder(id)

    fun observeNote(id: String): Flow<NoteEntity?> = dao.observeNote(id)
    fun search(query: String): Flow<List<NoteEntity>> = dao.search(query)
    fun searchWorkspace(projectId: String?, query: String): Flow<List<NoteEntity>> = dao.searchWorkspace(projectId, query)
    fun searchWorkspaceTag(projectId: String?, query: String, tagId: String, folderId: String?): Flow<List<NoteEntity>> = dao.searchWorkspaceTag(projectId, query, tagId, folderId)
    fun searchWorkspaceFolder(projectId: String?, query: String, folderId: String?): Flow<List<NoteEntity>> = dao.searchWorkspaceFolder(projectId, query, folderId)

    suspend fun importMarkdown(title: String, content: String, projectId: String?, folderId: String?): NoteEntity = database.withTransaction {
        requireScope(projectId, folderId, requireActiveFolder = true)
        val now = System.currentTimeMillis()
        val note = NoteEntity(UUID.randomUUID().toString(), title.trim().ifBlank { "无标题笔记" }, content,
            folderId, projectId, createdAt = now, updatedAt = now)
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
        requireScope(projectId, folderId, requireActiveFolder = true)
        val now = System.currentTimeMillis()
        val note = NoteEntity(id, cleanTitle.ifBlank { "无标题笔记" }, content,
            folderId, projectId, pinned,
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
        requireScope(projectId, folderId, requireActiveFolder = true)
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            folderId = folderId,
            projectId = projectId,
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
            requireScope(existing.projectId, folderId, requireActiveFolder = folderId != existing.folderId)
            val note = existing.copy(
                title = title.ifBlank { "无标题笔记" },
                content = content,
                folderId = folderId,
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
        database.withTransaction {
            requireProject(projectId)
            val note = dao.getNote(id) ?: return@withTransaction
            dao.moveToProject(id, projectId, null, System.currentTimeMillis())
        }
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

    suspend fun setFolderArchived(id: String, archived: Boolean) = database.withTransaction {
        requireNotNull(dao.getFolder(id)) { "文件夹不存在" }
        dao.archiveFolder(id, if (archived) System.currentTimeMillis() else null)
    }

    suspend fun setTagArchived(id: String, archived: Boolean) =
        dao.archiveTag(id, if (archived) System.currentTimeMillis() else null)

    suspend fun deleteFolder(id: String) {
        database.withTransaction {
            val folder = requireNotNull(dao.getFolder(id)) { "文件夹不存在" }
            dao.moveNotesFolder(id, folder.parentId)
            database.chatDao().moveSessionFolder(id, folder.parentId)
            database.chatDao().moveTemporaryDraftFolder(id, folder.parentId)
            dao.promoteFolders(id, folder.parentId)
            dao.deleteFolder(id)
        }
    }

    suspend fun folderDeletionSummary(id: String): FolderDeletionSummary {
        requireNotNull(dao.getFolder(id)) { "文件夹不存在" }
        val chatDao = database.chatDao()
        return FolderDeletionSummary(
            noteCount = dao.countNotesInFolder(id),
            chatCount = chatDao.countAllSessionsInFolder(id),
            childFolderCount = dao.countChildFolders(id),
            hiddenCount = dao.countHiddenNotesInFolder(id) + chatDao.countHiddenSessionsInFolder(id),
        )
    }

    suspend fun renameFolder(id: String, name: String) {
        requireNotNull(dao.getFolder(id)) { "文件夹不存在" }
        dao.renameFolder(id, name.trim().ifBlank { "新文件夹" })
    }
    suspend fun moveToFolder(id: String, folderId: String?) = database.withTransaction {
        val note = requireNotNull(dao.getNote(id)) { "笔记不存在" }
        requireScope(note.projectId, folderId, requireActiveFolder = true)
        dao.moveToFolder(id, folderId, System.currentTimeMillis())
    }

    suspend fun moveFolder(id: String, parentId: String?) = database.withTransaction {
        val folder = requireNotNull(dao.getFolder(id)) { "文件夹不存在" }
        if (parentId == id) error("文件夹不能移动到自身")
        if (parentId != null) {
            val parent = requireNotNull(dao.getFolder(parentId)) { "目标文件夹不存在" }
            require(parent.projectId == folder.projectId) { "不能移动到其他项目的文件夹" }
            require(!isEffectivelyArchived(parent.id)) { "不能移动到已归档的文件夹" }
            var cursor: FolderEntity? = parent
            val visited = mutableSetOf<String>()
            while (cursor != null) {
                require(visited.add(cursor.id)) { "文件夹树存在循环" }
                require(cursor.id != folder.id) { "不能移动到自己的子文件夹" }
                cursor = cursor.parentId?.let { dao.getFolder(it) }
            }
        }
        dao.setFolderParent(id, parentId)
    }
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

    suspend fun createFolder(name: String, projectId: String? = null, parentId: String? = null): FolderEntity = database.withTransaction {
        requireScope(projectId, parentId, requireActiveFolder = true)
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "新文件夹" },
            sort = (System.currentTimeMillis() % 100000).toInt(),
            projectId = projectId,
            parentId = parentId,
        )
        dao.upsertFolder(folder)
        folder
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

    private suspend fun requireProject(projectId: String?) {
        require(projectId == null || database.projectsDao().getProject(projectId) != null) { "项目不存在，请重新选择" }
    }

    private suspend fun requireScope(projectId: String?, folderId: String?, requireActiveFolder: Boolean) {
        requireProject(projectId)
        val folder = folderId?.let { requireNotNull(dao.getFolder(it)) { "文件夹不存在，请重新选择" } }
        require(folder == null || folder.projectId == projectId) { "文件夹不属于当前项目，请重新选择" }
        require(!requireActiveFolder || folder == null || !isEffectivelyArchived(folder.id)) { "文件夹已归档，请选择其他位置" }
    }

    private suspend fun isEffectivelyArchived(folderId: String): Boolean {
        var cursor = dao.getFolder(folderId)
        val visited = mutableSetOf<String>()
        while (cursor != null) {
            require(visited.add(cursor.id)) { "文件夹树存在循环" }
            if (cursor.archivedAt != null) return true
            cursor = cursor.parentId?.let { dao.getFolder(it) }
        }
        return false
    }

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
