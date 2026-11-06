package com.dspro.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dspro.android.data.ApiConfig
import com.dspro.android.data.ChatTurn
import com.dspro.android.data.DeepSeekClient
import com.dspro.android.data.StreamEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    config: ApiConfig,
    onSave: (ApiConfig) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onProviderChange: (String) -> ApiConfig,
    onMenu: (() -> Unit)? = null,
) {
    val c = Dsh.colors
    val scope = rememberCoroutineScope()
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    var provider by remember(config.provider) { mutableStateOf(config.provider) }
    var providerMenu by remember { mutableStateOf(false) }
    var webSearch by remember(config.webSearchEnabled) { mutableStateOf(config.webSearchEnabled) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun draft() = ApiConfig(
        apiKey = apiKey.trim(),
        baseUrl = baseUrl.trim(),
        model = model,
        effort = config.effort,
        provider = provider,
        webSearchEnabled = webSearch,
    )

    fun test() {
        val cfg = draft()
        if (cfg.apiKey.isBlank()) {
            testResult = false to "请先填写 API Key"
            return
        }
        testing = true
        testResult = null
        scope.launch {
            testResult = try {
                withTimeout(25_000) {
                    val event = DeepSeekClient(cfg)
                        .stream(listOf(ChatTurn("user", "只回复 ok")))
                        .first { it is StreamEvent.Content || it is StreamEvent.Failure || it is StreamEvent.Done }
                    when (event) {
                        is StreamEvent.Failure -> false to event.message
                        else -> true to "连接成功，模型可用"
                    }
                }
            } catch (e: Exception) {
                false to (e.message ?: "连接超时")
            }
            testing = false
        }
    }

    Column(Modifier.fillMaxSize().background(c.main)) {
        SimpleTopBar("API 设置", onBack = onBack, onMenu = onMenu)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 780.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    "接入真实模型",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "各服务商的 Key 分开保存在本机；请求会发往当前配置的 Base URL。",
                    color = c.muted,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(18.dp))

                ApiSection("连接")
                ExposedDropdownMenuBox(expanded = providerMenu, onExpandedChange = { providerMenu = it }) {
                    OutlinedTextField(
                        value = when (provider) { "openai" -> "OpenAI"; "aliyun" -> "阿里云百炼"; "custom" -> "OpenAI 兼容自定义"; else -> "DeepSeek" },
                        onValueChange = {}, readOnly = true, label = { Text("服务商") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        listOf("deepseek" to "DeepSeek", "openai" to "OpenAI", "aliyun" to "阿里云百炼", "custom" to "OpenAI 兼容自定义").forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                providerMenu = false
                                if (provider != id) {
                                    provider = id
                                    val selected = onProviderChange(id)
                                    apiKey = selected.apiKey
                                    baseUrl = selected.baseUrl
                                    model = selected.model
                                    webSearch = selected.webSearchEnabled
                                }
                            })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = c.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.hairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Key, null, Modifier.size(17.dp), tint = c.muted)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("API Key", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                Text("sk-… · 仅保存在本机加密存储", color = c.subtle, fontSize = 9.5.sp)
                            }
                            if (config.isConfigured) {
                                Text("已配置", color = c.accent, fontSize = 9.5.sp)
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = { Text("sk-...", fontSize = 12.sp, color = c.subtle) },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }, modifier = Modifier.size(34.dp)) {
                                    Icon(
                                        if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        "显示或隐藏 Key",
                                        Modifier.size(17.dp),
                                        tint = c.subtle,
                                    )
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Link, null, Modifier.size(17.dp), tint = c.muted)
                            Spacer(Modifier.width(9.dp))
                            Text("Base URL", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            singleLine = true,
                            placeholder = { Text("https://api.deepseek.com", fontSize = 12.sp, color = c.subtle) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("兼容 OpenAI 格式的代理地址也可以填在这里。", color = c.subtle, fontSize = 9.5.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (provider == "deepseek") {
                    Surface(color = c.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, c.hairline)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("联网搜索", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                Text("调用 DeepSeek Search API，可能产生额外费用", color = c.subtle, fontSize = 9.5.sp)
                            }
                            Switch(checked = webSearch, onCheckedChange = { webSearch = it })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                ApiSection("模型")
                Surface(
                    color = c.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.hairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (provider == "deepseek") ModelChoice(
                            title = "DeepSeek-V4.1-Flash",
                            subtitle = "快，支持视觉；适合日常对话",
                            selected = model == "deepseek-flash",
                            onClick = { model = "deepseek-flash" },
                        )
                        Spacer(Modifier.height(6.dp))
                        if (provider == "deepseek") ModelChoice(
                            title = "DeepSeek-V4-Pro",
                            subtitle = "更强推理，响应更慢，不支持视觉",
                            selected = model == "deepseek-v4-pro",
                            onClick = { model = "deepseek-v4-pro" },
                        ) else OutlinedTextField(model, { model = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    }
                }

                testResult?.let { (ok, message) ->
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = if (ok) c.accentSoft else Color(0xFFFFF0EC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (ok) c.accent.copy(alpha = 0.25f) else Color(0x33D92D20)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                null,
                                Modifier.size(16.dp),
                                tint = if (ok) c.accent else Color(0xFFD92D20),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(message, color = if (ok) c.accent else Color(0xFFB42318), fontSize = 10.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSave(draft()) },
                        enabled = apiKey.isNotBlank() && baseUrl.trim().startsWith("https://") && model.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Text("保存配置", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { test() },
                        enabled = !testing,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, c.hairlineStrong),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Text(if (testing) "测试中…" else "测试连接", color = c.muted, fontSize = 12.sp)
                    }
                }
                if (config.isConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "清除当前 API Key",
                        color = Color(0xFFD92D20),
                        fontSize = 10.5.sp,
                        modifier = Modifier
                            .clickable { onClear() }
                            .padding(vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "提示：API Key 通过 Android Keystore 加密后保存在本机；请求只会发送到你填写的 Base URL。",
                    color = c.subtle,
                    fontSize = 9.5.sp,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ApiSection(text: String) {
    Text(
        text.uppercase(),
        color = Dsh.colors.subtle,
        fontSize = 9.sp,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(start = 3.dp, bottom = 6.dp),
    )
}

@Composable
private fun ModelChoice(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val c = Dsh.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = c.muted, fontSize = 9.5.sp)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.subtle),
        )
    }
}
