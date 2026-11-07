package android.content
interface SharedPreferences {
    val all: Map<String, *>
    fun getString(key: String, default: String?): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun contains(key: String): Boolean
    fun edit(): Editor
    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun apply()
        fun commit(): Boolean
    }
}
open class Context {
    companion object { const val MODE_PRIVATE = 0 }
    open val applicationContext: Context get() = this
    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences = error("not implemented")
}
