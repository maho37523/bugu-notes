package com.dspro.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.Session
import com.dspro.android.data.notes.FolderEntity
import com.dspro.android.data.notes.NoteEntity
import com.dspro.android.data.notes.FolderScopeMigration
import com.dspro.android.data.notes.ScopedFolderRecord

@Composable
fun LibraryScreen(
    notes: List<NoteEntity>, sessions: List<Session>, folders: List<FolderEntity>,
    filter: ContentFilter, section: LibrarySection, folderId: String?,
    onFilter: (ContentFilter) -> Unit, onFolder: (String?) -> Unit,
    onOpenNote: (NoteEntity) -> Unit, onOpenChat: (Session) -> Unit,
    onNoteAction: (NoteEntity, ContentAction) -> Unit,
    onChatAction: (Session, ContentAction) -> Unit,
    onFolderAction: (FolderEntity, ContentAction) -> Unit,
    isWide: Boolean = false, treeOpen: Boolean = false, expandedFolderIds: List<String> = emptyList(),
    onTreeOpen: (Boolean) -> Unit = {}, onToggleFolder: (String) -> Unit = {},
    onNewNote: () -> Unit = {}, onNewChat: () -> Unit = {}, onNewFolder: (String?) -> Unit = {},
) {
    val c = Dsh.colors
    val foldersById = remember(folders) { folders.associateBy { it.id } }
    fun isArchived(id: String): Boolean {
        var cursor = foldersById[id]
        val seen = mutableSetOf<String>()
        while (cursor != null) {
            if (!seen.add(cursor.id) || cursor.archivedAt != null) return true
            cursor = cursor.parentId?.let(foldersById::get)
        }
        return false
    }
    val activeFolders = folders.filter { !isArchived(it.id) }
    val visibleTreeFolders = remember(activeFolders, expandedFolderIds) {
        val records = activeFolders.map { ScopedFolderRecord(it.id, it.name, it.sort, it.archivedAt, it.projectId, it.parentId) }
        val ids = FolderScopeMigration.visibleIds(records, expandedFolderIds.toSet())
        ids.mapNotNull { id -> activeFolders.firstOrNull { it.id == id } }
    }
    val visibleFolders = folders.filter { folder ->
        folder.parentId == folderId && when (section) {
            LibrarySection.Library -> !isArchived(folder.id)
            LibrarySection.Archive -> isArchived(folder.id) && (folderId != null || folder.parentId == null || !isArchived(folder.parentId!!))
            LibrarySection.Trash -> false
        }
    }
    val visibleNotes = notes.filter { note -> filter != ContentFilter.Chats && when (section) {
        LibrarySection.Library -> note.folderId == folderId && note.archivedAt == null && note.deletedAt == null
        LibrarySection.Archive -> note.deletedAt == null && if (folderId == null) note.archivedAt != null else note.folderId == folderId && isArchived(folderId)
        LibrarySection.Trash -> note.deletedAt != null
    } }
    val visibleChats = sessions.filter { session -> filter != ContentFilter.Notes && when (section) {
        LibrarySection.Library -> session.folderId == folderId && !session.archived && !session.deleted
        LibrarySection.Archive -> !session.deleted && if (folderId == null) session.archived else session.folderId == folderId && isArchived(folderId)
        LibrarySection.Trash -> session.deleted
    } }
    var createMenu by remember { mutableStateOf(false) }
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize().background(c.main)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContentFilter.entries.forEach { item ->
                        FilterChip(selected = filter == item, onClick = { onFilter(item) }, label = { Text(when (item) { ContentFilter.All -> "全部"; ContentFilter.Notes -> "笔记"; ContentFilter.Chats -> "对话" }) })
                    }
                }
                if (section == LibrarySection.Library && !isWide) TextButton(onClick = { onTreeOpen(true) }) { Text("目录") }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                item(key = "folder-breadcrumb") {
                    val path = buildList {
                        var current = folderId?.let(foldersById::get)
                        val seen = mutableSetOf<String>()
                        while (current != null && seen.add(current.id)) { add(current); current = current.parentId?.let(foldersById::get) }
                    }.asReversed()
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (folderId != null) IconButton(onClick = { onFolder(foldersById[folderId]?.parentId) }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Outlined.ArrowBack, "返回上一级", tint = c.muted)
                        }
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onFolder(null) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(if (section == LibrarySection.Archive) "归档" else if (section == LibrarySection.Trash) "回收站" else "资料库", color = c.muted) }
                            path.forEach { row ->
                                Text("/", color = c.subtle)
                                TextButton(onClick = { onFolder(row.id) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(row.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (row.id == folderId) c.accent else c.muted) }
                            }
                        }
                        if (section == LibrarySection.Library) Box {
                            IconButton(onClick = { createMenu = true }, modifier = Modifier.size(44.dp).semantics { contentDescription = "在当前位置新建" }) { Icon(androidx.compose.material.icons.Icons.Outlined.Add, "在当前位置新建", tint = c.accent) }
                            DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                                DropdownMenuItem(text = { Text("新建笔记") }, onClick = { createMenu = false; onNewNote() })
                                DropdownMenuItem(text = { Text("新建对话") }, onClick = { createMenu = false; onNewChat() })
                                DropdownMenuItem(text = { Text(if (folderId == null) "新建文件夹" else "新建子文件夹") }, onClick = { createMenu = false; onNewFolder(folderId) })
                            }
                        }
                    }
                }
                items(visibleFolders, key = { "folder:${it.id}" }) { folder ->
                    val folderActions = if (folder.archivedAt == null) listOf(ContentAction.Rename, ContentAction.MoveFolder, ContentAction.Archive, ContentAction.Trash) else listOf(ContentAction.Unarchive, ContentAction.Trash)
                    LibraryRow(icon = { Icon(Icons.Outlined.Folder, null) }, title = folder.name, subtitle = if (isArchived(folder.id)) "已归档文件夹" else "文件夹", onOpen = { onFolder(folder.id) }, actions = folderActions, action = { onFolderAction(folder, it) })
                }
                items(visibleNotes, key = { "note:${it.id}" }) { note ->
                    LibraryRow(icon = { Icon(Icons.Outlined.Description, null) }, title = note.title.ifBlank { "无标题笔记" }, subtitle = note.content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), onOpen = { if (section == LibrarySection.Library) onOpenNote(note) else onNoteAction(note, ContentAction.Unarchive) }, actions = actionsFor(section), action = { onNoteAction(note, it) }, restoreAction = if (section == LibrarySection.Archive) ({ onNoteAction(note, ContentAction.Unarchive) }) else null)
                }
                items(visibleChats, key = { "chat:${it.id}" }) { session ->
                    LibraryRow(icon = { Icon(Icons.Outlined.ChatBubbleOutline, null) }, title = session.title, subtitle = session.updated, onOpen = { if (section == LibrarySection.Library) onOpenChat(session) else onChatAction(session, ContentAction.Unarchive) }, actions = actionsFor(section), action = { onChatAction(session, it) }, restoreAction = if (section == LibrarySection.Archive) ({ onChatAction(session, ContentAction.Unarchive) }) else null)
                }
                if (visibleFolders.isEmpty() && visibleNotes.isEmpty() && visibleChats.isEmpty()) item {
                    Text("这里还没有内容", color = c.subtle, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(28.dp))
                }
            }
        }
    }
    if (isWide && section == LibrarySection.Library) {
        Row(Modifier.fillMaxSize()) {
            FolderTree(folders = activeFolders, rows = visibleTreeFolders, selectedId = folderId, expandedIds = expandedFolderIds.toSet(), onFolder = onFolder, onToggleFolder = onToggleFolder, onFolderAction = onFolderAction, onNewFolder = { onNewFolder(null) }, modifier = Modifier.width(230.dp).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    } else if (section == LibrarySection.Library) {
        val drawerState = rememberDrawerState(if (treeOpen) DrawerValue.Open else DrawerValue.Closed)
        LaunchedEffect(treeOpen) { if (treeOpen) drawerState.open() else drawerState.close() }
        LaunchedEffect(drawerState.isOpen) { onTreeOpen(drawerState.isOpen) }
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
            ModalDrawerSheet(drawerContainerColor = c.sidebar, modifier = Modifier.fillMaxWidth(.82f).widthIn(max = 320.dp)) {
                FolderTree(folders = activeFolders, rows = visibleTreeFolders, selectedId = folderId, expandedIds = expandedFolderIds.toSet(), onFolder = { onFolder(it); onTreeOpen(false) }, onToggleFolder = onToggleFolder, onFolderAction = onFolderAction, onNewFolder = { parent -> onNewFolder(parent); onTreeOpen(false) }, modifier = Modifier.fillMaxSize())
            }
        }) { content() }
    } else content()
}

