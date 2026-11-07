package com.dspro.android.data.notes

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.JdbcDatabase
import com.dspro.android.data.ChatMessage
import com.dspro.android.data.Role
import com.dspro.android.data.MsgStatus
import com.dspro.android.data.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

private fun verifyFolderScopeRules() {
    val collisionCandidate = "folder-v1-e9ca76675865f1bf44351c0996b19a538c2452414e13672083ff2c8e07ebbf15"
    val refs = listOf(
        FolderScopeReference("notes", "nA", "A", "D"),
        FolderScopeReference("chat_sessions", "20", "B", "D"),
        FolderScopeReference("temporary_chat_drafts", "global", null, "D"),
        FolderScopeReference("temporary_chat_drafts", "project:gone", "gone", "D"),
    )
    val plan = FolderScopeMigration.splitLegacy(
        folders = listOf(FolderScopeRecord("D", "Shared", 4, 99L), FolderScopeRecord(collisionCandidate, "Reserved", 7, null), FolderScopeRecord("E", "Empty", 8, null)),
        projectIds = setOf("A", "B"),
        references = refs,
    )
    val byId = plan.folders.associateBy { it.id }
    check(plan.folders.size == 5)
    check(byId.getValue("D").projectId == null && byId.getValue("D").archivedAt == 99L)
    check(byId.getValue("E").projectId == null)
    check(plan.referenceFolderIds.getValue(FolderScopeMigration.referenceKey("notes", "nA")) == "folder-v1-019d9946f75d20153ae5d206031c6edf5d8e9788180560576ea4bfad7ff4f7d4")
    check(plan.referenceFolderIds.getValue(FolderScopeMigration.referenceKey("chat_sessions", "20")) == "folder-v1-9251184c50b4bc9c4bc2dea098ba7102f920963673dfced7aa0148a04f5fb5e5")
    check(plan.referenceFolderIds.getValue(FolderScopeMigration.referenceKey("temporary_chat_drafts", "global")) == "D")
    check(plan.referenceFolderIds.getValue(FolderScopeMigration.referenceKey("temporary_chat_drafts", "project:gone")) == "D")

    val tree = listOf(
        ScopedFolderRecord("root", "Root", 1, null, "A"),
        ScopedFolderRecord("child", "Child", 1, null, "A", "root"),
        ScopedFolderRecord("sibling", "Sibling", 2, null, "A"),
    )
    check(FolderScopeMigration.visibleIds(tree, setOf("root")) == listOf("root", "child", "sibling"))
    check(FolderScopeMigration.visibleIds(tree, emptySet()) == listOf("root", "sibling"))
    FolderScopeMigration.validateScoped(tree, setOf("A", "B"), emptyList())
    check(runCatching {
        FolderScopeMigration.validateScoped(tree.map { if (it.id == "child") it.copy(parentId = "other") else it }, setOf("A", "B"), emptyList())
    }.isFailure)
    check(runCatching {
        FolderScopeMigration.validateScoped(tree.map { when (it.id) { "root" -> it.copy(parentId = "child"); "child" -> it.copy(parentId = "root"); else -> it } }, setOf("A", "B"), emptyList())
    }.isFailure)
}

private fun createSchema8ForFolderMigration(connection: java.sql.Connection, schemaPath: String) {
    val jdbc = androidx.sqlite.db.JdbcDatabase(connection)
    File(schemaPath).readText().split(';').map(String::trim).filter(String::isNotEmpty).forEach { sql ->
        if (sql.contains("CREATE TABLE IF NOT EXISTS `folders`")) return@forEach
        if (sql.contains("index_folders_projectId_parentId")) return@forEach
        jdbc.execSQL(sql)
    }
    jdbc.execSQL("CREATE TABLE folders (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sort INTEGER NOT NULL, archivedAt INTEGER)")
}

