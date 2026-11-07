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
import com.dspro.android.data.ChatClient
import com.dspro.android.data.chatgpt.ChatGptAccounts
import com.dspro.android.data.StreamEvent
import com.dspro.android.data.normalizedDestination
import com.dspro.android.data.requestDestinationError
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    config: ApiConfig,
    clientFactory: (ApiConfig) -> ChatClient,
    chatGptAccounts: ChatGptAccounts,
    onSave: (ApiConfig) -> Unit,
    onClear: (String) -> Unit,
    onBack: () -> Unit,
    onProviderChange: (String) -> ApiConfig,
    onMenu: (() -> Unit)? = null,
) {
    val c = Dsh.colors
    val scope = rememberCoroutineScope()
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    var workspaceId by remember(config.workspaceId) { mutableStateOf(config.workspaceId) }
    var provider by remember(config.provider) { mutableStateOf(config.provider) }
    var storageAvailable by remember(config.secureStorageAvailable) { mutableStateOf(config.secureStorageAvailable) }
    var trustDestination by remember(config.trustedBaseUrl) { mutableStateOf(normalizedDestination(config.baseUrl) != null && normalizedDestination(config.baseUrl) == normalizedDestination(config.trustedBaseUrl)) }
    var providerMenu by remember { mutableStateOf(false) }
    var webSearch by remember(config.webSearchEnabled) { mutableStateOf(config.webSearchEnabled) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testJob by remember { mutableStateOf<Job?>(null) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    if (provider == "chatgpt") {
        ChatGptSettingsScreen(chatGptAccounts, config, onSave, onBack, onApiKey = {
            val selected = onProviderChange("deepseek")
            provider = "deepseek"; apiKey = selected.apiKey; baseUrl = selected.baseUrl
            model = selected.model; workspaceId = selected.workspaceId
            webSearch = selected.webSearchEnabled; storageAvailable = selected.secureStorageAvailable
            trustDestination = normalizedDestination(selected.baseUrl) != null && normalizedDestination(selected.baseUrl) == normalizedDestination(selected.trustedBaseUrl)
        })
        return
    }

    fun draft() = ApiConfig(
        apiKey = apiKey.trim(),
        baseUrl = baseUrl.trim(),
        model = model,
        effort = config.effort,
        provider = provider,
        webSearchEnabled = webSearch,
        workspaceId = workspaceId.trim(),
        secureStorageAvailable = storageAvailable,
        trustedBaseUrl = if (trustDestination) baseUrl.trim() else "",
    )

    fun test() {
        val cfg = draft()
        if (!cfg.secureStorageAvailable || !onProviderChange(cfg.provider).secureStorageAvailable) {
            testResult = false to "安全存储不可用，请检查设备后重新配置"
            return
        }
        if (cfg.apiKey.isBlank()) {
            testResult = false to "请先填写 API Key"
            return
        }
        requestDestinationError(cfg)?.let {
            testResult = false to it
            return
        }
        testing = true
        testResult = null
        testJob = scope.launch {
            testResult = try {
                withTimeout(25_000) {
                    var terminal: Pair<Boolean, String>? = null
                    var sawText = false
                    clientFactory(cfg).stream(listOf(ChatTurn("user", "只回复 ok"))).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> if (event.text.isNotBlank()) sawText = true
                            is StreamEvent.Failure -> if (terminal == null) terminal = false to event.message
                            StreamEvent.Done -> if (terminal == null) terminal = if (sawText) true to "连接成功，模型可用" else false to "服务未返回正文"
                            else -> Unit
                        }
                    }
                    terminal ?: (false to "连接未完整结束")
                }
            } catch (e: TimeoutCancellationException) {
                false to "连接超时"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false to "连接失败或超时"
            } finally {
                testing = false
            }
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
                    "各服务商的 Key 分开保存在本机。连接测试会发送一条固定短消息，并可能产生 API 费用。",
                    color = c.muted,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                )
                if (!storageAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Text("安全存储不可用，无法读取、保存或发送 API Key；请检查设备后重新配置。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(18.dp))

                ApiSection("连接")
                ExposedDropdownMenuBox(expanded = providerMenu, onExpandedChange = { providerMenu = it }) {
                    OutlinedTextField(
                        value = when (provider) { "openai" -> "OpenAI"; "anthropic" -> "Claude"; "aliyun" -> "阿里云百炼"; "custom" -> "OpenAI 兼容自定义"; else -> "DeepSeek" },
                        onValueChange = {}, readOnly = true, label = { Text("服务商") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        listOf("deepseek" to "DeepSeek", "openai" to "OpenAI API Key", "chatgpt" to "ChatGPT 订阅登录", "anthropic" to "Claude", "aliyun" to "阿里云百炼", "custom" to "OpenAI 兼容自定义").forEach { (id, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                providerMenu = false
                                if (provider != id) {
                                    testJob?.cancel()
                                    testing = false
                                    testResult = null
                                    provider = id
                                    val selected = onProviderChange(id)
                                    storageAvailable = selected.secureStorageAvailable
                                    trustDestination = normalizedDestination(selected.baseUrl) != null && normalizedDestination(selected.baseUrl) == normalizedDestination(selected.trustedBaseUrl)
                                    apiKey = selected.apiKey
                                    baseUrl = selected.baseUrl
                                    model = selected.model
                                    workspaceId = selected.workspaceId
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
                            onValueChange = { baseUrl = it; trustDestination = false },
                            singleLine = true,
                            placeholder = { Text("https://api.deepseek.com", fontSize = 12.sp, color = c.subtle) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("请求内容和 Key 会发送到此地址；非官方地址需要确认。", color = c.subtle, fontSize = 9.5.sp)
                        if (requestDestinationError(draft()) != null || trustDestination) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = trustDestination, onCheckedChange = { trustDestination = it })
                                Text("我确认将此连接的 Key 和对话发送到该地址", color = c.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (provider == "anthropic") {
                    OutlinedTextField(workspaceId, { workspaceId = it }, label = { Text("Workspace ID（按需填写）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
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
                        enabled = storageAvailable && apiKey.isNotBlank() && requestDestinationError(draft()) == null && model.isNotBlank(),
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
                if (apiKey.isNotBlank() && storageAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "清除当前 API Key",
                        color = Color(0xFFD92D20),
                        fontSize = 10.5.sp,
                        modifier = Modifier
                            .clickable { testJob?.cancel(); testing = false; onClear(provider); apiKey = "" }
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
