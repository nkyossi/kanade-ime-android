package works.firstfruits.hiragana

/** Checks the two steps that make the IME usable; kept pure so it is easy to test. */
object Onboarding {
    /** The IME service after the slash of a flattened component name. */
    const val IME_SERVICE = "ime.HiraganaImeService"

    /**
     * True when [id] names this app's IME. [id] is a flattened component name whose
     * class part may be short (".ime.HiraganaImeService") or full.
     */
    fun isImeId(id: String?, packageName: String): Boolean {
        if (id == null) return false
        val slash = id.indexOf('/')
        if (slash <= 0) return false
        if (id.substring(0, slash) != packageName) return false
        val name = id.substring(slash + 1)
        val full = if (name.startsWith(".")) packageName + name else name
        return full == "$packageName.$IME_SERVICE"
    }

    /** True when one of the enabled IME [ids] is this app's IME. */
    fun isEnabled(ids: List<String?>, packageName: String): Boolean =
        ids.any { isImeId(it, packageName) }
}