@Composable
private fun FolderTree(
    folders: List<FolderEntity>, rows: List<FolderEntity>, selectedId: String?, expandedIds: Set<String>,
    onFolder: (String?) -> Unit, onToggleFolder: (String) -> Unit, onFolderAction: (FolderEntity, ContentAction) -> Unit,
    onNewFolder: (String?) -> Unit, modifier: Modifier = Modifier,
) {
    val c = Dsh.colors
    val children = remember(folders) { folders.groupBy { it.parentId } }
    Column(modifier.background(c.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("项目目录", color = c.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = { onNewFolder(null) }, modifier = Modifier.size(44.dp).semantics { contentDescription = "新建顶层文件夹" }) { Icon(androidx.compose.material.icons.Icons.Outlined.Add, "新建顶层文件夹", tint = c.accent) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp)) {
            item(key = "folder-tree-root") {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (selectedId == null) c.navActive else c.surface).clickable { onFolder(null) }.padding(horizontal = 9.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Folder, null, tint = c.muted, modifier = Modifier.size(18.dp)); Text("根目录", color = c.muted, modifier = Modifier.padding(start = 8.dp), fontSize = 13.sp)
                }
            }
            items(rows, key = { "tree:${it.id}" }) { folder ->
                val depth = generateSequence(folder.parentId) { parent -> folders.firstOrNull { it.id == parent }?.parentId }.count()
                val hasChildren = !children[folder.id].isNullOrEmpty()
                var menu by remember(folder.id) { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().padding(start = (depth * 14).dp).clip(RoundedCornerShape(8.dp)).background(if (selectedId == folder.id) c.navActive else c.surface).semantics { contentDescription = "${folder.name}，层级 ${depth + 1}${if (selectedId == folder.id) "，当前位置" else ""}" }, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (hasChildren) onToggleFolder(folder.id) else onFolder(folder.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(if (folder.id in expandedIds) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight, if (hasChildren) "${folder.name}，${if (folder.id in expandedIds) "折叠" else "展开"}" else "打开 ${folder.name}", tint = c.subtle, modifier = Modifier.size(18.dp))
                    }
                    Text(folder.name, color = if (selectedId == folder.id) c.navActiveText else c.muted, modifier = Modifier.weight(1f).clickable { onFolder(folder.id) }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.MoreHoriz, "${folder.name} 操作", tint = c.subtle) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf(ContentAction.Rename, ContentAction.MoveFolder, ContentAction.Archive, ContentAction.Trash).forEach { action -> DropdownMenuItem(text = { Text(actionLabel(action)) }, onClick = { menu = false; onFolderAction(folder, action) }) }
                            DropdownMenuItem(text = { Text("新建子文件夹") }, onClick = { menu = false; onFolder(folder.id); onNewFolder(folder.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun actionsFor(section: LibrarySection): List<ContentAction> = when (section) {
    LibrarySection.Library -> listOf(ContentAction.Rename, ContentAction.MoveFolder, ContentAction.MoveProject, ContentAction.Archive, ContentAction.Trash)
    LibrarySection.Archive -> listOf(ContentAction.Unarchive, ContentAction.Trash)
    LibrarySection.Trash -> listOf(ContentAction.Restore, ContentAction.DeletePermanently)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(icon: @Composable () -> Unit, title: String, subtitle: String, onOpen: () -> Unit, actions: List<ContentAction>, action: (ContentAction) -> Unit, restoreAction: (() -> Unit)? = null) {
    val c = Dsh.colors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(c.surface)
        .combinedClickable(onClick = onOpen, onLongClick = { menu = true }).padding(start = 14.dp, end = 5.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { CompositionLocalProvider(LocalContentColor provides c.subtle) { icon() } }
        Column(Modifier.weight(1f).padding(start = 9.dp, end = 4.dp)) {
            Text(title, color = c.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, color = c.subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            if (restoreAction != null) {
                IconButton(onClick = restoreAction, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.Restore, "恢复", tint = c.accent)
                }
            }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(44.dp)) { Icon(Icons.Outlined.MoreHoriz, "更多操作", tint = c.subtle) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                actions.forEach { a -> DropdownMenuItem(text = { Text(actionLabel(a)) }, onClick = { menu = false; action(a) }) }
            }
        }
    }
}

private fun actionLabel(action: ContentAction): String = when (action) {
    ContentAction.Rename -> "重命名"; ContentAction.MoveFolder -> "移动到文件夹"; ContentAction.MoveProject -> "移动到项目"
    ContentAction.Archive -> "归档"; ContentAction.Unarchive -> "取消归档"; ContentAction.Trash -> "移到回收站"
    ContentAction.Restore -> "恢复"; ContentAction.DeletePermanently -> "永久删除"
}