private fun verifyRoomFolderMigration(schemaPath: String) {
    Class.forName("org.sqlite.JDBC")
    val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    createSchema8ForFolderMigration(connection, schemaPath)
    val db = androidx.sqlite.db.JdbcDatabase(connection)
    db.execSQL("INSERT INTO projects(id,name,icon,color,instructions,memoryMode,archivedAt,createdAt,updatedAt) VALUES('A','A','📁',0,'','project_only',NULL,1,1)")
    db.execSQL("INSERT INTO projects(id,name,icon,color,instructions,memoryMode,archivedAt,createdAt,updatedAt) VALUES('B','B','📁',0,'','project_only',NULL,1,1)")
    db.execSQL("INSERT INTO folders(id,name,sort,archivedAt) VALUES('D','Shared',4,99)")
    db.execSQL("INSERT INTO folders(id,name,sort,archivedAt) VALUES('E','Empty',8,NULL)")
    db.execSQL("INSERT INTO notes(id,title,content,folderId,projectId,pinned,createdAt,updatedAt,deletedAt,archivedAt) VALUES('nA','A note','body','D','A',0,1,2,3,NULL)")
    db.execSQL("INSERT INTO chat_sessions(id,projectId,title,createdAt,updatedAt,archivedAt,titleManuallySet,folderId,deletedAt) VALUES(20,'B','B chat',1,2,4,1,'D',5)")
    db.execSQL("INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,updatedAt) VALUES('global',NULL,'D','global draft',3)")
    db.execSQL("INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,updatedAt) VALUES('project:gone','gone','D','orphan draft',4)")
    FolderScopeMigration.migrateLegacyDatabase(db)
    fun row(sql: String): List<String?> = connection.createStatement().use { statement ->
        statement.executeQuery(sql).use { rs -> check(rs.next()); (1..rs.metaData.columnCount).map { rs.getString(it) } }
    }
    val aFolder = row("SELECT folderId FROM notes WHERE id='nA'").single()
    val bFolder = row("SELECT folderId FROM chat_sessions WHERE id=20").single()
    check(aFolder == "folder-v1-e9ca76675865f1bf44351c0996b19a538c2452414e13672083ff2c8e07ebbf15")
    check(bFolder == "folder-v1-9251184c50b4bc9c4bc2dea098ba7102f920963673dfced7aa0148a04f5fb5e5")
    check(row("SELECT folderId FROM temporary_chat_drafts WHERE projectScope='global'").single() == "D")
    check(row("SELECT folderId FROM temporary_chat_drafts WHERE projectScope='project:gone'").single() == "D")
    check(row("SELECT projectId FROM folders WHERE id='D'").single() == null)
    check(row("SELECT projectId FROM folders WHERE id='$aFolder'").single() == "A")
    check(row("SELECT projectId FROM folders WHERE id='$bFolder'").single() == "B")
    check(row("SELECT projectId FROM folders WHERE id='E'").single() == null)
    check(row("SELECT deletedAt FROM notes WHERE id='nA'").single() == "3")
    check(row("SELECT archivedAt || ':' || deletedAt FROM chat_sessions WHERE id=20").single() == "4:5")
    check(row("SELECT text FROM temporary_chat_drafts WHERE projectScope='project:gone'").single() == "orphan draft")
    println("PASS: actual schema-8 migration splits shared/empty folders, remaps hidden content and retains orphan drafts")

    val invalid = DriverManager.getConnection("jdbc:sqlite::memory:")
    createSchema8ForFolderMigration(invalid, schemaPath)
    val invalidDb = androidx.sqlite.db.JdbcDatabase(invalid)
    invalidDb.execSQL("INSERT INTO folders(id,name,sort,archivedAt) VALUES('D','Shared',4,NULL)")
    invalidDb.execSQL("INSERT INTO notes(id,title,content,folderId,projectId,pinned,createdAt,updatedAt) VALUES('bad','Bad','body','D','missing',0,1,1)")
    invalidDb.execSQL("INSERT INTO temporary_chat_drafts(projectScope,folderId,text,updatedAt) VALUES('global','deleted-folder','preserve on rollback',7)")
    invalid.autoCommit = false
    check(runCatching { FolderScopeMigration.migrateLegacyDatabase(invalidDb) }.isFailure)
    invalid.rollback()
    invalid.autoCommit = true
    val folderColumns = invalidDb.query("PRAGMA table_info(folders)").let { cursor ->
        buildSet { cursor.use { while (it.moveToNext()) add(it.getString(1)) } }
    }
    check("projectId" !in folderColumns && invalidDb.query("SELECT id FROM folders").use { it.moveToNext() })
    check(invalidDb.query("SELECT folderId,text FROM temporary_chat_drafts").use {
        it.moveToNext() && it.getString(0) == "deleted-folder" && it.getString(1) == "preserve on rollback"
    })
    println("PASS: invalid project scope aborts migration and transaction rollback preserves the schema-8 folder table")
}

