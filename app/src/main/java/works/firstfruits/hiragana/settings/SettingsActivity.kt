package works.firstfruits.hiragana.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import works.firstfruits.hiragana.R
import works.firstfruits.hiragana.ime.AssetInstaller
import works.firstfruits.hiragana.keyboard.VowelLongPress
import java.io.File

/** The IME settings screen; changes are written to settings.json by [ImeSettings]. */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}

/** The list of settings, backed by settings.json instead of SharedPreferences. */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            SettingsDataStore(AssetInstaller.settingsFile(requireContext()))
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>(KEY_USER_DICTIONARY)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), UserDictionaryActivity::class.java))
            true
        }
        findPreference<Preference>(KEY_HISTORY_RESET)?.setOnPreferenceClickListener {
            confirmHistoryReset()
            true
        }
    }

    /** Asks before clearing the input history, then does it. */
    private fun confirmHistoryReset() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.history_reset_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val context = requireContext()
                clearHistory(AssetInstaller.userDir(context), AssetInstaller.settingsFile(context))
                Toast.makeText(context, R.string.history_reset_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val KEY_USER_DICTIONARY = "user_dictionary"
        const val KEY_HISTORY_RESET = "history_reset"
    }
}

/** A [PreferenceDataStore] that loads and saves [ImeSettings]. */
private class SettingsDataStore(private val file: File) : PreferenceDataStore() {
    private var settings = ImeSettings.load(file)

    override fun getString(key: String?, defValue: String?): String? = when (key) {
        ImeSettings.KEY_MODE -> settings.mode
        ImeSettings.KEY_DICTIONARY -> settings.dictionary
        ImeSettings.KEY_RARE_CENTER -> settings.rareCenter
        ImeSettings.KEY_RARE_UP -> settings.rareUp
        ImeSettings.KEY_RARE_DOWN -> settings.rareDown
        ImeSettings.KEY_VOWEL_LONG_PRESS -> settings.vowelLongPress.name.lowercase()
        else -> defValue
    }

    override fun putString(key: String?, value: String?) = update {
        when (key) {
            ImeSettings.KEY_MODE -> it.copy(mode = value ?: it.mode)
            ImeSettings.KEY_DICTIONARY -> it.copy(dictionary = value ?: it.dictionary)
            ImeSettings.KEY_RARE_CENTER -> it.copy(rareCenter = value ?: it.rareCenter)
            ImeSettings.KEY_RARE_UP -> it.copy(rareUp = value ?: it.rareUp)
            ImeSettings.KEY_RARE_DOWN -> it.copy(rareDown = value ?: it.rareDown)
            ImeSettings.KEY_VOWEL_LONG_PRESS -> {
                val press = VowelLongPress.entries.firstOrNull { it.name.lowercase() == value }
                if (press != null) it.copy(vowelLongPress = press) else it
            }
            else -> it
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = when (key) {
        ImeSettings.KEY_PERMISSIBLE -> settings.permissible
        ImeSettings.KEY_HALF_WIDTH_DIGITS -> settings.useHalfWidthDigits
        ImeSettings.KEY_NN_AS_JIS_X_4063 -> settings.nnAsJisX4063
        ImeSettings.KEY_THREE_STAGE_FLICK -> settings.threeStageFlick
        ImeSettings.KEY_LETTER_DOWN_DIGITS -> settings.letterDownDigits
        else -> defValue
    }

    override fun putBoolean(key: String?, value: Boolean) = update {
        when (key) {
            ImeSettings.KEY_PERMISSIBLE -> it.copy(permissible = value)
            ImeSettings.KEY_HALF_WIDTH_DIGITS -> it.copy(useHalfWidthDigits = value)
            ImeSettings.KEY_NN_AS_JIS_X_4063 -> it.copy(nnAsJisX4063 = value)
            ImeSettings.KEY_THREE_STAGE_FLICK -> it.copy(threeStageFlick = value)
            ImeSettings.KEY_LETTER_DOWN_DIGITS -> it.copy(letterDownDigits = value)
            else -> it
        }
    }

    /** Re-reads the file first, so other keys like history-generation stay fresh. */
    private fun update(change: (ImeSettings) -> ImeSettings) {
        val current = ImeSettings.load(file)
        val changed = change(current)
        if (changed != current) {
            changed.save(file)
            settings = changed
        }
    }
}
