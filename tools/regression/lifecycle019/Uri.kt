package android.net
data class Uri(val path: String) { companion object { fun parse(path: String) = Uri(path) } }
