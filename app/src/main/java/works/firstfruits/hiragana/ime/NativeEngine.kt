package works.firstfruits.hiragana.ime

import java.io.File

/**
 * The conversion engine of Hiragana IME (app/src/main/cpp/core) seen from Kotlin.
 *
 * Call every method except [create] on the main thread. [create] loads the
 * dictionaries and may take a while, so call it on a background thread.
 */
class NativeEngine private constructor(private var handle: Long) : AutoCloseable {

    /** What the engine asks of the text field; see InputConnectionClient. */
    interface Client {
        /** The text before the cursor, without the composing text; null if the field cannot tell. */
        fun textBeforeCursor(): String?

        /** Deletes [utf16Length] UTF-16 units before the cursor. */
        fun deleteBeforeCursor(utf16Length: Int)

        fun commitText(text: String)

        /** [spans] holds `start, end, style` (UTF-16 offsets) for each segment; style is one of PREEDIT_*. */
        fun updatePreedit(text: String, spans: IntArray)

        fun updateCandidates(candidates: Array<String>, cursor: Int, visible: Boolean)

        fun modeChanged(mode: Int)
    }

    fun setClient(client: Client?) = nativeSetClient(handle, client)

    /** Processes a key as an X keysym; see [Keysym]. Returns false if the app should process the key. */
    fun processKey(keysym: Int, keycode: Int, state: Int = 0): Boolean =
        nativeProcessKey(handle, keysym, keycode, state)

    /** Presses and releases a key. */
    fun tapKey(keysym: Int, keycode: Int, state: Int = 0): Boolean {
        val processed = processKey(keysym, keycode, state)
        processKey(keysym, keycode, state or Keysym.RELEASE_MASK)
        return processed
    }

    /** Inserts kana chosen on a software keyboard, e.g. by a flick. */
    fun insertKana(kana: String): Boolean = nativeInsertKana(handle, kana)

    /** Cycles the kana before the cursor: か→が→か, は→ば→ぱ→は, つ→っ→づ→つ. */
    fun cycleKana(): Boolean = nativeCycleKana(handle)

    /** Confirms the candidate at [index] of all the candidates given to Client.updateCandidates(). */
    fun candidateClicked(index: Int) = nativeCandidateClicked(handle, index)

    fun setSurroundingSupported(supported: Boolean) = nativeSetSurroundingSupported(handle, supported)
    fun focusIn() = nativeFocusIn(handle)
    fun focusOut() = nativeFocusOut(handle)
    fun reset() = nativeReset(handle)
    fun commitAll() = nativeCommitAll(handle)
    fun compositionTerminated() = nativeCompositionTerminated(handle)
    fun hasPendingInput(): Boolean = nativeHasPendingInput(handle)

    var mode: Int
        get() = nativeGetMode(handle)
        set(value) = nativeSetMode(handle, value, true)

    fun applySettings(settings: File, xkb: String = XKB) = nativeApplySettings(handle, settings.path, xkb)
    fun reloadDictionary() = nativeReloadDictionary(handle)

    /** Saves the input history and frees the engine. */
    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("hiragana_jni")
        }

        /** The software keyboards send the keys of a US keyboard. */
        const val XKB = "us"

        const val MODE_ALPHANUMERIC = 'A'.code
        const val MODE_HIRAGANA = 'あ'.code
        const val MODE_KATAKANA = 'ア'.code
        const val MODE_WIDE_ALPHANUMERIC = 'Ａ'.code
        const val MODE_HALFWIDTH_KATAKANA = 'ｱ'.code

        const val PREEDIT_INPUT = 0
        const val PREEDIT_CANDIDATE = 1
        const val PREEDIT_LOCKED = 2

        /**
         * Loads the engine. [systemDir] holds dic/ and layouts/; [userDir] holds the
         * user dictionary and the input history. Returns null on failure.
         */
        fun create(systemDir: File, userDir: File, settings: File, xkb: String = XKB): NativeEngine? {
            val handle = nativeCreate(systemDir.path, userDir.path, settings.path, xkb)
            return if (handle != 0L) NativeEngine(handle) else null
        }

        @JvmStatic private external fun nativeCreate(systemDir: String, userDir: String, settings: String, xkb: String): Long
        @JvmStatic private external fun nativeDestroy(handle: Long)
        @JvmStatic private external fun nativeSetClient(handle: Long, client: Client?)
        @JvmStatic private external fun nativeProcessKey(handle: Long, keysym: Int, keycode: Int, state: Int): Boolean
        @JvmStatic private external fun nativeInsertKana(handle: Long, kana: String): Boolean
        @JvmStatic private external fun nativeCycleKana(handle: Long): Boolean
        @JvmStatic private external fun nativeCandidateClicked(handle: Long, index: Int)
        @JvmStatic private external fun nativeSetSurroundingSupported(handle: Long, supported: Boolean)
        @JvmStatic private external fun nativeFocusIn(handle: Long)
        @JvmStatic private external fun nativeFocusOut(handle: Long)
        @JvmStatic private external fun nativeReset(handle: Long)
        @JvmStatic private external fun nativeCommitAll(handle: Long)
        @JvmStatic private external fun nativeCompositionTerminated(handle: Long)
        @JvmStatic private external fun nativeHasPendingInput(handle: Long): Boolean
        @JvmStatic private external fun nativeGetMode(handle: Long): Int
        @JvmStatic private external fun nativeSetMode(handle: Long, mode: Int, override: Boolean)
        @JvmStatic private external fun nativeApplySettings(handle: Long, settings: String, xkb: String)
        @JvmStatic private external fun nativeReloadDictionary(handle: Long)
    }
}
