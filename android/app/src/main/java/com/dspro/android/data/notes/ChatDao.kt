package com.dspro.android.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ChatMessageSearchRow(val id: Long, val sessionId: Long, val role: String, val text: String, val sessionTitle: String)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_drafts WHERE sessionId = :sessionId LIMIT 1")
    fun observeDraft(sessionId: Long): Flow<ChatDraftEntity?>

    @Query("SELECT * FROM chat_drafts WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getDraft(sessionId: Long): ChatDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: ChatDraftEntity)

    @Query("DELETE FROM chat_drafts WHERE sessionId = :sessionId")
    suspend fun clearDraft(sessionId: Long)

    @Query("SELECT * FROM temporary_chat_drafts WHERE projectScope = :scope LIMIT 1")
    suspend fun getTemporaryDraft(scope: String): TemporaryChatDraftEntity?

    @Query("SELECT * FROM temporary_chat_drafts WHERE projectId = :projectId LIMIT 1")
    suspend fun getTemporaryDraftForProject(projectId: String): TemporaryChatDraftEntity?

    @Query("SELECT d.* FROM temporary_chat_drafts d WHERE d.projectId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.id = d.projectId) ORDER BY d.updatedAt DESC LIMIT 1")
    suspend fun getOrphanTemporaryDraft(): TemporaryChatDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemporaryDraft(draft: TemporaryChatDraftEntity)

    @Query("DELETE FROM temporary_chat_drafts WHERE projectScope = :scope")
    suspend fun clearTemporaryDraft(scope: String)

    @Query("UPDATE temporary_chat_drafts SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderFromTemporaryDrafts(folderId: String)

    @Query("UPDATE temporary_chat_drafts SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun moveTemporaryDraftFolder(oldFolderId: String, newFolderId: String?)

    @Query("DELETE FROM temporary_chat_drafts WHERE projectId = :projectId")
    suspend fun deleteTemporaryDraftForProject(projectId: String)

    @Query("SELECT * FROM chat_sessions WHERE archivedAt IS NULL AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE archivedAt IS NOT NULL AND deletedAt IS NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    suspend fun allSessions(): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: Long): ChatSessionEntity?

    @Query("UPDATE chat_sessions SET title = :title, titleManuallySet = 1, updatedAt = :time WHERE id = :id")
    suspend fun renameSession(id: Long, title: String, time: Long): Int

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :time WHERE id = :id AND titleManuallySet = 0")
    suspend fun setAutoTitle(id: Long, title: String, time: Long): Int

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt, id")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt, id")
    suspend fun messages(sessionId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_sessions WHERE projectId IS :projectId AND deletedAt IS NULL AND (:folderId IS NULL OR folderId = :folderId) AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSessions(projectId: String?, query: String, folderId: String?): Flow<List<ChatSessionEntity>>

    @Query("SELECT m.id AS id, m.sessionId AS sessionId, m.role AS role, m.text AS text, s.title AS sessionTitle FROM chat_messages m JOIN chat_sessions s ON s.id = m.sessionId WHERE s.projectId IS :projectId AND s.deletedAt IS NULL AND (:folderId IS NULL OR s.folderId = :folderId) AND m.text LIKE '%' || :query || '%' ORDER BY m.createdAt DESC")
    fun searchMessages(projectId: String?, query: String, folderId: String?): Flow<List<ChatMessageSearchRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessages(sessionId: Long)

    @Query("UPDATE chat_sessions SET archivedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun archive(id: Long, time: Long)

    @Query("UPDATE chat_sessions SET deletedAt = :time, updatedAt = :time WHERE id = :id")
    suspend fun trash(id: Long, time: Long)

    @Query("UPDATE chat_sessions SET deletedAt = NULL, updatedAt = :time WHERE id = :id")
    suspend fun restore(id: Long, time: Long)

    @Query("UPDATE chat_sessions SET folderId = :folderId, updatedAt = :time WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: String?, time: Long)

    @Query("UPDATE chat_sessions SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String)

    @Query("UPDATE chat_sessions SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun moveSessionFolder(oldFolderId: String, newFolderId: String?)

    @Query("UPDATE chat_sessions SET archivedAt = NULL, updatedAt = :time WHERE id = :id")
    suspend fun unarchive(id: Long, time: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("UPDATE chat_sessions SET projectId = :projectId, folderId = :folderId, updatedAt = :time WHERE id = :id")
    suspend fun moveSession(id: Long, projectId: String?, folderId: String?, time: Long)

    @Query("SELECT COUNT(*) FROM chat_sessions WHERE folderId = :folderId")
    suspend fun countSessionsInFolder(folderId: String): Int

    @Query("SELECT COUNT(*) FROM chat_sessions WHERE folderId = :folderId AND (archivedAt IS NOT NULL OR deletedAt IS NOT NULL)")
    suspend fun countHiddenSessionsInFolder(folderId: String): Int

    @Query("SELECT COUNT(*) FROM chat_sessions WHERE folderId = :folderId")
    suspend fun countAllSessionsInFolder(folderId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM chat_drafts d JOIN chat_sessions s ON s.id = d.sessionId WHERE s.projectId = :projectId AND (length(trim(d.text)) > 0 OR d.quoteText IS NOT NULL OR d.quoteMessageId IS NOT NULL OR d.selectedNoteIdsJson != '[]' OR d.pendingAttachmentsJson != '[]'))")
    suspend fun projectHasContentDrafts(projectId: String): Boolean

    @Query("UPDATE chat_sessions SET projectId = :toProjectId, updatedAt = :time WHERE projectId = :fromProjectId")
    suspend fun moveAllToProject(fromProjectId: String, toProjectId: String?, time: Long)

    @Query("UPDATE chat_sessions SET archivedAt = :time, updatedAt = :time WHERE projectId IS :projectId AND archivedAt IS NULL AND deletedAt IS NULL")
    suspend fun archiveAllInProject(projectId: String?, time: Long)
}
