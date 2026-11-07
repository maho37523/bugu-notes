package com.dspro.android.data.chatgpt

import com.dspro.android.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private const val NOW = 1_800_000_000_000L
private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
private fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
private fun jwks(): JSONObject {
    val key = keys.public as RSAPublicKey
    return JSONObject().put("keys", JSONArray().put(JSONObject().put("kid", "fixture-key").put("kty", "RSA").put("alg", "RS256")
        .put("n", b64(key.modulus.toByteArray().dropWhile { it == 0.toByte() }.toByteArray()))
        .put("e", b64(key.publicExponent.toByteArray()))))
}
private fun claims(nonce: String, client: String = "issued-client", subject: String = "subject-a") = JSONObject()
    .put("iss", CHATGPT_ISSUER).put("sub", subject).put("aud", JSONArray().put(client)).put("nonce", nonce)
    .put("iat", NOW / 1000 - 10).put("exp", NOW / 1000 + 3600).put("email", "fixture@example.invalid")
private fun signed(claims: JSONObject, header: JSONObject = JSONObject().put("alg", "RS256").put("kid", "fixture-key")): String {
    val body = b64(header.toString().toByteArray()) + "." + b64(claims.toString().toByteArray())
    val signature = Signature.getInstance("SHA256withRSA").apply { initSign(keys.private); update(body.toByteArray()) }.sign()
    return body + "." + b64(signature)
}
private fun params(query: String) = query.split('&').associate {
    val pair = it.split('=', limit = 2)
    URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
}
private fun failure(block: () -> Unit) { check(runCatching(block).isFailure) { "Expected rejection" } }

private class MemoryStore(var value: ChatGptSnapshot = ChatGptSnapshot()) : ChatGptStore {
    var failRead = false
    var failWrite = false
    var saveDelay = 0L
    val saving = CountDownLatch(1)
    override fun load(): ChatGptSnapshot { check(!failRead); return value }
    override fun save(snapshot: ChatGptSnapshot) {
        check(!failWrite); saving.countDown()
        if (saveDelay > 0) Thread.sleep(saveDelay)
        value = snapshot
    }
}
private class Reply(val status: Int = 200, val text: String = "{}", val input: InputStream? = null)
private class FixtureConnection(url: URL, val reply: Reply) : HttpURLConnection(url) {
    val sent = ByteArrayOutputStream()
    @Volatile var disconnected = false
    override fun connect() {}
    override fun usingProxy() = false
    override fun disconnect() { disconnected = true; (reply.input as? BlockingInput)?.release?.countDown() }
    override fun getResponseCode() = reply.status
    override fun getOutputStream() = sent
    override fun getInputStream(): InputStream = reply.input ?: ByteArrayInputStream(reply.text.toByteArray())
    override fun getErrorStream(): InputStream = ByteArrayInputStream(reply.text.toByteArray())
}
private class BlockingInput : InputStream() {
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    override fun read(): Int { entered.countDown(); release.await(); throw java.io.IOException("fixture disconnected") }
}
private class Transport(val reply: (URL) -> Reply) {
    val connections = Collections.synchronizedList(mutableListOf<FixtureConnection>())
    val http = ChatGptHttp { url -> FixtureConnection(url, reply(url)).also { connections.add(it) } }
}
private fun metadata() = JSONObject().put("issuer", CHATGPT_ISSUER)
    .put("authorization_endpoint", "$CHATGPT_ISSUER/api/accounts/authorize")
    .put("token_endpoint", "$CHATGPT_ISSUER/api/accounts/oauth/token")
    .put("jwks_uri", "$CHATGPT_ISSUER/.well-known/jwks.json")
    .put("revocation_endpoint", "$CHATGPT_ISSUER/api/accounts/oauth/revoke")
private fun account(id: String = "a", expired: Boolean = false) = ChatGptCredential(id, "Account $id", "issued-$id", "subject-$id", "$id@example.invalid",
    "identity-$id", "access-$id", "refresh-$id", CHATGPT_SCOPES.split(' ').toSet(),
    if (expired) NOW - 1000 else NOW + 3_600_000, models = listOf(ChatGptModel("model-$id", "Model $id")))
private fun config(id: String = "a") = ApiConfig(provider = "chatgpt", baseUrl = CHATGPT_RESOURCE, model = "model-$id", chatGptAccountId = id)
private fun seeded(vararg accounts: ChatGptCredential) = MemoryStore(ChatGptSnapshot("urn:uuid:00000000-0000-4000-8000-000000000001", accounts.toList()))

