package com.dspro.android.data.notes

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class FolderScopeRecord(
    val id: String,
    val name: String,
    val sort: Int,
    val archivedAt: Long?,
)

data class FolderScopeReference(
    val table: String,
    val rowId: String,
    val projectId: String?,
    val folderId: String?,
)

data class ScopedFolderRecord(
    val id: String,
    val name: String,
    val sort: Int,
    val archivedAt: Long?,
    val projectId: String?,
    val parentId: String? = null,
)

data class FolderScopePlan(
    val folders: List<ScopedFolderRecord>,
    val referenceFolderIds: Map<String, String?>,
)

/** Pure deterministic rules shared by Room migration, backup conversion, and synthetic tests. */
object FolderScopeMigration {
    private const val HASH_PREFIX = "folder-v1-"
    private val unsignedUtf8Comparator = Comparator<String> { left, right ->
        val a = left.toByteArray(StandardCharsets.UTF_8)
        val b = right.toByteArray(StandardCharsets.UTF_8)
        var result = 0
        for (i in 0 until minOf(a.size, b.size)) {
            result = (a[i].toInt() and 0xff).compareTo(b[i].toInt() and 0xff)
            if (result != 0) break
        }
        if (result != 0) result else a.size.compareTo(b.size)
    }

    fun splitLegacy(
        folders: List<FolderScopeRecord>,
        projectIds: Set<String>,
        references: List<FolderScopeReference>,
    ): FolderScopePlan {
        require(folders.map { it.id }.distinct().size == folders.size) { "Duplicate legacy folder IDs" }
        val foldersById = folders.associateBy { it.id }
        references.forEach { reference ->
            val orphanTemporaryProject = reference.table == "temporary_chat_drafts" && reference.projectId != null && reference.projectId !in projectIds
            require(reference.projectId == null || reference.projectId in projectIds || orphanTemporaryProject) {
                "Invalid ${reference.table} project scope"
            }
            require(reference.folderId == null || reference.folderId in foldersById) {
                "Invalid ${reference.table}.folderId reference"
            }
            require(reference.table != "temporary_chat_drafts" ||
                reference.rowId == if (reference.projectId == null) "global" else "project:${reference.projectId}") {
                "Invalid temporary draft scope key"
            }
        }
        val duplicateReferences = references.groupBy { referenceKey(it.table, it.rowId) }.filterValues { it.size > 1 }
        require(duplicateReferences.isEmpty()) { "Duplicate content reference keys" }

        val scopesByFolder = references.asSequence()
            .filter { it.folderId != null }
            .groupBy({ it.folderId!! }, { effectiveProjectScope(it, projectIds) })
            .mapValues { (_, scopes) -> scopes.distinct().sortedWith(scopeComparator) }
        val reservedIds = folders.mapTo(mutableSetOf()) { it.id }
        val output = mutableListOf<ScopedFolderRecord>()
        val mapping = mutableMapOf<String, String?>()

        folders.sortedWith { a, b -> unsignedUtf8Comparator.compare(a.id, b.id) }.forEach { folder ->
            val scopes = scopesByFolder[folder.id].orEmpty().ifEmpty { listOf(null) }
            val retainScope = when {
                null in scopes -> null
                else -> scopes.first()
            }
            val idsByScope = mutableMapOf<String?, String>()
            for (scope in scopes) {
                val newId = if (scope == retainScope) folder.id else cloneId(folder.id, requireNotNull(scope), reservedIds)
                if (newId != folder.id) reservedIds += newId
                idsByScope[scope] = newId
                output += ScopedFolderRecord(folder.id.let { newId }, folder.name, folder.sort, folder.archivedAt, scope)
            }
            references.filter { it.folderId == folder.id }.forEach { reference ->
                mapping[referenceKey(reference.table, reference.rowId)] = idsByScope[effectiveProjectScope(reference, projectIds)]
            }
        }
        references.filter { it.folderId == null }.forEach { mapping[referenceKey(it.table, it.rowId)] = null }
        return FolderScopePlan(
            folders = output,
            referenceFolderIds = mapping,
        )
    }

