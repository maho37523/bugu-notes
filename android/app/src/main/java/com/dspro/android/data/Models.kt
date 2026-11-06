package com.dspro.android.data

enum class Role { USER, ASSISTANT }

enum class MsgStatus { DONE, THINKING, STREAMING, STOPPED }

data class Source(
    val title: String,
    val site: String,
    val date: String,
    val snippet: String,
    val url: String = "",
)

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val time: String,
    val status: MsgStatus = MsgStatus.DONE,
    val reasoning: String? = null,
    val queries: List<String> = emptyList(),
    val sources: List<Source> = emptyList(),
    val memory: List<String> = emptyList(),
    val reasoningExpanded: Boolean = false,
    val reasoningSeconds: Int = 0,
    val error: String? = null,
    val liked: Boolean? = null,
    val version: Int = 1,
    val versionCount: Int = 1,
    val attachment: String? = null,
    val noteRef: String? = null,
    val quoteMessageId: Long? = null,
    val quoteText: String? = null,
    val contextSnapshot: String? = null,
    val searching: Boolean = false,
    val searchStatus: String = "",
)

data class Session(
    val id: Long,
    val title: String,
    val updated: String,
    val messages: List<ChatMessage>,
    val projectId: String? = null,
    val archived: Boolean = false,
    val folderId: String? = null,
    val deleted: Boolean = false,
    val updatedAt: Long = 0,
)

data class AutomationTask(
    val id: Long,
    val title: String,
    val schedule: String,
    val prompt: String,
    val active: Boolean,
    val lastRun: String,
)

data class PluginInfo(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val icon: String,
)
