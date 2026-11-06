package com.dspro.android.data

fun main() {
    val legacy = mapOf("api_key" to "old-deepseek-key", "base_url" to "https://legacy.deepseek-proxy.example/v1", "model" to "old-model")
    val migrated = ApiConfigStoreCodec.migrateLegacy(legacy)
    check(migrated["deepseek_api_key"] == "old-deepseek-key")
    check(migrated["deepseek_base_url"] == "https://legacy.deepseek-proxy.example/v1")
    check(migrated["deepseek_model"] == "old-model")
    val openAi = ApiConfigStoreCodec.saveProvider(migrated, "openai", "openai-secret", "https://api.openai.com/v1", "gpt-test")
    val restarted = ApiConfigStoreCodec.migrateLegacy(openAi)
    check(restarted["deepseek_api_key"] == "old-deepseek-key" && restarted["deepseek_base_url"] == legacy["base_url"])
    check(restarted["openai_api_key"] == "openai-secret" && restarted["provider"] == "openai")
    val clearedOpenAi = ApiConfigStoreCodec.clearProviderKey(restarted, "openai")
    check("openai_api_key" !in clearedOpenAi && clearedOpenAi["deepseek_api_key"] == "old-deepseek-key")
    val clearedDeepSeek = ApiConfigStoreCodec.clearProviderKey(clearedOpenAi, "deepseek")
    check("api_key" !in clearedDeepSeek && "deepseek_api_key" !in clearedDeepSeek)
    check(clearedDeepSeek["openai_base_url"] == "https://api.openai.com/v1")

    val turns = listOf(ChatTurn("user", "你好"))
    val generic = buildChatRequestBody(ApiConfig("openai-secret", "https://api.openai.com/v1", "gpt-test", provider = "openai"), turns)
    check(generic.optString("model") == "gpt-test" && generic.optBoolean("stream"))
    check(!generic.has("thinking") && !generic.has("reasoning_effort"))
    val deepseek = buildChatRequestBody(ApiConfig("ds-secret", "https://api.deepseek.com", "deepseek-flash"), turns)
    check(deepseek.has("thinking") && deepseek.has("reasoning_effort"))
    check(!deepseek.toString().contains("ds-secret") && !generic.toString().contains("openai-secret"))

    val mixed = parseChatSseChunk("""{"choices":[{"delta":{"reasoning_content":"推理","content":"  正文 "}}]}""")
    check(mixed == listOf(StreamEvent.Reasoning("推理"), StreamEvent.Content("  正文 ")))
    check(parseChatSseChunk("""{"choices":[{"delta":{"content":" "}}]}""") == listOf(StreamEvent.Content(" ")))
    check(parseChatSseChunk("not json").isEmpty())

    val emojiQuery = "😀".repeat(130)
    check(normalizeSearchQuery(emojiQuery) == "😀".repeat(128))
    check(runCatching { searchEndpointFor("https://proxy.example/v1") }.isFailure)
    check(searchEndpointFor("https://api.deepseek.com/v1") == "https://api.deepseek.com/v1/tools/search")
    val resultJson = """{"results":[{"title":"A","url":"https://example.org/a","snippet":"摘录","summary":"摘要"},{"title":"伪造","url":"javascript:alert(1)"}]}"""
    val sources = parseSearchResponse(resultJson)
    check(sources.size == 1 && sources.single().url == "https://example.org/a" && sources.single().summary == "摘要")
    check(parseSearchResponse("{\"results\":[]}").isEmpty())
    check(searchHttpError(429, "{\"error\":{\"message\":\"rate limited\"}}") == "rate limited")
    check(searchHttpError(500, "oops").contains("HTTP 500"))
    println("PASS: provider migration/isolation/clear across restart; provider-specific payloads; SSE mixed and whitespace deltas; search query, URL, success/empty/error fixtures")
}
