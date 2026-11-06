package com.dspro.android.data.notes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

 data class WorkspaceSearchResult(val kind: String, val title: String, val snippet: String, val noteId: String? = null, val sessionId: Long? = null, val messageId: Long? = null)

class WorkspaceSearchService(private val notes: NotesRepository, private val chats: ChatRepository) {
    fun search(workspaceId: String?, query: String, tagId: String? = null, folderId: String? = null): Flow<List<WorkspaceSearchResult>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        val noteFlow = when {
            tagId != null -> notes.searchWorkspaceTag(workspaceId, q, tagId, folderId)
            folderId != null -> notes.searchWorkspaceFolder(workspaceId, q, folderId)
            else -> notes.searchWorkspace(workspaceId, q)
        }
        val sessionFlow = if (tagId == null) chats.searchSessions(workspaceId, q, folderId) else flowOf(emptyList<ChatSessionEntity>())
        val messageFlow = if (tagId == null) chats.searchMessages(workspaceId, q, folderId) else flowOf(emptyList<ChatMessageSearchRow>())
        return combine(noteFlow, sessionFlow, messageFlow) { foundNotes, sessions, messages ->
            buildList {
                foundNotes.forEach { add(WorkspaceSearchResult("note", it.title, it.content.take(180), noteId = it.id)) }
                sessions.forEach { add(WorkspaceSearchResult("chat", it.title, "会话", sessionId = it.id)) }
                messages.forEach { add(WorkspaceSearchResult("message", messages.firstOrNull { row -> row.sessionId == it.sessionId }?.sessionTitle.orEmpty(), it.text.take(180), sessionId = it.sessionId, messageId = it.id)) }
            }
        }
    }
}
