package com.dspro.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import java.util.concurrent.atomic.AtomicReference

data class ChatTurn(val role: String, val content: String, val imageBase64: String? = null, val imageMime: String = "image/jpeg")

sealed interface StreamEvent {
    data class Content(val text: String) : StreamEvent
    data class Reasoning(val text: String) : StreamEvent
    data class Failure(val message: String) : StreamEvent
    data object Done : StreamEvent
}

class DeepSeekClient(private val config: ApiConfig) {

    @OptIn(InternalCoroutinesApi::class)
    fun stream(turns: List<ChatTurn>): Flow<StreamEvent> = flow {
        if (config.apiKey.isBlank()) {
            emit(StreamEvent.Failure("还没有填写 API Key"))
            return@flow
        }
        val body = buildChatRequestBody(config, turns)

        var connection: HttpURLConnection? = null
        val activeConnection = AtomicReference<HttpURLConnection?>(null)
        val cancelHook = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { cause ->
            if (cause is CancellationException) activeConnection.getAndSet(null)?.disconnect()
        }
        try {
            connection = (URL(config.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 180_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Connection", "keep-alive")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }
            activeConnection.set(connection)
            currentCoroutineContext().ensureActive()
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val errorStream = connection.errorStream
                val errorText = if (errorStream != null) {
                    errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else ""
                emit(StreamEvent.Failure(parseError(code, errorText)))
                return@flow
            }

            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            reader.use { buffered ->
                var line: String? = buffered.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        if (data.isNotEmpty()) {
                            parseChatSseChunk(data).forEach { emit(it) }
                        }
                    }
                    line = buffered.readLine()
                }
            }
            emit(StreamEvent.Done)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            emit(StreamEvent.Failure(e.message ?: "网络请求失败"))
        } finally {
            cancelHook.dispose()
            activeConnection.getAndSet(null)?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun parseError(code: Int, raw: String): String {
        val message = try {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        }
        return when (code) {
            401 -> "API Key 无效，请检查后重试"
            402 -> "账户余额不足，请先充值"
            429 -> "请求太频繁，请稍后再试"
            else -> message?.takeIf { it.isNotBlank() } ?: "请求失败（HTTP $code）"
        }
    }
}
