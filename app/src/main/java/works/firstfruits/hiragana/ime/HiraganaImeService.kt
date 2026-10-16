package works.firstfruits.hiragana.ime

import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import works.firstfruits.hiragana.keyboard.CandidateBar
import works.firstfruits.hiragana.keyboard.FlickDirection
import works.firstfruits.hiragana.keyboard.Key
import works.firstfruits.hiragana.keyboard.KeyAction
import works.firstfruits.hiragana.keyboard.KeyboardInputView
import works.firstfruits.hiragana.keyboard.KeyboardLayout
import works.firstfruits.hiragana.keyboard.KeyboardOptions
import works.firstfruits.hiragana.keyboard.KeyboardView
import works.firstfruits.hiragana.keyboard.Layouts
import works.firstfruits.hiragana.keyboard.ShiftState
import works.firstfruits.hiragana.settings.ImeSettings
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
    private val shift = ShiftState()
    /** The settings that change the layouts; reloaded with the other settings. */
    private var options = KeyboardOptions()
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
        options = loadOptions()
        val bar = CandidateBar(this).apply {
            listener = CandidateBar.Listener { index -> onCandidate(index) }
        }
        val keyboard = KeyboardView(this).apply {
            threeStageFlick = SettingsJson.threeStageFlick(AssetInstaller.settingsFile(this@HiraganaImeService))
            layout = Layouts.byId(KeyboardLayout.QWERTY, options)
            listener = object : KeyboardView.Listener {
                override fun onKey(key: Key, direction: FlickDirection) =
                    this@HiraganaImeService.onKey(key, direction)

                override fun onKana(kana: String) = this@HiraganaImeService.onKana(kana)

                override fun onLongPress(key: Key) = this@HiraganaImeService.onLongPress(key)
            }
        }
        candidateBar = bar
        keyboardView = keyboard
        updateModeLabel()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar)
            addView(keyboard)
        }
        return KeyboardInputView(this).apply {
            this.keyboard = keyboard
            keyboard.guideOverlay = this
            addView(column, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            ))
        }
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        client.forgetComposing()
        engineCandidatesShown = false
        startEngine(attribute)
        refreshPreview()
    }

    override fun onFinishInput() {
        engine?.let { e ->
            edit {
                e.commitAll()
                e.focusOut()
            }
        }
        client.forgetComposing()
        engineCandidatesShown = false
        preview = emptyList()
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
            engineCandidatesShown = false
            candidateBar?.setCandidates(emptyList(), -1)
        }
        // The cursor may have moved to other kana.
        if (client.composing.isEmpty() && newSelStart == newSelEnd) refreshPreview()
    }

    // The candidate bar shows either the engine's candidates during a
    // conversion, or the preview of what the convert key would show.
    private var engineCandidatesShown = false
    private var preview: List<String> = emptyList()

    private fun onCandidate(index: Int) {
        val e = engine ?: return
        edit {
            if (engineCandidatesShown) {
                e.candidateClicked(index)
            } else {
                preview.getOrNull(index)?.let { e.previewSelected(it) }
            }
        }
        refreshPreview()
    }

    /** Shows the preview candidates unless the engine shows its own. */
    private fun refreshPreview() {
        val e = engine ?: return
        if (engineCandidatesShown) return
        // Keep the last preview while roman letters are pending, so that the
        // bar does not flicker between the keys of a kana.
        if (e.hasPendingInput()) return
        preview = e.previewCandidates()
        candidateBar?.setCandidates(preview, -1)
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
            keyboardView?.threeStageFlick = SettingsJson.threeStageFlick(settings)
            options = loadOptions()
            keyboardView?.let { it.layout = Layouts.byId(it.layout.id, options) }
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
        asciiField = isAsciiField(attribute)
        e.mode = if (asciiField) NativeEngine.MODE_ALPHANUMERIC else textMode
        ensureKanaMode()
        e.focusIn()
        updateModeLabel()
    }

    private var asciiField = false

    // The 12-key layout has kana only and no あ/A key, so it must not stay in
    // the alphanumeric mode, where the kana could not be converted. This holds in
    // every field, the ASCII ones included.
    private fun ensureKanaMode() {
        val e = engine ?: return
        if (keyboardView?.layout?.id != KeyboardLayout.FLICK) return
        if (e.mode == NativeEngine.MODE_ALPHANUMERIC || e.mode == NativeEngine.MODE_WIDE_ALPHANUMERIC) {
            edit { e.commitAll() }
            e.mode = NativeEngine.MODE_HIRAGANA
        }
    }

    /** The space of [action]: half-width in the A mode and full-width otherwise, or the other one when flipped. */
    private fun blankText(action: KeyAction.Blank): String {
        val alphanumeric = engine?.mode == NativeEngine.MODE_ALPHANUMERIC
        return if (alphanumeric != action.flipped) " " else "　"
    }

    /** Commits the kana being typed and switches the engine to the A mode. */
    private fun switchToAlphanumeric() {
        val e = engine ?: return
        if (e.mode == NativeEngine.MODE_ALPHANUMERIC) return
        edit {
            e.commitAll()
            e.mode = NativeEngine.MODE_ALPHANUMERIC
        }
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
        if (action is KeyAction.Flick) {
            // A flick key runs one of its actions, chosen by the direction.
            val chosen = action.actionFor(direction) ?: return
            onKey(Key(key.label, chosen), FlickDirection.CENTER)
            return
        }
        when (action) {
            is KeyAction.SwitchLayout -> {
                val fromFlick = keyboardView?.layout?.id == KeyboardLayout.FLICK
                keyboardView?.layout = Layouts.byId(action.id, options)
                // The 12-key layout is kana only, so the roman keys start in the A mode.
                if (fromFlick && action.id == KeyboardLayout.QWERTY) switchToAlphanumeric()
                ensureKanaMode()
                updateModeLabel()
                refreshPreview()
                return
            }
            KeyAction.Shift -> {
                shift.tap(SystemClock.uptimeMillis())
                showShift()
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
                    val c = letterChar(action, direction)
                    e.tapKey(c.code, Keysym.keycodeOf(c), if (c.isLetter() && shift.shifted) Keysym.SHIFT_MASK else 0)
                }
                is KeyAction.Kana -> action.kanaFor(direction)?.let { e.insertKana(it) } ?: true
                is KeyAction.Text -> {
                    e.commitAll()
                    false
                }
                KeyAction.Paste -> {
                    e.commitAll()
                    false
                }
                is KeyAction.Blank -> {
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
                is KeyAction.Flick -> true
                is KeyAction.SwitchLayout, KeyAction.Shift -> true
            }
            if (!processed) fallback(action, direction)
        }
        refreshPreview()
        // A digit from a down flick does not use up the shift.
        if (action is KeyAction.Letter && !(direction == FlickDirection.DOWN && action.down != null)) {
            shift.letterTyped()
            showShift()
        }
    }

    /** A long press sends the text of the key's long press, like a text key. */
    private fun onLongPress(key: Key) {
        val text = (key.action as? KeyAction.Letter)?.long ?: return
        onKey(Key(key.label, KeyAction.Text(text)), FlickDirection.CENTER)
    }

    /**
     * The character a letter key sends: its digit on a down flick, else the letter
     * in the shift state.
     */
    private fun letterChar(action: KeyAction.Letter, direction: FlickDirection): Char {
        val digit = action.down
        if (digit != null && direction == FlickDirection.DOWN) return digit
        return if (shift.shifted) action.char.uppercaseChar() else action.char
    }

    /** A three-stage flick chose [kana]; it is inserted like a plain kana key. */
    private fun onKana(kana: String) {
        val e = engine
        if (e == null) {
            // The dictionaries are still loading.
            currentInputConnection?.commitText(kana, 1)
            return
        }
        edit {
            if (!e.insertKana(kana)) currentInputConnection?.commitText(kana, 1)
        }
        refreshPreview()
    }

    /** What the app gets for a key the engine has not processed. */
    private fun fallback(action: KeyAction, direction: FlickDirection) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.Letter -> ic.commitText(letterChar(action, direction).toString(), 1)
            is KeyAction.Kana -> action.kanaFor(direction)?.let { ic.commitText(it, 1) }
            is KeyAction.Text -> ic.commitText(action.text, 1)
            is KeyAction.Blank -> ic.commitText(blankText(action), 1)
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
            KeyAction.Paste -> clipboardText()?.let { ic.commitText(it, 1) }
            else -> {}
        }
    }

    /** The clipboard text, or null when there is nothing to paste. */
    private fun clipboardText(): String? {
        val manager = getSystemService(ClipboardManager::class.java) ?: return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()?.takeIf { it.isNotEmpty() }
    }

    /** Shows the state of the shift on the keyboard. */
    private fun showShift() {
        keyboardView?.shifted = shift.shifted
        keyboardView?.capsLock = shift.locked
    }

    private fun loadOptions(): KeyboardOptions =
        ImeSettings.load(AssetInstaller.settingsFile(this)).keyboardOptions()

    private fun updateModeLabel() {
        // The 12-key layout is always in the hiragana mode; no need to show it.
        if (keyboardView?.layout?.id == KeyboardLayout.FLICK) {
            candidateBar?.setMode(null)
            return
        }
        val mode = engine?.mode ?: NativeEngine.MODE_ALPHANUMERIC
        candidateBar?.setMode(String(Character.toChars(mode)))
    }

    // InputConnectionClient.Listener

    override fun onCandidates(candidates: List<String>, cursor: Int) {
        engineCandidatesShown = candidates.isNotEmpty()
        if (engineCandidatesShown) preview = emptyList()
        candidateBar?.setCandidates(candidates, cursor)
    }

    override fun onModeChanged(mode: Int) {
        updateModeLabel()
    }

    companion object {
        private const val TAG = "HiraganaIME"
    }
}
