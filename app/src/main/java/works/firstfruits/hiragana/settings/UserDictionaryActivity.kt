package works.firstfruits.hiragana.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import works.firstfruits.hiragana.R
import works.firstfruits.hiragana.ime.AssetInstaller
import java.io.File

/** Edits my.dic: a list of "reading -> candidates", with add and long-press delete. */
class UserDictionaryActivity : AppCompatActivity() {
    private lateinit var dictionary: UserDictionary
    private lateinit var list: LinearLayout

    private val density get() = resources.displayMetrics.density
    private val dicFile: File get() = File(AssetInstaller.userDir(this), "my.dic")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dictionary = UserDictionary.load(dicFile)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val padding = (16 * density).toInt()
        val add = Button(this).apply {
            setText(R.string.user_dictionary_add)
            textSize = 18f
            setOnClickListener { showAddDialog() }
        }
        val scroll = ScrollView(this).apply { addView(list) }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(add)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val padding = (8 * density).toInt()
        for (entry in dictionary.entries()) {
            list.addView(TextView(this).apply {
                text = getString(
                    R.string.user_dictionary_row,
                    entry.reading,
                    entry.candidates.joinToString("、"),
                )
                textSize = 18f
                setPadding(0, padding, 0, padding)
                setOnLongClickListener {
                    confirmDelete(entry.reading)
                    true
                }
            })
        }
    }

    /** Asks for a reading and candidates, then adds and saves them. */
    private fun showAddDialog() {
        val reading = EditText(this).apply { hint = getString(R.string.user_dictionary_reading_hint) }
        val candidates = EditText(this).apply { hint = getString(R.string.user_dictionary_candidate_hint) }
        val pad = (16 * density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(reading)
            addView(candidates)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.user_dictionary_add)
            .setView(content)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val text = reading.text.toString().trim()
                val words = candidates.text.toString()
                    .split('、', ',', ' ', '　', '\t').map { it.trim() }.filter { it.isNotEmpty() }
                val error = when {
                    !UserDictionary.isValidReading(text) -> R.string.user_dictionary_error_reading
                    words.isEmpty() || words.any { !UserDictionary.isValidCandidate(it) } ->
                        R.string.user_dictionary_error_candidate
                    else -> 0
                }
                if (error != 0) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dictionary = dictionary.add(text, words)
                dictionary.save(dicFile)
                refresh()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(reading: String) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.user_dictionary_delete, reading))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                dictionary = dictionary.remove(reading)
                dictionary.save(dicFile)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
