package com.dspro.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.notes.FolderEntity
import com.dspro.android.data.notes.TagEntity
import com.dspro.android.data.notes.WorkspaceSearchResult

@Composable
fun WorkspaceSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<WorkspaceSearchResult>,
    folders: List<FolderEntity>,
    tags: List<TagEntity>,
    selectedFolder: String?,
    selectedTag: String?,
    selectedType: String?,
    scopeName: String,
    onFolder: (String?) -> Unit,
    onTag: (String?) -> Unit,
    onType: (String?) -> Unit,
    onOpen: (WorkspaceSearchResult) -> Unit,
    onBack: () -> Unit,
) {
    val c = Dsh.colors
    val visibleResults = results.filter {
        (selectedType == null || (selectedType == "note" && it.kind == "note") || (selectedType == "chat" && it.kind != "note")) &&
            (selectedTag == null || it.kind == "note")
    }
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("搜索", onBack = onBack)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("搜索当前范围的笔记和对话") },
            shape = RoundedCornerShape(12.dp),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            listOf(null to "全部", "note" to "笔记", "chat" to "对话").forEach { (key, label) ->
                FilterChip(selected = selectedType == key, onClick = { onType(key) }, label = { Text(label) }, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
            TextButton(onClick = { onFolder(null) }) { Text(if (selectedFolder == null) "✓ 全部文件夹" else "全部文件夹") }
            folders.filter { it.archivedAt == null }.forEach { folder -> TextButton(onClick = { onFolder(if (selectedFolder == folder.id) null else folder.id) }) { Text(if (selectedFolder == folder.id) "✓ ${folder.name}" else folder.name) } }
        }
        if (selectedType != "chat") {
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                TextButton(onClick = { onTag(null) }) { Text(if (selectedTag == null) "✓ 全部标签" else "全部标签") }
                tags.forEach { tag -> TextButton(onClick = { onTag(if (selectedTag == tag.id) null else tag.id) }) { Text(if (selectedTag == tag.id) "✓ ${tag.name}" else tag.name) } }
            }
        }
        Text("范围：$scopeName", color = c.subtle, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("输入关键词，搜索笔记、对话标题和消息", color = c.muted, fontSize = 12.sp) }
        } else if (visibleResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("当前范围没有匹配结果", color = c.muted, fontSize = 12.sp) }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(visibleResults, key = { "${it.kind}:${it.noteId}:${it.sessionId}:${it.messageId}" }) { result ->
                    Surface(color = c.surface, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, c.hairline), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpen(result) }) {
                        Column(Modifier.padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(when (result.kind) { "note" -> "笔记"; "message" -> "消息"; else -> "对话" }, color = c.accent, fontSize = 10.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(result.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(result.snippet, color = c.muted, fontSize = 11.sp, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}