private fun verifyMissingDraftFolderMigration(schemaPath: String) {
    val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    createSchema8ForFolderMigration(connection, schemaPath)
    val db = androidx.sqlite.db.JdbcDatabase(connection)
    db.execSQL("INSERT INTO projects(id,name,icon,color,instructions,memoryMode,createdAt,updatedAt) VALUES('A','A','folder',0,'','manual',1,1)")
    db.execSQL("INSERT INTO folders VALUES('D','Valid folder',0,NULL)")
    for ((scope, project) in listOf("global" to "NULL", "project:A" to "'A'", "project:gone" to "'gone'")) {
        db.execSQL("INSERT INTO temporary_chat_drafts(projectScope,projectId,folderId,text,quoteMessageId,quoteText,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt) VALUES('$scope',$project,'deleted-folder','draft $scope',42,'quote','[\"n1\"]','[{\"id\":\"image\",\"localPath\":\"/synthetic/image.png\"}]',123)")
    }
    fun rows(sql: String) = connection.createStatement().use { statement ->
        statement.executeQuery(sql).use { rs -> buildList {
            while (rs.next()) add((1..rs.metaData.columnCount).map { rs.getString(it) })
        } }
    }
    val fields = "projectScope,projectId,text,quoteMessageId,quoteText,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt"
    val before = rows("SELECT $fields FROM temporary_chat_drafts ORDER BY projectScope")
    connection.autoCommit = false
    FolderScopeMigration.migrateLegacyDatabase(db)
    connection.commit()
    check(rows("SELECT $fields FROM temporary_chat_drafts ORDER BY projectScope") == before)
    check(rows("SELECT folderId FROM temporary_chat_drafts").all { it.single() == null })
    check(rows("SELECT id,name FROM folders") == listOf(listOf("D", "Valid folder")))
    // Only the legacy SQL reader repairs stale draft navigation. Shared backup/v9 rules remain strict.
    val dangling = FolderScopeReference("temporary_chat_drafts", "global", null, "deleted-folder")
    check(runCatching { FolderScopeMigration.splitLegacy(emptyList(), emptySet(), listOf(dangling)) }.isFailure)
    check(runCatching { FolderScopeMigration.validateScoped(emptyList(), emptySet(), listOf(dangling)) }.isFailure)
    connection.close()
    println("PASS: legacy dangling draft folders detach to scope root without changing draft content, quotes, references or attachment JSON; backup/v9 validation remains strict")
}

