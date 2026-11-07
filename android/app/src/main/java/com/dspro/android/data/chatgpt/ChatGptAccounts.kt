package com.dspro.android.data.chatgpt

import com.dspro.android.data.ApiConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class ChatGptModel(val slug: String, val displayName: String)
data class ChatGptAccountSummary(
    val id: String, val label: String, val email: String, val signedIn: Boolean,
    val planEnabled: Boolean, val models: List<ChatGptModel>, val needsWelcome: Boolean,
    val pendingSignOut: Boolean,
)
data class ChatGptState(
    val accounts: List<ChatGptAccountSummary> = emptyList(),
    val secureStorageAvailable: Boolean = true, val busy: Boolean = false,
    val message: String = "", val lastSignedInId: String? = null, val loginRevision: Long = 0,
)

internal data class ChatGptCredential(
    val id: String, val label: String, val clientId: String, val subject: String, val email: String,
    val idToken: String = "", val accessToken: String = "", val refreshToken: String = "",
    val scopes: Set<String> = emptySet(), val expiresAt: Long = 0, val earliestRefreshAt: Long = 0,
    val models: List<ChatGptModel> = emptyList(), val welcomed: Boolean = false, val revoking: Boolean = false,
) {
    val signedIn get() = !revoking && (idToken.isNotEmpty() || accessToken.isNotEmpty())
    val planEnabled get() = signedIn && accessToken.isNotEmpty() && CHATGPT_PLAN_SCOPE in scopes
    override fun toString() = "ChatGptCredential([redacted])"
}
internal data class ChatGptSnapshot(val hostId: String = "", val accounts: List<ChatGptCredential> = emptyList()) {
    override fun toString() = "ChatGptSnapshot([redacted])"
}
internal interface ChatGptStore {
    fun load(): ChatGptSnapshot
    fun save(snapshot: ChatGptSnapshot)
}

