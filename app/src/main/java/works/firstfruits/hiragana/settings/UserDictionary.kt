package works.firstfruits.hiragana.settings

import java.io.File

/**
 * The user dictionary (my.dic) in the SKK format: a line is
 * "reading /candidate1/candidate2/". Comment lines starting with ';' are kept.
 */
data class UserDictionary(val lines: List<Line> = emptyList()) {

    /** One line of the file. */
    sealed interface Line

    /** A ';' comment line, kept as it is. */
    data class Comment(val text: String) : Line

    /** A reading with its candidates. */
    data class Entry(val reading: String, val candidates: List<String>) : Line

    /** The entries only, in file order. */
    fun entries(): List<Entry> = lines.filterIsInstance<Entry>()

    /** Adds [candidates] to [reading], merging with an entry that is already there. */
    fun add(reading: String, candidates: List<String>): UserDictionary {
        val merged = if (lines.any { it is Entry && it.reading == reading }) {
            lines.map {
                if (it is Entry && it.reading == reading) {
                    Entry(reading, (it.candidates + candidates).distinct())
                } else {
                    it
                }
            }
        } else {
            lines + Entry(reading, candidates.distinct())
        }
        return copy(lines = merged)
    }

    /** Removes the entry for [reading]. */
    fun remove(reading: String): UserDictionary =
        copy(lines = lines.filterNot { it is Entry && it.reading == reading })

    /** Writes the file through a temporary file in the same folder. */
    fun save(file: File) {
        file.parentFile?.mkdirs()
        val text = lines.joinToString("\n") { format(it) } + "\n"
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        /** True when [text] is a non-empty reading of hiragana only. */
        fun isValidReading(text: String): Boolean = text.isNotEmpty() && text.all { it in HIRAGANA }

        /** True when [text] is a candidate without the separators '/' and ';'. */
        fun isValidCandidate(text: String): Boolean = text.isNotEmpty() && '/' !in text && ';' !in text

        private val HIRAGANA = '\u3040'..'\u309F'

        /** The dictionary in [file], or an empty one when it is missing or broken. */
        fun load(file: File): UserDictionary {
            if (!file.isFile) return UserDictionary()
            val parsed = mutableListOf<Line>()
            try {
                for (raw in file.readLines()) {
                    val line = raw.trimEnd()
                    when {
                        line.isBlank() -> {}
                        line.startsWith(";") -> parsed += Comment(line)
                        else -> merge(parsed, line)
                    }
                }
            } catch (e: Exception) {
                return UserDictionary()
            }
            return UserDictionary(parsed)
        }

        private fun merge(lines: MutableList<Line>, line: String) {
            val space = line.indexOfFirst { it == ' ' || it == '\t' }
            val reading = if (space < 0) line else line.substring(0, space)
            if (reading.isEmpty()) return
            val candidates = if (space < 0) {
                emptyList()
            } else {
                line.substring(space).split('/').map { it.trim() }.filter { it.isNotEmpty() }
            }
            val index = lines.indexOfFirst { it is Entry && it.reading == reading }
            if (index >= 0) {
                val old = lines[index] as Entry
                lines[index] = Entry(reading, (old.candidates + candidates).distinct())
            } else {
                lines += Entry(reading, candidates)
            }
        }

        private fun format(line: Line): String = when (line) {
            is Comment -> line.text
            is Entry -> if (line.candidates.isEmpty()) {
                line.reading
            } else {
                line.reading + " " + line.candidates.joinToString("/", "/", "/")
            }
        }
    }
}
