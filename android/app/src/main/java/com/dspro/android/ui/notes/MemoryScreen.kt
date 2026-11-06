package com.dspro.android.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.notes.MemoryEntity
import com.dspro.android.ui.Dsh
import com.dspro.android.ui.DshIconButton
import com.dspro.android.ui.SimpleTopBar

@Composable
fun MemoryScreen(
    projectName: String?,
    memories: List<MemoryEntity>,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggle: (MemoryEntity, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val c = Dsh.colors
    var showAdd by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar(
            title = "记忆",
            onBack = onBack,
            action = {
                DshIconButton(Icons.Outlined.Add, "添加记忆", { showAdd = true }, size = 32.dp, iconSize = 20.dp)
            },
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text(
                if (projectName == null) "全局记忆" else "项目记忆 · $projectName",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (projectName == null) "启用的全局记忆用于未分类中的对话。" else "启用的记忆仅用于当前项目的对话。",
                color = c.muted,
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (memories.isEmpty()) {
                item {
                    Text("还没有记忆。\n在对话消息的「记住」里添加，或点右上角 +。", color = c.subtle, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 50.dp))
                }
            }
            items(memories, key = { it.id }) { memory ->
                Surface(
                    color = c.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.hairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(memory.content, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                if (memory.enabled) "已启用 · 对话会使用" else "已停用",
                                color = if (memory.enabled) c.accent else c.subtle,
                                fontSize = 9.sp,
                            )
                        }
                        Switch(
                            checked = memory.enabled,
                            onCheckedChange = { onToggle(memory, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = c.accent,
                                uncheckedThumbColor = c.subtle,
                                uncheckedTrackColor = c.chipBg,
                                uncheckedBorderColor = c.hairlineStrong,
                            ),
                        )
                        DshIconButton(Icons.Outlined.DeleteOutline, "删除", { onDelete(memory.id) }, size = 28.dp, iconSize = 15.dp)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = c.surface,
            title = { Text("添加项目记忆", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("例如：这个项目先给结论，再给依据", fontSize = 12.sp, color = c.subtle) },
                        minLines = 3,
                        textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("只对当前项目生效；可在对话里手动添加。", color = c.subtle, fontSize = 9.5.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) onAdd(text.trim())
                    text = ""
                    showAdd = false
                }) { Text("添加", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消", color = c.muted) } },
        )
    }
}
