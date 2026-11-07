package com.dspro.android.data.notes
import androidx.sqlite.db.JdbcDatabase
class NotesDatabase(val openHelper: Helper) {
 companion object { const val SCHEMA_VERSION = 9 }
 private val notes = daoAdapter(NotesDao::class.java, openHelper.jdbc.connection)
 private val chats = daoAdapter(ChatDao::class.java, openHelper.jdbc.connection)
 private val projects = daoAdapter(ProjectsDao::class.java, openHelper.jdbc.connection)
 private val memories = daoAdapter(MemoryDao::class.java, openHelper.jdbc.connection)
 fun notesDao() = notes
 fun chatDao() = chats
 fun projectsDao() = projects
 fun memoryDao() = memories
}
class Helper(val jdbc:JdbcDatabase) { val readableDatabase get()=jdbc; val writableDatabase get()=jdbc }
