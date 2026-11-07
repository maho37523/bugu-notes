package com.dspro.android

import android.content.Context
import com.dspro.android.data.*
import com.dspro.android.data.ApiConfigStore
import com.dspro.android.data.ApiConfig
import com.dspro.android.data.ChatClient
import com.dspro.android.data.chatgpt.AndroidChatGptStore
import com.dspro.android.data.chatgpt.ChatGptAccounts
import com.dspro.android.data.chatgpt.ChatGptClient
import com.dspro.android.data.DeepSeekClient
import com.dspro.android.data.notes.ChatRepository
import com.dspro.android.data.notes.MemoryRepository
import com.dspro.android.data.notes.NotesDatabase
import com.dspro.android.data.notes.NotesRepository
import com.dspro.android.data.notes.ProjectsRepository
import com.dspro.android.data.notes.ContextSnapshotService
import com.dspro.android.data.notes.WorkspaceSearchService
import com.dspro.android.data.notes.BackupService
import com.dspro.android.data.notes.MarkdownImportService

class AppContainer(context: Context) {
    private val database = NotesDatabase.create(context.applicationContext)
    val notesRepository = NotesRepository(database)
    val projectsRepository = ProjectsRepository(database)
    val chatRepository = ChatRepository(database)
    val memoryRepository = MemoryRepository(database)
    val contextSnapshotService = ContextSnapshotService(notesRepository)
    val workspaceSearchService = WorkspaceSearchService(notesRepository, chatRepository)
    val backupService = BackupService(context.applicationContext, database)
    val markdownImportService = MarkdownImportService(notesRepository)
    val apiConfigStore = ApiConfigStore(context.applicationContext)
    val chatGptAccounts = ChatGptAccounts(AndroidChatGptStore(context.applicationContext))
    fun loadConfig(provider: String? = null) = chatGptAccounts.configuration(apiConfigStore.load(provider))
    fun composerConnections(currentProvider: String): List<ComposerConnection> =
        listOf("deepseek", "openai", "chatgpt", "anthropic", "aliyun", "custom").mapNotNull { provider ->
            val cfg = loadConfig(provider)
            if (!cfg.isConfigured && provider != currentProvider) null else {
                val catalog = chatGptAccounts.state.value.accounts.find { it.id == cfg.chatGptAccountId }?.models.orEmpty()
                val models = if (provider == "chatgpt") catalog.map { it.slug to it.displayName }
                    else (listOf(cfg.model) + suggestedChatModels(provider)).filter { it.isNotBlank() }.distinct().map { it to it }
                ComposerConnection(provider, providerLabel(provider), cfg.model, models.map { (id, label) ->
                    val stored = apiConfigStore.load(provider, id)
                    val advertised = catalog.find { it.slug == id }?.reasoningLevels
                    ComposerModel(id, label, effectiveThinkingEffort(stored, advertised), supportedThinkingEfforts(provider, id, advertised))
                })
            }
        }
    fun chatClient(config: ApiConfig): ChatClient = if (config.provider == "chatgpt") ChatGptClient(chatGptAccounts, config) else DeepSeekClient(config)
}
