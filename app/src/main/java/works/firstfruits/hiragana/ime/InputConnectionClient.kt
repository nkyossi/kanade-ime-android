package works.firstfruits.hiragana.ime

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.UnderlineSpan
import android.view.inputmethod.InputConnection

/**
 * NativeEngine.Client on top of an InputConnection.
 *
 * The engine edits the text before the cursor directly (as ibus-hiragana does
 * with the surrounding text) and shows only the current candidate and the
 * pending roman letters as the composing text.
 */
class InputConnectionClient(
    private val connection: () -> InputConnection?,
    private val listener: Listener,
) : NativeEngine.Client {

    interface Listener {
        fun onCandidates(candidates: List<String>, cursor: Int)
        fun onModeChanged(mode: Int)
    }

    /** The composing text this client has set, as the engine sees it. */
    var composing: String = ""
        private set

    /** Forgets the composing text, e.g. when the editor has dropped it by itself. */
    fun forgetComposing() {
        composing = ""
    }

    override fun textBeforeCursor(): String? {
        val ic = connection() ?: return null
        val text = ic.getTextBeforeCursor(MAX_TEXT_BEFORE_CURSOR + composing.length, 0) ?: return null
        val s = text.toString()
        return if (composing.isNotEmpty() && s.endsWith(composing)) s.dropLast(composing.length) else s
    }

    override fun deleteBeforeCursor(utf16Length: Int) {
        val ic = connection() ?: return
        clearComposing(ic)
        ic.deleteSurroundingText(utf16Length, 0)
    }

    override fun commitText(text: String) {
        val ic = connection() ?: return
        clearComposing(ic)
        ic.commitText(text, 1)
    }

    override fun updatePreedit(text: String, spans: IntArray) {
        val ic = connection() ?: return
        if (text.isEmpty()) {
            if (composing.isNotEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                composing = ""
            }
            return
        }
        if (text == composing) return
        val s = SpannableString(text)
        for (i in 0 until spans.size / 3) {
            val start = spans[i * 3]
            val end = spans[i * 3 + 1]
            if (end <= start) continue
            s.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_COMPOSING)
            if (spans[i * 3 + 2] == NativeEngine.PREEDIT_CANDIDATE) {
                s.setSpan(BackgroundColorSpan(CANDIDATE_BACKGROUND), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        ic.setComposingText(s, 1)
        composing = text
    }

    override fun updateCandidates(candidates: Array<String>, cursor: Int, visible: Boolean) {
        listener.onCandidates(if (visible) candidates.toList() else emptyList(), cursor)
    }

    override fun modeChanged(mode: Int) = listener.onModeChanged(mode)

    // Removes the composing text so that the following edit happens on the
    // committed text only; the engine sets the composing text again at the end
    // of every key.
    private fun clearComposing(ic: InputConnection) {
        if (composing.isEmpty()) return
        ic.setComposingText("", 1)
        ic.finishComposingText()
        composing = ""
    }

    companion object {
        /** Enough for the longest reading in the dictionaries. */
        const val MAX_TEXT_BEFORE_CURSOR = 256
        private val CANDIDATE_BACKGROUND = Color.argb(0x40, 0x33, 0x66, 0xcc)
    }
}
