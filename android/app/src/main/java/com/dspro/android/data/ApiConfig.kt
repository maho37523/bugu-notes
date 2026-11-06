package com.dspro.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class ThinkingEffort(val apiValue: String, val label: String) {
    None("none", "无"), Low("low", "低"), High("high", "高"), Max("max", "极高"),
}

data class ApiConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-flash",
    val effort: ThinkingEffort = ThinkingEffort.High,
    val provider: String = "deepseek",
    val webSearchEnabled: Boolean = false,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    val displayModel: String
        get() = when (model) {
            "deepseek-v4-pro" -> "DeepSeek-V4-Pro"
            "deepseek-flash" -> "DeepSeek-V4.1-Flash"
            else -> model
        }

    val endpoint: String
        get() {
            val base = baseUrl.trim().trimEnd('/')
            return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        }
}

class ApiConfigStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                "ds_api_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            // Some OEM devices fail to provision the keystore. Keep the prototype usable.
            appContext.getSharedPreferences("ds_api_fallback", Context.MODE_PRIVATE)
        }
    }

    fun load(providerOverride: String? = null): ApiConfig {
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
            "aliyun" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "custom" -> ""
            else -> "https://api.deepseek.com"
        }
        val defaultModel = when (provider) { "openai" -> "gpt-4o-mini"; "aliyun" -> "qwen-plus"; "custom" -> ""; else -> "deepseek-flash" }
        return ApiConfig(
            apiKey = key,
            baseUrl = storedBase ?: if (provider == "deepseek") prefs.getString("base_url", defaultBase) ?: defaultBase else defaultBase,
            model = storedModel ?: if (provider == "deepseek") prefs.getString("model", defaultModel) ?: defaultModel else defaultModel,
            effort = ThinkingEffort.entries.firstOrNull { it.apiValue == prefs.getString("reasoning_effort", null) }
                ?: if (prefs.getBoolean("thinking", true)) ThinkingEffort.High else ThinkingEffort.None,
            provider = provider,
            webSearchEnabled = prefs.getBoolean("web_search_enabled", false),
        )
    }

    fun save(config: ApiConfig) {
        val next = ApiConfigStoreCodec.saveProvider(prefs.all, config.provider, config.apiKey, config.baseUrl, config.model)
        prefs.edit()
            .putString("provider", next["provider"].toString())
            .putString("${config.provider}_api_key", next["${config.provider}_api_key"].toString())
            .putString("${config.provider}_base_url", next["${config.provider}_base_url"].toString())
            .putString("${config.provider}_model", next["${config.provider}_model"].toString())
            .putBoolean("web_search_enabled", config.webSearchEnabled)
            .putString("reasoning_effort", config.effort.apiValue)
            .apply()
    }

    fun clear(provider: String) {
        val next = ApiConfigStoreCodec.clearProviderKey(prefs.all, provider)
        prefs.edit().apply {
            if ("${provider}_api_key" !in next) remove("${provider}_api_key")
            if (provider == "deepseek" && "api_key" !in next) remove("api_key")
        }.apply()
    }
}
