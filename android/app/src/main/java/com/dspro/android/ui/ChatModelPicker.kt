package com.dspro.android.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dspro.android.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Contains display metadata only. Credentials stay in the existing config/account stores. */
@Composable
fun ChatModelPicker(
    connections: List<ComposerConnection>, currentProvider: String,
    onDismiss: () -> Unit, onConfigure: () -> Unit,
    onRefreshModels: suspend () -> List<ComposerConnection>,
    onApply: (String, String, ThinkingEffort) -> Unit,
) {
    var choices by remember { mutableStateOf(connections) }
    var refreshing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf(currentProvider) }
    val connection = choices.find { it.provider == provider }
    var model by remember(provider) { mutableStateOf(connection?.model.orEmpty()) }
    val selected = connection?.models?.find { it.id == model }
    var effort by remember(provider, model, selected) { mutableStateOf(selected?.effort ?: ThinkingEffort.Auto) }
    var modelMenu by remember { mutableStateOf(false) }
    var manual by remember(provider) { mutableStateOf(false) }
    val levels = selected?.supportedEfforts ?: supportedThinkingEfforts(provider, model)
    val validModel = model.isNotBlank() && model.length <= 200 && model.none { it.isWhitespace() || it.isISOControl() } &&
        (provider != "chatgpt" || selected != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模型与思考强度") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("连接")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.forEach { item ->
                        FilterChip(selected = provider == item.provider, onClick = { provider = item.provider; modelMenu = false }, enabled = !refreshing, label = { Text(item.label) })
                    }
                }
                if (connections.isEmpty()) Text("请先配置一个模型连接")
                Text("模型")
                Box {
                    OutlinedButton(onClick = { modelMenu = true }, enabled = !refreshing && connection?.models?.isNotEmpty() == true, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.label ?: model.ifBlank { "暂无可用模型" })
                    }
                    DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                        connection?.models?.forEach { item ->
                            DropdownMenuItem(text = { Text(if (item.id == model) "✓  ${item.label}" else item.label) }, onClick = { model = item.id; modelMenu = false; manual = false })
                        }
                    }
                }
                if (provider == "chatgpt") {
                    TextButton(onClick = {
                        scope.launch {
                            refreshing = true; status = ""
                            try { choices = onRefreshModels(); status = "模型列表已刷新" }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { status = "刷新未完成，请检查连接或重新登录" }
                            finally { refreshing = false }
                        }
                    }, enabled = !refreshing) { Text(if (refreshing) "正在刷新模型…" else "刷新可用模型") }
                    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                }
                if (provider != "chatgpt") {
                    TextButton(onClick = { manual = !manual }) { Text(if (manual) "收起模型 ID" else "输入其他模型 ID") }
                    if (manual) OutlinedTextField(value = model, onValueChange = { if (it.length <= 200) model = it }, label = { Text("模型 ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("列表为常用模型，是否可用以此连接的权限为准。", style = MaterialTheme.typography.bodySmall)
                }
                Text("思考强度")
                Column {
                    levels.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { level -> FilterChip(selected = effort == level, onClick = { effort = level }, label = { Text(level.label) }) }
                        }
                    }
                }
                if (levels.size == 1) Text("此模型未声明可调强度，将使用模型默认设置。", style = MaterialTheme.typography.bodySmall)
                Text("选择仅影响后续发送的消息。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onConfigure) { Text("管理连接") }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(provider, model, effort) }, enabled = !refreshing && connection != null && validModel) { Text("使用此模型") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
