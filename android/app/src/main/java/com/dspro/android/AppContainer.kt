package com.dspro.android

import android.content.Context
import com.dspro.android.data.ApiConfigStore
import com.dspro.android.data.ApiConfig
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
    fun chatClient(config: ApiConfig) = DeepSeekClient(config)
}
