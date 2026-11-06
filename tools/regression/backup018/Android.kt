package android.content
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import android.net.Uri
open class Context(val filesDir: File, val contentResolver: ContentResolver = ContentResolver())
class ContentResolver {
    fun openInputStream(uri: Uri): InputStream? = File(uri.path).inputStream()
    fun openOutputStream(uri: Uri, mode: String = "wt"): OutputStream? = File(uri.path).outputStream()
}
class ContentValues {
    val values = linkedMapOf<String, Any?>()
    fun putNull(key: String) { values[key] = null }
    fun put(key: String, value: String) { values[key] = value }
    fun put(key: String, value: Int) { values[key] = value }
    fun put(key: String, value: Long) { values[key] = value }
    fun put(key: String, value: Double) { values[key] = value }
}
