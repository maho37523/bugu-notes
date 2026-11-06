package com.dspro.android.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dspro.android.data.notes.FolderEntity
import com.dspro.android.data.notes.TagEntity
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshIconButton

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NotesScreen(
    viewModel: NotesViewModel,
    onOpenTrash: () -> Unit,
    onOpenNote: (String) -> Unit,
    onSearch: () -> Unit,
    collectionsOnly: Boolean = false,
    onBack: () -> Unit = {},
) {
    val c = Dsh.colors
    val items by viewModel.items.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.currentFolder.collectAsStateWithLifecycle()
    var showFolderDialog by remember { mutableStateOf(false) }
    var manageCollections by remember { mutableStateOf(false) }
    var folderActions by remember { mutableStateOf<FolderEntity?>(null) }
    var tagActions by remember { mutableStateOf<TagEntity?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }

    BackHandler(enabled = collectionsOnly || selectedFolder != null) {
        if (collectionsOnly) onBack() else viewModel.setFolder(null)
    }

    Column(Modifier.fillMaxSize().background(c.main)) {
        if (collectionsOnly) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DshIconButton(Icons.Outlined.ArrowBack, "返回", onBack, size = 44.dp, iconSize = 19.dp)
                Text("分类管理", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("文件夹", modifier = Modifier.weight(1f), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { showFolderDialog = true }) { Text("＋ 新建文件夹", color = c.accent) }
                    }
                }
                items(folders, key = { "manage-folder-${it.id}" }) { folder ->
                    CollectionRow(folder.name, folder.archivedAt != null) { folderActions = folder }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("标签", modifier = Modifier.weight(1f), color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { showTagDialog = true }) { Text("＋ 新建标签", color = c.accent) }
                    }
                }
                items(tags, key = { "manage-tag-${it.id}" }) { tag ->
                    CollectionRow(tag.name, tag.archivedAt != null) { tagActions = tag }
                }
            }
        } else {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                DshIconButton(Icons.Outlined.Close, "退出选择", {
                    selectionMode = false
                    selectedIds = emptySet()
                }, size = 34.dp, iconSize = 20.dp)
                Text("已选 ${selectedIds.size}", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                DshIconButton(Icons.Outlined.DeleteOutline, "移动到垃圾桶", {
                    if (selectedIds.isNotEmpty()) confirmDelete = true
                }, size = 34.dp, iconSize = 19.dp)
            } else {
                if (selectedFolder != null) DshIconButton(Icons.Outlined.ArrowBack, "返回文件夹列表", { viewModel.setFolder(null) }, size = 40.dp, iconSize = 19.dp)
                selectedFolder?.let { id -> Text(folders.firstOrNull { it.id == id }?.name.orEmpty(), modifier = Modifier.widthIn(max = 110.dp), color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.weight(1f))
                DshIconButton(Icons.Outlined.Search, "搜索笔记和对话", onSearch, size = 44.dp, iconSize = 19.dp)
                DshIconButton(Icons.Outlined.Tune, "管理文件夹和标签", {
                    manageCollections = true
                }, size = 44.dp, iconSize = 18.dp)
                DshIconButton(Icons.Outlined.DeleteOutline, "垃圾桶", onOpenTrash, size = 44.dp, iconSize = 18.dp)
                Button(
                    onClick = { viewModel.createNote(selectedFolder) { onOpenNote(it) } },
                    modifier = Modifier.heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(Icons.Outlined.Add, "", Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selectedFolder == null) items(folders.filter { it.archivedAt == null }, key = { "folder-row-${it.id}" }) { folder ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).combinedClickable(
                        onClick = { viewModel.setFolder(folder.id) },
                        onLongClick = { folderActions = folder },
                    ).background(c.surface).padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Folder, null, Modifier.size(18.dp), tint = c.accent)
                    Spacer(Modifier.width(10.dp))
                    Text(folder.name, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("打开", color = c.subtle, fontSize = 10.sp)
                }
            }
            if (items.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(34.dp), tint = c.subtle)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "还没有笔记",
                            color = c.muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点右上角新建，或从对话里保存一条回答",
                            color = c.subtle,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            items(items, key = { it.note.id }) { item ->
                NoteCard(
                    item = item,
                    selectionMode = selectionMode,
                    selected = selectedIds.contains(item.note.id),
                    onToggleSelect = {
                        selectedIds = if (selectedIds.contains(item.note.id)) selectedIds - item.note.id else selectedIds + item.note.id
                    },
                    onEnterSelection = {
                        selectionMode = true
                        selectedIds = selectedIds + item.note.id
                    },
                    onOpen = { onOpenNote(item.note.id) },
                    onTogglePin = { viewModel.togglePin(item.note) },
                    onDelete = { viewModel.deleteNote(item.note.id) },
                )
            }
        }
        }
    }

    if (manageCollections && !collectionsOnly) {
        AlertDialog(
            onDismissRequest = { manageCollections = false },
            title = { Text("文件夹与标签") },
            text = {
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    item {
                        Row {
                            TextButton(onClick = { manageCollections = false; showFolderDialog = true }) { Text("+ 文件夹") }
                            TextButton(onClick = { manageCollections = false; showTagDialog = true }) { Text("+ 标签") }
                        }
                    }
                    item { Text("文件夹", color = c.muted, fontSize = 12.sp) }
                    items(folders, key = { "folder-${it.id}" }) { folder ->
                        CollectionRow(folder.name, folder.archivedAt != null) {
                            manageCollections = false
                            folderActions = folder
                        }
                    }
                    item { Text("标签", color = c.muted, fontSize = 12.sp) }
                    items(tags, key = { "tag-${it.id}" }) { tag ->
                        CollectionRow(tag.name, tag.archivedAt != null) {
                            manageCollections = false
                            tagActions = tag
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { manageCollections = false }) { Text("完成") } },
        )
    }
    folderActions?.let { folder ->
        CollectionActions(
            folder.name, "文件夹", folder.archivedAt != null,
            "文件夹中的笔记会变为未分类，正文会保留。",
            onArchive = { viewModel.changeFolder(folder.id, folder.archivedAt == null) },
            onDelete = { viewModel.changeFolder(folder.id, null) },
            onDismiss = { folderActions = null },
        )
    }
    tagActions?.let { tag ->
        CollectionActions(
            tag.name, "标签", tag.archivedAt != null,
            "这个标签会从所有笔记移除，正文会保留。",
            onArchive = { viewModel.archiveTag(tag.id, tag.archivedAt == null) },
            onDelete = { viewModel.deleteTag(tag.id) },
            onDismiss = { tagActions = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            title = { Text("把 ${selectedIds.size} 篇笔记移到垃圾桶？", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("可以在垃圾桶恢复或永久删除。", color = c.muted, fontSize = 11.5.sp) },
            confirmButton = {
                TextButton(onClick = {
                    selectedIds.forEach { viewModel.deleteNote(it) }
                    selectedIds = emptySet()
                    selectionMode = false
                    confirmDelete = false
                }) { Text("移到垃圾桶", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消", color = c.muted) } },
        )
    }
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = c.surface,
            title = { Text("新建文件夹", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newFolder,
                    onValueChange = { newFolder = it },
                    placeholder = { Text("例如：产品 / 阅读", fontSize = 12.sp, color = c.subtle) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(newFolder)
                    newFolder = ""
                    showFolderDialog = false
                }) { Text("创建", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showFolderDialog = false }) { Text("取消", color = c.muted) } },
        )
    }
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = c.surface,
            title = { Text("新建标签", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    placeholder = { Text("例如：灵感 / 待办", fontSize = 12.sp, color = c.subtle) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createTag(newTag)
                    newTag = ""
                    showTagDialog = false
                }) { Text("创建", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text("取消", color = c.muted) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionRow(name: String, archived: Boolean, onActions: () -> Unit) {
    Text(
        if (archived) "$name · 已归档" else name,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onActions, onLongClick = onActions)
            .padding(vertical = 12.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    item: NoteListItem,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onEnterSelection: () -> Unit,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Dsh.colors
    var menu by remember { mutableStateOf(false) }
    Surface(
        color = c.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, c.hairline),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).combinedClickable(
            onClick = { if (selectionMode) onToggleSelect() else onOpen() },
            onLongClick = { if (!selectionMode) onEnterSelection() },
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(checkedColor = c.accent),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (item.note.pinned) {
                    Icon(Icons.Outlined.PushPin, null, Modifier.size(13.dp), tint = c.accent)
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    item.note.title.ifBlank { "无标题笔记" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    DshIconButton(Icons.Outlined.MoreHoriz, "更多", { menu = true }, size = 26.dp, iconSize = 16.dp)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (item.note.pinned) "取消置顶" else "置顶", fontSize = 12.sp) },
                            onClick = { menu = false; onTogglePin() },
                        )
                        DropdownMenuItem(
                            text = { Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            val excerpt = stripMarkdown(item.note.content).take(96)
            if (excerpt.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(excerpt, color = c.muted, fontSize = 11.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.folderName?.let {
                    Icon(Icons.Outlined.Folder, null, Modifier.size(11.dp), tint = c.subtle)
                    Spacer(Modifier.width(3.dp))
                    Text(it, color = c.subtle, fontSize = 9.5.sp)
                    Spacer(Modifier.width(8.dp))
                }
                item.tags.take(3).forEach { tag ->
                    Text("#${tag.name}", color = c.accent, fontSize = 9.5.sp, modifier = Modifier.padding(end = 6.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(relativeTime(item.note.updatedAt), color = c.subtle, fontSize = 9.sp)
            }
        }
    }
}

private fun stripMarkdown(markdown: String): String =
    markdown
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), " ")
        .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[\\[([^\\]]+)]]"), "$1")
        .replace(Regex("[#>*_`~-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun relativeTime(time: Long): String {
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 7 * 86_400_000 -> "${diff / 86_400_000} 天前"
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(time))
    }
}
