package com.dspro.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.Session

enum class SidebarItem(val label: String) {
    NewChat("新会话"), History("对话列表"), Theme("主题切换"),
}

enum class WorkspaceSidebarItem { Library, Notes, Chats, Archive, Trash, Categories, Settings }

/** Navigation for the unified workspace; callers keep the selected type filter across sections. */
@Composable
fun WorkspaceSidebar(
    selected: WorkspaceSidebarItem,
    visibleItems: Set<WorkspaceSidebarItem> = WorkspaceSidebarItem.entries.toSet(),
    dark: Boolean,
    onSelect: (WorkspaceSidebarItem) -> Unit,
    onToggleDark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Dsh.colors
    Column(modifier.fillMaxHeight().background(c.sidebar).padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(14.dp)); DshWordmark(); Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            listOf(
                WorkspaceSidebarItem.Library to (Icons.Outlined.Folder to "资料库"),
                WorkspaceSidebarItem.Notes to (Icons.Outlined.Description to "仅笔记"),
                WorkspaceSidebarItem.Chats to (Icons.Outlined.ChatBubbleOutline to "仅对话"),
                WorkspaceSidebarItem.Archive to (Icons.Outlined.Archive to "归档"),
                WorkspaceSidebarItem.Trash to (Icons.Outlined.DeleteOutline to "回收站"),
                WorkspaceSidebarItem.Categories to (Icons.Outlined.Tune to "分类管理"),
            ).filter { it.first in visibleItems }.forEach { (entry, pair) ->
                val (icon, label) = pair
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (selected == entry) c.navActive else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(entry) }.padding(horizontal = 10.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, Modifier.size(18.dp), tint = if (selected == entry) c.navActiveText else c.muted)
                    Text(label, color = if (selected == entry) c.navActiveText else c.muted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp)); DshHairline()
        if (WorkspaceSidebarItem.Settings in visibleItems) Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onSelect(WorkspaceSidebarItem.Settings) }.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Settings, null, Modifier.size(17.dp), tint = c.subtle)
            Text("全局设置", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 9.dp))
            IconButton(onClick = onToggleDark, modifier = Modifier.size(44.dp)) {
                Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "切换主题", Modifier.size(17.dp), tint = c.subtle)
            }
        }
    }
}

@Composable
fun SidebarContent(
    sessions: List<Session>,
    activeId: Long,
    dark: Boolean,
    onNewChat: () -> Unit,
    onSelectSession: (Session) -> Unit,
    onTasks: () -> Unit,
    onPlugins: () -> Unit,
    onProjects: () -> Unit,
    onMemory: () -> Unit,
    onArchived: () -> Unit,
    onSettings: () -> Unit,
    visibleItems: Set<SidebarItem>,
    onDeleteSession: (Session) -> Unit,
    onArchiveSession: (Session) -> Unit,
    onMoveSession: (Session) -> Unit,
    onRenameSession: (Session) -> Unit,
    onToggleDark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Dsh.colors
    Column(
        modifier
            .fillMaxHeight()
            .background(c.sidebar)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        DshWordmark()
        Spacer(Modifier.height(18.dp))

        if (SidebarItem.NewChat in visibleItems) Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .clickable { onNewChat() },
            color = c.main,
            shape = RoundedCornerShape(11.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, c.hairlineStrong),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(17.dp), tint = c.muted)
                Text("新会话", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (SidebarItem.History in visibleItems) {
                Spacer(Modifier.height(12.dp))
                DshHairline()
                Text("对话列表", color = c.subtle, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                sessions.forEach { session ->
                    key(session.id) { SessionRow(
                        session = session,
                        active = session.id == activeId,
                        onClick = { onSelectSession(session) },
                        onDelete = { onDeleteSession(session) },
                        onArchive = { onArchiveSession(session) },
                        onMove = { onMoveSession(session) },
                        onRename = { onRenameSession(session) },
                    ) }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        DshHairline()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onSettings).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Settings, null, Modifier.size(16.dp), tint = c.subtle)
            Text("设置", color = c.muted, fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(start = 9.dp))
            if (SidebarItem.Theme in visibleItems) IconButton(onClick = onToggleDark, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    "切换主题",
                    Modifier.size(16.dp),
                    tint = c.subtle,
                )
            }
        }
    }
}

@Composable
private fun SidebarNavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(17.dp), tint = c.muted)
        Text(label, color = c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun SessionRow(session: Session, active: Boolean, onClick: () -> Unit, onDelete: () -> Unit, onArchive: () -> Unit, onMove: () -> Unit, onRename: () -> Unit) {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) c.navActive else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            null,
            Modifier.size(14.dp),
            tint = if (active) c.navActiveText else c.subtle,
        )
        Text(
            session.title,
            color = if (active) c.navActiveText else c.muted,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box {
            var menu by remember { mutableStateOf(false) }
            IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.MoreHoriz, "对话操作：${session.title}", Modifier.size(18.dp), tint = c.muted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("重命名对话") }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text("归档对话") }, onClick = { menu = false; onArchive() })
                DropdownMenuItem(text = { Text("移动对话") }, onClick = { menu = false; onMove() })
                DropdownMenuItem(text = { Text("删除对话") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}
