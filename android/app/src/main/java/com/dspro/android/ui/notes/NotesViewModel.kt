package com.dspro.android.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dspro.android.DsApplication
import com.dspro.android.data.notes.FolderEntity
import com.dspro.android.data.notes.NoteEntity
import com.dspro.android.data.notes.TagEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteListItem(
    val note: NoteEntity,
    val tags: List<TagEntity>,
    val folderName: String?,
)

data class FolderLoadState(val projectId: String?, val loaded: Boolean)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DsApplication).container.notesRepository

    private val folderProject = MutableStateFlow<String?>(null)
    val folderLoadState = MutableStateFlow(FolderLoadState(null, false))

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders: StateFlow<List<FolderEntity>> = folderProject
        .flatMapLatest { projectId ->
            repository.observeFolders(projectId)
                .onStart { folderLoadState.value = FolderLoadState(projectId, false) }
                .onEach { folderLoadState.value = FolderLoadState(projectId, true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTags: StateFlow<List<TagEntity>> = repository.tags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val query = MutableStateFlow("")
    private val folderFilter = MutableStateFlow<String?>(null)
    private val tagFilter = MutableStateFlow<String?>(null)
    private val projectFilter = MutableStateFlow<String?>(null)
    private val searchActive = MutableStateFlow(false)

    val currentQuery: StateFlow<String> = query
    val isSearchActive: StateFlow<Boolean> = searchActive
    val currentFolder: StateFlow<String?> = folderFilter
    val currentTag: StateFlow<String?> = tagFilter
    val currentProject: StateFlow<String?> = projectFilter

    @OptIn(ExperimentalCoroutinesApi::class)
    val pickerItems: StateFlow<List<NoteListItem>> = combine(repository.notes, folders, allTags, repository.noteTags, projectFilter) { notes, folderRows, tagRows, refs, projectId ->
        val tagsById = tagRows.associateBy { it.id }
        val foldersById = folderRows.associateBy { it.id }
        val refsByNote = refs.groupBy { it.noteId }
        notes.filter { it.projectId == projectId }.map { note ->
            NoteListItem(note, refsByNote[note.id].orEmpty().mapNotNull { tagsById[it.tagId] }, note.folderId?.let { foldersById[it]?.name })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val visibleNotes = query
        .debounce(120)
        .flatMapLatest { q ->
            if (q.isBlank()) repository.notes else repository.search(q.trim())
        }

    private data class NoteData(
        val notes: List<NoteEntity>,
        val folders: List<FolderEntity>,
        val tags: List<TagEntity>,
        val refs: List<com.dspro.android.data.notes.NoteTagCrossRef>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseData = combine(
        visibleNotes,
        folders,
        allTags,
        repository.noteTags,
    ) { notes, folders, tags, refs -> NoteData(notes, folders, tags, refs) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<NoteListItem>> = combine(baseData, folderFilter, tagFilter, projectFilter, searchActive) { data, folderId, tagId, projectId, isSearching ->
        val tagsById = data.tags.associateBy { it.id }
        val foldersById = data.folders.associateBy { it.id }
        val refsByNote = data.refs.groupBy { it.noteId }
        data.notes
            .filter { note ->
                val folder = note.folderId?.let { foldersById[it] }
                note.projectId == projectId &&
                    ((isSearching || query.value.isNotBlank() || tagId != null) || (folderId != null && note.folderId == folderId) ||
                        (folderId == null && (note.folderId == null || folder == null || folder.archivedAt != null))) &&
                    (tagId == null || refsByNote[note.id].orEmpty().any { it.tagId == tagId })
            }
            .map { note ->
                NoteListItem(
                    note = note,
                    tags = refsByNote[note.id].orEmpty().mapNotNull { tagsById[it.tagId] },
                    folderName = note.folderId?.let { foldersById[it]?.name },
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFolder(folderId: String?) {
        folderFilter.value = folderId
    }

    fun setTag(tagId: String?) {
        tagFilter.value = tagId
    }

    fun setSearchActive(active: Boolean) { searchActive.value = active }

    fun setProject(projectId: String?) {
        projectFilter.value = projectId
        folderProject.value = projectId
        folderFilter.value = null
        tagFilter.value = null
        query.value = ""
        searchActive.value = false
    }

    fun createNote(folderId: String? = null, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val note = repository.createNote(
                folderId = folderId,
                title = "无标题笔记",
                content = "",
                projectId = currentProject.value,
            )
            onCreated(note.id)
        }
    }

    fun createFolder(name: String, parentId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createFolder(name.trim(), currentProject.value, parentId) }
    }

    fun createTag(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createTag(name.trim()) }
    }

    fun changeFolder(id: String, archive: Boolean?) {
        viewModelScope.launch {
            if (archive == null) repository.deleteFolder(id) else repository.setFolderArchived(id, archive)
            if (folderFilter.value == id && archive != false) folderFilter.value = null
        }
    }

    fun archiveTag(id: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setTagArchived(id, archived)
            if (tagFilter.value == id && archived) tagFilter.value = null
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch {
            repository.deleteTag(id)
            if (tagFilter.value == id) tagFilter.value = null
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(
                id = note.id,
                title = note.title,
                content = note.content,
                folderId = note.folderId,
                pinned = !note.pinned,
            )
        }
    }
}
