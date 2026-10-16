package works.firstfruits.hiragana.keyboard

import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    private val layouts = listOf(
        QwertyLayout.layout,
        FlickLayout.layout,
        SymbolLayout.numbers,
        SymbolLayout.symbols,
    )

    @Test
    fun switchLayoutTargetsAreRegistered() {
        val ids = layouts.map { it.id }.toSet()
        for (layout in layouts) for (row in layout.rows) for (key in row.keys) {
            for (action in switchTargets(key.action)) {
                assertTrue("${layout.id} -> ${action.id}", action.id in ids)
            }
        }
    }

    private fun switchTargets(action: KeyAction): List<KeyAction.SwitchLayout> = when (action) {
        is KeyAction.SwitchLayout -> listOf(action)
        is KeyAction.Flick -> action.actions.filterIsInstance<KeyAction.SwitchLayout>()
        else -> emptyList()
    }

    @Test
    fun everyLayoutHasBackspaceAndEnter() {
        for (layout in layouts) {
            val actions = layout.rows.flatMap { it.keys }.map { it.action }
            assertTrue("${layout.id} has no ⌫", actions.any { it is KeyAction.Backspace })
            assertTrue("${layout.id} has no 確定", actions.any { it == KeyAction.Enter })
        }
    }

    @Test
    fun rowsAreNotEmpty() {
        for (layout in layouts) for (row in layout.rows) {
            assertTrue(layout.id, row.keys.isNotEmpty())
        }
    }
}
