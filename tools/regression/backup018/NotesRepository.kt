package com.dspro.android.data.notes
data class Note(val id:String,val title:String,val content:String,val projectId:String?=null,val deletedAt:Long?=null)
class NotesRepository(val all:Map<String,Note> = emptyMap()) { suspend fun getNote(id:String):Note? = all[id] }
