package com.dspro.android.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dspro.android.data.notes.NoteEntity
import com.dspro.android.data.notes.NotesRepository
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshIconButton
import com.dspro.android.ui.SimpleTopBar
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(
    repository: NotesRepository,
    onBack: () -> Unit,
) {
    val c = Dsh.colors
    val scope = rememberCoroutineScope()
    val notes by repository.observeTrash().collectAsStateWithLifecycle(initialValue = emptyList())
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar(
            title = "垃圾桶",
            onBack = onBack,
            action = {
                if (notes.isNotEmpty()) {
                    Text(
                        "清空",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { confirmEmpty = true }.padding(8.dp),
                    )
                }
            },
        )
        Text(
            "笔记删除后会在这里保留 30 天（当前版本暂不自动清理）。",
            color = c.muted,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (notes.isEmpty()) {
                item {
                    Text("垃圾桶是空的", color = c.subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 50.dp))
                }
            }
            items(notes, key = { it.id }) { note ->
                val checked = selected.contains(note.id)
                Surface(
                    color = if (checked) c.accentSoft else c.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (checked) c.accent.copy(alpha = 0.25f) else c.hairline),
                    modifier = Modifier.fillMaxWidth().clickable {
                        selected = if (checked) selected - note.id else selected + note.id
                    },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selected = if (checked) selected - note.id else selected + note.id
                            },
                            colors = CheckboxDefaults.colors(checkedColor = c.accent),
                        )
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title.ifBlank { "无标题笔记" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(note.content.take(60).replace("\n", " "), color = c.subtle, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().background(c.surface).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已选 ${selected.size}", color = c.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        selected.forEach { repository.restoreNote(it) }
                        selected = emptySet()
                    }
                }) {
                    Icon(Icons.Outlined.Restore, null, Modifier.size(15.dp), tint = c.accent)
                    Spacer(Modifier.width(4.dp))
                    Text("恢复", color = c.accent, fontSize = 11.sp)
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteForever, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("永久删除", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "永久删除 ${selected.size} 篇笔记？",
            body = "这个操作不可撤销，笔记内容和版本历史都会被删除。",
            confirmLabel = "永久删除",
            onConfirm = {
                scope.launch {
                    selected.forEach { repository.permanentlyDelete(it) }
                    selected = emptySet()
                    confirmDelete = false
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (confirmEmpty) {
        ConfirmDialog(
            title = "清空垃圾桶？",
            body = "所有笔记和版本历史都会被永久删除，不可撤销。",
            confirmLabel = "清空",
            onConfirm = {
                scope.launch {
                    notes.forEach { repository.permanentlyDelete(it.id) }
                    confirmEmpty = false
                }
            },
            onDismiss = { confirmEmpty = false },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Dsh.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = { Text(body, color = c.muted, fontSize = 11.5.sp, lineHeight = 17.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = c.muted) } },
    )
}
