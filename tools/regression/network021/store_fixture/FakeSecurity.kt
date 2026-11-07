package androidx.security.crypto
import android.content.Context
import android.content.SharedPreferences
object SecurityFixture {
    var failInit = false
    lateinit var prefs: SharedPreferences
}
class MasterKey {
    enum class KeyScheme { AES256_GCM }
    class Builder(context: Context) {
        fun setKeyScheme(scheme: KeyScheme): Builder = this
        fun build(): MasterKey { if (SecurityFixture.failInit) error("simulated Keystore failure"); return MasterKey() }
    }
}
object EncryptedSharedPreferences {
    enum class PrefKeyEncryptionScheme { AES256_SIV }
    enum class PrefValueEncryptionScheme { AES256_GCM }
    fun create(context: Context, name: String, key: MasterKey, keyScheme: PrefKeyEncryptionScheme, valueScheme: PrefValueEncryptionScheme): SharedPreferences = SecurityFixture.prefs
}
