package com.dspro.android.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStream
import java.io.FilterInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private class FakeHttp(url: URL, private val status: Int, private val response: String) : HttpURLConnection(url) {
    val sent = ByteArrayOutputStream()
    var closed = false
    override fun connect() { connected = true }
    override fun disconnect() { closed = true }
    override fun usingProxy() = false
    override fun getResponseCode() = status
    override fun getOutputStream() = sent
    override fun getInputStream(): InputStream = object : FilterInputStream(ByteArrayInputStream(response.toByteArray(Charsets.UTF_8))) {
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, minOf(length, 1))
    }
}

private class BlockingHttp(url: URL) : HttpURLConnection(url) {
    val entered = CountDownLatch(1)
    val released = CountDownLatch(1)
    var closed = false
    override fun connect() { connected = true }
    override fun disconnect() { closed = true; released.countDown() }
    override fun usingProxy() = false
    override fun getResponseCode() = 200
    override fun getOutputStream() = ByteArrayOutputStream()
    override fun getInputStream(): InputStream = object : InputStream() {
        override fun read(): Int { entered.countDown(); released.await(5, TimeUnit.SECONDS); return -1 }
    }
}

fun main() {
    val unavailable = ApiConfig(apiKey = "synthetic-key", secureStorageAvailable = false)
    check(!unavailable.isConfigured)
    val blocked = runBlocking { DeepSeekClient(unavailable).stream(listOf(ChatTurn("user", "test"))).first() }
    check(blocked is StreamEvent.Failure && blocked.message.contains("安全存储"))
    check(requestDestinationError(ApiConfig(apiKey = "synthetic", baseUrl = "https://api.openai.com/v1", provider = "openai")) == null)
    val proxy = ApiConfig(apiKey = "synthetic", baseUrl = "https://proxy.example/v1", provider = "openai")
    check(requestDestinationError(proxy) != null)
    check(requestDestinationError(proxy.copy(trustedBaseUrl = "https://proxy.example/v1")) == null)
    check(requestDestinationError(proxy.copy(baseUrl = "https://proxy.example/other", trustedBaseUrl = "https://proxy.example/v1")) != null)
    check(requestDestinationError(proxy.copy(baseUrl = "https://api.openai.com.evil.example/v1")) != null)
    check(requestDestinationError(proxy.copy(baseUrl = "https://user@api.openai.com/v1")) != null)
    check(requestDestinationError(proxy.copy(baseUrl = "http://api.openai.com/v1")) != null)
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
    val claude = ApiConfigStoreCodec.saveProvider(openAi, "anthropic", "claude-secret", "https://api.anthropic.com/v1", "claude-sonnet-5-5", "workspace-test")
    check(claude["anthropic_workspace_id"] == "workspace-test")
    check(claude["anthropic_api_key"] == "claude-secret" && claude["openai_api_key"] == "openai-secret")
    val clearedClaude = ApiConfigStoreCodec.clearProviderKey(claude, "anthropic")
    check("anthropic_api_key" !in clearedClaude && clearedClaude["openai_api_key"] == "openai-secret")
    check(clearedClaude["anthropic_workspace_id"] == "workspace-test")
    val claudeRequest = buildClaudeRequestBody(
        ApiConfig(provider = "anthropic", model = "claude-sonnet-5-5"),
        listOf(ChatTurn("system", "project rules"), ChatTurn("context", "note A"), ChatTurn("user", "question A"),
            ChatTurn("assistant", "answer A"), ChatTurn("context", "note B"), ChatTurn("user", "question B", "aGVsbG8=", "image/png")),
    )
    check(claudeRequest.optString("system") == "project rules" && claudeRequest.optInt("max_tokens") == 4096)
    check(claudeRequest.getJSONObject("thinking").optString("type") == "adaptive")
    check(claudeRequest.getJSONObject("output_config").optString("effort") == "high")
    val messages = claudeRequest.getJSONArray("messages")
    check(messages.length() == 3 && messages.getJSONObject(0).optString("role") == "user")
    check(messages.getJSONObject(0).getJSONArray("content").getJSONObject(0).optString("text").contains("note A"))
    check(messages.getJSONObject(2).getJSONArray("content").getJSONObject(0).optString("text").contains("note B"))
    check(messages.getJSONObject(2).getJSONArray("content").getJSONObject(2).optString("type") == "image")
    check(imageRequestError(ApiConfig(provider = "anthropic", model = "unknown"), "image/png", 100) != null)
    check(imageRequestError(ApiConfig(provider = "anthropic", model = "claude-sonnet-5-5"), "image/png", 100) == null)
    val stream = ChatStreamDecoder(true)
    check(stream.accept("""{"type":"message_start"}""").isEmpty())
    check(stream.accept("""{"type":"content_block_start","index":0,"content_block":{"type":"thinking"}}""").isEmpty())
    check(stream.accept("""{"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"private"}}""").isEmpty())
    check(stream.accept("""{"type":"content_block_stop","index":0}""").isEmpty())
    check(stream.accept("""{"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}""").isEmpty())
    check(stream.accept("""{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":" hello "}}""") == listOf(StreamEvent.Content(" hello ")))
    check(stream.accept("""{"type":"content_block_stop","index":1}""").isEmpty())
    check(stream.accept("""{"type":"message_delta","delta":{"stop_reason":"end_turn"}}""").isEmpty())
    check(stream.accept("""{"type":"message_stop"}""") == listOf(StreamEvent.Done))
    check(stream.endOfInput().isEmpty())
    val truncated = ChatStreamDecoder(false)
    check(truncated.accept("""{"choices":[{"delta":{"content":"partial"},"finish_reason":null}]}""") == listOf(StreamEvent.Content("partial")))
    check(truncated.endOfInput().single() is StreamEvent.Failure)
    check(truncated.accept("[DONE]").isEmpty())
    val complete = ChatStreamDecoder(false)
    check(complete.accept("""{"choices":[{"delta":{},"finish_reason":"stop"}]}""").isEmpty())
    check(complete.accept("[DONE]") == listOf(StreamEvent.Done))
    val conflict = ChatStreamDecoder(false)
    conflict.accept("""{"choices":[{"delta":{},"finish_reason":"length"}]}""")
    check(conflict.accept("""{"choices":[{"delta":{},"finish_reason":"stop"}]}""").single() is StreamEvent.Failure)
    check(conflict.accept("[DONE]").isEmpty())
    val errorStream = ChatStreamDecoder(false)
    check(errorStream.accept("""{"error":{"message":"synthetic"}}""").single() is StreamEvent.Failure)
    check(errorStream.accept("[DONE]").isEmpty())
    val limited = ChatStreamDecoder(false)
    check(limited.accept("""{"choices":[{"delta":{},"finish_reason":"length"}]}""").isEmpty())
    check(limited.accept("[DONE]").single() is StreamEvent.Failure)
    val malformed = ChatStreamDecoder(false)
    check(malformed.accept("not-json").single() is StreamEvent.Failure)
    check(malformed.accept("[DONE]").isEmpty())
    val refused = ChatStreamDecoder(true)
    refused.accept("""{"type":"message_start"}""")
    refused.accept("""{"type":"message_delta","delta":{"stop_reason":"refusal"}}""")
    check(refused.accept("""{"type":"message_stop"}""").single() is StreamEvent.Failure)
    val toolStream = ChatStreamDecoder(true)
    toolStream.accept("""{"type":"message_start"}""")
    check(toolStream.accept("""{"type":"content_block_start","index":0,"content_block":{"type":"tool_use"}}""").single() is StreamEvent.Failure)
    check(toolStream.accept("""{"type":"message_stop"}""").isEmpty())
    val claudeSse = listOf(
        ": ping", "", "data:", "",
        "data: {\"type\":\"message_start\"}", "",
        "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}", "",
        "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\" 答😀 \"}}", "",
        "data: {\"type\":\"content_block_stop\",\"index\":0}", "",
        "data: {\"type\":\"message_delta\",", "data: \"delta\":{\"stop_reason\":\"end_turn\"}}", "",
        "data: {\"type\":\"message_stop\"}", "", "",
    ).joinToString("\n")
    lateinit var fake: FakeHttp
    val claudeCfg = ApiConfig(apiKey = "synthetic-secret", baseUrl = "https://api.anthropic.com/v1", model = "claude-sonnet-5-5", provider = "anthropic", workspaceId = "synthetic-workspace")
    val fakeEvents = runBlocking {
        DeepSeekClient(claudeCfg) { url -> FakeHttp(url, 200, claudeSse).also { fake = it } }
            .stream(listOf(ChatTurn("user", "synthetic prompt"))).toList()
    }
    check(fakeEvents == listOf(StreamEvent.Content(" 答😀 "), StreamEvent.Done))
    check(fake.url.path == "/v1/messages" && fake.getRequestProperty("anthropic-version") == "2026-10-21")
    check(fake.getRequestProperty("anthropic-workspace-id") == "synthetic-workspace")
    check(!fake.instanceFollowRedirects && fake.closed && !fake.sent.toString().contains("synthetic-secret"))
    val redirectEvents = runBlocking { DeepSeekClient(claudeCfg) { url -> FakeHttp(url, 302, "") }.stream(listOf(ChatTurn("user", "x"))).toList() }
    check(redirectEvents.single() is StreamEvent.Failure)
    for (code in listOf(401, 402, 403, 429, 500, 529)) {
        val events = runBlocking { DeepSeekClient(claudeCfg) { url -> FakeHttp(url, code, "synthetic-secret") }.stream(listOf(ChatTurn("user", "x"))).toList() }
        check(events.size == 1 && events.single() is StreamEvent.Failure && !events.toString().contains("synthetic-secret"))
    }
    val openAiSse = "data: {\"choices\":[{\"delta\":{\"content\":\"part\"},\"finish_reason\":null}]}\n\n" +
        "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n"
    val openAiCfg = ApiConfig(apiKey = "synthetic-openai", baseUrl = "https://api.openai.com/v1", model = "gpt-5.4-mini-2026-03-17", provider = "openai")
    lateinit var openAiHttp: FakeHttp
    val openAiEvents = runBlocking { DeepSeekClient(openAiCfg) { url -> FakeHttp(url, 200, openAiSse).also { openAiHttp = it } }.stream(listOf(ChatTurn("user", "x"))).toList() }
    check(openAiEvents == listOf(StreamEvent.Content("part"), StreamEvent.Done))
    check(openAiHttp.url.path == "/v1/chat/completions" && !openAiHttp.sent.toString().contains("synthetic-openai"))
    val partialEvents = runBlocking { DeepSeekClient(openAiCfg) { url -> FakeHttp(url, 200, "data: {\"choices\":[{\"delta\":{\"content\":\"partial\"},\"finish_reason\":null}]}\n\n") }.stream(listOf(ChatTurn("user", "x"))).toList() }
    check(partialEvents.size == 2 && partialEvents.first() == StreamEvent.Content("partial") && partialEvents.last() is StreamEvent.Failure)
    var opened = false
    val blockedDestination = runBlocking { DeepSeekClient(proxy) { url -> opened = true; FakeHttp(url, 200, "") }.stream(listOf(ChatTurn("user", "x"))).toList() }
    check(!opened && blockedDestination.single() is StreamEvent.Failure)
    val blockingRef = AtomicReference<BlockingHttp?>(null)
    runBlocking {
        val job = launch { DeepSeekClient(claudeCfg) { url -> BlockingHttp(url).also { blockingRef.set(it) } }.stream(listOf(ChatTurn("user", "x"))).collect {} }
        withTimeout(5000) {
            while (blockingRef.get() == null) kotlinx.coroutines.yield()
            check(blockingRef.get()!!.entered.await(3, TimeUnit.SECONDS))
            job.cancelAndJoin()
        }
    }
    check(blockingRef.get()!!.closed)

    val turns = listOf(ChatTurn("user", "你好"))
    val generic = buildChatRequestBody(ApiConfig("openai-secret", "https://api.openai.com/v1", "gpt-test", provider = "openai"), turns)
    check(generic.optString("model") == "gpt-test" && generic.optBoolean("stream"))
    check(!generic.has("thinking") && !generic.has("reasoning_effort"))
    check(buildChatRequestBody(ApiConfig("synthetic", "https://api.openai.com/v1", "gpt-5.4-mini-2026-03-17", provider = "openai"), turns).optInt("max_completion_tokens") == 4096)
    val deepseek = buildChatRequestBody(ApiConfig("ds-secret", "https://api.deepseek.com", "deepseek-flash"), turns)
    check(deepseek.has("thinking") && deepseek.has("reasoning_effort"))
    check(!deepseek.toString().contains("ds-secret") && !generic.toString().contains("openai-secret"))

    for (level in supportedThinkingEfforts("openai", "gpt-5.4-mini")) {
        val body = buildChatRequestBody(ApiConfig(provider = "openai", model = "gpt-5.4-mini", effort = level), turns)
        check(if (level == ThinkingEffort.Auto) !body.has("reasoning_effort") else body.optString("reasoning_effort") == level.apiValue)
    }
    check(supportedThinkingEfforts("openai", "gpt-4o-mini") == listOf(ThinkingEffort.Auto))
    check(!buildChatRequestBody(ApiConfig(provider = "custom", model = "unknown", effort = ThinkingEffort.Max), turns).has("reasoning_effort"))
    check(ThinkingEffort.None !in supportedThinkingEfforts("chatgpt", "gpt-6.1-sol"))
    check(supportedThinkingEfforts("chatgpt", "new-model", listOf("low", "ultra", "new-future-value")) == listOf(ThinkingEffort.Auto, ThinkingEffort.Low, ThinkingEffort.Ultra))
    check(supportedThinkingEfforts("chatgpt", "gpt-6.1-sol", emptyList()) == listOf(ThinkingEffort.Auto))
    for (level in listOf(ThinkingEffort.Low, ThinkingEffort.Medium, ThinkingEffort.High, ThinkingEffort.XHigh, ThinkingEffort.Max)) {
        val body = buildClaudeRequestBody(ApiConfig(provider = "anthropic", model = "claude-sonnet-5-5", effort = level), turns)
        check(body.getJSONObject("output_config").optString("effort") == level.apiValue)
        check(body.getJSONObject("thinking").optString("type") == "adaptive")
    }
    val qwen = buildChatRequestBody(ApiConfig(provider = "aliyun", model = "qwen3.8-max", effort = ThinkingEffort.Low), turns)
    check(qwen.optBoolean("enable_thinking") && qwen.optString("reasoning_effort") == "low" && !qwen.has("thinking_budget"))
    val noQwenThinking = buildChatRequestBody(ApiConfig(provider = "aliyun", model = "qwen3.8-max", effort = ThinkingEffort.None), turns)
    check(!noQwenThinking.optBoolean("enable_thinking") && !noQwenThinking.has("reasoning_effort"))
    println("PASS: composer capability policy maps explicit effort to OpenAI/Claude/Qwen; unknown models omit unsupported fields")

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
    check(searchHttpError(429) == "DeepSeek 搜索失败（HTTP 429）")
    check(searchHttpError(500).contains("HTTP 500"))
    println("PASS: provider migration/isolation/clear across restart; provider-specific payloads; SSE mixed and whitespace deltas; search query, URL, success/empty/error fixtures")
}
