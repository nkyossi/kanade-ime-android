package works.firstfruits.hiragana

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

/** The launcher screen: leads the user to enable the keyboard and lets them try it. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(Button(context).apply {
                setText(R.string.open_ime_settings)
                setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            })
            addView(Button(context).apply {
                setText(R.string.choose_ime)
                setOnClickListener { getSystemService(InputMethodManager::class.java).showInputMethodPicker() }
            })
            addView(EditText(context).apply {
                setHint(R.string.try_here)
                minLines = 4
            })
        })
    }
}
