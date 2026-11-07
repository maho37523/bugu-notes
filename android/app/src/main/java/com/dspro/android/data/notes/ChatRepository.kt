package com.dspro.android.data.notes

import androidx.room.withTransaction
import com.dspro.android.data.ChatMessage
import com.dspro.android.data.MsgStatus
import com.dspro.android.data.Role
import com.dspro.android.data.Session
import com.dspro.android.data.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class DraftAttachment(val id: String, val name: String, val mime: String, val localPath: String, val size: Long, val textContent: String? = null)
data class ChatDraft(val sessionId: Long, val text: String = "", val quoteMessageId: Long? = null, val quoteText: String? = null, val selectedNoteIds: List<String> = emptyList(), val pendingAttachments: List<DraftAttachment> = emptyList(), val folderId: String? = null)

class ChatRepository(private val database: NotesDatabase) {
    private val dao = database.chatDao()

    val activeSessions: Flow<List<Session>> = dao.observeSessions().map { list -> list.map(::toDomain) }
    val archivedSessions: Flow<List<Session>> = dao.observeArchived().map { list -> list.map(::toDomain) }
    val trashedSessions: Flow<List<Session>> = dao.observeTrash().map { list -> list.map(::toDomain) }

    fun messages(sessionId: Long): Flow<List<ChatMessage>> =
        dao.observeMessages(sessionId).map { list -> list.map(::toDomain) }

    fun searchSessions(projectId: String?, query: String, folderId: String? = null) = dao.searchSessions(projectId, query, folderId)
    fun searchMessages(projectId: String?, query: String, folderId: String? = null) = dao.searchMessages(projectId, query, folderId)

    fun observeDraft(sessionId: Long): Flow<ChatDraft?> = dao.observeDraft(sessionId).map { it?.toDomainDraft() }
    suspend fun getDraft(sessionId: Long): ChatDraft? = dao.getDraft(sessionId)?.toDomainDraft()
    suspend fun saveDraft(draft: ChatDraft) {
        database.withTransaction {
            if (dao.getSession(draft.sessionId) == null) return@withTransaction
            dao.upsertDraft(ChatDraftEntity(draft.sessionId, draft.text, draft.quoteMessageId, draft.quoteText,
                JSONArray(draft.selectedNoteIds.distinct()).toString(), encodeAttachments(draft.pendingAttachments), System.currentTimeMillis()))
        }
    }
    suspend fun clearDraft(sessionId: Long) = dao.clearDraft(sessionId)

    private fun scope(projectId: String?) = projectId?.let { "project:$it" } ?: "global"
    suspend fun getTemporaryDraft(projectId: String?): ChatDraft? = (dao.getTemporaryDraft(scope(projectId))
        ?: if (projectId == null) dao.getOrphanTemporaryDraft() else null)?.let {
        ChatDraft(0L, it.text, it.quoteMessageId, it.quoteText, decodeStrings(it.selectedNoteIdsJson), decodeAttachments(it.pendingAttachmentsJson), it.folderId)
    }
    suspend fun saveTemporaryDraft(projectId: String?, draft: ChatDraft) {
        database.withTransaction {
            val globalScope = scope(projectId)
            requireScope(projectId, draft.folderId, requireActiveFolder = draft.folderId != null)
            if (projectId == null && dao.getTemporaryDraft(globalScope) == null) {
                val orphan = dao.getOrphanTemporaryDraft()
                if (orphan != null) {
                    require(orphan.text.isBlank() && orphan.quoteText.isNullOrBlank() &&
                        decodeStrings(orphan.selectedNoteIdsJson).isEmpty() && decodeAttachments(orphan.pendingAttachmentsJson).isEmpty()) {
                        "存在未处理的项目草稿，不能覆盖全局草稿"
                    }
                    dao.clearTemporaryDraft(orphan.projectScope)
                }
            }
            dao.upsertTemporaryDraft(TemporaryChatDraftEntity(globalScope, projectId, draft.folderId, draft.text,
                draft.quoteMessageId, draft.quoteText, JSONArray(draft.selectedNoteIds.distinct()).toString(), encodeAttachments(draft.pendingAttachments), System.currentTimeMillis()))
        }
    }
    suspend fun clearTemporaryDraft(projectId: String?) = database.withTransaction {
        val key = scope(projectId)
        if (dao.getTemporaryDraft(key) != null) dao.clearTemporaryDraft(key)
        else if (projectId == null) dao.getOrphanTemporaryDraft()?.let { dao.clearTemporaryDraft(it.projectScope) }
    }

