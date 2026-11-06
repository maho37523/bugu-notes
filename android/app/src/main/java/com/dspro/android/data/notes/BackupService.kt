package com.dspro.android.data.notes

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

 data class BackupSummary(val version: Int, val counts: Map<String, Int>, val attachmentCount: Int = 0)

/** Full local user data archive. API configuration lives in secure preferences and is intentionally excluded. */
class BackupService(private val context: Context, private val database: NotesDatabase) {
    private val tables = listOf("notes", "folders", "tags", "note_tags", "note_links", "note_revisions", "projects", "chat_sessions", "chat_messages", "memories", "attachments", "chat_drafts", "temporary_chat_drafts")
    private val maxArchiveBytes = 100L * 1024 * 1024

    suspend fun exportTo(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
      database.withTransaction {
        val db = database.openHelper.readableDatabase
        val root = JSONObject().put("format", "dspro-user-backup").put("version", 1).put("databaseVersion", NotesDatabase.SCHEMA_VERSION)
        val data = JSONObject()
        val counts = linkedMapOf<String, Int>()
        tables.forEach { table ->
            val rows = JSONArray()
            db.query("SELECT * FROM `$table`").use { cursor -> while (cursor.moveToNext()) rows.put(cursor.toJson()) }
            data.put(table, rows); counts[table] = rows.length()
        }
        root.put("data", data)
        val attachmentFiles = linkedMapOf<String, File>()
        val attachmentDir = File(context.filesDir, "attachments")
        require(!attachmentDir.exists() || attachmentDir.isDirectory) { "Attachment directory is invalid" }
        val paths = linkedMapOf<String, String>()
        var totalFileBytes = 0L
        attachmentDir.walkTopDown().filter { it.isFile }.forEachIndexed { index, file ->
            require(index < BackupZipCodec.MAX_ENTRIES) { "Too many attachment files" }
            val size = file.length()
            require(size <= BackupZipCodec.MAX_ENTRY_BYTES && totalFileBytes + size <= maxArchiveBytes) { "Attachment data exceeds backup size limit" }
            totalFileBytes += size
            val name = "files/f${index.toString().padStart(6, '0')}.bin"
            paths[file.canonicalPath] = name
            attachmentFiles[name] = file
        }
        val attachments = data.getJSONArray("attachments")
        for (i in 0 until attachments.length()) {
            val row = attachments.getJSONObject(i)
            val rawPath = row.optString("localPath")
            val archivePath = if (rawPath.isBlank()) null else paths[runCatching { File(rawPath).canonicalPath }.getOrDefault("")]
            require(rawPath.isBlank() || archivePath != null) { "An attachment file is missing or outside the app attachment folder" }
            row.put("archivePath", archivePath ?: JSONObject.NULL)
            row.remove("localPath")
        }
        val drafts = data.getJSONArray("chat_drafts")
        for (i in 0 until drafts.length()) {
            val draft = drafts.getJSONObject(i)
            val list = JSONArray(draft.optString("pendingAttachmentsJson", "[]"))
            for (j in 0 until list.length()) {
                val item = list.getJSONObject(j)
                val rawPath = item.optString("localPath")
                val archivePath = if (rawPath.isBlank()) null else paths[runCatching { File(rawPath).canonicalPath }.getOrDefault("")]
                require(rawPath.isBlank() || archivePath != null) { "A draft attachment file is missing or outside the app attachment folder" }
                item.put("archivePath", archivePath ?: JSONObject.NULL)
                item.remove("localPath")
            }
            draft.put("pendingAttachmentsJson", list.toString())
        }
        rewriteDraftAttachmentPaths(data.getJSONArray("temporary_chat_drafts"), paths)
        val messages = data.getJSONArray("chat_messages")
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            val rawSnapshot = message.optNullableString("contextSnapshot") ?: continue
            ContextSnapshotService.decodeJson(rawSnapshot)
            val snapshot = JSONObject(rawSnapshot)
            val rawPath = snapshot.optNullableString("imagePath")
            if (rawPath != null) {
                val archivePath = paths[runCatching { File(rawPath).canonicalPath }.getOrDefault("")]
                require(archivePath != null) { "A request image snapshot file is missing or outside the app attachment folder" }
                snapshot.put("imagePath", archivePath)
                message.put("contextSnapshot", snapshot.toString())
            }
        }
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open backup destination")
        BackupZipCodec.write(output, root.toString().toByteArray(Charsets.UTF_8), attachmentFiles)
        BackupSummary(1, counts, attachmentFiles.size)
      }
    }

    suspend fun inspect(uri: Uri): BackupSummary = withContext(Dispatchers.IO) { readArchive(uri).let { (root, files) -> summary(root).copy(attachmentCount = files.size) } }

    suspend fun restore(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val (root, files) = readArchive(uri)
        val summary = summary(root).copy(attachmentCount = files.size)
        val db = database.openHelper.writableDatabase
        val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val incomingDir = File(attachmentDir, ".restore-${UUID.randomUUID()}").apply { mkdirs() }
        val staged = mutableMapOf<String, File>()
        try {
            files.forEach { (archivePath, bytes) ->
                val id = archivePath.substringAfter("files/").removeSuffix(".bin")
                require(id.matches(Regex("f[0-9]{6}")))
                val stage = File(incomingDir, "$id.bin")
                stage.writeBytes(bytes); staged[archivePath] = stage
            }
            database.withTransaction {
                val data = root.getJSONObject("data")
                tables.asReversed().forEach { db.execSQL("DELETE FROM `$it`") }
                tables.forEach { table ->
                    val rows = data.getJSONArray(table)
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONObject(i)
                        val columns = db.query("PRAGMA table_info(`$table`)").use { c -> buildSet { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) } }
                        val values = ContentValues()
                        row.keys().forEach { key -> if (key in columns) putJson(values, key, row.opt(key)) }
                        if (table == "attachments") {
                            val archivePath = row.optNullableString("archivePath")
                            val diskName = archivePath?.substringAfter("files/")?.removeSuffix(".bin")
                            values.put("localPath", diskName?.let { File(incomingDir, "$it.bin").absolutePath } ?: "")
                        }
                        if (table == "chat_drafts") {
                            val list = JSONArray(row.optString("pendingAttachmentsJson", "[]"))
                            for (j in 0 until list.length()) {
                                val item = list.getJSONObject(j)
                                val archivePath = item.optNullableString("archivePath")
                                val diskName = archivePath?.substringAfter("files/")?.removeSuffix(".bin")
                                item.put("localPath", diskName?.let { File(incomingDir, "$it.bin").absolutePath } ?: "")
                                item.remove("archivePath")
                            }
                            values.put("pendingAttachmentsJson", list.toString())
                        }
                        if (table == "temporary_chat_drafts") {
                            val list = JSONArray(row.optString("pendingAttachmentsJson", "[]"))
                            for (j in 0 until list.length()) {
                                val item = list.getJSONObject(j)
                                val archivePath = item.optNullableString("archivePath")
                                val diskName = archivePath?.substringAfter("files/")?.removeSuffix(".bin")
                                item.put("localPath", diskName?.let { File(incomingDir, "$it.bin").absolutePath } ?: "")
                                item.remove("archivePath")
                            }
                            values.put("pendingAttachmentsJson", list.toString())
                        }
                        if (table == "chat_messages") {
                            val rawSnapshot = row.optNullableString("contextSnapshot")
                            if (rawSnapshot != null) {
                                ContextSnapshotService.decodeJson(rawSnapshot)
                                val snapshot = JSONObject(rawSnapshot)
                                val archivePath = snapshot.optNullableString("imagePath")
                                val diskName = archivePath?.substringAfter("files/")?.removeSuffix(".bin")
                                if (archivePath != null) snapshot.put("imagePath", File(incomingDir, "$diskName.bin").absolutePath)
                                values.put("contextSnapshot", snapshot.toString())
                            }
                        }
                        check(db.insert(table, 0, values) >= 0) { "Could not restore $table record" }
                    }
                }
                staged.values.forEach { check(it.isFile) { "Could not stage attachment" } }
            }
        } catch (e: Exception) {
            incomingDir.deleteRecursively()
            throw e
        }
        // The DB now points at incomingDir. Cleanup is best-effort and must never roll back/delete committed files.
        runCatching { attachmentDir.listFiles()?.filter { it != incomingDir }?.forEach { it.deleteRecursively() } }
        summary
    }

    private fun readArchive(uri: Uri): Pair<JSONObject, Map<String, ByteArray>> {
        val input = context.contentResolver.openInputStream(uri) ?: error("Could not open backup file")
        val entries = BackupZipCodec.read(input)
        val manifest = entries["manifest.json"] ?: error("Backup manifest missing")
        val root = JSONObject(String(manifest, Charsets.UTF_8))
        require(root.optString("format") == "dspro-user-backup" && root.optInt("version") == 1 && root.optInt("databaseVersion") in 6..NotesDatabase.SCHEMA_VERSION) { "Unsupported backup format or database version" }
        val data = root.getJSONObject("data")
        if (root.optInt("databaseVersion") == 6) {
            data.put("temporary_chat_drafts", JSONArray())
            val notes = data.getJSONArray("notes")
            for (i in 0 until notes.length()) notes.getJSONObject(i).put("archivedAt", JSONObject.NULL)
            val sessions = data.getJSONArray("chat_sessions")
            for (i in 0 until sessions.length()) sessions.getJSONObject(i).apply {
                put("folderId", JSONObject.NULL)
                put("deletedAt", JSONObject.NULL)
            }
        }
        tables.forEach { table -> require(data.opt(table) is JSONArray && data.getJSONArray(table).length() <= 200000) { "Invalid $table records" } }
        val allowed = tables.toSet()
        require(data.keys().asSequence().all { it in allowed }) { "Unexpected data table" }
        validateRelations(data)
        val archivePaths = data.getJSONArray("attachments")
        for (i in 0 until archivePaths.length()) {
            val row = archivePaths.getJSONObject(i)
            row.optNullableString("archivePath")?.let { path ->
                require(path.matches(Regex("files/f[0-9]{6}\\.bin")) && path in entries) { "Attachment data is missing" }
            }
        }
        val drafts = data.getJSONArray("chat_drafts")
        for (i in 0 until drafts.length()) {
            val list = JSONArray(drafts.getJSONObject(i).optString("pendingAttachmentsJson", "[]"))
            for (j in 0 until list.length()) list.getJSONObject(j).optNullableString("archivePath")?.let { path ->
                require(path.matches(Regex("files/f[0-9]{6}\\.bin")) && path in entries) { "Draft attachment data is missing" }
            }
        }
        val temporaryDrafts = data.getJSONArray("temporary_chat_drafts")
        for (i in 0 until temporaryDrafts.length()) {
            val list = JSONArray(temporaryDrafts.getJSONObject(i).optString("pendingAttachmentsJson", "[]"))
            for (j in 0 until list.length()) list.getJSONObject(j).optNullableString("archivePath")?.let { path ->
                require(path.matches(Regex("files/f[0-9]{6}\\.bin")) && path in entries) { "Temporary draft attachment data is missing" }
            }
        }
        val messages = data.getJSONArray("chat_messages")
        for (i in 0 until messages.length()) {
            val rawSnapshot = messages.getJSONObject(i).optNullableString("contextSnapshot") ?: continue
            val path = ContextSnapshotService.decodeJson(rawSnapshot).imagePath ?: continue
            require(path.matches(Regex("files/f[0-9]{6}\\.bin")) && path in entries) { "Request image snapshot data is missing" }
        }
        return root to entries.filterKeys { it.startsWith("files/") }
    }

    private fun summary(root: JSONObject): BackupSummary {
        val data = root.getJSONObject("data")
        return BackupSummary(1, tables.associateWith { data.getJSONArray(it).length() }, data.getJSONArray("attachments").length())
    }

    private fun validateRelations(data: JSONObject) {
        fun ids(table: String, field: String): Set<String> {
            val rows = data.getJSONArray(table)
            val values = (0 until rows.length()).map { rows.getJSONObject(it).get(field).toString() }
            require(values.distinct().size == values.size) { "Duplicate $table keys" }
            return values.toSet()
        }
        fun nullable(row: JSONObject, key: String): String? = row.optNullableString(key)
        fun checkRefs(table: String, key: String, valid: Set<String>) {
            val rows = data.getJSONArray(table)
            for (i in 0 until rows.length()) {
                val id = nullable(rows.getJSONObject(i), key)
                require(id == null || id in valid) { "Invalid $table.$key reference" }
            }
        }
        val projectIds = ids("projects", "id")
        val folderIds = ids("folders", "id")
        val tagIds = ids("tags", "id")
        val noteIds = ids("notes", "id")
        val sessionIds = ids("chat_sessions", "id")
        ids("temporary_chat_drafts", "projectScope")
        checkRefs("notes", "projectId", projectIds)
        checkRefs("notes", "folderId", folderIds)
        checkRefs("chat_sessions", "projectId", projectIds)
        checkRefs("memories", "projectId", projectIds)
        checkRefs("note_revisions", "noteId", noteIds)
        checkRefs("note_links", "fromNoteId", noteIds)
        checkRefs("note_links", "toNoteId", noteIds)
        checkRefs("chat_messages", "sessionId", sessionIds)
        checkRefs("chat_drafts", "sessionId", sessionIds)
        checkRefs("temporary_chat_drafts", "folderId", folderIds)
        checkRefs("note_tags", "noteId", noteIds)
        checkRefs("note_tags", "tagId", tagIds)
        val cross = data.getJSONArray("note_tags")
        val pairs = (0 until cross.length()).map { "${cross.getJSONObject(it).getString("noteId")}\u0000${cross.getJSONObject(it).getString("tagId")}" }
        require(pairs.distinct().size == pairs.size) { "Duplicate note tag references" }
        val drafts = data.getJSONArray("chat_drafts")
        for (i in 0 until drafts.length()) {
            val draft = drafts.getJSONObject(i)
            val noteIdsJson = JSONArray(draft.optString("selectedNoteIdsJson", "[]"))
            require(noteIdsJson.length() <= ContextSnapshotService.MAX_NOTES) { "Too many draft note references" }
            val pending = JSONArray(draft.optString("pendingAttachmentsJson", "[]"))
            require(pending.length() <= 8) { "Too many draft attachments" }
        }
        val temporary = data.getJSONArray("temporary_chat_drafts")
        for (i in 0 until temporary.length()) {
            val draft = temporary.getJSONObject(i)
            val expectedScope = draft.optNullableString("projectId")?.let { "project:$it" } ?: "global"
            require(draft.optString("projectScope") == expectedScope) { "Invalid temporary draft project scope" }
            require(JSONArray(draft.optString("selectedNoteIdsJson", "[]")).length() <= ContextSnapshotService.MAX_NOTES) { "Too many draft note references" }
            require(JSONArray(draft.optString("pendingAttachmentsJson", "[]")).length() <= 8) { "Too many draft attachments" }
        }
    }
    private fun rewriteDraftAttachmentPaths(rows: JSONArray, paths: Map<String, String>) {
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val list = JSONArray(row.optString("pendingAttachmentsJson", "[]"))
            for (j in 0 until list.length()) {
                val item = list.getJSONObject(j)
                val rawPath = item.optString("localPath")
                val archivePath = if (rawPath.isBlank()) null else paths[runCatching { File(rawPath).canonicalPath }.getOrDefault("")]
                require(rawPath.isBlank() || archivePath != null) { "A temporary draft attachment file is missing or outside the app attachment folder" }
                item.put("archivePath", archivePath ?: JSONObject.NULL)
                item.remove("localPath")
            }
            row.put("pendingAttachmentsJson", list.toString())
        }
    }

    private fun Cursor.toJson(): JSONObject = JSONObject().also { row ->
        for (i in 0 until columnCount) if (!isNull(i)) row.put(getColumnName(i), when (getType(i)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(i)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
            Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(getBlob(i), android.util.Base64.NO_WRAP)
            else -> getString(i)
        }) else row.put(getColumnName(i), JSONObject.NULL)
    }
    private fun putJson(values: ContentValues, key: String, value: Any?) {
        when (value) {
            null, JSONObject.NULL -> values.putNull(key)
            is Boolean -> values.put(key, if (value) 1 else 0)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Double -> values.put(key, value)
            else -> values.put(key, value.toString())
        }
    }
    private fun JSONObject.optNullableString(key: String): String? = opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()
}
