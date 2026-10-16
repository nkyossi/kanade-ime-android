package works.firstfruits.hiragana

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    /** The application ID differs from the package of the classes. */
    private val pkg = "works.firstfruits.kanade"
    private val service = "works.firstfruits.hiragana.ime.HiraganaImeService"

    @Test
    fun fullComponentNameIsThisIme() {
        assertTrue(Onboarding.isImeId("$pkg/$service", pkg))
    }

    @Test
    fun shortComponentNameIsThisImeWhenTheIdIsThePackage() {
        val ns = "works.firstfruits.hiragana"
        assertTrue(Onboarding.isImeId("$ns/.ime.HiraganaImeService", ns))
    }

    @Test
    fun otherImesAreNotThisIme() {
        assertFalse(Onboarding.isImeId("com.example/.OtherIme", pkg))
        assertFalse(Onboarding.isImeId("$pkg/.ime.HiraganaImeService", pkg))
        assertFalse(Onboarding.isImeId("com.example/$service", pkg))
    }

    @Test
    fun missingOrMalformedIdIsNotThisIme() {
        assertFalse(Onboarding.isImeId(null, pkg))
        assertFalse(Onboarding.isImeId(pkg, pkg))
        assertFalse(Onboarding.isImeId("", pkg))
    }

    @Test
    fun enabledWhenTheListHasThisIme() {
        assertTrue(Onboarding.isEnabled(listOf("com.example/.OtherIme", "$pkg/$service"), pkg))
        assertFalse(Onboarding.isEnabled(listOf("com.example/.OtherIme"), pkg))
        assertFalse(Onboarding.isEnabled(emptyList(), pkg))
    }
}
