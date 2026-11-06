package com.dspro.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.Session
import com.dspro.android.data.notes.FolderEntity
import com.dspro.android.data.notes.NoteEntity

@Composable
fun LibraryScreen(
    notes: List<NoteEntity>, sessions: List<Session>, folders: List<FolderEntity>,
    filter: ContentFilter, section: LibrarySection, folderId: String?,
    onFilter: (ContentFilter) -> Unit, onFolder: (String?) -> Unit,
    onOpenNote: (NoteEntity) -> Unit, onOpenChat: (Session) -> Unit,
    onNoteAction: (NoteEntity, ContentAction) -> Unit,
    onChatAction: (Session, ContentAction) -> Unit,
    onFolderAction: (FolderEntity, ContentAction) -> Unit,
) {
    val c = Dsh.colors
    val activeFolderIds = remember(folders) { folders.filter { it.archivedAt == null }.mapTo(mutableSetOf()) { it.id } }
    fun matchesFolder(id: String?): Boolean = when {
        section != LibrarySection.Library -> true
        folderId != null -> id == folderId
        else -> id == null || id !in activeFolderIds
    }
    val visibleNotes = notes.filter { filter != ContentFilter.Chats && matchesFolder(it.folderId) }
    val visibleChats = sessions.filter { filter != ContentFilter.Notes && matchesFolder(it.folderId) }
    val visibleFolders = if (folderId == null && section == LibrarySection.Library) folders.filter { it.archivedAt == null } else emptyList()
    Column(Modifier.fillMaxSize().background(c.main)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ContentFilter.entries.forEach { item ->
                FilterChip(selected = filter == item, onClick = { onFilter(item) }, label = { Text(when (item) { ContentFilter.All -> "全部"; ContentFilter.Notes -> "笔记"; ContentFilter.Chats -> "对话" }) })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            if (folderId != null) item(key = "folder-breadcrumb") {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFolder(null) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Outlined.ArrowBack, "返回资料库", tint = c.muted)
                    }
                    Text(folders.firstOrNull { it.id == folderId }?.name ?: "文件夹内容", color = c.muted, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            items(visibleFolders, key = { "folder:${it.id}" }) { folder ->
                LibraryRow(icon = { Icon(Icons.Outlined.Folder, null) }, title = folder.name, subtitle = "文件夹", onOpen = { onFolder(folder.id) }, actions = listOf(ContentAction.Rename, ContentAction.Trash), action = { onFolderAction(folder, it) })
            }
            items(visibleNotes, key = { "note:${it.id}" }) { note ->
                val actions = actionsFor(section)
                LibraryRow(icon = { Icon(Icons.Outlined.Description, null) }, title = note.title.ifBlank { "无标题笔记" }, subtitle = note.content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(), onOpen = { if (section == LibrarySection.Library) onOpenNote(note) else if (section == LibrarySection.Archive) onNoteAction(note, ContentAction.Unarchive) }, actions = actions, action = { onNoteAction(note, it) }, restoreAction = if (section == LibrarySection.Archive) ({ onNoteAction(note, ContentAction.Unarchive) }) else null)
            }
            items(visibleChats, key = { "chat:${it.id}" }) { session ->
                LibraryRow(icon = { Icon(Icons.Outlined.ChatBubbleOutline, null) }, title = session.title, subtitle = session.updated, onOpen = { if (section == LibrarySection.Library) onOpenChat(session) else if (section == LibrarySection.Archive) onChatAction(session, ContentAction.Unarchive) }, actions = actionsFor(section), action = { onChatAction(session, it) }, restoreAction = if (section == LibrarySection.Archive) ({ onChatAction(session, ContentAction.Unarchive) }) else null)
            }
            if (visibleFolders.isEmpty() && visibleNotes.isEmpty() && visibleChats.isEmpty()) item {
                Text("这里还没有内容", color = c.subtle, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(28.dp))
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
