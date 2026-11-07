package com.dspro.android.data.chatgpt

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

const val CHATGPT_ISSUER = "https://auth.openai.com"
const val CHATGPT_RESOURCE = "https://api.openai.com/v1"
const val CHATGPT_PLAN_SCOPE = "chatgpt.tokens.use.direct"
const val CHATGPT_USAGE_URL = "https://chatgpt.com/settings/usage"
const val CHATGPT_SCOPES = "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"

class ChatGptProblem(message: String) : Exception(message)
internal class InvalidCallback : Exception()
internal class DeclinedConsent : Exception()

internal fun encodeForm(fields: Map<String, String>): String = fields.entries.joinToString("&") {
    URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
}

internal fun randomOAuthValue(bytes: Int = 32): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(ByteArray(bytes).also { SecureRandom().nextBytes(it) })

internal fun pkceChallenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
    .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

internal class OAuthCode(val code: String, val clientId: String) {
    override fun toString() = "OAuthCode([redacted])"
}

/** Bound only to IPv4 loopback. Nothing sensitive is returned in the browser page. */
internal class OAuthAttempt(
    val hostId: String,
    val savedClientId: String? = null,
    private val idTokenHint: String? = null,
    private val consent: Boolean = false,
    private val clock: () -> Long = System::currentTimeMillis,
    val state: String = randomOAuthValue(),
    val nonce: String = randomOAuthValue(),
    val verifier: String = randomOAuthValue(64),
) : AutoCloseable {
    private val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 1000 }
    private val consumed = AtomicBoolean(false)
    private val deadline = clock() + 5 * 60_000L
    val redirectUri = "http://127.0.0.1:${server.localPort}/auth/callback"

    fun authorizationUrl(): String {
        val fields = linkedMapOf(
            "client_id" to (savedClientId ?: "dynamic_agent_client"),
            "ext_agent_host_id" to hostId,
            "response_type" to "code", "redirect_uri" to redirectUri,
            "scope" to CHATGPT_SCOPES, "resource" to CHATGPT_RESOURCE,
            "state" to state, "nonce" to nonce,
            "code_challenge_method" to "S256", "code_challenge" to pkceChallenge(verifier),
        )
        if (savedClientId == null) fields["agent_name_hint"] = "布谷笔记"
        else if (!idTokenHint.isNullOrBlank()) fields["id_token_hint"] = idTokenHint
        if (consent) fields["prompt"] = "consent"
        return "$CHATGPT_ISSUER/api/accounts/authorize?${encodeForm(fields)}"
    }

    fun consume(target: String): OAuthCode {
        if (clock() >= deadline || consumed.get()) throw InvalidCallback()
        val uri = runCatching { URI(target) }.getOrElse { throw InvalidCallback() }
        if (uri.isAbsolute || uri.rawAuthority != null || uri.path != "/auth/callback" || uri.fragment != null) throw InvalidCallback()
        val fields = linkedMapOf<String, String>()
        try {
            uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.forEach {
                val pair = it.split('=', limit = 2)
                val key = URLDecoder.decode(pair[0], "UTF-8")
                if (fields.containsKey(key)) throw InvalidCallback()
                fields[key] = URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }
        } catch (_: IllegalArgumentException) { throw InvalidCallback() }
        if (!MessageDigest.isEqual(state.toByteArray(), fields["state"].orEmpty().toByteArray())) throw InvalidCallback()
        val returnedClient = fields["client_id"]
        if (savedClientId != null && returnedClient != null && returnedClient != savedClientId) throw InvalidCallback()
        val client = savedClientId ?: returnedClient
        if (fields.containsKey("error")) {
            if (!consumed.compareAndSet(false, true)) throw InvalidCallback()
            throw DeclinedConsent()
        }
        if (client.isNullOrBlank() || client == "dynamic_agent_client" || fields["code"].isNullOrBlank()) throw InvalidCallback()
        if (!consumed.compareAndSet(false, true)) throw InvalidCallback()
        return OAuthCode(fields.getValue("code"), client)
    }

    fun awaitCallback(): OAuthCode {
        while (!server.isClosed && clock() < deadline) {
            val socket = try { server.accept() } catch (_: SocketTimeoutException) { continue }
            catch (_: Exception) { throw CancellationException("登录已取消") }
            socket.use {
                it.soTimeout = 1500
                try {
                    val request = readLine(it, 8192).split(' ')
                    if (request.size != 3 || request[0] != "GET") throw InvalidCallback()
                    var total = 0
                    var host = ""
                    while (true) {
                        val line = readLine(it, 8192)
                        total += line.length
                        if (total > 16384) throw InvalidCallback()
                        if (line.isEmpty()) break
                        if (line.substringBefore(':').equals("host", true)) host = line.substringAfter(':').trim()
                    }
                    if (host != "127.0.0.1:${server.localPort}") throw InvalidCallback()
                    val code = consume(request[1])
                    respond(it, 200, "授权结果已接收，请返回布谷笔记完成登录。")
                    return code
                } catch (_: DeclinedConsent) {
                    respond(it, 200, "授权未完成，请返回布谷笔记。")
                    throw ChatGptProblem("ChatGPT 授权未完成，原有连接保持不变")
                } catch (_: InvalidCallback) { respond(it, 400, "无法验证此回调，请返回应用重试。") }
                catch (_: java.io.IOException) { /* A stray/partial local connection must not consume the attempt. */ }
            }
        }
        if (server.isClosed) throw CancellationException("登录已取消")
        throw ChatGptProblem("登录已超时，请返回应用重新开始")
    }

    private fun readLine(socket: Socket, maximum: Int): String {
        val bytes = ArrayList<Byte>()
        while (bytes.size <= maximum) {
            val b = socket.getInputStream().read()
            if (b < 0) throw InvalidCallback()
            if (b == 10) return bytes.toByteArray().toString(Charsets.US_ASCII).trimEnd('\r')
            bytes.add(b.toByte())
        }
        throw InvalidCallback()
    }

    private fun respond(socket: Socket, status: Int, text: String) {
        runCatching {
            val body = ("<!doctype html><html lang=zh-CN><meta charset=utf-8>" +
                "<meta name=viewport content='width=device-width,initial-scale=1'><title>返回布谷笔记</title>" +
                "<style>body{font:18px system-ui;margin:48px 24px;line-height:1.7;color:#173e35}main{max-width:520px;margin:auto}</style>" +
                "<main><h1>返回布谷笔记</h1><p>$text</p>" +
                "<p>这是手机本机的登录交接页面（127.0.0.1），不是错误。请通过最近任务切回布谷笔记，应用会显示最终登录结果。</p>" +
                "<p>可以关闭此浏览器页面。</p></main></html>").toByteArray()
            socket.getOutputStream().apply {
                write(("HTTP/1.1 $status ${if (status == 200) "OK" else "Bad Request"}\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\n" +
                    "Referrer-Policy: no-referrer\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
                write(body); flush()
            }
        }
    }

    override fun close() { runCatching { server.close() } }
    override fun toString() = "OAuthAttempt([redacted])"
}

internal data class VerifiedChatGptIdentity(val subject: String, val email: String, val name: String)

/** Standard JCA RS256 verification; no identity is trusted before signature verification. */
internal fun verifyChatGptIdToken(token: String, jwks: JSONObject, clientId: String, nonce: String,
                                  now: Long = System.currentTimeMillis() / 1000): VerifiedChatGptIdentity {
    require(token.length <= 128_000)
    val parts = token.split('.')
    require(parts.size == 3 && parts.all { it.matches(Regex("[A-Za-z0-9_-]+")) })
    fun decode(part: String) = Base64.getUrlDecoder().decode(part)
    val header = JSONObject(decode(parts[0]).toString(Charsets.UTF_8))
    require(header.opt("alg") == "RS256" && !header.has("crit"))
    val kid = header.opt("kid") as? String ?: error("Missing key identifier")
    val keys = jwks.getJSONArray("keys")
    val matching = (0 until keys.length()).map { keys.getJSONObject(it) }.filter { it.opt("kid") == kid }
    require(matching.size == 1)
    val key = matching.single()
    require(key.opt("kty") == "RSA" && (!key.has("use") || key.opt("use") == "sig") &&
        (!key.has("alg") || key.opt("alg") == "RS256"))
    val modulus = BigInteger(1, decode(key.getString("n")))
    require(modulus.bitLength() in 2048..8192)
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, BigInteger(1, decode(key.getString("e")))))
    val signature = Signature.getInstance("SHA256withRSA")
    signature.initVerify(publicKey)
    signature.update("${parts[0]}.${parts[1]}".toByteArray(Charsets.US_ASCII))
    require(signature.verify(decode(parts[2])))
    val claims = JSONObject(decode(parts[1]).toString(Charsets.UTF_8))
    require(claims.opt("iss") == CHATGPT_ISSUER && claims.opt("nonce") == nonce)
    val aud = claims.opt("aud")
    val audiences = when (aud) {
        is String -> listOf(aud)
        is JSONArray -> (0 until aud.length()).map { aud.getString(it) }
        else -> emptyList()
    }
    require(clientId in audiences && (audiences.size == 1 || claims.opt("azp") == clientId))
    if (claims.has("azp")) require(claims.opt("azp") == clientId)
    fun epoch(name: String): Long {
        val n = claims.opt(name) as? Number ?: error("Missing time claim")
        require(n.toDouble().isFinite() && n.toDouble() == n.toLong().toDouble() && n.toLong() >= 0)
        return n.toLong()
    }
    require(epoch("exp") > now - 5 && epoch("iat") <= now + 5 && epoch("exp") > epoch("iat"))
    if (claims.has("nbf")) require(epoch("nbf") <= now + 5)
    val subject = claims.opt("sub") as? String ?: error("Missing subject")
    require(subject.isNotBlank())
    return VerifiedChatGptIdentity(subject, claims.opt("email") as? String ?: "", claims.opt("name") as? String ?: "")
}
