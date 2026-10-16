package works.firstfruits.hiragana

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    private val pkg = "works.firstfruits.hiragana"

    @Test
    fun shortComponentNameIsThisIme() {
        assertTrue(Onboarding.isImeId("$pkg/.ime.HiraganaImeService", pkg))
    }

    @Test
    fun fullComponentNameIsThisIme() {
        assertTrue(Onboarding.isImeId("$pkg/$pkg.ime.HiraganaImeService", pkg))
    }

    @Test
    fun otherImesAreNotThisIme() {
        assertFalse(Onboarding.isImeId("com.example/.OtherIme", pkg))
        assertFalse(Onboarding.isImeId("$pkg/.ime.OtherIme", pkg))
    }

    @Test
    fun missingOrMalformedIdIsNotThisIme() {
        assertFalse(Onboarding.isImeId(null, pkg))
        assertFalse(Onboarding.isImeId(pkg, pkg))
        assertFalse(Onboarding.isImeId("", pkg))
    }

    @Test
    fun enabledWhenTheListHasThisIme() {
        assertTrue(Onboarding.isEnabled(listOf("com.example/.OtherIme", "$pkg/.ime.HiraganaImeService"), pkg))
        assertFalse(Onboarding.isEnabled(listOf("com.example/.OtherIme"), pkg))
        assertFalse(Onboarding.isEnabled(emptyList(), pkg))
    }
}
