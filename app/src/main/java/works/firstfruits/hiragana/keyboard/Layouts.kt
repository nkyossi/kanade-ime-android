package works.firstfruits.hiragana.keyboard

/** All the layouts by their id; KeyAction.SwitchLayout(id) switches to one of them. */
object Layouts {
    private val all: Map<String, KeyboardLayout> = listOf(
        QwertyLayout.layout,
        FlickLayout.layout,
    ).associateBy { it.id }

    /** The layout for [id], or the QWERTY layout for an unknown id. */
    fun byId(id: String): KeyboardLayout = all[id] ?: QwertyLayout.layout
}
