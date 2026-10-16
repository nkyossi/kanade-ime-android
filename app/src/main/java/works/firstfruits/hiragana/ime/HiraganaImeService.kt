package works.firstfruits.hiragana.ime

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import works.firstfruits.hiragana.keyboard.CandidateBar
import works.firstfruits.hiragana.keyboard.FlickDirection
import works.firstfruits.hiragana.keyboard.Key
import works.firstfruits.hiragana.keyboard.KeyAction
import works.firstfruits.hiragana.keyboard.KeyboardView
import works.firstfruits.hiragana.keyboard.Layouts
import java.io.File
import java.util.concurrent.Executors

/**
 * The input method service: passes the keys of the software keyboard to the
 * engine and the engine's edits to the text field.
 */
class HiraganaImeService : InputMethodService(), InputConnectionClient.Listener {

    private var engine: NativeEngine? = null
    private val client = InputConnectionClient({ currentInputConnection }, this)
    private val loader = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var keyboardView: KeyboardView? = null
    private var candidateBar: CandidateBar? = null
    private var shifted = false
    /** The mode to start a normal text field in. */
    private var textMode = NativeEngine.MODE_HIRAGANA

    override fun onCreate() {
        super.onCreate()
        loader.execute {
            AssetInstaller.install(this)
            val e = NativeEngine.create(
                AssetInstaller.systemDir(this),
                AssetInstaller.userDir(this),
                AssetInstaller.settingsFile(this),
            )
            mainHandler.post {
                if (e == null) {
                    Log.e(TAG, "could not load the engine")
                    return@post
                }
                engine = e
                e.setClient(client)
                currentInputEditorInfo?.let { startEngine(it) }
            }
        }
    }

