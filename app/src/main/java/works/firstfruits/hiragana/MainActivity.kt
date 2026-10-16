package works.firstfruits.hiragana

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** The onboarding screen: enable the IME, pick it, then try it. */
class MainActivity : Activity() {
    private lateinit var enabledStatus: TextView
    private lateinit var enabledButton: Button
    private lateinit var selectedStatus: TextView
    private lateinit var selectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)

            addView(heading(R.string.onboarding_title))

            addView(label(R.string.onboarding_step1))
            enabledStatus = status()
            addView(enabledStatus)
            enabledButton = button(R.string.open_ime_settings) {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            addView(enabledButton)

            addView(label(R.string.onboarding_step2))
            selectedStatus = status()
            addView(selectedStatus)
            selectButton = button(R.string.choose_ime) {
                inputMethodManager()?.showInputMethodPicker()
            }
            addView(selectButton)

            addView(label(R.string.onboarding_step3))
            addView(EditText(context).apply {
                hint = getString(R.string.try_here)
                textSize = 18f
                minLines = 4
            })
            addView(label(R.string.try_hint))
        })
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateState()
    }

    /** Shows what is already done and enables step 2 only after step 1. */
    private fun updateState() {
        val enabled = Onboarding.isEnabled(enabledImeIds(), packageName)
        enabledStatus.text = if (enabled) getString(R.string.done_mark) else ""
        enabledButton.visibility = if (enabled) View.GONE else View.VISIBLE

        val selected = Onboarding.isImeId(currentImeId(), packageName)
        selectedStatus.text = if (selected) getString(R.string.done_mark) else ""
        selectButton.visibility = if (selected) View.GONE else View.VISIBLE
        selectButton.isEnabled = enabled
    }

    private fun enabledImeIds(): List<String> =
        inputMethodManager()?.enabledInputMethodList?.map { it.id }.orEmpty()

    private fun currentImeId(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

    private fun inputMethodManager(): InputMethodManager? =
        getSystemService(InputMethodManager::class.java)

    private fun heading(resId: Int) = TextView(this).apply {
        setText(resId)
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun label(resId: Int) = TextView(this).apply {
        setText(resId)
        textSize = 18f
    }

    private fun status() = TextView(this).apply { textSize = 18f }

    private fun button(resId: Int, onClick: () -> Unit) = Button(this).apply {
        setText(resId)
        textSize = 18f
        setOnClickListener { onClick() }
    }
}
