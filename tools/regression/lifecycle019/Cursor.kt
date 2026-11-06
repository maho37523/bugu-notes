package android.database
interface Cursor : AutoCloseable {
    val columnCount: Int
    fun moveToNext(): Boolean
    fun isNull(i: Int): Boolean
    fun getColumnName(i: Int): String
    fun getColumnIndexOrThrow(name: String): Int
    fun getType(i: Int): Int
    fun getLong(i: Int): Long
    fun getDouble(i: Int): Double
    fun getBlob(i: Int): ByteArray
    fun getString(i: Int): String
    companion object { const val FIELD_TYPE_INTEGER=1; const val FIELD_TYPE_FLOAT=2; const val FIELD_TYPE_STRING=3; const val FIELD_TYPE_BLOB=4 }
}
