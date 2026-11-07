package com.dspro.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

data class DeepSeekSearchResult(val title: String, val url: String, val snippet: String, val summary: String)

class DeepSeekSearchClient(private val config: ApiConfig) {
    @OptIn(InternalCoroutinesApi::class)
    suspend fun search(rawQuery: String): List<DeepSeekSearchResult> = withContext(Dispatchers.IO) {
        require(config.provider == "deepseek") { "联网搜索仅支持 DeepSeek" }
        require(config.secureStorageAvailable) { "安全存储不可用" }
        require(config.apiKey.isNotBlank()) { "请先填写 DeepSeek API Key" }
        val endpoint = searchEndpointFor(config.baseUrl)
        val query = normalizeSearchQuery(rawQuery)
        require(query.isNotBlank()) { "搜索内容不能为空" }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection)
        val activeConnection = AtomicReference<HttpURLConnection?>(connection)
        val cancelHook = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { cause ->
            if (cause is CancellationException) activeConnection.getAndSet(null)?.disconnect()
        }
        try {
            currentCoroutineContext().ensureActive()
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            val body = JSONObject().put("query", query).put("count", 5).put("need_content", true)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException(searchHttpError(code))
            }
            parseSearchResponse(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } finally {
            cancelHook.dispose()
            activeConnection.getAndSet(null)?.disconnect()
        }
    }
}
