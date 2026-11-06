package androidx.sqlite.db
import android.content.ContentValues
import android.database.Cursor
import java.sql.Connection
interface SupportSQLiteDatabase {
    fun query(sql: String): Cursor
    fun execSQL(sql: String)
    fun insert(table: String, conflict: Int, values: ContentValues): Long
}
class JdbcDatabase(val connection: Connection) : SupportSQLiteDatabase {
    override fun execSQL(sql: String) { connection.createStatement().use { it.execute(sql) } }
    override fun query(sql: String): Cursor {
        val rows = mutableListOf<List<Any?>>()
        val names = mutableListOf<String>()
        connection.createStatement().use { statement -> statement.executeQuery(sql).use { rs ->
            (1..rs.metaData.columnCount).forEach { names.add(rs.metaData.getColumnName(it)) }
            while(rs.next()) rows.add((1..names.size).map { rs.getObject(it) })
        } }
        return object : Cursor {
            var position=-1
            override val columnCount get()=names.size
            override fun moveToNext()=++position < rows.size
            override fun isNull(i:Int)=rows[position][i]==null
            override fun getColumnName(i:Int)=names[i]
            override fun getColumnIndexOrThrow(name:String)=names.indexOf(name).also { require(it>=0) }
            override fun getType(i:Int)=when(rows[position][i]) { is ByteArray -> 4; is Double,is Float -> 2; is Number -> 1; else -> 3 }
            override fun getLong(i:Int)=(rows[position][i] as Number).toLong()
            override fun getDouble(i:Int)=(rows[position][i] as Number).toDouble()
            override fun getBlob(i:Int)=rows[position][i] as ByteArray
            override fun getString(i:Int)=rows[position][i].toString()
            override fun close() {}
        }
    }
    override fun insert(table:String, conflict:Int, values:ContentValues):Long {
        val columns=values.values.keys.joinToString(",") { "`$it`" }
        val placeholders=values.values.keys.joinToString(",") { "?" }
        connection.prepareStatement("INSERT INTO `$table` ($columns) VALUES ($placeholders)").use { stmt ->
            values.values.values.forEachIndexed { i,value -> stmt.setObject(i+1,value) }
            return stmt.executeUpdate().toLong()
        }
    }
}
