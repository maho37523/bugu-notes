package com.dspro.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class ThinkingEffort(val apiValue: String, val label: String) {
    Auto("", "模型默认"), None("none", "不思考"), Minimal("minimal", "最少"), Low("low", "低"),
    Medium("medium", "中"), High("high", "高"), XHigh("xhigh", "很高"), Max("max", "极高"), Ultra("ultra", "最高"),
}

data class ApiConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-flash",
    val effort: ThinkingEffort = ThinkingEffort.High,
    val provider: String = "deepseek",
    val webSearchEnabled: Boolean = false,
    val workspaceId: String = "",
    val secureStorageAvailable: Boolean = true,
    val trustedBaseUrl: String = "",
    val chatGptAccountId: String = "",
    val chatGptPlanEnabled: Boolean = false,
    val chatGptAccountLabel: String = "",
    val chatGptModelLabel: String = "",
) {
    val isConfigured: Boolean get() = secureStorageAvailable && if (provider == "chatgpt")
        chatGptAccountId.isNotBlank() && chatGptPlanEnabled && model.isNotBlank() else apiKey.isNotBlank()

    val displayModel: String
        get() = if (provider == "chatgpt" && chatGptModelLabel.isNotBlank()) chatGptModelLabel else when (model) {
            "deepseek-v4-pro" -> "DeepSeek-V4-Pro"
            "deepseek-flash" -> "DeepSeek-V4.1-Flash"
            else -> model
        }

    val endpoint: String
        get() {
            if (provider == "chatgpt") return "https://api.openai.com/v1/responses"
            val base = baseUrl.trim().trimEnd('/')
            val suffix = if (provider == "anthropic") "/messages" else "/chat/completions"
            return if (base.endsWith(suffix)) base else "$base$suffix"
        }
}

class ApiConfigStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext, "ds_api_secure", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    fun load(providerOverride: String? = null, modelOverride: String? = null, accountOverride: String? = null): ApiConfig = runCatching {
        val prefs = prefs ?: return ApiConfig(provider = providerOverride ?: "deepseek", secureStorageAvailable = false)
        val provider = providerOverride ?: (prefs.getString("provider", "deepseek") ?: "deepseek")
        if (provider == "deepseek") {
            val migrated = ApiConfigStoreCodec.migrateLegacy(prefs.all)
            prefs.edit().apply {
                listOf("deepseek_api_key", "deepseek_base_url", "deepseek_model").forEach { key ->
                    if (!prefs.contains(key)) putString(key, migrated[key]?.toString().orEmpty())
                }
            }.apply()
        }
        val legacyKey = prefs.getString("api_key", "") ?: ""
        val storedKey = prefs.getString("${provider}_api_key", null)
        val storedBase = prefs.getString("${provider}_base_url", null)
        val storedModel = prefs.getString("${provider}_model", null)
        val key = storedKey ?: if (provider == "deepseek") legacyKey else ""
        val defaultBase = when (provider) {
            "openai" -> "https://api.openai.com/v1"
            "chatgpt" -> "https://api.openai.com/v1"
            "anthropic" -> "https://api.anthropic.com/v1"
            "aliyun" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "custom" -> ""
            else -> "https://api.deepseek.com"
        }
        val defaultModel = when (provider) { "chatgpt" -> ""; "openai" -> "gpt-4o-mini"; "anthropic" -> "claude-sonnet-5-5"; "aliyun" -> "qwen-plus"; "custom" -> ""; else -> "deepseek-flash" }
        val selectedModel = modelOverride ?: storedModel ?: if (provider == "deepseek") prefs.getString("model", defaultModel) ?: defaultModel else defaultModel
        val accountId = if (provider == "chatgpt") accountOverride ?: prefs.getString("chatgpt_account_id", "").orEmpty() else ""
        val effortKey = effortPreferenceKey(provider, selectedModel, accountId)
        val storedEffort = prefs.getString(effortKey, null) ?: prefs.getString("${provider}_reasoning_effort", null)
        ApiConfig(
            apiKey = if (provider == "chatgpt") "" else key,
            baseUrl = storedBase ?: if (provider == "deepseek") prefs.getString("base_url", defaultBase) ?: defaultBase else defaultBase,
            model = selectedModel,
            effort = ThinkingEffort.entries.firstOrNull { it.apiValue == storedEffort }
                ?: if (provider == "deepseek") ThinkingEffort.entries.firstOrNull { it.apiValue == prefs.getString("reasoning_effort", null) }
                    ?: if (prefs.getBoolean("thinking", true)) ThinkingEffort.High else ThinkingEffort.None
                else ThinkingEffort.Auto,
            provider = provider,
            webSearchEnabled = prefs.getBoolean("web_search_enabled", false),
            workspaceId = prefs.getString("${provider}_workspace_id", "") ?: "",
            trustedBaseUrl = prefs.getString("${provider}_trusted_base_url", "") ?: "",
            chatGptAccountId = accountId,
        )
    }.getOrElse { ApiConfig(provider = providerOverride ?: "deepseek", secureStorageAvailable = false) }

    fun save(config: ApiConfig): Boolean = runCatching {
        if (!config.secureStorageAvailable) return false
        val prefs = prefs ?: return false
        if (config.provider == "chatgpt" && config.apiKey.isNotEmpty()) return false
        val next = ApiConfigStoreCodec.saveProvider(prefs.all, config.provider, config.apiKey, config.baseUrl, config.model, config.workspaceId)
        prefs.edit()
            .putString("provider", next["provider"].toString())
            .putString("${config.provider}_api_key", next["${config.provider}_api_key"].toString())
            .putString("${config.provider}_base_url", next["${config.provider}_base_url"].toString())
            .putString("${config.provider}_model", next["${config.provider}_model"].toString())
            .putString("${config.provider}_workspace_id", next["${config.provider}_workspace_id"].toString())
            .putString("${config.provider}_trusted_base_url", config.trustedBaseUrl)
            .putString(effortPreferenceKey(config.provider, config.model, config.chatGptAccountId), config.effort.apiValue)
            .apply {
                if (config.provider == "chatgpt") putString("chatgpt_account_id", config.chatGptAccountId)
                else {
                    putBoolean("web_search_enabled", config.webSearchEnabled)
                    if (config.provider == "deepseek") putString("reasoning_effort", config.effort.apiValue)
                }
            }
            .commit()
    }.getOrDefault(false)

    fun clear(provider: String): Boolean = runCatching {
        val prefs = prefs ?: return false
        val next = ApiConfigStoreCodec.clearProviderKey(prefs.all, provider)
        val secureCleared = prefs.edit().apply {
            if ("${provider}_api_key" !in next) remove("${provider}_api_key")
            if (provider == "deepseek" && "api_key" !in next) remove("api_key")
        }.commit()
        if (!secureCleared) return false
        appContext.getSharedPreferences("ds_api_fallback", Context.MODE_PRIVATE).edit().apply {
            remove("${provider}_api_key")
            if (provider == "deepseek") remove("api_key")
        }.commit()
    }.getOrDefault(false)
}

internal fun effortPreferenceKey(provider: String, model: String, accountId: String = "") =
    "effort:$provider:${accountId.length}:$accountId:$model"