fun main(args:Array<String>) = runBlocking {
 verifyFolderScopeRules()
 println("PASS: deterministic folder split reserves legacy IDs, retains global IDs, and preserves orphan draft folder scope")
 verifyRoomFolderMigration(args.single())
 verifyMissingDraftFolderMigration(args.single())
 Class.forName("org.sqlite.JDBC")
 val connection=DriverManager.getConnection("jdbc:sqlite::memory:")
 val jdbc=JdbcDatabase(connection)
 File(args.single()).readText().split(';').filter { it.isNotBlank() }.forEach { jdbc.execSQL(it) }
 val database=NotesDatabase(Helper(jdbc))
 val notes=NotesRepository(database)
 val chats=ChatRepository(database)
 fun count(table:String)=connection.createStatement().use { it.executeQuery("SELECT COUNT(*) FROM `$table`").use { rs -> rs.next();rs.getInt(1) } }
 val folder=notes.createFolder("共同文件夹")
 val newId="new-note"
 check(notes.upsertNewNoteDraft(newId,"","",folder.id,null,false,false)==null)
 check(count("notes")==0)
 notes.setTags(newId,listOf("missing"));check(count("note_tags")==0)
 val saved=notes.upsertNewNoteDraft(newId,"","第一行\n第二行",folder.id,null,false,false)!!
 check(saved.content=="第一行\n第二行" && saved.folderId==folder.id)
 notes.saveVersion(newId,saved.content,"first")
 check(count("note_revisions")==1)
 check(notes.upsertNewNoteDraft(newId,"自动标题"," \n ",folder.id,null,false,false)==null)
 check(count("notes")==0 && count("note_revisions")==0 && count("note_links")==0)
 notes.upsertNewNoteDraft("named","主动命名","",null,null,false,true)
 check(notes.getNote("named")?.title=="主动命名")
 val existing=notes.createNote(title="旧笔记",content="old")
 notes.updateNote(existing.id,"旧笔记","",null,false)
 check(notes.getNote(existing.id)!=null)

 // Folder scope is enforced at repository boundaries, and deleting a parent
 // promotes direct content and children without changing project ownership.
 database.projectsDao().upsert(ProjectEntity("scope-A","A",createdAt=1,updatedAt=1))
 database.projectsDao().upsert(ProjectEntity("scope-B","B",createdAt=1,updatedAt=1))
 val scopeA=notes.createFolder("A root",projectId="scope-A")
 val scopeB=notes.createFolder("B root",projectId="scope-B")
 val scopeChild=notes.createFolder("A child",projectId="scope-A",parentId=scopeA.id)
 check(runCatching { notes.createNote(title="invalid",folderId=scopeB.id,projectId="scope-A") }.isFailure)
 check(runCatching { notes.moveFolder(scopeA.id,scopeB.id) }.isFailure)
 check(runCatching { notes.moveFolder(scopeA.id,scopeChild.id) }.isFailure)
 check(runCatching { chats.createSession("scope-A",folderId=scopeB.id) }.isFailure)
 val directScoped=notes.createNote(title="direct",folderId=scopeA.id,projectId="scope-A")
 val childScoped=notes.createNote(title="child",folderId=scopeChild.id,projectId="scope-A")
 notes.deleteFolder(scopeA.id)
 check(notes.getNote(directScoped.id)?.let { it.projectId=="scope-A" && it.folderId==null }==true)
 check(notes.getNote(childScoped.id)?.let { it.projectId=="scope-A" && it.folderId==scopeChild.id }==true)
 check(notes.getFolder(scopeChild.id)?.let { it.projectId=="scope-A" && it.parentId==null }==true)
 println("PASS: repository rejects cross-project folders/cycles and deletion promotes direct content and child folders")

 println("PASS: actual note repository discards blank/new-cleared notes, cleans revisions, preserves named and existing empty notes")

 val root=Files.createTempDirectory("dspro019-data").toFile()
 val attachment=File(root,"attachments/photo.png").apply { parentFile.mkdirs();writeBytes(byteArrayOf(1,3,5,7)) }
 val draft=ChatDraft(0L,"还没发送",quoteText="引用",selectedNoteIds=listOf("named"),pendingAttachments=listOf(DraftAttachment("img","photo.png","image/png",attachment.absolutePath,4)),folderId=folder.id)
 chats.saveTemporaryDraft(null,draft)
 check(count("chat_sessions")==0 && chats.getTemporaryDraft(null)==draft)
 database.projectsDao().upsert(ProjectEntity("p","Project",createdAt=1,updatedAt=1))
 val projectFolder=notes.createFolder("项目目录",projectId="p")
 chats.saveTemporaryDraft("p",draft.copy(text="独立项目草稿",selectedNoteIds=emptyList(),folderId=projectFolder.id))
 check(chats.getTemporaryDraft(null)?.text=="还没发送" && chats.getTemporaryDraft("p")?.text=="独立项目草稿")

 val deletingProject=ProjectEntity("delete-project","待删项目",createdAt=1,updatedAt=1)
 database.projectsDao().upsert(deletingProject)
 val deleteRoot=notes.createFolder("待删根",projectId=deletingProject.id)
 val deleteChild=notes.createFolder("待删子目录",projectId=deletingProject.id,parentId=deleteRoot.id)
 val deleteNote=notes.createNote(title="待迁移笔记",folderId=deleteChild.id,projectId=deletingProject.id)
 val deleteChat=chats.createSession(deletingProject.id,"待迁移对话",deleteChild.id)
 database.memoryDao().upsert(MemoryEntity("delete-memory",deletingProject.id,"合成记忆",createdAt=1))
 chats.saveTemporaryDraft(deletingProject.id,ChatDraft(0,"未发送",folderId=deleteChild.id))
 val projectRepository=ProjectsRepository(database)
 check(runCatching { projectRepository.delete(deletingProject.id) }.isFailure)
 check(projectRepository.get(deletingProject.id)!=null && notes.getNote(deleteNote.id)?.projectId==deletingProject.id)
 check(chats.getTemporaryDraft(deletingProject.id)?.text=="未发送")
 chats.clearTemporaryDraft(deletingProject.id)
 chats.saveTemporaryDraft(deletingProject.id,ChatDraft(0,folderId=deleteChild.id))
 projectRepository.delete(deletingProject.id)
 check(projectRepository.get(deletingProject.id)==null)
 check(notes.getNote(deleteNote.id)?.let { it.projectId==null && it.folderId==deleteChild.id }==true)
 check(chats.loadSessions().first { it.id==deleteChat.id }.let { it.projectId==null && it.folderId==deleteChild.id })
 check(notes.getFolder(deleteRoot.id)?.let { it.projectId==null && it.parentId==null }==true)
 check(notes.getFolder(deleteChild.id)?.let { it.projectId==null && it.parentId==deleteRoot.id }==true)
 check(chats.getTemporaryDraft(deletingProject.id)==null && database.memoryDao().activeForProject(deletingProject.id).isEmpty())
 println("PASS: project deletion blocks content drafts, clears empty drafts, moves content and folder trees global, and deletes project memories")
 chats.permanentlyDelete(listOf(deleteChat.id))
 notes.deleteFolder(deleteRoot.id)
 notes.permanentlyDelete(deleteNote.id)

 val firstMessages=listOf(ChatMessage(101,Role.USER,"首条消息","12:00"),ChatMessage(102,Role.ASSISTANT,"回答","12:00",sources=listOf(Source("真实网页","example.org","","摘要","https://example.org/a")),queries=listOf("测试问题")))
 check(runCatching{chats.commitTemporaryChat(null,folder.id,"title",emptyList())}.isFailure)
 jdbc.execSQL("CREATE TRIGGER reject_assistant BEFORE INSERT ON chat_messages WHEN NEW.role='assistant' BEGIN SELECT RAISE(ABORT,'forced failure'); END")
 check(runCatching{chats.commitTemporaryChat(null,folder.id,"title",firstMessages)}.isFailure)
 check(count("chat_sessions")==0 && count("chat_messages")==0 && chats.getTemporaryDraft(null)?.text==draft.text)
 jdbc.execSQL("DROP TRIGGER reject_assistant")
 val committed=chats.commitTemporaryChat(null,folder.id,"真实对话",firstMessages)
 check(chats.loadMessages(committed.id).first{it.id==102L}.sources.single().url=="https://example.org/a")
 val failedAnswer=firstMessages[1].copy(text="部分回答",status=MsgStatus.STOPPED,error="合成错误")
 chats.saveSession(committed.copy(messages=listOf(firstMessages[0],failedAnswer)))
 val reloadedFailed=chats.loadMessages(committed.id).first{it.id==102L}
 check(reloadedFailed.text=="部分回答" && reloadedFailed.status==MsgStatus.STOPPED && reloadedFailed.error==null)
 chats.saveSession(committed)
 check(count("chat_sessions")==1 && count("chat_messages")==2 && chats.getTemporaryDraft(null)==null)
 check(committed.folderId==folder.id && chats.loadSessions().single().folderId==folder.id)
 println("PASS: actual chat repository keeps unsent drafts outside sessions, isolates scopes, rolls back failed first send, and commits once atomically")

 val inFolder=notes.createNote(title="Folder note",content="body",folderId=folder.id)
 val search=WorkspaceSearchService(notes,chats)
 check(search.search(null,"真实",folderId=folder.id).first().any{it.kind=="chat" && it.sessionId==committed.id})
 check(search.search(null,"首条",folderId=folder.id).first().any{it.kind=="message" && it.messageId==101L})
 check(search.search(null,"真实",folderId="other").first().isEmpty())
 check(search.search(null,"真实",tagId="tag",folderId=folder.id).first().isEmpty())
 println("PASS: real workspace search filters conversation titles/messages by shared folder and limits tag search to notes")
 val originalNote = notes.getNote(inFolder.id)!!
 notes.archive(inFolder.id);check(notes.archivedNotes.first().any{it.id==inFolder.id});check(notes.notes.first().none{it.id==inFolder.id})
 notes.unarchive(inFolder.id)
 val restoredArchive = notes.getNote(inFolder.id)!!
 check(restoredArchive.archivedAt == null && restoredArchive.updatedAt >= originalNote.updatedAt)
 check(restoredArchive.content == originalNote.content && restoredArchive.folderId == originalNote.folderId && restoredArchive.projectId == originalNote.projectId)
 notes.archive(inFolder.id)
 chats.archive(committed.id);check(chats.archivedSessions.first().any{it.id==committed.id})
 chats.trash(listOf(committed.id));check(chats.archivedSessions.first().none{it.id==committed.id});check(chats.trashedSessions.first().single().id==committed.id)
 chats.restore(committed.id);check(chats.loadSessions().single().folderId==folder.id)
 notes.deleteFolder(folder.id)
 check(notes.getNote(inFolder.id)?.folderId==null && chats.loadSessions().single().folderId==null && chats.getTemporaryDraft("p")?.folderId==projectFolder.id)
 notes.deleteNote(inFolder.id);check(notes.observeTrash().first().any{it.id==inFolder.id});notes.restoreNote(inFolder.id);check(notes.getNote(inFolder.id)?.deletedAt==null)
 println("PASS: shared folder deletion retains notes/chats/drafts; archive/trash/restore preserve real records")

 // Orphan project drafts are recovered one at a time without recreating empty conversations.
 database.notesDao().moveFoldersToProject("p",null)
 database.projectsDao().delete("p")
 check(chats.getTemporaryDraft(null)?.text=="独立项目草稿")
 chats.commitTemporaryChat(null,null,"恢复草稿",listOf(ChatMessage(103,Role.USER,"恢复后发送","12:01")))
 check(chats.getTemporaryDraft(null)==null)

 val imageSnapshot=ContextSnapshotService(notes).encode(ContextSnapshot(emptyList(),imagePath=attachment.absolutePath,imageMime="image/png",imageName="photo.png"))
 connection.prepareStatement("UPDATE chat_messages SET contextSnapshot=? WHERE id=101").use { it.setString(1,imageSnapshot);it.executeUpdate() }
 chats.saveTemporaryDraft(null,draft.copy(folderId=null))
 val backup=BackupService(Context(root),database)
 val archive=File(root,"new.zip")
 backup.exportTo(Uri(archive.absolutePath))
 val manifest=ZipInputStream(archive.inputStream()).use { zip ->
  var text="";while(true) { val entry=zip.nextEntry?:break;if(entry.name=="manifest.json") text=String(zip.readBytes()) };text
 }
 check(JSONObject(manifest).getJSONObject("data").getJSONArray("temporary_chat_drafts").length()==1)
 chats.clearTemporaryDraft(null)

 // A real SQL error after restore has deleted/reinserted rows must roll back
 // the whole database and discard staged files without touching old files.
 val beforeCounts=JSONObject(manifest).getJSONObject("data").keys().asSequence().associateWith { count(it) }
 val beforeNote=notes.getNote("named")
 val beforeMessages=chats.loadMessages(committed.id)
 val attachmentsDir=File(root,"attachments")
 fun attachmentBytes()=attachmentsDir.walkTopDown().filter { it.isFile }.associate { it.relativeTo(attachmentsDir).path to it.readBytes().toList() }
 val beforeAttachments=attachmentBytes()
 jdbc.execSQL("CREATE TRIGGER reject_restore BEFORE INSERT ON chat_messages BEGIN SELECT RAISE(ABORT,'forced restore failure'); END")
 val restoreFailure=runCatching { backup.restore(Uri(archive.absolutePath)) }.exceptionOrNull()
 jdbc.execSQL("DROP TRIGGER reject_restore")
 check(restoreFailure!=null && generateSequence(restoreFailure) { it.cause }.any { it.message?.contains("forced restore failure")==true }) { "unexpected restore failure: ${generateSequence(restoreFailure) { it.cause }.map { it.message }.toList()}" }
 check(beforeCounts.all { (table, expected) -> count(table)==expected })
 check(notes.getNote("named")==beforeNote && chats.loadMessages(committed.id)==beforeMessages)
 check(chats.getTemporaryDraft(null)==null && attachmentBytes()==beforeAttachments)
 println("PASS: forced restore SQL failure rolls back all table counts, preserves records and old attachments, and removes staged files")

 backup.restore(Uri(archive.absolutePath))
 check(chats.loadMessages(committed.id).first{it.id==102L}.let { it.sources.single().url=="https://example.org/a" && it.queries==listOf("测试问题") })
 val restored=chats.getTemporaryDraft(null)!!
 check(restored.text==draft.text && File(restored.pendingAttachments.single().localPath).readBytes().contentEquals(byteArrayOf(1,3,5,7)))
 val restoredSnapshot=chats.loadMessages(committed.id).first{it.id==101L}.contextSnapshot!!
 check(File(ContextSnapshotService.decodeJson(restoredSnapshot).imagePath!!).isFile)

 // A schema-8 archive has legacy shared folders, with no projectId/parentId
 // columns. Exercise the real ZIP reader, conversion, relation validation,
 // attachment staging, and database restore using synthetic records only.
 val v8Root=JSONObject(manifest).put("databaseVersion",8)
 val v8Data=v8Root.getJSONObject("data")
 val v8Projects=v8Data.getJSONArray("projects")
 fun projectRow(id:String)=JSONObject().put("id",id).put("name",id).put("icon","📁").put("color",0)
  .put("instructions","").put("memoryMode","project_only").put("archivedAt",JSONObject.NULL)
  .put("createdAt",1).put("updatedAt",1)
 v8Projects.put(projectRow("legacy-A"));v8Projects.put(projectRow("legacy-B"))
 v8Data.put("folders",JSONArray())
 val v8Folders=v8Data.getJSONArray("folders")
 v8Folders.put(JSONObject().put("id","legacy-shared").put("name","旧共享目录").put("sort",4).put("archivedAt",99))
 for (i in 0 until v8Data.getJSONArray("notes").length()) v8Data.getJSONArray("notes").getJSONObject(i).put("folderId",JSONObject.NULL)
 for (i in 0 until v8Data.getJSONArray("chat_sessions").length()) v8Data.getJSONArray("chat_sessions").getJSONObject(i).put("folderId",JSONObject.NULL)
 for (i in 0 until v8Data.getJSONArray("temporary_chat_drafts").length()) v8Data.getJSONArray("temporary_chat_drafts").getJSONObject(i).put("folderId",JSONObject.NULL)
 // Make an archived note, an archived chat, a global draft, and a project
 // draft all reference the same legacy folder from different scopes.
 val archivedNoteRow=(0 until v8Data.getJSONArray("notes").length()).map { v8Data.getJSONArray("notes").getJSONObject(it) }
  .first { it.getString("id")=="named" }
 archivedNoteRow.put("projectId","legacy-A").put("folderId","legacy-shared").put("archivedAt",77)
 val archivedChatRow=(0 until v8Data.getJSONArray("chat_sessions").length()).map { v8Data.getJSONArray("chat_sessions").getJSONObject(it) }
  .first { it.optLong("id")==committed.id }
 archivedChatRow.put("projectId","legacy-B").put("folderId","legacy-shared").put("archivedAt",88)
 val globalDraftRow=v8Data.getJSONArray("temporary_chat_drafts").getJSONObject(0).put("folderId","legacy-shared")
 val projectDraftRow=JSONObject(globalDraftRow.toString()).put("projectScope","project:legacy-A").put("projectId","legacy-A")
  .put("text","legacy project draft").put("folderId","legacy-shared")
 v8Data.getJSONArray("temporary_chat_drafts").put(projectDraftRow)
 val v8Archive=File(root,"v8-shared-folders.zip")
 ZipOutputStream(v8Archive.outputStream()).use { out -> ZipInputStream(archive.inputStream()).use { input -> while(true) { val e=input.nextEntry?:break;out.putNextEntry(ZipEntry(e.name));out.write(if(e.name=="manifest.json")v8Root.toString().toByteArray() else input.readBytes());out.closeEntry() } } }
 backup.restore(Uri(v8Archive.absolutePath))
 val convertedFolders=connection.createStatement().use { statement -> statement.executeQuery("SELECT id,projectId,archivedAt FROM folders").use { rs -> buildList { while(rs.next()) add(Triple(rs.getString(1),rs.getString(2),rs.getString(3))) } } }
 check(convertedFolders.size==3 && convertedFolders.any { it.first=="legacy-shared" && it.second==null && it.third=="99" })
 val projectAFolder=convertedFolders.single { it.second=="legacy-A" }.first
 val projectBFolder=convertedFolders.single { it.second=="legacy-B" }.first
 check(projectAFolder.startsWith("folder-v1-") && projectBFolder.startsWith("folder-v1-") && projectAFolder!=projectBFolder)
 check(notes.getNote("named")?.let { it.projectId=="legacy-A" && it.folderId==projectAFolder && it.archivedAt==77L }==true)
 check(chats.loadSessions().first { it.id==committed.id }.let { it.projectId=="legacy-B" && it.folderId==projectBFolder && it.archived })
 check(connection.createStatement().use { it.executeQuery("SELECT archivedAt FROM chat_sessions WHERE id=${committed.id}").use { rs -> rs.next();rs.getLong(1)==88L } })
 check(chats.getTemporaryDraft(null)?.folderId=="legacy-shared")
 check(chats.getTemporaryDraft("legacy-A")?.let { it.folderId==projectAFolder && it.text=="legacy project draft" }==true)
 println("PASS: actual schema-8 ZIP restore splits shared folders across global/project scopes and preserves archived records and drafts")

 val v7Root=JSONObject(manifest).put("databaseVersion",7)
 val v7Messages=v7Root.getJSONObject("data").getJSONArray("chat_messages")
 for (i in 0 until v7Messages.length()) { val row=v7Messages.getJSONObject(i);row.remove("sourcesJson");row.remove("queriesJson") }
 val v7Archive=File(root,"v7-v020.zip")
 ZipOutputStream(v7Archive.outputStream()).use { out -> ZipInputStream(archive.inputStream()).use { input -> while(true) { val e=input.nextEntry?:break;out.putNextEntry(ZipEntry(e.name));out.write(if(e.name=="manifest.json")v7Root.toString().toByteArray() else input.readBytes());out.closeEntry() } } }
 backup.restore(Uri(v7Archive.absolutePath))
 check(chats.loadMessages(committed.id).first{it.id==102L}.let { it.sources.isEmpty() && it.queries.isEmpty() })
 // The v6 archive is generated by stripping only fields/table absent from an actual new export.
 val oldRoot=JSONObject(manifest).put("databaseVersion",6)
 val oldData=oldRoot.getJSONObject("data");oldData.remove("temporary_chat_drafts")
 for (i in 0 until oldData.getJSONArray("notes").length()) oldData.getJSONArray("notes").getJSONObject(i).remove("archivedAt")
 for (i in 0 until oldData.getJSONArray("chat_sessions").length()) { val item=oldData.getJSONArray("chat_sessions").getJSONObject(i);item.remove("folderId");item.remove("deletedAt") }
 val oldArchive=File(root,"v6.zip")
 ZipOutputStream(oldArchive.outputStream()).use { out -> ZipInputStream(archive.inputStream()).use { input -> while(true) { val e=input.nextEntry?:break;out.putNextEntry(ZipEntry(e.name));out.write(if(e.name=="manifest.json")oldRoot.toString().toByteArray() else input.readBytes());out.closeEntry() } } }
 check(backup.inspect(Uri(oldArchive.absolutePath)).counts["temporary_chat_drafts"]==0)
 backup.restore(Uri(oldArchive.absolutePath))
 check(chats.getTemporaryDraft(null)==null && count("chat_sessions")==2)
 check(notes.getNote("named")?.title=="主动命名")
 println("PASS: schema9 backup roundtrip remaps temporary/image paths and synthetic schema7/schema6 backups restore compatibly")
 chats.trash(listOf(committed.id));chats.permanentlyDelete(listOf(committed.id));check(chats.loadSessions().none{it.id==committed.id});check(chats.loadMessages(committed.id).isEmpty())
 root.deleteRecursively();connection.close()
}