private fun cryptoAndCallbacks() {
    check(pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk") == "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
    val valid = signed(claims("nonce"))
    check(verifyChatGptIdToken(valid, jwks(), "issued-client", "nonce", NOW / 1000).subject == "subject-a")
    for (bad in listOf(claims("wrong"), claims("nonce").put("iss", "https://other.invalid"),
        claims("nonce").put("aud", "other-client"), claims("nonce").put("sub", ""),
        claims("nonce").put("exp", NOW / 1000 - 20), claims("nonce").put("iat", NOW / 1000 + 30),
        claims("nonce").put("nbf", NOW / 1000 + 30), claims("nonce").put("aud", JSONArray().put("issued-client").put("other")))) {
        failure { verifyChatGptIdToken(signed(bad), jwks(), "issued-client", "nonce", NOW / 1000) }
    }
    failure { verifyChatGptIdToken(valid.dropLast(5) + "AAAAA", jwks(), "issued-client", "nonce", NOW / 1000) }
    failure { verifyChatGptIdToken(signed(claims("nonce"), JSONObject().put("alg", "none").put("kid", "fixture-key")), jwks(), "issued-client", "nonce", NOW / 1000) }
    failure { verifyChatGptIdToken(signed(claims("nonce"), JSONObject().put("alg", "RS256").put("kid", "unknown")), jwks(), "issued-client", "nonce", NOW / 1000) }
    OAuthAttempt("urn:uuid:fixture").use { a ->
        val query = params(URI(a.authorizationUrl()).rawQuery)
        check(query["client_id"] == "dynamic_agent_client" && query["agent_name_hint"] == "布谷笔记")
        check(query["resource"] == CHATGPT_RESOURCE && CHATGPT_PLAN_SCOPE in query.getValue("scope"))
        check(query["code_challenge"] == pkceChallenge(a.verifier) && URI(a.redirectUri).host == "127.0.0.1")
        failure { a.consume("/auth/callback?state=wrong&code=code&client_id=issued-client") }
        failure { a.consume("/callback?state=${a.state}&code=code&client_id=issued-client") }
        failure { a.consume("/auth/callback?state=${a.state}&state=${a.state}&code=code&client_id=issued-client") }
        check(a.consume("/auth/callback?state=${a.state}&code=code&client_id=issued-client").clientId == "issued-client")
        failure { a.consume("/auth/callback?state=${a.state}&code=code&client_id=issued-client") }
        check("code" !in a.toString() && a.verifier !in a.toString())
    }
    OAuthAttempt("urn:uuid:fixture", "saved-client").use { a ->
        val q = params(URI(a.authorizationUrl()).rawQuery)
        check(q["client_id"] == "saved-client" && "agent_name_hint" !in q)
        failure { a.consume("/auth/callback?state=${a.state}&code=c&client_id=wrong") }
        check(a.consume("/auth/callback?state=${a.state}&code=c").clientId == "saved-client")
    }
    var now = 0L
    OAuthAttempt("urn:uuid:fixture", clock = { now }).use { a ->
        now = 400_000
        failure { a.consume("/auth/callback?state=${a.state}&code=c&client_id=id") }
    }
    OAuthAttempt("urn:uuid:fixture").use { a -> failure { a.consume("/auth/callback?state=${a.state}&error=access_denied") } }
    println("PASS: real RSA signatures, claims, PKCE, strict loopback callback, state, nonce, expiry, client binding and replay rejection")
}

private suspend fun loginLifecycle() = coroutineScope {
    val nonce = AtomicReference("")
    val subject = AtomicReference("subject-a")
    val grant = AtomicReference(CHATGPT_SCOPES)
    val attempts = Collections.synchronizedList(mutableListOf<Map<String, String>>())
    val callbacks = mutableListOf<Job>()
    val failFirstToken = AtomicInteger(1)
    val transport = Transport { url -> when (url.path) {
        "/.well-known/openid-configuration" -> Reply(text = metadata().toString())
        "/.well-known/jwks.json" -> Reply(text = jwks().toString())
        "/api/accounts/oauth/token" -> if (failFirstToken.getAndDecrement() > 0) Reply(400, "{\"error\":\"invalid_grant\"}") else Reply(text = JSONObject()
            .put("id_token", signed(claims(nonce.get(), "issued-client", subject.get())))
            .put("access_token", "fixture-access").put("refresh_token", "fixture-refresh").put("token_type", "Bearer")
            .put("expires_in", 3600).put("scope", grant.get()).toString())
        "/v1/models" -> Reply(text = "{\"models\":[{\"slug\":\"hidden\",\"display_name\":\"Hidden\",\"visibility\":\"hide\"},{\"slug\":\"server-first\",\"display_name\":\"Server first\",\"visibility\":\"list\"}]}")
        else -> error("Unexpected fixture endpoint")
    } }
    val store = MemoryStore()
    val service = ChatGptAccounts(store, transport.http) { NOW }
    val browser: suspend (String) -> Unit = { url ->
        val p = params(URI(url).rawQuery); attempts.add(p); nonce.set(p.getValue("nonce"))
        check("id_token_hint" !in p)
        callbacks += launch(Dispatchers.IO) {
            val redirect = URI(p.getValue("redirect_uri"))
            Socket("127.0.0.1", redirect.port).use { s ->
                s.soTimeout = 3000
                val target = redirect.path + "?" + encodeForm(mapOf("state" to p.getValue("state"), "code" to "fixture-code", "client_id" to "issued-client"))
                s.getOutputStream().write("GET $target HTTP/1.1\r\nHost: 127.0.0.1:${redirect.port}\r\n\r\n".toByteArray())
                val page = s.getInputStream().bufferedReader().readText()
                check(page.startsWith("HTTP/1.1 200") && page.contains("127.0.0.1") && page.contains("返回布谷笔记"))
                check(!page.contains("fixture-code") && !page.contains(p.getValue("state")))
            }
        }
    }
    withTimeout(10_000) { service.signIn(openBrowser = browser) }
    callbacks.forEach { it.join() }
    check(attempts.size == 2 && attempts[1]["client_id"] == "issued-client" && "agent_name_hint" !in attempts[1])
    val stored = store.value.accounts.single()
    check(stored.clientId == "issued-client" && stored.models.map { it.slug } == listOf("server-first"))
    check(stored.planEnabled && store.value.hostId.startsWith("urn:uuid:"))
    check(stored.accessToken !in service.state.value.toString() && stored.idToken !in stored.toString())
    val exchanges = transport.connections.filter { it.url.path.endsWith("/oauth/token") }
    exchanges.forEachIndexed { i, conn ->
        val body = params(conn.sent.toString("UTF-8"))
        check(body["client_id"] == "issued-client" && body["redirect_uri"] == attempts[i]["redirect_uri"])
        check(pkceChallenge(body.getValue("code_verifier")) == attempts[i]["code_challenge"])
        check(!conn.instanceFollowRedirects)
    }
    val prior = store.value
    subject.set("different-subject")
    withTimeout(8000) { service.signIn(stored.id, openBrowser = browser) }
    check(store.value == prior && service.state.value.message.contains("返回账号"))
    subject.set("subject-a"); grant.set("openid profile email")
    withTimeout(8000) { service.signIn(stored.id, requestConsent = true, openBrowser = browser) }
    check(!store.value.accounts.single().planEnabled && store.value.accounts.single().signedIn)
    check(attempts.last()["prompt"] == "consent")
    check(!service.configuration(ApiConfig(provider = "chatgpt", chatGptAccountId = stored.id, model = "server-first")).isConfigured)
    callbacks.forEach { it.join() }
    val cancelledStore = MemoryStore()
    val cancelled = ChatGptAccounts(cancelledStore, transport.http) { NOW }
    val started = CountDownLatch(1)
    val job = launch { cancelled.signIn { started.countDown() } }
    while (started.count > 0) delay(10)
    cancelled.cancelSignIn(); withTimeout(2000) { job.join() }
    check(cancelledStore.value.accounts.isEmpty() && !cancelled.state.value.busy)
    println("PASS: actual local HTTP callback + signed login, issued-client retry, model catalog, account mismatch, missing grant and cancellation preserve identity boundaries")
}

private suspend fun refreshAndLogout() = coroutineScope {
    val calls = AtomicInteger()
    val transport = Transport { url -> when (url.path) {
        "/.well-known/openid-configuration" -> Reply(text = metadata().toString())
        "/api/accounts/oauth/token" -> { calls.incrementAndGet(); Reply(text = "{\"access_token\":\"rotated-access\",\"refresh_token\":\"rotated-refresh\",\"token_type\":\"Bearer\",\"expires_in\":3600}") }
        "/api/accounts/oauth/revoke" -> Reply()
        else -> error("Unexpected fixture endpoint")
    } }
    val store = seeded(account(expired = true), account("b"))
    val service = ChatGptAccounts(store, transport.http) { NOW }
    (1..8).map { async(Dispatchers.Default) { service.credentialFor("a").accessToken } }.awaitAll().forEach { check(it == "rotated-access") }
    check(calls.get() == 1 && store.value.accounts.find { it.id == "a" }?.refreshToken == "rotated-refresh")
    val other = store.value.accounts.find { it.id == "b" }
    var stopped = false
    service.watchSignOut("a") { stopped = true }
    service.signOut("a")
    val loggedOut = store.value.accounts.find { it.id == "a" }!!
    check(stopped && loggedOut.accessToken.isEmpty() && loggedOut.refreshToken.isEmpty() && loggedOut.idToken.isEmpty())
    check(loggedOut.clientId == "issued-a" && store.value.accounts.find { it.id == "b" } == other)
    check(store.value.hostId.isNotBlank())
    val broken = MemoryStore().apply { failRead = true }
    check(!ChatGptAccounts(broken, transport.http) { NOW }.state.value.secureStorageAvailable)
    val writeStore = seeded(account()).apply { failWrite = true }
    val writeService = ChatGptAccounts(writeStore, transport.http) { NOW }
    check(runCatching { writeService.acknowledgeWelcome("a") }.isFailure)
    check(!writeService.configuration(config()).isConfigured)
    val deniedTransport = Transport { url -> if (url.path.contains("openid")) Reply(text = metadata().toString()) else Reply(400, "{\"error\":\"invalid_grant\"}") }
    val deniedStore = seeded(account(expired = true), account("b"))
    val denied = ChatGptAccounts(deniedStore, deniedTransport.http) { NOW }
    check(runCatching { denied.credentialFor("a") }.isFailure)
    check(deniedStore.value.accounts.find { it.id == "a" }?.accessToken == "" && deniedStore.value.accounts.find { it.id == "b" }?.accessToken == "access-b")
    val outage = Transport { url -> if (url.path.contains("openid")) Reply(text = metadata().toString()) else Reply(503) }
    val outageStore = seeded(account())
    val outageService = ChatGptAccounts(outageStore, outage.http) { NOW }
    outageService.signOut("a")
    check(outageStore.value.accounts.single().refreshToken.isBlank() && outageService.state.value.message.contains("未确认远端撤销"))
    val atomicStore = seeded(account(expired = true)).apply { saveDelay = 150 }
    val atomicService = ChatGptAccounts(atomicStore, transport.http) { NOW }
    val refreshing = launch(Dispatchers.Default) { atomicService.credentialFor("a") }
    while (atomicStore.saving.count > 0) delay(5)
    refreshing.cancelAndJoin()
    check(atomicStore.value.accounts.single().refreshToken == "rotated-refresh")
    println("PASS: concurrent refresh rotates once, cancellation preserves the new token, logout/account isolation, terminal refresh failure and storage failure close connections safely")
}

private suspend fun protocolAndTransport() = coroutineScope {
    val request = buildChatGptRequest("catalog-model", listOf(ChatTurn("system", "instructions"), ChatTurn("context", "reference"), ChatTurn("user", "question"), ChatTurn("assistant", "prior answer")))
    check(!request.optBoolean("store") && request.optBoolean("stream"))
    check(request.getJSONArray("input").getJSONObject(0).getString("role") == "developer")
    check(request.getJSONArray("input").getJSONObject(1).getString("role") == "user")
    for (field in listOf("messages", "temperature", "previous_response_id", "max_output_tokens", "tools")) check(!request.has(field))
    failure { buildChatGptRequest("model", listOf(ChatTurn("user", "picture", "fixture-base64"))) }
    check(!request.has("reasoning"))
    val effortRequest = buildChatGptRequest("catalog-model", listOf(ChatTurn("user", "question")), com.dspro.android.data.ThinkingEffort.Medium)
    check(effortRequest.getJSONObject("reasoning").optString("effort") == "medium" && !effortRequest.has("reasoning_effort"))
    val parsed = parseChatGptModel(JSONObject("""{"slug":"new-model","display_name":"New model","supported_reasoning_levels":[{"effort":"low"},{"effort":"medium"},{"effort":"xhigh"}]}"""))
    check(parsed.reasoningLevels == listOf("low", "medium", "xhigh"))
    check(parseChatGptModel(JSONObject("""{"slug":"old-model","display_name":"Old model"}""")).reasoningLevels == null)
    val complete = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"hello\"}\n\ndata: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}\n\n"
    val transport = Transport { Reply(text = complete) }
    val service = ChatGptAccounts(seeded(account().copy(models = listOf(ChatGptModel("model-a", "Model a", listOf("low", "medium", "high"))))), transport.http) { NOW }
    val cfg = service.configuration(config().copy(effort = ThinkingEffort.Medium))
    val events = ChatGptClient(service, cfg).stream(listOf(ChatTurn("user", "question"))).toList()
    check(events == listOf(StreamEvent.Content("hello"), StreamEvent.Done))
    val conn = transport.connections.single()
    check(conn.url.toString() == "$CHATGPT_RESOURCE/responses" && conn.getRequestProperty("Authorization") == "Bearer access-a" && !conn.instanceFollowRedirects && conn.disconnected)
    check(JSONObject(conn.sent.toString("UTF-8")).getJSONObject("reasoning").optString("effort") == "medium")
    val unknown = ChatGptClient(service, cfg.copy(model = "not-in-catalog")).stream(listOf(ChatTurn("user", "question"))).toList()
    check(unknown.single() is StreamEvent.Failure && transport.connections.size == 1)
    val wrong = ChatGptClient(service, cfg.copy(apiKey = "synthetic-api-key")).stream(listOf(ChatTurn("user", "question"))).toList()
    check(wrong.single() is StreamEvent.Failure && transport.connections.size == 1)
    val truncation = ChatGptStreamDecoder()
    check(truncation.accept("{\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}").single() is StreamEvent.Content)
    check(truncation.endOfInput().single() is StreamEvent.Failure)
    val limited = ChatGptStreamDecoder()
    check(limited.accept("{\"type\":\"response.failed\",\"response\":{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}}").single().let { it is StreamEvent.Failure && it.message.contains("用量上限") })
    check(limited.accept("{\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}").isEmpty())
    check(ChatGptStreamDecoder().accept("{\"type\":\"response.incomplete\"}").single() is StreamEvent.Failure)
    check(ChatGptStreamDecoder().accept("not-json").single() is StreamEvent.Failure)
    for (url in listOf("http://auth.openai.com/token", "https://evil.invalid/token", "https://auth.openai.com.evil.invalid/token", "https://user@auth.openai.com/token")) {
        check(runCatching { transport.http.request(url) }.isFailure)
    }
    check(transport.connections.size == 1)
    val blocking = BlockingInput()
    val hanging = Transport { Reply(input = blocking) }
    val hangService = ChatGptAccounts(seeded(account()), hanging.http) { NOW }
    val job = launch { ChatGptClient(hangService, hangService.configuration(config())).stream(listOf(ChatTurn("user", "question"))).toList() }
    while (blocking.entered.count > 0) delay(10)
    withTimeout(2000) { job.cancelAndJoin() }
    check(hanging.connections.single().disconnected)
    val logoutInput = BlockingInput()
    val logoutTransport = Transport { url -> when (url.path) {
        "/v1/responses" -> Reply(input = logoutInput)
        "/.well-known/openid-configuration" -> Reply(text = metadata().toString())
        "/api/accounts/oauth/revoke" -> Reply()
        else -> error("Unexpected fixture endpoint")
    } }
    val logoutService = ChatGptAccounts(seeded(account()), logoutTransport.http) { NOW }
    val receiving = launch { ChatGptClient(logoutService, logoutService.configuration(config())).stream(listOf(ChatTurn("user", "question"))).toList() }
    while (logoutInput.entered.count > 0) delay(10)
    logoutService.signOut("a")
    withTimeout(2000) { receiving.join() }
    check(logoutTransport.connections.first().disconnected && !logoutService.configuration(config()).isConfigured)
    println("PASS: account-bound bearer, official hosts/no redirects, catalog-only models, no API fallback, Responses fields/terminal states and blocking-read cancellation")
}

fun main() = runBlocking {
    cryptoAndCallbacks()
    loginLifecycle()
    refreshAndLogout()
    protocolAndTransport()
}
