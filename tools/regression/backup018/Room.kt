package androidx.room
import com.dspro.android.data.notes.NotesDatabase
suspend fun <T> NotesDatabase.withTransaction(block:suspend ()->T):T {
    val conn = openHelper.jdbc.connection
    conn.autoCommit=false
    try { val result=block();conn.commit();return result }
    catch(error:Throwable) { conn.rollback();throw error }
    finally { conn.autoCommit=true }
}