    suspend fun commitTemporaryChat(projectId: String?, folderId: String?, title: String, messages: List<ChatMessage>): Session = database.withTransaction {
        require(messages.isNotEmpty()) { "Cannot commit an empty conversation" }
        requireScope(projectId, folderId, requireActiveFolder = folderId != null)
        val now = System.currentTimeMillis()
        val id = now * 1000 + (0..999).random()
        val cleanTitle = title.trim().ifBlank { "新会话" }.take(80)
        val targetFolder = folderId
        dao.upsertSession(ChatSessionEntity(id, projectId, cleanTitle, now, now, folderId = targetFolder))
        messages.forEachIndexed { index, message -> dao.upsertMessage(toEntity(message, id, now + index)) }
        val draftScope = scope(projectId)
        if (dao.getTemporaryDraft(draftScope) != null) dao.clearTemporaryDraft(draftScope)
        else if (projectId == null) dao.getOrphanTemporaryDraft()?.let { dao.clearTemporaryDraft(it.projectScope) }
        Session(id, cleanTitle, "刚刚", messages, projectId, folderId = targetFolder, updatedAt = now)
    }

    private fun decodeStrings(json: String): List<String> = runCatching { JSONArray(json).let { a -> (0 until a.length()).map { a.getString(it) } } }.getOrDefault(emptyList())
    private fun ChatDraftEntity.toDomainDraft(): ChatDraft = ChatDraft(sessionId, text, quoteMessageId, quoteText,
        decodeStrings(selectedNoteIdsJson),
        decodeAttachments(pendingAttachmentsJson))
    private fun encodeAttachments(items: List<DraftAttachment>) = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("mime", it.mime).put("localPath", it.localPath).put("size", it.size).put("textContent", it.textContent)) }
    }.toString()
    private fun decodeAttachments(json: String): List<DraftAttachment> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { i -> array.getJSONObject(i).let { DraftAttachment(it.optString("id"), it.optString("name"), it.optString("mime"), it.optString("localPath"), it.optLong("size"), if (it.isNull("textContent")) null else it.optString("textContent")) } }
    }.getOrDefault(emptyList())

    suspend fun loadSessions(includeArchived: Boolean = true): List<Session> {
        val sessions = dao.allSessions()
        return sessions.map { entity ->
            toDomain(entity, dao.messages(entity.id).map(::toDomain))
        }
    }

    suspend fun loadMessages(sessionId: Long): List<ChatMessage> =
        dao.messages(sessionId).map(::toDomain)

    suspend fun saveSession(session: Session) {
        database.withTransaction {
            val existing = dao.getSession(session.id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            dao.upsertSession(existing.copy(updatedAt = now))
            dao.clearMessages(session.id)
            session.messages.forEachIndexed { index, message ->
                dao.upsertMessage(toEntity(message, session.id, now + index))
            }
        }
    }

    suspend fun renameSession(sessionId: Long, title: String): Boolean {
        val clean = title.trim().take(80)
        if (clean.isBlank()) return false
        return dao.renameSession(sessionId, clean, System.currentTimeMillis()) > 0
    }

    suspend fun setAutomaticTitle(sessionId: Long, title: String): Boolean {
        val clean = title.trim().take(80)
        if (clean.isBlank()) return false
        return dao.setAutoTitle(sessionId, clean, System.currentTimeMillis()) > 0
    }

    suspend fun createSession(projectId: String?, title: String = "新会话", folderId: String? = null): Session = database.withTransaction {
        requireScope(projectId, folderId, requireActiveFolder = folderId != null)
        val now = System.currentTimeMillis()
        val id = now * 1000 + (0..999).random()
        val targetFolder = folderId
        val session = Session(
            id = id,
            title = title,
            updated = "刚刚",
            messages = emptyList(),
            projectId = projectId,
            folderId = targetFolder,
        )
        dao.upsertSession(
            ChatSessionEntity(
                id = id,
                projectId = projectId,
                title = title,
                createdAt = now,
                updatedAt = now,
                folderId = targetFolder,
            )
        )
        session
    }

    suspend fun archive(sessionId: Long) {
        dao.archive(sessionId, System.currentTimeMillis())
    }

    suspend fun unarchive(sessionId: Long) {
        dao.unarchive(sessionId, System.currentTimeMillis())
    }

    suspend fun trash(ids: Collection<Long>) { val now = System.currentTimeMillis(); database.withTransaction { ids.distinct().forEach { dao.trash(it, now) } } }
    suspend fun restore(id: Long) = dao.restore(id, System.currentTimeMillis())
    suspend fun permanentlyDelete(ids: Collection<Long>) = database.withTransaction { ids.distinct().forEach { dao.clearMessages(it); dao.clearDraft(it); dao.deleteSession(it) } }
    suspend fun moveToFolder(id: Long, folderId: String?) = database.withTransaction {
        val session = requireNotNull(dao.getSession(id)) { "对话不存在" }
        requireScope(session.projectId, folderId, requireActiveFolder = folderId != null)
        dao.moveToFolder(id, folderId, System.currentTimeMillis())
    }
    fun observeTrash(): Flow<List<Session>> = dao.observeTrash().map { rows -> rows.map(::toDomain) }
    fun observeArchived(): Flow<List<Session>> = dao.observeArchived().map { rows -> rows.map(::toDomain) }

    suspend fun archiveAll(projectId: String?) {
        dao.archiveAllInProject(projectId, System.currentTimeMillis())
    }

    suspend fun move(id: Long, projectId: String?) = database.withTransaction {
        requireScope(projectId, null, requireActiveFolder = false)
        dao.moveSession(id, projectId, null, System.currentTimeMillis())
    }

    suspend fun delete(sessionIds: Collection<Long>, createReplacement: Boolean, replacementProjectId: String?): Session? =
        database.withTransaction {
            sessionIds.forEach { id ->
                dao.clearMessages(id)
                dao.clearDraft(id)
                dao.deleteSession(id)
            }
            if (createReplacement) createSession(replacementProjectId) else null
        }

    private suspend fun requireScope(projectId: String?, folderId: String?, requireActiveFolder: Boolean) {
        require(projectId == null || database.projectsDao().getProject(projectId) != null) { "项目不存在，请重新选择" }
        val folder = folderId?.let { requireNotNull(database.notesDao().getFolder(it)) { "文件夹不存在，请重新选择" } }
        require(folder == null || folder.projectId == projectId) { "文件夹不属于当前项目，请重新选择" }
        if (requireActiveFolder && folder != null) {
            var cursor: FolderEntity? = folder
            val visited = mutableSetOf<String>()
            while (cursor != null) {
                require(visited.add(cursor.id)) { "文件夹树存在循环" }
                require(cursor.archivedAt == null) { "文件夹已归档，请选择其他位置" }
                cursor = cursor.parentId?.let { database.notesDao().getFolder(it) }
            }
        }
    }

    private fun toDomain(entity: ChatSessionEntity, messages: List<ChatMessage> = emptyList()): Session =
        Session(
            id = entity.id,
            title = entity.title,
            updated = "刚刚",
            messages = messages,
            projectId = entity.projectId,
            archived = entity.archivedAt != null,
            folderId = entity.folderId,
            deleted = entity.deletedAt != null,
            updatedAt = entity.updatedAt,
        )

    private fun toDomain(entity: ChatMessageEntity): ChatMessage =
        ChatMessage(
            id = entity.id,
            role = if (entity.role == "user") Role.USER else Role.ASSISTANT,
            text = entity.text,
            time = entity.time,
            reasoning = entity.reasoning,
            sources = runCatching {
                val rows = JSONArray(entity.sourcesJson)
                (0 until rows.length()).mapNotNull { i -> rows.optJSONObject(i)?.let { row ->
                    Source(row.optString("title"), row.optString("site"), row.optString("date"), row.optString("snippet"), row.optString("url"))
                } }
            }.getOrDefault(emptyList()),
            queries = runCatching { val rows = JSONArray(entity.queriesJson); (0 until rows.length()).map { rows.optString(it) } }.getOrDefault(emptyList()),
            status = runCatching { MsgStatus.valueOf(entity.status.uppercase()) }.getOrDefault(MsgStatus.DONE),
            attachment = entity.attachment,
            noteRef = entity.noteRef,
            quoteMessageId = entity.quoteMessageId,
            quoteText = entity.quoteText,
            contextSnapshot = entity.contextSnapshot,
            liked = null,
        )

    private fun toEntity(message: ChatMessage, sessionId: Long, createdAt: Long): ChatMessageEntity =
        ChatMessageEntity(
            id = message.id,
            sessionId = sessionId,
            role = if (message.role == Role.USER) "user" else "assistant",
            text = message.text,
            reasoning = message.reasoning,
            status = message.status.name.lowercase(),
            time = message.time,
            attachment = message.attachment,
            noteRef = message.noteRef,
            quoteMessageId = message.quoteMessageId,
            quoteText = message.quoteText,
            contextSnapshot = message.contextSnapshot,
            sourcesJson = JSONArray().apply { message.sources.forEach { source ->
                put(JSONObject().put("title", source.title).put("site", source.site).put("date", source.date).put("snippet", source.snippet).put("url", source.url))
            } }.toString(),
            queriesJson = JSONArray(message.queries).toString(),
            createdAt = createdAt,
        )
}
