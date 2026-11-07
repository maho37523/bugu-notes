package com.dspro.android.ui.notes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dspro.android.data.notes.NoteEntity
import com.dspro.android.data.notes.NoteRevisionEntity
import com.dspro.android.data.notes.NotesRepository
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshHairline
import com.dspro.android.ui.DshIconButton
import com.dspro.android.ui.DshPill
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private data class NoteDraft(val title: String, val folderId: String?, val pinned: Boolean, val revision: Int)

/** Optional holder supplied by the caller to keep an unsaved note intact across editor remounts. */
@Stable
class NewNoteEditorState(
    title: String = "",
    content: String = "",
    folderId: String? = null,
    projectId: String? = null,
    pinned: Boolean = false,
    titleEdited: Boolean = false,
) {
    var title by mutableStateOf(title)
    var content by mutableStateOf(content)
    var folderId by mutableStateOf(folderId)
    var projectId by mutableStateOf(projectId)
    var pinned by mutableStateOf(pinned)
    var titleEdited by mutableStateOf(titleEdited)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String,
    repository: NotesRepository,
    onQuoteToChat: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    newDraft: NoteEntity? = null,
    newDraftState: NewNoteEditorState? = null,
) {
    val c = Dsh.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveLock = remember(noteId) { Mutex() }
    val allTags by repository.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    val revisions by repository.observeRevisions(noteId).collectAsStateWithLifecycle(initialValue = emptyList())

    var title by remember(noteId, newDraftState) { mutableStateOf(newDraftState?.title.orEmpty()) }
    var titleEdited by remember(noteId, newDraftState) { mutableStateOf(newDraftState?.titleEdited ?: false) }
    var document by remember(noteId, newDraft, newDraftState) {
        mutableStateOf(if (newDraft != null) NoteDocument(newDraftState?.content ?: newDraft.content) else null)
    }
    var folderId by remember(noteId, newDraft, newDraftState) { mutableStateOf(if (newDraftState != null) newDraftState.folderId else newDraft?.folderId) }
    var projectId by remember(noteId, newDraft, newDraftState) { mutableStateOf(if (newDraftState != null) newDraftState.projectId else newDraft?.projectId) }
    val folders by remember(projectId) { repository.observeFolders(projectId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var pinned by remember(noteId, newDraft, newDraftState) { mutableStateOf(newDraftState?.pinned ?: newDraft?.pinned ?: false) }
    var selectedTagIds by remember(noteId) { mutableStateOf<List<String>>(emptyList()) }
    val loaded = document != null
    val meaningfulNewNote = newDraft != null && (document?.readMarkdown()?.isNotBlank() == true || (titleEdited && title.isNotBlank()))
    var saveLabel by remember(noteId) { mutableStateOf("自动保存") }
    var showMore by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showVersionSave by remember { mutableStateOf(false) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var versionName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var newTagName by remember { mutableStateOf("") }
    var leaving by remember(noteId) { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (newDraft != null) return@LaunchedEffect
        val note = repository.getNote(noteId) ?: newDraft
        if (note != null) {
            title = note.title
            folderId = note.folderId
            projectId = note.projectId
            pinned = note.pinned
            selectedTagIds = repository.tagIds(noteId)
            if (document == null) document = NoteDocument(note.content)
        }
    }

    val currentDraftContent = document?.readMarkdown().orEmpty()
    SideEffect {
        if (newDraft != null && newDraftState != null) {
            newDraftState.title = title
            newDraftState.titleEdited = titleEdited
            newDraftState.content = currentDraftContent
            newDraftState.folderId = folderId
            newDraftState.projectId = projectId
            newDraftState.pinned = pinned
        }
    }

    suspend fun persistDraft() {
        saveLock.withLock {
            val current = document ?: return@withLock
            val draft = NoteDraft(title, folderId, pinned, current.revision)
            val markdown = current.readMarkdown()
            val savedTitle = draft.title.ifBlank {
                markdown.lineSequence().firstOrNull { it.isNotBlank() }
                    ?.trim()?.trimStart('#')?.trim()?.take(40).orEmpty()
            }
            if (newDraft != null) {
                val saved = repository.upsertNewNoteDraft(noteId, draft.title, markdown, draft.folderId, projectId, draft.pinned, titleEdited)
                if (saved != null) repository.setTags(noteId, selectedTagIds)
            } else repository.updateNote(
                id = noteId, title = savedTitle, content = markdown,
                folderId = draft.folderId, pinned = draft.pinned,
            )
            if (document === current && draft == NoteDraft(title, folderId, pinned, current.revision)) {
                if (title.isBlank()) title = savedTitle
                saveLabel = "已保存"
            }
        }
    }

    suspend fun trySave(): Boolean = try {
        persistDraft()
        true
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        saveLabel = "保存失败"
        false
    }

    LaunchedEffect(noteId) {
        snapshotFlow { document?.let { it to NoteDraft(title, folderId, pinned, it.revision) } }
            .collectLatest { draft ->
                if (draft != null) {
                    saveLabel = "自动保存中"
                    delay(500)
                    trySave()
                }
            }
    }

    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && document != null) {
                lifecycleOwner.lifecycleScope.launch { trySave() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (document != null) lifecycleOwner.lifecycleScope.launch { trySave() }
        }
    }

    LaunchedEffect(selectedTagIds, loaded, newDraft, meaningfulNewNote) {
        if (loaded && (newDraft == null || meaningfulNewNote)) repository.setTags(noteId, selectedTagIds)
    }

    LaunchedEffect(noteId) {
        while (true) {
            delay(10 * 60 * 1000L)
            val markdown = document?.readMarkdown()
            val canKeepNewDraft = newDraft == null || !markdown.isNullOrBlank() || (titleEdited && title.isNotBlank())
            if (!markdown.isNullOrBlank() && canKeepNewDraft) {
                if (newDraft != null && !trySave()) continue
                repository.saveVersion(noteId, markdown, "自动检查点", "auto")
            }
        }
    }

    fun saveAndLeave(action: suspend () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            try {
                if (trySave()) action()
                else Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
            } finally {
                leaving = false
            }
        }
    }

    fun openTagSheet() {
        if (newDraft == null) {
            showTagSheet = true
            return
        }
        scope.launch {
            val content = document?.readMarkdown().orEmpty()
            if (content.isNotBlank() || (titleEdited && title.isNotBlank())) {
                if (trySave()) showTagSheet = true
            }
        }
    }

    BackHandler { saveAndLeave { onBack() } }

    fun copyAll() {
        scope.launch {
            val markdown = document?.readMarkdown() ?: return@launch
            clipboard.setText(AnnotatedString(markdown))
            Toast.makeText(context, "已复制全文", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val markdown = document?.readMarkdown() ?: return@launch
                try {
                    withContext(Dispatchers.IO) {
                        val stream = checkNotNull(context.contentResolver.openOutputStream(uri))
                        stream.use { it.write(markdown.toByteArray(Charsets.UTF_8)) }
                    }
                    Toast.makeText(context, "已导出 Markdown", Toast.LENGTH_SHORT).show()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(c.main).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).background(c.surface).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DshIconButton(Icons.Outlined.ArrowBack, "返回", { saveAndLeave { onBack() } }, size = 44.dp, iconSize = 20.dp)
                Column(Modifier.weight(1f)) {
                    Text(saveLabel, color = c.muted, fontSize = 11.sp)
                }
                if (loaded) {
                    DshIconButton(Icons.Outlined.ChatBubbleOutline, "引用到对话", {
                        saveAndLeave {
                            val markdown = document?.readMarkdown().orEmpty()
                            onQuoteToChat("《${title.ifBlank { "笔记" }}》\n\n$markdown".take(4000))
                        }
                    }, size = 44.dp, iconSize = 20.dp)
                }
                Box {
                    DshIconButton(Icons.Outlined.MoreHoriz, "更多", { if (loaded) showMore = true }, size = 44.dp, iconSize = 21.dp)
                    DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                        DropdownMenuItem(
                            text = { Text("复制全文") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)) },
                            onClick = { showMore = false; copyAll() },
                        )
                        if (newDraft == null || meaningfulNewNote) {
                            DropdownMenuItem(
                                text = { Text("保存版本…", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Save, null, Modifier.size(16.dp), tint = c.muted) },
                                onClick = { showMore = false; showVersionSave = true },
                            )
                            DropdownMenuItem(
                                text = { Text("版本历史（${revisions.size}）", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = c.muted) },
                                onClick = { showMore = false; showVersionHistory = true },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("导出 Markdown", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Save, null, Modifier.size(16.dp), tint = c.muted) },
                            onClick = {
                                showMore = false
                                val safeName = (title.ifBlank { "笔记" }).replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                exportLauncher.launch("$safeName.md")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (pinned) "取消置顶" else "置顶笔记", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, null, Modifier.size(16.dp), tint = c.muted) },
                            onClick = { showMore = false; pinned = !pinned },
                        )
                        DropdownMenuItem(
                            text = { Text("移到回收站", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMore = false; showDeleteDialog = true },
                        )
                    }
                }
                DshIconButton(Icons.Outlined.Settings, "设置", { saveAndLeave { onSettings() } }, size = 44.dp)
            }
            DshHairline()

            BasicTextField(
                value = title,
                onValueChange = { title = it; if (newDraft != null) titleEdited = true },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    if (title.isBlank()) Text("无标题笔记", color = c.subtle, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    inner()
                },
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box {
                    var folderMenu by remember { mutableStateOf(false) }
                    DshPill(
                        text = folders.firstOrNull { it.id == folderId }?.name ?: "未分类",
                        leading = { Icon(Icons.Outlined.Folder, null, Modifier.size(11.dp), tint = c.subtle) },
                        trailing = { Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(11.dp), tint = c.subtle) },
                        onClick = { folderMenu = true },
                    )
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("未分类", fontSize = 12.sp) },
                            onClick = { folderId = null; folderMenu = false },
                        )
                        folders.filter { it.archivedAt == null }.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name, fontSize = 12.sp) },
                                onClick = { folderId = folder.id; folderMenu = false },
                            )
                        }
                        DshHairline()
                        DropdownMenuItem(
                            text = { Text("＋ 新建分类", fontSize = 12.sp, color = c.accent) },
                            onClick = { folderMenu = false; showNewFolder = true },
                        )
                    }
                }
                selectedTagIds.mapNotNull { id -> allTags.firstOrNull { it.id == id } }.take(3).forEach { tag ->
                    DshPill(text = "#${tag.name}", onClick = { openTagSheet() })
                }
                DshPill(
                    text = "标签",
                    leading = { Icon(Icons.Outlined.Add, null, Modifier.size(11.dp), tint = c.subtle) },
                    onClick = { openTagSheet() },
                )
                if (pinned) Icon(Icons.Outlined.PushPin, null, Modifier.size(13.dp), tint = c.accent)
            }

            Spacer(Modifier.height(8.dp))
            DshHairline()
            val current = document
            if (current != null) {
                key(current) {
                    ContinuousNoteEditor(
                        document = current,
                        onWikiLink = { target ->
                            saveAndLeave {
                                val note = repository.findOrCreateByTitle(target, projectId)
                                onOpenNote(note.id)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("加载中…", color = c.subtle, fontSize = 12.sp)
                }
            }
        }
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            containerColor = c.surface,
            title = { Text("新建分类", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("例如：产品 / 阅读", fontSize = 12.sp, color = c.subtle) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        scope.launch {
                            val folder = repository.createFolder(newFolderName.trim(), projectId)
                            folderId = folder.id
                            newFolderName = ""
                            showNewFolder = false
                        }
                    }
                }) { Text("创建并选中", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("取消", color = c.muted) } },
        )
    }

    if (showVersionSave) {
        AlertDialog(
            onDismissRequest = { showVersionSave = false },
            containerColor = c.surface,
            title = { Text("保存版本", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        placeholder = { Text("版本名称，例如：方案定稿", fontSize = 12.sp, color = c.subtle) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("工作副本仍会自动保存；这个版本不会被自动覆盖。", color = c.subtle, fontSize = 9.5.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val markdown = document?.readMarkdown() ?: return@launch
                        if (newDraft != null && markdown.isBlank() && !(titleEdited && title.isNotBlank())) {
                            showVersionSave = false
                            return@launch
                        }
                        if (newDraft != null && !trySave()) return@launch
                        repository.saveVersion(noteId, markdown, versionName.ifBlank { "手动版本" }, "manual")
                        versionName = ""
                        showVersionSave = false
                        Toast.makeText(context, "版本已保存", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showVersionSave = false }) { Text("取消", color = c.muted) } },
        )
    }

    if (showVersionHistory) {
        ModalBottomSheet(
            onDismissRequest = { showVersionHistory = false },
            containerColor = c.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = c.hairlineStrong) },
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("版本历史", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("恢复会生成一个新版本，不覆盖现有历史。", color = c.muted, fontSize = 10.5.sp)
                Spacer(Modifier.height(12.dp))
                if (revisions.isEmpty()) {
                    Text("还没有保存版本。用「保存版本」创建一个命名版本。", color = c.subtle, fontSize = 12.sp)
                } else {
                    revisions.take(20).forEach { revision ->
                        VersionRow(revision) {
                            scope.launch {
                                val markdown = document?.readMarkdown() ?: return@launch
                                if (newDraft != null && markdown.isBlank() && !(titleEdited && title.isNotBlank())) {
                                    showVersionHistory = false
                                    return@launch
                                }
                                if (newDraft != null && !trySave()) return@launch
                                repository.saveVersion(noteId, markdown, "恢复前", "auto")
                                document = NoteDocument(revision.content)
                                showVersionHistory = false
                                Toast.makeText(context, "已恢复 v${revision.version}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTagSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTagSheet = false },
            containerColor = c.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = c.hairlineStrong) },
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("标签", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allTags.filter { it.archivedAt == null || it.id in selectedTagIds }.forEach { tag ->
                        val selected = selectedTagIds.contains(tag.id)
                        Surface(
                            color = if (selected) c.accentSoft else Color.Transparent,
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, if (selected) c.accent.copy(alpha = 0.25f) else c.hairline),
                            modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable {
                                selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                            },
                        ) {
                            Text(
                                "#${tag.name}",
                                color = if (selected) c.accent else c.muted,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = { Text("新标签", fontSize = 11.sp, color = c.subtle) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(9.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                scope.launch {
                                    val tag = repository.createTag(newTagName.trim())
                                    selectedTagIds = selectedTagIds + tag.id
                                    newTagName = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                        shape = RoundedCornerShape(9.dp),
                    ) { Text("添加", fontSize = 11.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Text("点选标签后会自动保存", color = c.subtle, fontSize = 9.5.sp)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = c.surface,
            title = { Text("移到回收站？", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("笔记会进入回收站，可在回收站恢复或永久删除。", color = c.muted, fontSize = 11.5.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        repository.deleteNote(noteId)
                        onBack()
                    }
                }) { Text("移到回收站", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = c.muted) } },
        )
    }
}

@Composable
private fun VersionRow(revision: NoteRevisionEntity, onRestore: () -> Unit) {
    val c = Dsh.colors
    Surface(
        color = c.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, c.hairline),
        modifier = Modifier.fillMaxWidth().clickable { onRestore() },
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "v${revision.version} · ${revision.name.ifBlank { revision.reason }}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    (if (revision.kind == "manual") "手动版本" else "自动检查点") + " · " + relativeTime(revision.createdAt),
                    color = c.subtle,
                    fontSize = 9.5.sp,
                )
            }
            Text("恢复", color = c.accent, fontSize = 10.5.sp)
        }
    }
}


private fun relativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time))
    }
}
