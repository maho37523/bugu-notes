package com.dspro.android.data

import org.json.JSONArray
import org.json.JSONObject

internal fun buildChatRequestBody(config: ApiConfig, turns: List<ChatTurn>): JSONObject = JSONObject().apply {
    put("model", config.model)
    put("stream", true)
    put("messages", JSONArray().apply {
        turns.forEach { turn ->
            val message = JSONObject().put("role", turn.role)
            if (turn.imageBase64 != null) {
                message.put("content", JSONArray()
                    .put(JSONObject().put("type", "text").put("text", turn.content))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:${turn.imageMime};base64,${turn.imageBase64}"))))
            } else message.put("content", turn.content)
            put(message)
        }
    })
    if (config.provider == "deepseek") {
        put("thinking", JSONObject().put("type", if (config.effort == ThinkingEffort.None) "disabled" else "enabled"))
        put("reasoning_effort", config.effort.apiValue)
    }
}

internal fun parseChatSseChunk(data: String): List<StreamEvent> {
    return try {
        val choices = JSONObject(data).optJSONArray("choices") ?: return emptyList()
        if (choices.length() == 0) return emptyList()
        val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return emptyList()
        val reasoning = if (delta.isNull("reasoning_content")) "" else delta.optString("reasoning_content")
        val fallback = if (delta.isNull("reasoning")) "" else delta.optString("reasoning")
        val content = if (delta.isNull("content")) "" else delta.optString("content")
        buildList {
            (reasoning.ifEmpty { fallback }).takeIf { it.isNotEmpty() }?.let { add(StreamEvent.Reasoning(it)) }
            if (content.isNotEmpty()) add(StreamEvent.Content(content))
        }
    } catch (_: Exception) { emptyList() }
}

internal fun normalizeSearchQuery(raw: String): String {
    val codePoints = raw.codePoints().limit(128).toArray()
    return String(codePoints, 0, codePoints.size).trim()
}

internal fun searchEndpointFor(baseUrl: String): String {
    val uri = java.net.URI(baseUrl.trim())
    require(uri.scheme == "https" && uri.host.equals("api.deepseek.com", ignoreCase = true)) {
        "DeepSeek 联网搜索仅向官方 api.deepseek.com 发请求；请将 DeepSeek Base URL 设为官方地址"
    }
    return "https://api.deepseek.com/v1/tools/search"
}

internal fun parseSearchResponse(json: String): List<DeepSeekSearchResult> {
    val results = JSONObject(json).optJSONArray("results") ?: JSONArray()
    return buildList {
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val url = item.optString("url").takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: continue
            add(DeepSeekSearchResult(item.optString("title").ifBlank { url }, url, item.optString("snippet"),
                item.optString("summary").ifBlank { item.optString("content") }.take(4000)))
        }
    }
}

internal fun searchHttpError(code: Int, body: String): String {
    val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
    return message?.takeIf { it.isNotBlank() } ?: "DeepSeek 搜索失败（HTTP $code）${body.take(240)}"
}

/** Pure SharedPreferences migration/save rules, kept separate for offline isolation regression. */
internal object ApiConfigStoreCodec {
    fun migrateLegacy(values: Map<String, *>): Map<String, Any> {
        val next = values.entries.associateTo(linkedMapOf()) { it.key to (it.value ?: "") }
        if (next["deepseek_api_key"] == null || next["deepseek_base_url"] == null || next["deepseek_model"] == null) {
            if (next["deepseek_api_key"] == null) next["deepseek_api_key"] = next["api_key"] ?: ""
            if (next["deepseek_base_url"] == null) next["deepseek_base_url"] = next["base_url"] ?: "https://api.deepseek.com"
            if (next["deepseek_model"] == null) next["deepseek_model"] = next["model"] ?: "deepseek-flash"
        }
        return next
    }

    fun saveProvider(values: Map<String, *>, provider: String, apiKey: String, baseUrl: String, model: String): Map<String, Any> =
        values.entries.associateTo(linkedMapOf()) { it.key to (it.value ?: "") }.apply {
            put("provider", provider)
            put("${provider}_api_key", apiKey)
            put("${provider}_base_url", baseUrl)
            put("${provider}_model", model)
        }

    fun clearProviderKey(values: Map<String, *>, provider: String): Map<String, Any> =
        values.entries.associateTo(linkedMapOf()) { it.key to (it.value ?: "") }.apply {
            remove("${provider}_api_key")
            if (provider == "deepseek") remove("api_key")
        }
}
