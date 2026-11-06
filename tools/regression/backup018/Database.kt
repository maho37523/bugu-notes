package com.dspro.android.data.notes
import androidx.sqlite.db.JdbcDatabase
class NotesDatabase(val openHelper: Helper) { companion object { const val SCHEMA_VERSION = 6 } }
class Helper(val jdbc:JdbcDatabase) { val readableDatabase get()=jdbc; val writableDatabase get()=jdbc }
