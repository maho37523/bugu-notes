package com.dspro.android.data.chatgpt

import com.dspro.android.data.ApiConfig
import com.dspro.android.data.ChatClient
import com.dspro.android.data.ChatTurn
import com.dspro.android.data.StreamEvent
import com.dspro.android.data.effectiveThinkingEffort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class ChatGptClient(private val accounts: ChatGptAccounts, private val config: ApiConfig) : ChatClient {
    @OptIn(InternalCoroutinesApi::class)
    override fun stream(turns: List<ChatTurn>): Flow<StreamEvent> = flow {
        val active = AtomicReference<HttpURLConnection?>(null)
        var signOutHook: AutoCloseable? = null
        val cancelHook = currentCoroutineContext().job.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
            if (it is CancellationException) active.getAndSet(null)?.disconnect()
        }
        try {
            if (config.provider != "chatgpt" || config.apiKey.isNotEmpty()) throw ChatGptProblem("订阅连接配置无效，请重新选择 ChatGPT 账号")
            if (!accounts.configuration(config).isConfigured) throw ChatGptProblem("请先登录并授权使用 ChatGPT 订阅")
            // Reject unsupported input before token refresh or inference.
            if (config.model.isBlank()) throw ChatGptProblem("请选择模型")
            if (turns.any { it.imageBase64 != null }) throw ChatGptProblem("ChatGPT 订阅连接首版支持文本、笔记与文本附件，请先移除图片")
            val credential = accounts.credentialFor(config.chatGptAccountId)
            val model = credential.models.find { it.slug == config.model }
                ?: throw ChatGptProblem("此模型不在所选账号的目录中，请刷新模型后重新选择")
            val body = buildChatGptRequest(config.model, turns, effectiveThinkingEffort(config, model.reasoningLevels))
            val requestJob = currentCoroutineContext().job
            signOutHook = accounts.watchSignOut(credential.id) {
                requestJob.cancel(CancellationException("ChatGPT 账号已退出"))
                active.getAndSet(null)?.disconnect()
            }
            val connection = accounts.http.openConnection(URL("$CHATGPT_RESOURCE/responses")).apply {
                requestMethod = "POST"; doOutput = true
                connectTimeout = 15_000; readTimeout = 180_000
                useCaches = false; instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Authorization", "Bearer ${credential.accessToken}")
            }
            active.set(connection)
            currentCoroutineContext().ensureActive()
            if (!accounts.configuration(config).isConfigured) throw ChatGptProblem("ChatGPT 账号已退出，已停止发送")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) {
                val error = connection.errorStream?.use { readChatGptBody(it).toString(Charsets.UTF_8) }.orEmpty()
                throw chatGptHttpProblem(connection.responseCode, error)
            }
            val decoder = ChatGptStreamDecoder()
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val data = mutableListOf<String>()
                var size = 0
                while (!decoder.finished) {
                    currentCoroutineContext().ensureActive()
                    val line = readLine(reader) ?: break
                    if (line.isEmpty()) {
                        if (data.isNotEmpty()) decoder.accept(data.joinToString("\n")).forEach { emit(it) }
                        data.clear(); size = 0
                    } else if (line.startsWith("data:")) {
                        val value = line.substring(5).removePrefix(" ")
                        size += value.length
                        if (size > 1_048_576) throw ChatGptProblem("ChatGPT 流事件过大，已停止读取")
                        data.add(value)
                    }
                }
                if (!decoder.finished && data.isNotEmpty()) decoder.accept(data.joinToString("\n")).forEach { emit(it) }
                decoder.endOfInput().forEach { emit(it) }
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            emit(StreamEvent.Failure(if (e is ChatGptProblem || e is ChatGptHttpProblem) e.message.orEmpty() else "ChatGPT 连接失败，请检查网络后重试"))
        } finally { cancelHook.dispose(); signOutHook?.close(); active.getAndSet(null)?.disconnect() }
    }.flowOn(Dispatchers.IO)

    private fun readLine(reader: BufferedReader): String? {
        val line = StringBuilder()
        while (line.length <= 1_048_576) {
            val char = reader.read()
            if (char < 0) return if (line.isEmpty()) null else line.toString().trimEnd('\r')
            if (char == 10) return line.toString().trimEnd('\r')
            line.append(char.toChar())
        }
        throw ChatGptProblem("ChatGPT 流事件过大，已停止读取")
    }
}