    fun migrateLegacyDatabase(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN projectId TEXT")
        db.execSQL("ALTER TABLE folders ADD COLUMN parentId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_folders_projectId_parentId ON folders(projectId, parentId)")

        val projectIds = mutableSetOf<String>()
        db.query("SELECT id FROM projects").use { cursor ->
            while (cursor.moveToNext()) projectIds += cursor.getString(0)
        }
        val folders = mutableListOf<FolderScopeRecord>()
        db.query("SELECT id, name, sort, archivedAt FROM folders").use { cursor ->
            while (cursor.moveToNext()) folders += FolderScopeRecord(
                id = cursor.getString(0),
                name = cursor.getString(1),
                sort = cursor.getString(2).toInt(),
                archivedAt = if (cursor.isNull(3)) null else cursor.getLong(3),
            )
        }
        val references = mutableListOf<FolderScopeReference>()
        db.query("SELECT id, projectId, folderId FROM notes").use { cursor ->
            while (cursor.moveToNext()) references += FolderScopeReference(
                "notes", cursor.getString(0), cursor.stringOrNull(1), cursor.stringOrNull(2),
            )
        }
        db.query("SELECT id, projectId, folderId FROM chat_sessions").use { cursor ->
            while (cursor.moveToNext()) references += FolderScopeReference(
                "chat_sessions", cursor.getLong(0).toString(), cursor.stringOrNull(1), cursor.stringOrNull(2),
            )
        }
        // Older drafts can outlive their folder. Keep all draft data and detach only
        // that stale navigation reference; backup/v9 validation stays strict.
        val legacyFolderIds = folders.mapTo(hashSetOf()) { it.id }
        db.query("SELECT projectScope, projectId, folderId FROM temporary_chat_drafts").use { cursor ->
            while (cursor.moveToNext()) references += FolderScopeReference(
                "temporary_chat_drafts", cursor.getString(0), cursor.stringOrNull(1),
                cursor.stringOrNull(2)?.takeIf { it in legacyFolderIds },
            )
        }
        val plan = splitLegacy(folders, projectIds, references)

        db.execSQL("DELETE FROM folders")
        val insertFolder = db.compileStatement(
            "INSERT INTO folders(id, name, sort, archivedAt, projectId, parentId) VALUES(?, ?, ?, ?, ?, ?)"
        )
        plan.folders.forEach { folder ->
            insertFolder.clearBindings()
            insertFolder.bindString(1, folder.id)
            insertFolder.bindString(2, folder.name)
            insertFolder.bindLong(3, folder.sort.toLong())
            if (folder.archivedAt == null) insertFolder.bindNull(4) else insertFolder.bindLong(4, folder.archivedAt)
            if (folder.projectId == null) insertFolder.bindNull(5) else insertFolder.bindString(5, folder.projectId)
            if (folder.parentId == null) insertFolder.bindNull(6) else insertFolder.bindString(6, folder.parentId)
            insertFolder.executeInsert()
        }
        references.forEach { reference ->
            val newFolderId = plan.referenceFolderIds[referenceKey(reference.table, reference.rowId)]
            val keyColumn = if (reference.table == "temporary_chat_drafts") "projectScope" else "id"
            val update = db.compileStatement("UPDATE ${reference.table} SET folderId = ? WHERE $keyColumn = ?")
            if (newFolderId == null) update.bindNull(1) else update.bindString(1, newFolderId)
            if (reference.table == "chat_sessions") update.bindLong(2, reference.rowId.toLong())
            else update.bindString(2, reference.rowId)
            update.executeUpdateDelete()
        }
        validateScoped(plan.folders, projectIds, references.map { reference ->
            reference.copy(folderId = plan.referenceFolderIds[referenceKey(reference.table, reference.rowId)])
        })
    }

