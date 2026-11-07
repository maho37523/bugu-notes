package com.dspro.android.testing

import android.app.Activity
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import com.dspro.android.data.ApiConfig
import com.dspro.android.data.ThinkingEffort
import com.dspro.android.data.ApiConfigStore
import com.dspro.android.data.chatgpt.*
import java.io.File
import java.net.Socket
import java.net.URI
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

/** Framework-only runner: installed solely by the guarded disposable-AVD script. No JUnit dependency. */
class ChatGptInstrumentation : Instrumentation() {
    private var syntheticAvd = ""
    private var seedUi = false
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        syntheticAvd = arguments?.getString("synthetic_avd").orEmpty()
        seedUi = arguments?.getString("seed_ui") == "true"
        start()
    }
    override fun onStart() {
        val result = Bundle()
        try {
            check(Build.HARDWARE in setOf("ranchu", "goldfish") && syntheticAvd.startsWith("BuguSmoke"))
            if (seedUi) {
                seedComposerFixture()
                result.putString("stream", "PASS: synthetic composer fixture seeded; never send inference")
                finish(Activity.RESULT_OK, result)
                return
            }
            loopback()
            crypto()
            secureStore()
            result.putString("stream", "PASS: Android loopback callback, RS256 validation and actual encrypted-store isolation")
            finish(Activity.RESULT_OK, result)
        } catch (_: Throwable) {
            result.putString("stream", "FAIL: Android ChatGPT isolated instrumentation; no credentials logged")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
    private fun seedComposerFixture() {
        val store = AndroidChatGptStore(targetContext)
        val original = store.load()
        check(original.accounts.isEmpty())
        val id = "composer-fixture-account"
        store.save(ChatGptSnapshot(original.hostId.ifBlank { "urn:uuid:${UUID.randomUUID()}" }, listOf(
            ChatGptCredential(id, "测试账号", "fixture-client", "fixture-subject", "private-fixture@example.invalid",
                idToken = "synthetic-not-a-token", accessToken = "synthetic-never-send", scopes = setOf(CHATGPT_PLAN_SCOPE),
                expiresAt = System.currentTimeMillis() + 86_400_000, welcomed = true,
                models = listOf(ChatGptModel("fixture-fast", "测试快速模型", listOf("low", "medium", "high")),
                    ChatGptModel("fixture-deep", "测试深思模型", listOf("high", "xhigh", "max")))))))
        val api = ApiConfigStore(targetContext)
        for ((provider, model, base) in listOf(
            Triple("deepseek", "deepseek-flash", "https://api.deepseek.com"),
            Triple("openai", "gpt-5.4-mini", "https://api.openai.com/v1"),
            Triple("anthropic", "claude-sonnet-5-5", "https://api.anthropic.com/v1"),
            Triple("aliyun", "qwen3.8-max", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
            Triple("custom", "fixture-custom", "https://fixture.invalid/v1"),
        )) check(api.save(ApiConfig(provider = provider, model = model, baseUrl = base, apiKey = "synthetic-never-send", effort = ThinkingEffort.Auto, trustedBaseUrl = base)))
        check(api.save(ApiConfig(provider = "chatgpt", model = "fixture-fast", baseUrl = CHATGPT_RESOURCE, chatGptAccountId = id, effort = ThinkingEffort.Medium)))
    }
    private fun loopback() {
        OAuthAttempt("urn:uuid:${UUID.randomUUID()}").use { attempt ->
            val uri = URI(attempt.redirectUri)
            check(uri.host == "127.0.0.1")
            val pending = FutureTask { attempt.awaitCallback() }
            val thread = Thread(pending).apply { start() }
            fun request(state: String): String = Socket(uri.host, uri.port).use { socket ->
                socket.soTimeout = 3000
                val path = uri.path + "?" + encodeForm(mapOf("state" to state, "code" to "fixture-code", "client_id" to "fixture-client"))
                socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: 127.0.0.1:${uri.port}\r\n\r\n".toByteArray())
                socket.getInputStream().bufferedReader().readLine()
            }
            try {
                check(request("wrong-state").contains("400"))
                check(!pending.isDone)
                check(request(attempt.state).contains("200"))
                val code = pending.get(3, TimeUnit.SECONDS)
                check(code.clientId == "fixture-client" && code.code == "fixture-code")
            } finally { attempt.close(); pending.cancel(true); thread.join(3000) }
        }
    }
    private fun crypto() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val pub = pair.public as RSAPublicKey
        val jwks = JSONObject().put("keys", JSONArray().put(JSONObject().put("kid", "test").put("kty", "RSA")
            .put("n", b64(pub.modulus.toByteArray().dropWhile { it == 0.toByte() }.toByteArray())).put("e", b64(pub.publicExponent.toByteArray()))))
        val now = System.currentTimeMillis() / 1000
        val body = b64(JSONObject().put("alg", "RS256").put("kid", "test").toString().toByteArray()) + "." +
            b64(JSONObject().put("iss", CHATGPT_ISSUER).put("aud", "test-client").put("sub", "test-subject")
                .put("nonce", "test-nonce").put("iat", now).put("exp", now + 60).toString().toByteArray())
        val signature = Signature.getInstance("SHA256withRSA").apply { initSign(pair.private); update(body.toByteArray()) }.sign()
        val token = body + "." + b64(signature)
        check(verifyChatGptIdToken(token, jwks, "test-client", "test-nonce").subject == "test-subject")
        check(runCatching { verifyChatGptIdToken(token, jwks, "test-client", "wrong-nonce") }.isFailure)
    }
    private fun secureStore() {
        val context = targetContext
        val store = AndroidChatGptStore(context)
        val original = store.load()
        // Refuse existing authorizations even in an emulator. The host guard is an additional check.
        check(original.accounts.isEmpty())
        val apiBefore = ApiConfigStore(context).load("openai")
        val marker = "fixture-${UUID.randomUUID()}"
        val hostId = original.hostId.ifBlank { "urn:uuid:${UUID.randomUUID()}" }
        val account = ChatGptCredential("fixture-id", "Fixture", "fixture-client", "fixture-subject", "fixture@example.invalid",
            idToken = marker + "-identity", accessToken = marker + "-access", refreshToken = marker + "-refresh",
            scopes = setOf(CHATGPT_PLAN_SCOPE), expiresAt = System.currentTimeMillis() + 60_000)
        try {
            val value = ChatGptSnapshot(hostId, listOf(account))
            store.save(value)
            check(store.load() == value)
            val xml = File(context.applicationInfo.dataDir, "shared_prefs/ds_chatgpt_secure.xml").readText()
            check(marker !in xml && "fixture-subject" !in xml)
            check(ApiConfigStore(context).load("openai") == apiBefore)
        } finally { store.save(original.copy(hostId = hostId)) }
    }
}
