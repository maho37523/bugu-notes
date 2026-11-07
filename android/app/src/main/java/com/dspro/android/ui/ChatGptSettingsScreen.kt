package com.dspro.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.dspro.android.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dspro.android.data.ApiConfig
import com.dspro.android.data.chatgpt.CHATGPT_RESOURCE
import com.dspro.android.data.chatgpt.CHATGPT_USAGE_URL
import com.dspro.android.data.chatgpt.ChatGptAccounts
import com.dspro.android.data.chatgpt.ChatGptProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ChatGptSettingsScreen(
    accounts: ChatGptAccounts, current: ApiConfig, onUse: (ApiConfig) -> Unit,
    onBack: () -> Unit, onApiKey: () -> Unit,
) {
    val state by accounts.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(current.chatGptAccountId.ifBlank { state.accounts.firstOrNull()?.id.orEmpty() }) }
    var model by remember { mutableStateOf(if (current.provider == "chatgpt") current.model else "") }
    var modelMenu by remember { mutableStateOf(false) }
    var operating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val account = state.accounts.find { it.id == selected }
    val initialLoginRevision = remember { state.loginRevision }
    val busy = operating || state.busy

    fun openBrowser(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { throw ChatGptProblem("未找到可用系统浏览器，请先启用浏览器后重试") }
    }
    fun runOperation(action: suspend () -> Unit) {
        scope.launch {
            operating = true; message = ""
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: ChatGptProblem) { message = e.message.orEmpty() }
            catch (_: Exception) { message = "ChatGPT 操作未完成，请检查网络后重试" }
            finally { operating = false }
        }
    }
    LaunchedEffect(state.loginRevision) {
        if (state.loginRevision != initialLoginRevision) state.lastSignedInId?.let { selected = it }
    }
    LaunchedEffect(selected, account?.models) {
        if (account?.models?.none { it.slug == model } != false) model = account?.models?.firstOrNull()?.slug.orEmpty()
    }
    DisposableEffect(accounts) { onDispose { accounts.cancelSignIn() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).statusBarsPadding().navigationBarsPadding()) {
        TextButton(onClick = onBack) { Text("返回设置") }
        Text("ChatGPT 订阅连接", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("在系统浏览器登录并授权，用现有 ChatGPT 套餐完成符合条件的请求。可用模型和限额以账号实际权限为准。", fontSize = 13.sp)
        Text("此连接与 OpenAI API Key 分开保存；达到限额时不会自动转用付费 API。首版支持文本、笔记和文本附件。", fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (!state.secureStorageAvailable || !current.secureStorageAvailable) {
            Text("安全存储不可用，无法登录、保存或发送授权凭据。", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { runOperation { accounts.signIn(openBrowser = { openBrowser(it) }) } },
            enabled = !busy && state.secureStorageAvailable && current.secureStorageAvailable,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_chatgpt_sign_in), contentDescription = null,
                modifier = Modifier.size(21.dp), tint = Color.Unspecified)
            Spacer(Modifier.width(8.dp))
            Text("Continue with ChatGPT")
        }
        if (state.busy) TextButton(onClick = accounts::cancelSignIn) { Text("取消登录") }
        val status = message.ifBlank { state.message }
        if (status.isNotBlank()) Text(status, modifier = Modifier.padding(vertical = 10.dp), fontSize = 12.sp)

        state.accounts.forEach { item ->
            OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(enabled = !busy) { selected = item.id }) {
                Row(Modifier.fillMaxWidth().padding(10.dp)) {
                    RadioButton(selected = selected == item.id, onClick = { selected = item.id }, enabled = !busy)
                    Column(Modifier.weight(1f)) {
                        Text(item.label, fontWeight = FontWeight.Medium)
                        Text(item.email.ifBlank { "已验证 ChatGPT 账号" }, fontSize = 12.sp)
                        Text(when {
                            item.pendingSignOut -> "上次退出未完成，请完成退出"
                            !item.signedIn -> "已退出，可重新登录"
                            item.planEnabled -> "已授权使用 ChatGPT 订阅"
                            else -> "已登录，尚未授权使用订阅"
                        }, fontSize = 12.sp)
                    }
                }
            }
        }
        if (account != null) {
            Spacer(Modifier.height(10.dp))
            if (account.signedIn && account.planEnabled) {
                Box {
                    OutlinedButton(onClick = { modelMenu = true }, enabled = !busy && account.models.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text(account.models.find { it.slug == model }?.displayName ?: "请刷新并选择模型")
                    }
                    DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                        account.models.forEach { item -> DropdownMenuItem(text = { Text(item.displayName) }, onClick = { model = item.slug; modelMenu = false }) }
                    }
                }
                TextButton(onClick = { runOperation { accounts.refreshModels(account.id) } }, enabled = !busy) { Text("刷新此账号的模型") }
                TextButton(onClick = { runOperation { accounts.signIn(account.id, openBrowser = { openBrowser(it) }) } }, enabled = !busy) { Text("重新登录此账号") }
            } else if (!account.pendingSignOut) {
                OutlinedButton(onClick = { runOperation { accounts.signIn(account.id, requestConsent = !account.planEnabled, openBrowser = { openBrowser(it) }) } }, enabled = !busy && state.secureStorageAvailable) {
                    Text(if (account.signedIn) "授权使用订阅" else "重新登录此账号")
                }
            }
            if (account.signedIn || account.pendingSignOut) {
                TextButton(onClick = { runOperation { accounts.signOut(account.id) } }, enabled = !busy) { Text(if (account.pendingSignOut) "完成退出" else "退出此账号") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (account != null) onUse(accounts.configuration(current.copy(provider = "chatgpt", apiKey = "", baseUrl = CHATGPT_RESOURCE,
                model = model, webSearchEnabled = false, trustedBaseUrl = "", chatGptAccountId = account.id)))
        }, enabled = !busy && account?.planEnabled == true && account.models.any { it.slug == model } &&
            state.secureStorageAvailable && current.secureStorageAvailable, modifier = Modifier.fillMaxWidth()) { Text("使用此订阅连接") }
        TextButton(onClick = { runCatching { openBrowser(CHATGPT_USAGE_URL) }.onFailure { message = "无法打开用量管理，请启用系统浏览器" } }) { Text("管理 ChatGPT 用量") }
        TextButton(onClick = onApiKey, enabled = !busy) { Text("切换到 API Key 接入设置") }
    }
    if (account?.needsWelcome == true && !busy && state.secureStorageAvailable && current.secureStorageAvailable) AlertDialog(
        onDismissRequest = { runOperation { accounts.acknowledgeWelcome(account.id) } },
        title = { Text("你已授权使用 ChatGPT 订阅") },
        text = { Text("选择此连接后，符合条件的 AI 请求将使用你的 ChatGPT 套餐。额度与其他使用共享，可在 ChatGPT 设置中管理本应用的限制。") },
        confirmButton = { TextButton(onClick = { runOperation { accounts.acknowledgeWelcome(account.id) } }) { Text("我知道了") } },
    )
}