/** No UI, Android identity, or Codex credential access. One mutex owns rotating-token persistence. */
class ChatGptAccounts internal constructor(
    private val store: ChatGptStore,
    internal val http: ChatGptHttp = ChatGptHttp(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow(ChatGptState())
    val state: StateFlow<ChatGptState> = mutableState.asStateFlow()
    private val mutex = Mutex()
    private val loginMutex = Mutex()
    private val pending = AtomicReference<OAuthAttempt?>(null)
    private val loginJob = AtomicReference<Job?>(null)
    private val closing = ConcurrentHashMap.newKeySet<String>()
    private val disconnectors = mutableMapOf<String, MutableSet<() -> Unit>>()
    @Volatile private var disabled = false
    @Volatile private var snapshot = try { store.load() } catch (_: Exception) {
        disabled = true
        mutableState.value = ChatGptState(secureStorageAvailable = false, message = "ChatGPT 安全存储不可用，连接已禁用")
        ChatGptSnapshot()
    }
    init { publish() }

    private fun publish(message: String? = null) {
        val accounts = snapshot.accounts.map { a -> ChatGptAccountSummary(a.id, a.label, a.email,
            a.signedIn, a.planEnabled && !disabled, a.models, a.planEnabled && !a.welcomed, a.revoking) }
        mutableState.update { it.copy(accounts = accounts, secureStorageAvailable = !disabled, message = message ?: it.message) }
    }

    private fun requireStore() { if (disabled) throw ChatGptProblem("ChatGPT 安全存储不可用，无法登录或发送请求") }
    private suspend fun persist(next: ChatGptSnapshot) {
        requireStore()
        withContext(NonCancellable + Dispatchers.IO) {
            try { store.save(next) }
            catch (_: Exception) {
                disabled = true
                publish("ChatGPT 安全存储更新失败，连接已禁用；如正在退出，请同时在 ChatGPT 设置中断开应用")
                throw ChatGptProblem(state.value.message)
            }
            snapshot = next
            publish()
        }
    }
    private suspend fun replace(account: ChatGptCredential) {
        val next = snapshot.accounts.filterNot { it.id == account.id } + account
        persist(snapshot.copy(accounts = next))
    }

    fun configuration(config: ApiConfig): ApiConfig {
        if (config.provider != "chatgpt") return config
        val account = snapshot.accounts.find { it.id == config.chatGptAccountId }
        return config.copy(apiKey = "", baseUrl = CHATGPT_RESOURCE, trustedBaseUrl = "", webSearchEnabled = false,
            secureStorageAvailable = config.secureStorageAvailable && !disabled,
            chatGptPlanEnabled = account?.planEnabled == true && account.id !in closing,
            chatGptAccountLabel = account?.let { "${it.label} · ${it.email.ifBlank { "ChatGPT" }}" }.orEmpty(),
            chatGptModelLabel = account?.models?.find { it.slug == config.model }?.displayName.orEmpty())
    }

    fun cancelSignIn() { pending.getAndSet(null)?.close(); loginJob.get()?.cancel() }

    private suspend fun discovery(): JSONObject {
        val json = http.request("$CHATGPT_ISSUER/.well-known/openid-configuration")
        if (json.opt("issuer") != CHATGPT_ISSUER) throw ChatGptProblem("无法核验 ChatGPT 授权服务")
        for (key in listOf("authorization_endpoint", "token_endpoint", "jwks_uri", "revocation_endpoint")) {
            val value = json.opt(key) as? String ?: throw ChatGptProblem("ChatGPT 授权服务配置不完整")
            try { requireOfficialChatGptUrl(value, "auth.openai.com") }
            catch (_: Exception) { throw ChatGptProblem("ChatGPT 授权服务地址不符合要求") }
        }
        // The documented OSS authorize endpoint is fixed; do not replace it from untrusted metadata.
        if (json.getString("authorization_endpoint") != "$CHATGPT_ISSUER/api/accounts/authorize") throw ChatGptProblem("ChatGPT 登录入口已变更，请更新应用")
        return json
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun signIn(accountId: String? = null, requestConsent: Boolean = false, openBrowser: suspend (String) -> Unit) {
        if (!loginMutex.tryLock()) throw ChatGptProblem("已有登录正在进行，请完成或取消后重试")
        val job = currentCoroutineContext().job
        loginJob.set(job)
        mutableState.update { it.copy(busy = true, message = "正在准备 ChatGPT 登录") }
        try {
            val initial = mutex.withLock {
                requireStore()
                if (snapshot.hostId.isEmpty()) persist(snapshot.copy(hostId = "urn:uuid:${UUID.randomUUID()}"))
                val selected = accountId?.let { id -> snapshot.accounts.find { it.id == id } ?: throw ChatGptProblem("请选择有效账号") }
                if (selected?.revoking == true) throw ChatGptProblem("上次退出尚未完成，请先完成退出")
                snapshot.hostId to selected
            }
            val metadata = discovery()
            val expected = initial.second
            var clientId = expected?.clientId
            // Android may record external Intent URLs. Omit optional ID-token hints;
            // the supported account-selector flow still verifies the selected identity.
            var hint: String? = null
            for (retry in 0..1) {
                val attempt = withContext(Dispatchers.IO) { OAuthAttempt(initial.first, clientId, hint, requestConsent, clock) }
                pending.set(attempt)
                val cancelHook = job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { if (it is CancellationException) attempt.close() }
                var returnedClient: String? = null
                try {
                    mutableState.update { it.copy(message = "请在系统浏览器完成登录，完成后返回布谷笔记") }
                    openBrowser(attempt.authorizationUrl())
                    val code = withContext(Dispatchers.IO) { attempt.awaitCallback() }
                    returnedClient = code.clientId
                    val tokens = http.request(metadata.getString("token_endpoint"), linkedMapOf(
                        "grant_type" to "authorization_code", "client_id" to code.clientId,
                        "code" to code.code, "code_verifier" to attempt.verifier,
                        "redirect_uri" to attempt.redirectUri, "resource" to CHATGPT_RESOURCE))
                    val idToken = tokens.opt("id_token") as? String ?: throw ChatGptProblem("授权结果缺少可验证的身份")
                    val identity = try {
                        verifyChatGptIdToken(idToken, http.request(metadata.getString("jwks_uri")), code.clientId, attempt.nonce, clock() / 1000)
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { throw ChatGptProblem("无法验证 ChatGPT 身份，原有账号保持不变") }
                    if (expected != null && (expected.clientId != code.clientId || expected.subject != identity.subject)) throw ChatGptProblem("返回账号与所选账号不同，原有账号保持不变")
                    val id = MessageDigest.getInstance("SHA-256").digest("$CHATGPT_ISSUER\u0000${code.clientId}\u0000${identity.subject}".toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    val credentials = mutex.withLock {
                        currentCoroutineContext().ensureActive()
                        requireStore()
                        val existing = snapshot.accounts.find { it.id == id }
                        val base = ChatGptCredential(id, existing?.label ?: "账号 ${snapshot.accounts.size + 1}", code.clientId,
                            identity.subject, identity.email, idToken = idToken, welcomed = existing?.welcomed ?: false)
                        val result = tokenUpdate(base, tokens, refreshing = false)
                        replace(result)
                        result
                    }
                    mutableState.update { it.copy(lastSignedInId = id, loginRevision = it.loginRevision + 1) }
                    if (credentials.planEnabled) {
                        try { refreshModels(id); publish("ChatGPT 已登录，可选择账号和模型") }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { publish("ChatGPT 已登录，但模型列表未能读取，请点击刷新模型") }
                    } else publish("账号已登录，但未授予订阅调用权限，请选择“授权使用订阅”")
                    return
                } catch (e: ChatGptHttpProblem) {
                    if (e.code == "invalid_grant" && retry == 0 && returnedClient != null) {
                        clientId = returnedClient; hint = null
                    } else throw e
                } finally { cancelHook.dispose(); attempt.close(); pending.compareAndSet(attempt, null) }
            }
        } catch (e: CancellationException) { publish("ChatGPT 登录已取消，原有连接保持不变"); throw e }
        catch (e: Exception) {
            publish(if (e is ChatGptProblem || e is ChatGptHttpProblem) e.message else "ChatGPT 登录未完成，请重试")
        } finally {
            loginJob.compareAndSet(job, null)
            mutableState.update { it.copy(busy = false) }
            loginMutex.unlock()
        }
    }

    private fun tokenUpdate(base: ChatGptCredential, json: JSONObject, refreshing: Boolean): ChatGptCredential {
        val access = json.opt("access_token") as? String ?: ""
        val refresh = json.opt("refresh_token") as? String ?: ""
        val scopes = (json.opt("scope") as? String)?.split(' ')?.filter { it.isNotBlank() }?.toSet()
            ?: if (refreshing) base.scopes else emptySet()
        if (access.isNotEmpty() && !(json.opt("token_type") as? String).equals("Bearer", true)) throw ChatGptProblem("ChatGPT 返回了不支持的授权类型")
        if ((refreshing || CHATGPT_PLAN_SCOPE in scopes) && access.isBlank()) throw ChatGptProblem("ChatGPT 授权结果缺少访问凭据")
        if ((refreshing || "offline_access" in scopes) && refresh.isBlank()) throw ChatGptProblem("ChatGPT 授权结果缺少可更新的会话")
        val seconds = json.optLong("expires_in")
        if (access.isNotEmpty() && seconds !in 1..86400) throw ChatGptProblem("ChatGPT 返回了无效的授权期限")
        return base.copy(accessToken = access, refreshToken = refresh, scopes = scopes,
            expiresAt = if (access.isEmpty()) 0 else clock() + seconds * 1000,
            earliestRefreshAt = json.optLong("earliest_refresh_at").coerceAtLeast(0) * 1000)
    }

    internal suspend fun credentialFor(id: String): ChatGptCredential = mutex.withLock {
        requireStore()
        val account = snapshot.accounts.find { it.id == id } ?: throw ChatGptProblem("请先登录 ChatGPT")
        if (id in closing || !account.planEnabled) throw ChatGptProblem("此 ChatGPT 账号尚未获得订阅权限，或已退出登录")
        if (account.expiresAt > clock() + 60_000) return@withLock account
        if (clock() < account.earliestRefreshAt) {
            if (account.expiresAt > clock()) return@withLock account
            throw ChatGptProblem("ChatGPT 暂不允许更新此授权，请稍后重试")
        }
        if (account.refreshToken.isBlank()) throw ChatGptProblem("ChatGPT 授权已过期，请重新登录")
        try {
            val metadata = discovery()
            val response = http.request(metadata.getString("token_endpoint"), linkedMapOf("grant_type" to "refresh_token",
                "client_id" to account.clientId, "refresh_token" to account.refreshToken, "resource" to CHATGPT_RESOURCE))
            val next = tokenUpdate(account, response, refreshing = true)
            replace(next)
            if (!next.planEnabled) throw ChatGptProblem("ChatGPT 订阅权限已变更，请重新授权")
            next
        } catch (e: ChatGptHttpProblem) {
            if (e.code in setOf("invalid_grant", "invalid_refresh_token", "token_expired", "refresh_token_expired",
                    "refresh_token_invalidated", "refresh_token_reused")) replace(account.copy(accessToken = "", refreshToken = "", idToken = "", scopes = emptySet(), models = emptyList()))
            throw e
        }
    }

    suspend fun refreshModels(id: String) {
        val credential = credentialFor(id)
        val json = http.request("$CHATGPT_RESOURCE/models", bearer = credential.accessToken)
        val rows = json.optJSONArray("models") ?: throw ChatGptProblem("ChatGPT 模型目录格式无效")
        if (rows.length() > 1000) throw ChatGptProblem("ChatGPT 模型目录过大")
        val models = (0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.opt("visibility") == "list" }.map {
            val slug = it.opt("slug") as? String ?: throw ChatGptProblem("ChatGPT 模型标识无效")
            val name = it.opt("display_name") as? String ?: throw ChatGptProblem("ChatGPT 模型名称无效")
            if (slug.isBlank() || name.isBlank() || slug.length > 200 || name.length > 200) throw ChatGptProblem("ChatGPT 模型目录无效")
            ChatGptModel(slug, name)
        }.distinctBy { it.slug }
        mutex.withLock {
            val latest = snapshot.accounts.find { it.id == id }
            if (latest?.accessToken == credential.accessToken && latest.planEnabled && id !in closing) replace(latest.copy(models = models))
        }
        if (models.isEmpty()) throw ChatGptProblem("此 ChatGPT 账号暂未提供可用的订阅模型")
    }

    suspend fun acknowledgeWelcome(id: String) = mutex.withLock {
        snapshot.accounts.find { it.id == id }?.let { replace(it.copy(welcomed = true)) }
    }

    internal fun watchSignOut(id: String, disconnect: () -> Unit): AutoCloseable {
        synchronized(disconnectors) {
            requireStore()
            if (id in closing || snapshot.accounts.none { it.id == id && it.planEnabled }) throw ChatGptProblem("ChatGPT 账号已退出")
            disconnectors.getOrPut(id) { mutableSetOf() }.add(disconnect)
        }
        return AutoCloseable { synchronized(disconnectors) { disconnectors[id]?.remove(disconnect) } }
    }

    suspend fun signOut(id: String) = withContext(NonCancellable + Dispatchers.IO) {
        closing.add(id)
        val callbacks = synchronized(disconnectors) { disconnectors[id]?.toList().orEmpty() }
        callbacks.forEach { runCatching { it() } }
        var revoked = false
        try {
            mutex.withLock {
                requireStore()
                val account = snapshot.accounts.find { it.id == id } ?: return@withLock
                // Persist a non-usable state before the network step; process death cannot resume inference.
                replace(account.copy(revoking = true))
                try {
                    if (account.refreshToken.isBlank()) revoked = true
                    else {
                        for (attempt in 0..1) {
                            try {
                                val metadata = discovery()
                                http.request(metadata.getString("revocation_endpoint"), linkedMapOf(
                                    "token" to account.refreshToken, "token_type_hint" to "refresh_token", "client_id" to account.clientId))
                                revoked = true; break
                            } catch (_: Exception) { if (attempt == 0) delay(250) }
                        }
                    }
                } finally {
                    replace(account.copy(accessToken = "", refreshToken = "", idToken = "", scopes = emptySet(), models = emptyList(), revoking = false))
                }
            }
            publish(if (revoked) "已退出 ChatGPT，其他连接保持不变" else "已在本机退出，但未确认远端撤销；请在 ChatGPT 设置中断开本应用")
        } finally { closing.remove(id) }
    }
}
