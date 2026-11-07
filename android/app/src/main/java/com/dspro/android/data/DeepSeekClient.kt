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

interface ChatClient {
    fun stream(turns: List<ChatTurn>): Flow<StreamEvent>
}

class DeepSeekClient(
    private val config: ApiConfig,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : ChatClient {

    @OptIn(InternalCoroutinesApi::class)
    override fun stream(turns: List<ChatTurn>): Flow<StreamEvent> = flow {
        if (config.provider == "chatgpt") {
            emit(StreamEvent.Failure("ChatGPT 订阅必须使用专用授权连接"))
            return@flow
        }
        requestConfigError(config)?.let {
            emit(StreamEvent.Failure(it))
            return@flow
        }
        val body = try {
            turns.forEach { turn -> turn.imageBase64?.let { image ->
                imageRequestError(config, turn.imageMime ?: "", image.length)?.let { error(it) }
            } }
            if (config.provider == "anthropic") buildClaudeRequestBody(config, turns) else buildChatRequestBody(config, turns)
        } catch (_: Exception) {
            emit(StreamEvent.Failure("消息或图片不符合当前模型的要求"))
            return@flow
        }

        var connection: HttpURLConnection? = null
        val activeConnection = AtomicReference<HttpURLConnection?>(null)
        val cancelHook = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { cause ->
            if (cause is CancellationException) activeConnection.getAndSet(null)?.disconnect()
        }
        try {
            connection = openConnection(URL(config.endpoint)).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 180_000
                doOutput = true
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Connection", "keep-alive")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                if (config.provider == "anthropic") {
                    setRequestProperty("anthropic-version", "2026-10-21")
                    if (config.workspaceId.isNotBlank()) setRequestProperty("anthropic-workspace-id", config.workspaceId.trim())
                }
            }
            activeConnection.set(connection)
            currentCoroutineContext().ensureActive()
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                emit(StreamEvent.Failure(parseError(code)))
                return@flow
            }

            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            val decoder = ChatStreamDecoder(config.provider == "anthropic")
            reader.use { buffered ->
                val data = mutableListOf<String>()
                while (true) {
                    val line = buffered.readLine() ?: break
                    if (line.isEmpty()) {
                        if (data.isNotEmpty()) {
                            val frame = data.joinToString("\n")
                            if (frame.isNotBlank()) decoder.accept(frame).forEach { emit(it) }
                            data.clear()
                            if (decoder.isFinished) break
                        }
                    } else if (line.startsWith("data:")) {
                        val value = line.substring(5)
                        data.add(if (value.startsWith(" ")) value.substring(1) else value)
                    }
                }
                if (data.isNotEmpty()) {
                    val frame = data.joinToString("\n")
                    if (frame.isNotBlank()) decoder.accept(frame).forEach { emit(it) }
                }
                decoder.endOfInput().forEach { emit(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            emit(StreamEvent.Failure("网络请求失败，请检查连接后重试"))
        } finally {
            cancelHook.dispose()
            activeConnection.getAndSet(null)?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun parseError(code: Int): String {
        return when (code) {
            401 -> "API Key 无效，请检查后重试"
            403 -> "当前账号无权访问该服务或模型"
            402 -> "账户余额不足，请先充值"
            429 -> "请求太频繁，请稍后再试"
            else -> "请求失败（HTTP $code）"
        }
    }
}
