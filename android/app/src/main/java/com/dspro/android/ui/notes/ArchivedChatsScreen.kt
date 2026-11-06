package com.dspro.android.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.Session
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.SimpleTopBar
import kotlinx.coroutines.launch

@Composable
fun ArchivedChatsScreen(
    sessions: List<Session>,
    projectName: (String?) -> String,
    onRestore: (Session) -> Unit,
    onDelete: (List<Session>) -> Unit,
    onBack: () -> Unit,
) {
    val c = Dsh.colors
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar(title = "已归档会话", onBack = onBack)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (sessions.isEmpty()) {
                item { Text("没有已归档会话", color = c.subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 50.dp)) }
            }
            items(sessions, key = { it.id }) { session ->
                val checked = selected.contains(session.id)
                Surface(
                    color = if (checked) c.accentSoft else c.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (checked) c.accent.copy(alpha = 0.25f) else c.hairline),
                    modifier = Modifier.fillMaxWidth().clickable {
                        selected = if (checked) selected - session.id else selected + session.id
                    },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selected = if (checked) selected - session.id else selected + session.id },
                            colors = CheckboxDefaults.colors(checkedColor = c.accent),
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Text(projectName(session.projectId), color = c.subtle, fontSize = 9.5.sp)
                        }
                        TextButton(onClick = { onRestore(session) }) {
                            Text("恢复", color = c.accent, fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已选 ${selected.size}", color = c.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    sessions.filter { selected.contains(it.id) }.forEach(onRestore)
                    selected = emptySet()
                }) {
                    Icon(Icons.Outlined.Restore, null, Modifier.size(15.dp), tint = c.accent)
                    Text("恢复", color = c.accent, fontSize = 11.sp)
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteForever, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                    Text("永久删除", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            title = { Text("永久删除 ${selected.size} 个会话？", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("聊天记录会从本机删除，不可撤销。", color = c.muted, fontSize = 11.5.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(sessions.filter { selected.contains(it.id) })
                    selected = emptySet()
                    confirmDelete = false
                }) { Text("永久删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消", color = c.muted) } },
        )
    }
}