    private fun Cursor.stringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

    fun validateScoped(
        folders: List<ScopedFolderRecord>,
        projectIds: Set<String>,
        references: List<FolderScopeReference>,
    ) {
        require(folders.map { it.id }.distinct().size == folders.size) { "Duplicate folder IDs" }
        val byId = folders.associateBy { it.id }
        folders.forEach { folder ->
            require(folder.projectId == null || folder.projectId in projectIds) { "Invalid folder project scope" }
            val parent = folder.parentId?.let { byId[it] ?: error("Missing folder parent") }
            require(parent == null || parent.projectId == folder.projectId) { "Folder parent belongs to another project" }
        }
        val state = mutableMapOf<String, Int>()
        folders.forEach { folder ->
            if (state[folder.id] == 2) return@forEach
            val path = mutableListOf<String>()
            var current: String? = folder.id
            while (current != null && state[current] != 2) {
                if (state[current] == 1) error("Folder tree contains a cycle")
                state[current] = 1
                path += current
                current = byId.getValue(current).parentId
            }
            path.forEach { state[it] = 2 }
        }
        references.forEach { reference ->
            val orphanTemporaryProject = reference.table == "temporary_chat_drafts" && reference.projectId != null && reference.projectId !in projectIds
            require(reference.projectId == null || reference.projectId in projectIds || orphanTemporaryProject) {
                "Invalid ${reference.table} project scope"
            }
            if (reference.table == "temporary_chat_drafts") {
                require(reference.rowId == if (reference.projectId == null) "global" else "project:${reference.projectId}") {
                    "Invalid temporary draft scope key"
                }
            }
            val folder = reference.folderId?.let { byId[it] ?: error("Invalid ${reference.table}.folderId reference") }
            val expectedScope = effectiveProjectScope(reference, projectIds)
            require(folder == null || folder.projectId == expectedScope) {
                "${reference.table}.folderId belongs to another project (folder=${folder?.projectId}, reference=$expectedScope)"
            }
        }
    }

    fun visibleIds(folders: List<ScopedFolderRecord>, expandedIds: Set<String>): List<String> {
        val children = folders.groupBy { it.parentId }
        val comparator = compareBy<ScopedFolderRecord>({ it.sort }, { it.name.lowercase() }, { it.id })
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<ScopedFolderRecord>()
        children[null].orEmpty().sortedWith(comparator).asReversed().forEach(stack::addLast)
        while (stack.isNotEmpty()) {
            val folder = stack.removeLast()
            if (!visited.add(folder.id)) continue
            result += folder.id
            if (folder.id in expandedIds) children[folder.id].orEmpty().sortedWith(comparator).asReversed().forEach(stack::addLast)
        }
        return result
    }

    private val scopeComparator = Comparator<String?> { left, right ->
        when {
            left == null && right == null -> 0
            left == null -> -1
            right == null -> 1
            else -> unsignedUtf8Comparator.compare(left, right)
        }
    }

    fun referenceKey(table: String, rowId: String) = "$table\u0000$rowId"

    private fun effectiveProjectScope(reference: FolderScopeReference, projectIds: Set<String>): String? =
        reference.projectId?.takeIf { it in projectIds }

    private fun cloneId(oldId: String, projectId: String, usedIds: Set<String>): String {
        var retry = 0L
        while (true) {
            val digest = MessageDigest.getInstance("SHA-256")
            listOf("bugu-folder-v1", oldId, projectId, retry.toString()).forEach { value ->
                val bytes = value.toByteArray(StandardCharsets.UTF_8)
                digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
                digest.update(bytes)
            }
            val candidate = HASH_PREFIX + digest.digest().joinToString("") { "%02x".format(it) }
            if (candidate !in usedIds) return candidate
            retry++
            check(retry >= 0L) { "Folder ID collision retry overflow" }
        }
    }
}
