package androidx.room
import com.dspro.android.data.notes.NotesDatabase
annotation class Entity(val tableName:String = "", val indices:Array<Index> = [], val primaryKeys:Array<String> = [])
annotation class Index(vararg val value:String)
annotation class ColumnInfo(val defaultValue:String = "")
annotation class PrimaryKey
annotation class Dao
annotation class Query(val value:String)
annotation class Insert(val onConflict:Int = 0)
object OnConflictStrategy { const val REPLACE = 1 }
suspend fun <T> NotesDatabase.withTransaction(block:suspend ()->T):T {
 val connection=openHelper.jdbc.connection
 if (!connection.autoCommit) return block()
 connection.autoCommit=false
 try { val result=block();connection.commit();return result }
 catch(error:Throwable) { connection.rollback();throw error }
 finally { connection.autoCommit=true }
}
