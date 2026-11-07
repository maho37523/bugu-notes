package com.dspro.android.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL AND archivedAt IS NULL LIMIT 1")
    fun observeNote(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNote(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title AND deletedAt IS NULL AND archivedAt IS NULL LIMIT 1")
    suspend fun findByTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND archivedAt IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY pinned DESC, updatedAt DESC")
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE projectId IS :projectId AND deletedAt IS NULL AND archivedAt IS NULL AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY pinned DESC, updatedAt DESC")
    fun searchWorkspace(projectId: String?, query: String): Flow<List<NoteEntity>>

    @Query("SELECT n.* FROM notes n JOIN note_tags nt ON n.id = nt.noteId WHERE n.projectId IS :projectId AND n.deletedAt IS NULL AND n.archivedAt IS NULL AND nt.tagId = :tagId AND (:folderId IS NULL OR n.folderId = :folderId) AND (n.title LIKE '%' || :query || '%' OR n.content LIKE '%' || :query || '%') ORDER BY n.pinned DESC, n.updatedAt DESC")
    fun searchWorkspaceTag(projectId: String?, query: String, tagId: String, folderId: String?): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE projectId IS :projectId AND deletedAt IS NULL AND archivedAt IS NULL AND (:folderId IS NULL OR folderId = :folderId) AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY pinned DESC, updatedAt DESC")
    fun searchWorkspaceFolder(projectId: String?, query: String, folderId: String?): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title = :title AND projectId IS :projectId AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun findInProject(title: String, projectId: String?): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteDraftNote(id: String)

    @Query("SELECT * FROM notes WHERE archivedAt IS NOT NULL AND deletedAt IS NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET archivedAt = :time, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, time: Long?, updatedAt: Long)

    @Query("UPDATE notes SET folderId = :folderId, updatedAt = :time WHERE id = :id")
    suspend fun moveToFolder(id: String, folderId: String?, time: Long)

    @Query("UPDATE notes SET deletedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun softDelete(id: String, time: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM folders ORDER BY projectId, parentId, sort, name, id")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE projectId IS :projectId ORDER BY parentId, sort, name, id")
    fun observeFolders(projectId: String?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE projectId IS :projectId ORDER BY parentId, sort, name, id")
    suspend fun foldersForProject(projectId: String?): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolder(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun renameFolder(id: String, name: String)

    @Query("SELECT id FROM folders WHERE id = :id")
    suspend fun existingFolder(id: String): String?

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :id")
    suspend fun clearFolderFromNotes(id: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("UPDATE folders SET archivedAt = :time WHERE id = :id")
    suspend fun archiveFolder(id: String, time: Long?)

    @Query("UPDATE folders SET parentId = :parentId WHERE id = :id")
    suspend fun setFolderParent(id: String, parentId: String?)

    @Query("UPDATE folders SET projectId = :projectId WHERE projectId = :fromProjectId")
    suspend fun moveFoldersToProject(fromProjectId: String, projectId: String?)

    @Query("UPDATE folders SET parentId = :newParentId WHERE parentId = :oldParentId")
    suspend fun promoteFolders(oldParentId: String, newParentId: String?)

    @Query("UPDATE notes SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun moveNotesFolder(oldFolderId: String, newFolderId: String?)

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId")
    suspend fun countNotesInFolder(folderId: String): Int

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND (archivedAt IS NOT NULL OR deletedAt IS NOT NULL)")
    suspend fun countHiddenNotesInFolder(folderId: String): Int

    @Query("SELECT COUNT(*) FROM folders WHERE parentId = :folderId")
    suspend fun countChildFolders(folderId: String): Int

    @Query("SELECT COUNT(*) FROM folders f JOIN folders a ON f.id = a.id WHERE f.parentId = :folderId AND a.archivedAt IS NOT NULL")
    suspend fun countDirectArchivedChildFolders(folderId: String): Int

    @Query("UPDATE tags SET archivedAt = :time WHERE id = :id")
    suspend fun archiveTag(id: String, time: Long?)

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: String)

    @Query("DELETE FROM note_tags WHERE tagId = :id")
    suspend fun clearTagFromNotes(id: String)

    @Query("SELECT * FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagCrossRef>>

    @Query("SELECT * FROM note_tags WHERE noteId = :noteId")
    suspend fun tagRefs(noteId: String): List<NoteTagCrossRef>

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearTags(noteId: String)

    @Query("INSERT OR IGNORE INTO note_tags(noteId, tagId) SELECT :noteId, id FROM tags WHERE id = :tagId")
    suspend fun insertTagRef(noteId: String, tagId: String)

    @Query("SELECT * FROM note_links WHERE fromNoteId = :noteId")
    fun observeLinksFrom(noteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE toNoteId = :noteId")
    fun observeBacklinks(noteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE toNoteId = :noteId")
    suspend fun backlinks(noteId: String): List<NoteLinkEntity>

    @Query("DELETE FROM note_links WHERE fromNoteId = :noteId")
    suspend fun clearLinksFrom(noteId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Query("UPDATE note_links SET toNoteId = :toNoteId WHERE toTitle = :title")
    suspend fun resolveLinks(title: String, toNoteId: String)

    @Insert
    suspend fun insertRevision(revision: NoteRevisionEntity)

    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT 100")
    fun observeRevisions(noteId: String): Flow<List<NoteRevisionEntity>>

    @Query("SELECT * FROM note_revisions WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT 100")
    suspend fun revisions(noteId: String): List<NoteRevisionEntity>

    @Query("SELECT MAX(version) FROM note_revisions WHERE noteId = :noteId")
    suspend fun maxVersion(noteId: String): Int?

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun trashBefore(cutoff: Long): List<NoteEntity>

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :time WHERE id = :id")
    suspend fun restore(id: String, time: Long)

    @Query("DELETE FROM note_revisions WHERE noteId = :id")
    suspend fun deleteRevisions(id: String)

    @Query("DELETE FROM note_tags WHERE noteId = :id")
    suspend fun deleteTagRefs(id: String)

    @Query("DELETE FROM note_links WHERE fromNoteId = :id OR toNoteId = :id")
    suspend fun deleteLinks(id: String)

    @Query("UPDATE notes SET projectId = :projectId, folderId = :folderId, updatedAt = :time WHERE id = :id")
    suspend fun moveToProject(id: String, projectId: String?, folderId: String?, time: Long)

    @Query("UPDATE notes SET projectId = :toProjectId, updatedAt = :time WHERE projectId = :fromProjectId")
    suspend fun moveAllToProject(fromProjectId: String, toProjectId: String?, time: Long)
}