    override fun onDestroy() {
        engine?.close()
        engine = null
        loader.shutdown()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val bar = CandidateBar(this).apply {
            listener = CandidateBar.Listener { index -> edit { engine?.candidateClicked(index) } }
        }
        val keyboard = KeyboardView(this).apply {
            listener = KeyboardView.Listener { key, direction -> onKey(key, direction) }
        }
        candidateBar = bar
        keyboardView = keyboard
        updateModeLabel()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar)
            addView(keyboard)
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        client.forgetComposing()
        startEngine(attribute)
    }

    override fun onFinishInput() {
        engine?.let { e ->
            edit {
                e.commitAll()
                e.focusOut()
            }
        }
        client.forgetComposing()
        candidateBar?.setCandidates(emptyList(), -1)
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // The editor has dropped the composing text by itself, e.g. when the
        // user has moved the cursor during the conversion.
        if (client.composing.isNotEmpty() && candidatesStart == -1) {
            client.forgetComposing()
            engine?.compositionTerminated()
            candidateBar?.setCandidates(emptyList(), -1)
        }
    }

    // The settings screen and the user dictionary editor only write their
    // files; pick up the changes when a text field gets the focus.
    private var settingsStamp = 0L
    private var userDictionaryStamp = 0L

    private fun reloadIfChanged(e: NativeEngine) {
        val settings = AssetInstaller.settingsFile(this)
        if (settings.lastModified() != settingsStamp) {
            if (settingsStamp != 0L) e.applySettings(settings)
            settingsStamp = settings.lastModified()
            textMode = SettingsJson.startMode(settings) ?: textMode
        }
        val userDictionary = File(AssetInstaller.userDir(this), "my.dic")
        if (userDictionary.lastModified() != userDictionaryStamp) {
            if (userDictionaryStamp != 0L) e.reloadDictionary()
            userDictionaryStamp = userDictionary.lastModified()
        }
    }

    private fun startEngine(attribute: EditorInfo) {
        val e = engine ?: return
        reloadIfChanged(e)
        val supported = attribute.inputType != InputType.TYPE_NULL &&
            currentInputConnection?.getTextBeforeCursor(1, 0) != null
        e.setSurroundingSupported(supported)
        e.mode = if (isAsciiField(attribute)) NativeEngine.MODE_ALPHANUMERIC else textMode
        e.focusIn()
        updateModeLabel()
    }

    private fun isAsciiField(attribute: EditorInfo): Boolean {
        val type = attribute.inputType
        return when (type and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> true
            InputType.TYPE_CLASS_TEXT -> when (type and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_URI -> true
                else -> false
            }
            else -> false
        }
    }

    /** Runs [block] as one batch edit of the text field. */
    private inline fun edit(block: () -> Unit) {
        val ic = currentInputConnection
        ic?.beginBatchEdit()
        try {
            block()
        } finally {
            ic?.endBatchEdit()
        }
    }

    private fun onKey(key: Key, direction: FlickDirection) {
        val action = key.action
        when (action) {
            is KeyAction.SwitchLayout -> {
                keyboardView?.layout = Layouts.byId(action.id)
                return
            }
            KeyAction.Shift -> {
                setShifted(!shifted)
                return
            }
            else -> {}
        }
        val e = engine
        if (e == null) {
            // The dictionaries are still loading.
            fallback(action, direction)
            return
        }
        edit {
            val processed = when (action) {
                is KeyAction.Letter -> {
                    val c = if (shifted) action.char.uppercaseChar() else action.char
                    e.tapKey(c.code, Keysym.keycodeOf(c), if (shifted) Keysym.SHIFT_MASK else 0)
                }
                is KeyAction.Kana -> action.kanaFor(direction)?.let { e.insertKana(it) } ?: true
                is KeyAction.Text -> {
                    e.commitAll()
                    false
                }
                KeyAction.Convert, KeyAction.Space -> e.tapKey(Keysym.SPACE, Keysym.KEY_SPACE)
                KeyAction.Okurigana -> e.tapKey(Keysym.HENKAN, Keysym.KEY_HENKAN, Keysym.SHIFT_MASK)
                KeyAction.Katakana -> e.tapKey(Keysym.SHIFT_R, Keysym.KEY_RIGHTSHIFT)
                KeyAction.Shrink -> e.tapKey(Keysym.TAB, Keysym.KEY_TAB)
                KeyAction.Expand -> e.tapKey(Keysym.TAB, Keysym.KEY_TAB, Keysym.SHIFT_MASK)
                KeyAction.Enter -> e.tapKey(Keysym.RETURN, Keysym.KEY_ENTER)
                KeyAction.Cancel -> e.tapKey(Keysym.ESCAPE, Keysym.KEY_ESC)
                KeyAction.Backspace -> e.tapKey(Keysym.BACKSPACE, Keysym.KEY_BACKSPACE)
                KeyAction.KanaCycle -> e.cycleKana()
                KeyAction.ToggleMode -> {
                    e.commitAll()
                    e.mode = if (e.mode == NativeEngine.MODE_ALPHANUMERIC) NativeEngine.MODE_HIRAGANA
                    else NativeEngine.MODE_ALPHANUMERIC
                    true
                }
                KeyAction.CursorLeft, KeyAction.CursorRight -> {
                    e.commitAll()
                    false
                }
                is KeyAction.SwitchLayout, KeyAction.Shift -> true
            }
            if (!processed) fallback(action, direction)
        }
        if (shifted && action is KeyAction.Letter) setShifted(false)
    }

    /** What the app gets for a key the engine has not processed. */
    private fun fallback(action: KeyAction, direction: FlickDirection) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.Letter -> ic.commitText(
                (if (shifted) action.char.uppercaseChar() else action.char).toString(), 1)
            is KeyAction.Kana -> action.kanaFor(direction)?.let { ic.commitText(it, 1) }
            is KeyAction.Text -> ic.commitText(action.text, 1)
            KeyAction.Convert, KeyAction.Space -> ic.commitText(" ", 1)
            KeyAction.Enter -> {
                val info = currentInputEditorInfo
                val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                val noEnterAction = info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                if (!noEnterAction && actionId != EditorInfo.IME_ACTION_NONE &&
                    actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            KeyAction.Backspace -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            KeyAction.Shrink, KeyAction.Expand -> sendDownUpKeyEvents(KeyEvent.KEYCODE_TAB)
            KeyAction.CursorLeft -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            KeyAction.CursorRight -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            else -> {}
        }
    }

    private fun setShifted(value: Boolean) {
        shifted = value
        keyboardView?.shifted = value
    }

    private fun updateModeLabel() {
        val mode = engine?.mode ?: NativeEngine.MODE_ALPHANUMERIC
        candidateBar?.setMode(String(Character.toChars(mode)))
    }

    // InputConnectionClient.Listener

    override fun onCandidates(candidates: List<String>, cursor: Int) {
        candidateBar?.setCandidates(candidates, cursor)
    }

    override fun onModeChanged(mode: Int) {
        updateModeLabel()
    }

    companion object {
        private const val TAG = "HiraganaIME"
    }
}
