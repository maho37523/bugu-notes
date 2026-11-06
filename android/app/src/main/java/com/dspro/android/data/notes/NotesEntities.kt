package com.dspro.android.data.notes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index("folderId"), Index("updatedAt"), Index("deletedAt"), Index("archivedAt")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val projectId: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val archivedAt: Long? = null,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sort: Int = 0,
    val archivedAt: Long? = null,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int = 0,
    val archivedAt: Long? = null,
)

@Entity(tableName = "note_tags", primaryKeys = ["noteId", "tagId"])
data class NoteTagCrossRef(
    val noteId: String,
    val tagId: String,
)

@Entity(tableName = "note_links", primaryKeys = ["fromNoteId", "toTitle"])
data class NoteLinkEntity(
    val fromNoteId: String,
    val toTitle: String,
    val toNoteId: String? = null,
)

@Entity(tableName = "note_revisions", indices = [Index("noteId")])
data class NoteRevisionEntity(
    @PrimaryKey val id: String,
    val noteId: String,
    val content: String,
    val createdAt: Long,
    val reason: String,
    @ColumnInfo(defaultValue = "") val name: String = "",
    @ColumnInfo(defaultValue = "0") val version: Int = 0,
    @ColumnInfo(defaultValue = "auto") val kind: String = "auto",
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "📁",
    val color: Int = 0,
    val instructions: String = "",
    val memoryMode: String = "project_only",
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: Long,
    val projectId: String? = null,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val titleManuallySet: Boolean = false,
    val folderId: String? = null,
    val deletedAt: Long? = null,
)

@Entity(tableName = "temporary_chat_drafts")
data class TemporaryChatDraftEntity(
    @PrimaryKey val projectScope: String,
    val projectId: String? = null,
    val folderId: String? = null,
    val text: String,
    val quoteMessageId: Long? = null,
    val quoteText: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val selectedNoteIdsJson: String = "[]",
    @ColumnInfo(defaultValue = "'[]'") val pendingAttachmentsJson: String = "[]",
    val updatedAt: Long,
)

@Entity(tableName = "chat_messages", indices = [Index("sessionId")])
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val sessionId: Long,
    val role: String,
    val text: String,
    val reasoning: String? = null,
    val status: String = "done",
    val time: String = "",
    val attachment: String? = null,
    val noteRef: String? = null,
    val quoteMessageId: Long? = null,
    val quoteText: String? = null,
    val createdAt: Long = 0,
    val contextSnapshot: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val sourcesJson: String = "[]",
    @ColumnInfo(defaultValue = "'[]'") val queriesJson: String = "[]",
)

@Entity(tableName = "chat_drafts")
data class ChatDraftEntity(
    @PrimaryKey val sessionId: Long,
    val text: String,
    val quoteMessageId: Long? = null,
    val quoteText: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val selectedNoteIdsJson: String = "[]",
    @ColumnInfo(defaultValue = "'[]'") val pendingAttachmentsJson: String = "[]",
    val updatedAt: Long,
)

@Entity(tableName = "memories", indices = [Index("projectId")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val projectId: String? = null,
    val content: String,
    val sourceType: String = "manual",
    val sourceId: String? = null,
    val createdAt: Long,
    val enabled: Boolean = true,
)

@Entity(tableName = "attachments", indices = [Index("ownerId")])
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: String,
    val name: String,
    val mime: String,
    val localPath: String,
    val size: Long,
    val textContent: String? = null,
    val createdAt: Long,
)
