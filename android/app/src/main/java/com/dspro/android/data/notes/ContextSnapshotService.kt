package com.dspro.android.data.notes

import org.json.JSONArray
import org.json.JSONObject

data class ContextNoteSnapshot(val id: String, val title: String, val source: String, val content: String, val truncated: Boolean, val originalLength: Int)
data class ContextSnapshot(
    val notes: List<ContextNoteSnapshot>,
    val quoteText: String? = null,
    val attachmentText: String? = null,
    val imagePath: String? = null,
    val imageMime: String? = null,
    val imageName: String? = null,
)

/** Captures the exact note text supplied to a request so later retries never read newer note edits. */
class ContextSnapshotService(private val notes: NotesRepository) {
    suspend fun build(projectId: String?, selectedNoteIds: List<String>, quoteText: String? = null, attachmentText: String? = null, imagePath: String? = null, imageMime: String? = null, imageName: String? = null): ContextSnapshot {
        val ids = selectedNoteIds.distinct()
        require(ids.size <= MAX_NOTES) { "最多可引用 8 条笔记" }
        val found = ids.map { id -> notes.getNote(id) }
        require(found.all { it != null }) { "部分引用笔记已不存在，请重新选择" }
        val live = found.filterNotNull()
        require(live.all { it.deletedAt == null && it.projectId == projectId }) { "部分引用笔记已删除或已移出当前项目，请重新选择" }
        val perNote = (TOTAL_BUDGET / live.size.coerceAtLeast(1)).coerceAtMost(PER_NOTE_CAP)
        return ContextSnapshot(live.map { note ->
            val body = note.content.take(perNote)
            ContextNoteSnapshot(note.id, note.title, "note", body, body.length < note.content.length, note.content.length)
        }, quoteText?.take(4000), attachmentText?.take(20000), imagePath, imageMime, imageName)
    }
    fun encode(snapshot: ContextSnapshot): String = JSONObject().put("version", 1).put("notes", JSONArray().apply {
        snapshot.notes.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("source", it.source).put("content", it.content).put("truncated", it.truncated).put("originalLength", it.originalLength)) }
    }).put("quoteText", snapshot.quoteText).put("attachmentText", snapshot.attachmentText)
        .put("imagePath", snapshot.imagePath).put("imageMime", snapshot.imageMime).put("imageName", snapshot.imageName).toString()
    fun asSystemContext(snapshot: ContextSnapshot): String? = buildList {
        if (snapshot.notes.isNotEmpty()) add("用户引用了以下笔记。请结合这些内容回答：\n\n" + snapshot.notes.joinToString("\n\n---\n\n") { "《${it.title}》\n${it.content}" })
        snapshot.attachmentText?.takeIf { it.isNotBlank() }?.let { add("用户上传了附件，以下是提取出的文本：\n\n$it") }
    }.joinToString("\n\n").takeIf { it.isNotBlank() }
    fun decode(json: String): ContextSnapshot = decodeJson(json)

    companion object {
      const val MAX_NOTES = 8
      const val TOTAL_BUDGET = 12000
      const val PER_NOTE_CAP = 6000
      fun decodeJson(json: String): ContextSnapshot {
        val root = JSONObject(json)
        require(root.optInt("version") == 1) { "Unsupported context snapshot version" }
        val a = root.getJSONArray("notes")
        require(a.length() <= MAX_NOTES) { "Context snapshot has too many notes" }
        val notes = (0 until a.length()).map { i -> a.getJSONObject(i).let { item ->
            val content = item.getString("content")
            val originalLength = item.optInt("originalLength", content.length)
            val note = ContextNoteSnapshot(item.getString("id"), item.getString("title"), item.optString("source", "note"), content, item.optBoolean("truncated"), originalLength)
            require(note.id.isNotBlank() && note.source.isNotBlank() && content.length <= PER_NOTE_CAP && originalLength >= content.length) { "Invalid context note" }
            require(note.truncated == (originalLength > content.length)) { "Invalid context truncation metadata" }
            note
        } }
        require(notes.sumOf { it.content.length } <= TOTAL_BUDGET) { "Context snapshot exceeds its text budget" }
        val quote = root.opt("quoteText")?.takeUnless { it == JSONObject.NULL }?.toString()
        val attachment = root.opt("attachmentText")?.takeUnless { it == JSONObject.NULL }?.toString()
        require((quote?.length ?: 0) <= 4000 && (attachment?.length ?: 0) <= 20000) { "Context snapshot text is too large" }
        val imagePath = root.opt("imagePath")?.takeUnless { it == JSONObject.NULL }?.toString()
        val imageMime = root.opt("imageMime")?.takeUnless { it == JSONObject.NULL }?.toString()
        val imageName = root.opt("imageName")?.takeUnless { it == JSONObject.NULL }?.toString()
        require(imagePath == null || imageMime?.startsWith("image/") == true) { "Invalid context image metadata" }
        return ContextSnapshot(notes, quote, attachment, imagePath, imageMime, imageName)
      }
    }
}
