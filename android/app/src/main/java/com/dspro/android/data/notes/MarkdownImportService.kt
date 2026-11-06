package com.dspro.android.data.notes

class MarkdownImportService(private val notes: NotesRepository) {
    suspend fun importMarkdown(title: String, content: String, projectId: String?, folderId: String?): NoteEntity =
        notes.importMarkdown(title, content, projectId, folderId)
}
