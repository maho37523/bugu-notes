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

fun main(args:Array<String>) = runBlocking {
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
 println("PASS: actual note repository discards blank/new-cleared notes, cleans revisions, preserves named and existing empty notes")

 val root=Files.createTempDirectory("dspro019-data").toFile()
 val attachment=File(root,"attachments/photo.png").apply { parentFile.mkdirs();writeBytes(byteArrayOf(1,3,5,7)) }
 val draft=ChatDraft(0L,"还没发送",quoteText="引用",selectedNoteIds=listOf("named"),pendingAttachments=listOf(DraftAttachment("img","photo.png","image/png",attachment.absolutePath,4)),folderId=folder.id)
 chats.saveTemporaryDraft(null,draft)
 check(count("chat_sessions")==0 && chats.getTemporaryDraft(null)==draft)
 database.projectsDao().upsert(ProjectEntity("p","Project",createdAt=1,updatedAt=1))
 chats.saveTemporaryDraft("p",draft.copy(text="独立项目草稿",selectedNoteIds=emptyList()))
 check(chats.getTemporaryDraft(null)?.text=="还没发送" && chats.getTemporaryDraft("p")?.text=="独立项目草稿")
 val firstMessages=listOf(ChatMessage(101,Role.USER,"首条消息","12:00"),ChatMessage(102,Role.ASSISTANT,"回答","12:00",sources=listOf(Source("真实网页","example.org","","摘要","https://example.org/a")),queries=listOf("测试问题")))
 check(runCatching{chats.commitTemporaryChat(null,folder.id,"title",emptyList())}.isFailure)
 jdbc.execSQL("CREATE TRIGGER reject_assistant BEFORE INSERT ON chat_messages WHEN NEW.role='assistant' BEGIN SELECT RAISE(ABORT,'forced failure'); END")
 check(runCatching{chats.commitTemporaryChat(null,folder.id,"title",firstMessages)}.isFailure)
 check(count("chat_sessions")==0 && count("chat_messages")==0 && chats.getTemporaryDraft(null)?.text==draft.text)
 jdbc.execSQL("DROP TRIGGER reject_assistant")
 val committed=chats.commitTemporaryChat(null,folder.id,"真实对话",firstMessages)
 check(chats.loadMessages(committed.id).first{it.id==102L}.sources.single().url=="https://example.org/a")
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
 check(notes.getNote(inFolder.id)?.folderId==null && chats.loadSessions().single().folderId==null && chats.getTemporaryDraft("p")?.folderId==null)
 notes.deleteNote(inFolder.id);check(notes.observeTrash().first().any{it.id==inFolder.id});notes.restoreNote(inFolder.id);check(notes.getNote(inFolder.id)?.deletedAt==null)
 println("PASS: shared folder deletion retains notes/chats/drafts; archive/trash/restore preserve real records")

 // Orphan project drafts are recovered one at a time without recreating empty conversations.
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
 backup.restore(Uri(archive.absolutePath))
 check(chats.loadMessages(committed.id).first{it.id==102L}.let { it.sources.single().url=="https://example.org/a" && it.queries==listOf("测试问题") })
 val restored=chats.getTemporaryDraft(null)!!
 check(restored.text==draft.text && File(restored.pendingAttachments.single().localPath).readBytes().contentEquals(byteArrayOf(1,3,5,7)))
 val restoredSnapshot=chats.loadMessages(committed.id).first{it.id==101L}.contextSnapshot!!
 check(File(ContextSnapshotService.decodeJson(restoredSnapshot).imagePath!!).isFile)
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
 println("PASS: actual v7 backup roundtrip remaps temporary/image paths and v0.1.8 schema6 backup restores compatibly")
 chats.trash(listOf(committed.id));chats.permanentlyDelete(listOf(committed.id));check(chats.loadSessions().none{it.id==committed.id});check(chats.loadMessages(committed.id).isEmpty())
 root.deleteRecursively();connection.close()
}
