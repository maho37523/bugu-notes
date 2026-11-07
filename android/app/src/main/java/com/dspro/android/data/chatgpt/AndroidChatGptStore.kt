package com.dspro.android.data.chatgpt

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/** Independent encrypted storage, excluded from both system backup and device transfer. */
internal class AndroidChatGptStore(context: Context) : ChatGptStore {
    private val app = context.applicationContext
    private val prefs by lazy {
        val master = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(app, "ds_chatgpt_secure", master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    override fun load(): ChatGptSnapshot {
        val text = prefs.getString("registrations", null) ?: return ChatGptSnapshot()
        require(text.length <= 4_000_000)
        val json = JSONObject(text)
        require(json.optInt("format") == 1)
        val rows = json.getJSONArray("accounts")
        require(rows.length() <= 100)
        val accounts = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val scopes = row.getJSONArray("scopes")
            val models = row.getJSONArray("models")
            ChatGptCredential(
                id = row.getString("id"), label = row.getString("label"),
                clientId = row.getString("client_id"), subject = row.getString("subject"), email = row.getString("email"),
                idToken = row.getString("id_token"), accessToken = row.getString("access_token"), refreshToken = row.getString("refresh_token"),
                scopes = (0 until scopes.length()).map { scopes.getString(it) }.toSet(),
                expiresAt = row.optLong("expires_at"), earliestRefreshAt = row.optLong("earliest_refresh_at"),
                models = (0 until models.length()).map { models.getJSONObject(it).let { m -> parseChatGptModel(m) } },
                welcomed = row.optBoolean("welcomed"), revoking = row.optBoolean("revoking"),
            ).also { require(it.id.isNotBlank() && it.clientId.isNotBlank() && it.clientId != "dynamic_agent_client" && it.subject.isNotBlank()) }
        }
        require(accounts.distinctBy { it.id }.size == accounts.size)
        val host = json.getString("host_id")
        require(host.startsWith("urn:uuid:") && host.length < 100)
        return ChatGptSnapshot(host, accounts)
    }

    override fun save(snapshot: ChatGptSnapshot) {
        val json = JSONObject().put("format", 1).put("host_id", snapshot.hostId).put("accounts", JSONArray().apply {
            snapshot.accounts.forEach { account ->
                put(JSONObject().put("id", account.id).put("label", account.label)
                    .put("client_id", account.clientId).put("subject", account.subject).put("email", account.email)
                    .put("id_token", account.idToken).put("access_token", account.accessToken).put("refresh_token", account.refreshToken)
                    .put("scopes", JSONArray(account.scopes.toList())).put("expires_at", account.expiresAt)
                    .put("earliest_refresh_at", account.earliestRefreshAt).put("welcomed", account.welcomed).put("revoking", account.revoking)
                    .put("models", JSONArray().apply { account.models.forEach { put(JSONObject().put("slug", it.slug).put("display_name", it.displayName).apply {
                        it.reasoningLevels?.let { levels -> put("supported_reasoning_levels", JSONArray().apply {
                            levels.forEach { level -> put(JSONObject().put("effort", level)) }
                        }) }
                    }) } }))
            }
        })
        check(prefs.edit().putString("registrations", json.toString()).commit()) { "Secure credential persistence failed" }
    }
}
