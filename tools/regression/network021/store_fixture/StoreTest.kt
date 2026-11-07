package com.dspro.android.data
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.SecurityFixture
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
}
