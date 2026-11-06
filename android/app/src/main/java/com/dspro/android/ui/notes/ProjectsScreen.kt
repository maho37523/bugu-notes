package com.dspro.android.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.notes.ProjectEntity
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshIconButton
import com.dspro.android.ui.SimpleTopBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    activeProjectId: String?,
    onSelect: (ProjectEntity?) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (ProjectEntity) -> Unit,
    onArchive: (ProjectEntity, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val c = Dsh.colors
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<ProjectEntity?>(null) }
    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar("项目", onBack, action = {
            DshIconButton(Icons.Outlined.Add, "新建项目", { showCreate = true }, size = 44.dp)
        })
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ProjectRow("未分类", "未分类中的对话使用全局记忆", activeProjectId == null, { onSelect(null) })
            }
            items(projects.filter { it.archivedAt == null }, key = { it.id }) { project ->
                ProjectRow(project.name, "项目记忆", project.id == activeProjectId, { onSelect(project) }) {
                    target = project
                }
            }
            if (projects.any { it.archivedAt != null }) {
                item {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(if (showArchived) "隐藏已归档项目" else "查看已归档项目")
                    }
                }
                if (showArchived) items(projects.filter { it.archivedAt != null }, key = { it.id }) { project ->
                    ProjectRow(project.name, "已归档", false, { target = project }, { target = project })
                }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建项目") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("项目名称") }) },
            confirmButton = {
                TextButton(onClick = { onCreate(name.trim()); name = ""; showCreate = false }, enabled = name.isNotBlank()) {
                    Text("创建")
                }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } },
        )
    }
    target?.let { project ->
        CollectionActions(
            name = project.name,
            kind = "项目",
            archived = project.archivedAt != null,
            deleteDetail = "项目中的笔记和对话会移到未分类，项目记忆会删除。",
            onArchive = { onArchive(project, project.archivedAt == null) },
            onDelete = { onDelete(project) },
            onDismiss = { target = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectRow(name: String, detail: String, selected: Boolean, onSelect: () -> Unit, onActions: (() -> Unit)? = null) {
    val c = Dsh.colors
    Surface(
        color = if (selected) c.accentSoft else c.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onSelect,
            onLongClickLabel = if (onActions != null) "项目操作" else null,
            onLongClick = onActions,
        ),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(detail, fontSize = 11.sp, color = c.muted)
            }
            if (selected) Icon(Icons.Outlined.Check, "当前范围", Modifier.size(17.dp), tint = c.accent)
            if (onActions != null) DshIconButton(Icons.Outlined.MoreHoriz, "项目操作", onActions, size = 44.dp)
        }
    }
}

@Composable
internal fun CollectionActions(
    name: String,
    kind: String,
    archived: Boolean,
    deleteDetail: String,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (confirmingDelete) "删除${kind}「$name」？" else name) },
        text = { Text(if (confirmingDelete) deleteDetail else if (archived) "已归档$kind" else "${kind}操作") },
        confirmButton = {
            TextButton(onClick = {
                if (confirmingDelete) {
                    onDelete()
                    onDismiss()
                } else confirmingDelete = true
            }) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                if (!confirmingDelete) TextButton(onClick = { onArchive(); onDismiss() }) {
                    Text(if (archived) "取消归档" else "归档")
                }
            }
        },
    )
}
