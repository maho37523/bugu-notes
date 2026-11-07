package com.dspro.android.data
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.SecurityFixture
import com.dspro.android.data.chatgpt.*
class FakePrefs: SharedPreferences {
    val data = linkedMapOf<String, Any>()
    var failRead = false
    var failCommit = false
    override val all: Map<String, *> get() { check(!failRead); return data.toMap() }
    override fun getString(key: String, default: String?): String? { check(!failRead); return data[key] as? String ?: default }
    override fun getBoolean(key: String, default: Boolean): Boolean { check(!failRead); return data[key] as? Boolean ?: default }
    override fun contains(key: String): Boolean { check(!failRead); return data.containsKey(key) }
    override fun edit(): SharedPreferences.Editor = Editor()
    inner class Editor: SharedPreferences.Editor {
        val changes = linkedMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun apply() { check(commit()) }
        override fun commit(): Boolean { if (failCommit) return false; changes.forEach { (key, value) -> if (value == null) data.remove(key) else data[key] = value }; return true }
    }
}
class FakeContext: Context() {
    val fallback = FakePrefs()
    var fallbackReads = 0
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences { check(name == "ds_api_fallback"); fallbackReads++; return fallback }
}
fun main() {
    val ctx = FakeContext()
    val secure = FakePrefs()
    SecurityFixture.prefs = secure
    SecurityFixture.failInit = true
    val unavailable = ApiConfigStore(ctx)
    check(!unavailable.load().secureStorageAvailable)
    check(!unavailable.save(ApiConfig(apiKey = "synthetic")))
    check(ctx.fallbackReads == 0 && ctx.fallback.data.isEmpty())
    SecurityFixture.failInit = false
    val store = ApiConfigStore(ctx)
    check(store.save(ApiConfig(apiKey = "synthetic-openai", provider = "openai", model = "m")))
    check(store.save(ApiConfig(apiKey = "synthetic-claude", provider = "anthropic", model = "m", workspaceId = "workspace")))
    check(store.load("openai").apiKey == "synthetic-openai")
    check(store.load("anthropic").workspaceId == "workspace")
    ctx.fallback.data["anthropic_api_key"] = "old-synthetic"
    check(store.clear("anthropic"))
    check(store.load("anthropic").apiKey.isEmpty())
    check(store.load("openai").apiKey == "synthetic-openai")
    check("anthropic_api_key" !in ctx.fallback.data)
    secure.data["api_key"] = "legacy-synthetic"
    ctx.fallback.data["api_key"] = "legacy-fallback-synthetic"
    check(store.load("deepseek").apiKey == "legacy-synthetic")
    check(store.clear("deepseek"))
    check(store.load("deepseek").apiKey.isEmpty())
    check("api_key" !in ctx.fallback.data)
    secure.failRead = true
    check(!store.load("openai").secureStorageAvailable)
    check(!store.save(ApiConfig(apiKey = "synthetic")))
    secure.failRead = false
    secure.failCommit = true
    check(!store.save(ApiConfig(apiKey = "synthetic")))
    check(!store.clear("openai"))
    secure.failCommit = false
    ctx.fallback.failCommit = true
    check(!store.clear("openai"))
    check(store.load("openai").apiKey.isEmpty())
    println("PASS: secure-store init/read/write failure, provider isolation, explicit fallback cleanup")
    ctx.fallback.failCommit = false
    val priorFallbackReads = ctx.fallbackReads
    secure.data["openai_api_key"] = "preserved-synthetic-api-key"
    secure.data["web_search_enabled"] = true
    secure.data["reasoning_effort"] = "low"
    check(store.save(ApiConfig(provider = "chatgpt", model = "catalog-model", chatGptAccountId = "registration")))
    check(secure.data["web_search_enabled"] == true && secure.data["reasoning_effort"] == "low")
    check(store.load("openai").apiKey == "preserved-synthetic-api-key")
    check(store.load("chatgpt").chatGptAccountId == "registration" && !store.load("chatgpt").isConfigured)
    check(!store.save(ApiConfig(provider = "chatgpt", apiKey = "synthetic-token-misuse")))
    // Independent provider/model/account preferences, preserving existing credentials.
    check(store.load("openai", "gpt-5.4-mini").effort == ThinkingEffort.Auto)
    check(store.save(store.load("openai", "gpt-5.4-mini").copy(effort = ThinkingEffort.Medium)))
    check(store.save(store.load("openai", "gpt-6.1-sol").copy(effort = ThinkingEffort.Max)))
    check(store.load("openai", "gpt-5.4-mini").effort == ThinkingEffort.Medium)
    check(store.load("openai", "gpt-6.1-sol").effort == ThinkingEffort.Max)
    check(store.load("openai").apiKey == "preserved-synthetic-api-key")
    check(store.load("deepseek").effort == ThinkingEffort.Low)
    check(store.save(store.load("chatgpt", "catalog-model").copy(effort = ThinkingEffort.XHigh)))
    check(store.load("chatgpt", "catalog-model").effort == ThinkingEffort.XHigh)
    check(store.save(store.load("chatgpt").copy(chatGptAccountId = "different-registration", effort = ThinkingEffort.Auto)))
    check(store.load("chatgpt", "catalog-model").effort == ThinkingEffort.Auto)
    check(store.save(store.load("chatgpt").copy(chatGptAccountId = "registration", effort = ThinkingEffort.XHigh)))
    check(store.load("chatgpt", "catalog-model", "registration").effort == ThinkingEffort.XHigh)
    check(store.load("chatgpt", "catalog-model", "different-registration").effort == ThinkingEffort.Auto)
    val tokenPrefs = FakePrefs()
    SecurityFixture.byName["ds_chatgpt_secure"] = tokenPrefs
    val credentials = ChatGptCredential("registration", "Account", "issued-client", "subject", "fixture@example.invalid",
        "synthetic-id", "synthetic-access", "synthetic-refresh", CHATGPT_SCOPES.split(' ').toSet(), Long.MAX_VALUE,
        models = listOf(ChatGptModel("catalog-model", "Catalog model", listOf("low", "medium", "max"))))
    val tokenStore = AndroidChatGptStore(ctx)
    val snapshot = ChatGptSnapshot("urn:uuid:00000000-0000-4000-8000-000000000001", listOf(credentials))
    tokenStore.save(snapshot)
    check(tokenStore.load() == snapshot)
    check("registrations" !in secure.data && tokenPrefs.data.keys == setOf("registrations"))
    check(ctx.fallbackReads == priorFallbackReads)
    tokenPrefs.failCommit = true
    check(runCatching { tokenStore.save(snapshot.copy(accounts = emptyList())) }.isFailure)
    check(tokenStore.load() == snapshot)
    tokenPrefs.failCommit = false
    tokenPrefs.data["registrations"] = "damaged-json"
    check(!ChatGptAccounts(AndroidChatGptStore(ctx)).state.value.secureStorageAvailable)
    SecurityFixture.failInit = true
    check(!ChatGptAccounts(AndroidChatGptStore(ctx)).state.value.secureStorageAvailable)
    println("PASS: subscription selection preserves API settings, separate encrypted registration records, atomic write failure and corrupt/Keystore failure close access")
}
