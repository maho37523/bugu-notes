package com.dspro.android.data.notes

import android.content.Context
import android.net.Uri
import androidx.sqlite.db.JdbcDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun main(args:Array<String>) = runBlocking {
    Class.forName("org.sqlite.JDBC")
    val connection=DriverManager.getConnection("jdbc:sqlite::memory:")
    val jdbc=JdbcDatabase(connection)
    File(args.single()).readText().split(';').filter { it.isNotBlank() }.forEach { jdbc.execSQL(it) }
    val root=Files.createTempDirectory("backup018-user").toFile()
    val attachments=File(root,"attachments").apply { mkdirs() }
    val photo=File(attachments,"photo.png").apply { writeBytes(byteArrayOf(1,2,3,4)) }
    val nested=File(attachments,"nested/file.txt").apply { parentFile.mkdirs();writeText("all owned files") }
    val draftAttachment=JSONArray().put(JSONObject().put("id","photo").put("name","photo.png").put("mime","image/png").put("localPath",photo.absolutePath).put("size",4).put("textContent",JSONObject.NULL)).toString()
    jdbc.execSQL("INSERT INTO notes(id,title,content,pinned,createdAt,updatedAt) VALUES('n','Title','Original body',0,1,2)")
    jdbc.execSQL("INSERT INTO chat_sessions(id,title,createdAt,updatedAt,titleManuallySet) VALUES(1,'Manual',1,2,1)")
    jdbc.execSQL("INSERT INTO chat_messages(id,sessionId,role,text,status,time,createdAt) VALUES(2,1,'user','Question','done','12:00',3)")
    connection.prepareStatement("INSERT INTO chat_drafts(sessionId,text,selectedNoteIdsJson,pendingAttachmentsJson,updatedAt) VALUES(1,'Unsent','[\"n\"]',?,4)").use { it.setString(1,draftAttachment);it.executeUpdate() }
    connection.prepareStatement("INSERT INTO attachments(id,ownerType,ownerId,name,mime,localPath,size,createdAt) VALUES('a','chat','1','photo.png','image/png',?,4,1)").use { it.setString(1,photo.absolutePath);it.executeUpdate() }
    val requestSnapshot=ContextSnapshotService(NotesRepository()).encode(ContextSnapshot(emptyList(),imagePath=photo.absolutePath,imageMime="image/png",imageName="photo.png"))
    connection.prepareStatement("UPDATE chat_messages SET contextSnapshot=? WHERE id=2").use { it.setString(1,requestSnapshot);it.executeUpdate() }
    val service=BackupService(Context(root),NotesDatabase(Helper(jdbc)))
    val archive=File(root,"test.zip")
    val summary=service.exportTo(Uri(archive.absolutePath))
    check(summary.attachmentCount==2)
    check(service.inspect(Uri(archive.absolutePath)).counts["chat_drafts"]==1)
    var manifest=""
    ZipInputStream(archive.inputStream()).use { zip -> while(true) { val entry=zip.nextEntry?:break;if(entry.name=="manifest.json")manifest=String(zip.readBytes()) } }
    check(!manifest.contains("api_key") && !manifest.contains(root.absolutePath)) { "Keys or device paths leaked into manifest" }
    jdbc.execSQL("UPDATE notes SET content='Changed'")
    val restored=service.restore(Uri(archive.absolutePath))
    check(restored.attachmentCount==2)
    jdbc.query("SELECT content FROM notes WHERE id='n'").use { check(it.moveToNext()&&it.getString(0)=="Original body") }
    jdbc.query("SELECT localPath FROM attachments WHERE id='a'").use { check(it.moveToNext());check(File(it.getString(0)).readBytes().contentEquals(byteArrayOf(1,2,3,4))) }
    jdbc.query("SELECT pendingAttachmentsJson FROM chat_drafts WHERE sessionId=1").use {
        check(it.moveToNext());val a=JSONArray(it.getString(0)).getJSONObject(0);check(File(a.getString("localPath")).isFile);check(a.isNull("textContent"))
    }
    jdbc.query("SELECT contextSnapshot FROM chat_messages WHERE id=2").use {
        check(it.moveToNext());val snapshot=ContextSnapshotService.decodeJson(it.getString(0));check(File(snapshot.imagePath!!).readBytes().contentEquals(byteArrayOf(1,2,3,4)))
    }
    val allFiles=attachments.walkTopDown().filter{it.isFile}.toList()
    check(allFiles.size==2 && allFiles.any{runCatching{it.readText()=="all owned files"}.getOrDefault(false)})
    val malicious=File(root,"bad.zip")
    ZipOutputStream(malicious.outputStream()).use { zip -> zip.putNextEntry(ZipEntry("../escape"));zip.write(byteArrayOf(1));zip.closeEntry() }
    check(runCatching{service.restore(Uri(malicious.absolutePath))}.isFailure)
    jdbc.query("SELECT content FROM notes WHERE id='n'").use { check(it.moveToNext()&&it.getString(0)=="Original body") }
    check(attachments.walkTopDown().count{it.isFile}==2)
    // A syntactically valid manifest with a row violating the real SQLite schema must roll back.
    val invalidRoot=JSONObject(manifest)
    invalidRoot.getJSONObject("data").put("notes",JSONArray().put(JSONObject().put("id","n")))
    val invalid=File(root,"invalid.zip")
    ZipOutputStream(invalid.outputStream()).use { out ->
        ZipInputStream(archive.inputStream()).use { input -> while(true) { val entry=input.nextEntry?:break;out.putNextEntry(ZipEntry(entry.name));out.write(if(entry.name=="manifest.json") invalidRoot.toString().toByteArray() else input.readBytes());out.closeEntry() } }
    }
    check(runCatching{service.restore(Uri(invalid.absolutePath))}.isFailure)
    jdbc.query("SELECT content FROM notes WHERE id='n'").use { check(it.moveToNext()&&it.getString(0)=="Original body") }
    check(attachments.walkTopDown().count{it.isFile}==2)
    val live=NotesRepository(mapOf("long" to Note("long","Long","x".repeat(7000)),"moved" to Note("moved","Moved","secret","project"),"deleted" to Note("deleted","Deleted","old",deletedAt=1)))
    val snapshots=ContextSnapshotService(live)
    val captured=snapshots.build(null,listOf("long"),null,null)
    check(captured.notes.single().content.length==6000 && captured.notes.single().truncated)
    check(snapshots.decode(snapshots.encode(captured))==captured)
    val noText=ContextSnapshotService.decodeJson(snapshots.encode(ContextSnapshot(emptyList())))
    check(noText.quoteText==null && noText.attachmentText==null && noText.imagePath==null)
    for (bad in listOf("missing","moved","deleted")) check(runCatching { snapshots.build(null,listOf(bad)) }.isFailure)
    root.deleteRecursively();connection.close()
    println("PASS: actual BackupService + real SQLite roundtrip, all files and draft remapping, path rejection image snapshot replay, failed SQL rollback, note scope/deletion/truncation and null JSON")
}
