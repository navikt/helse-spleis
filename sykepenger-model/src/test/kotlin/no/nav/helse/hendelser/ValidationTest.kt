package no.nav.helse.hendelser

import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class ValidationTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun success() {
        var success = false
        validation(aktivitetslogg) {
            onValidationFailed { fail("Uventet kall") }
            successBlock()
            onSuccess { success = true }
        }
        assertTrue(success)
    }

    @Test
    fun failure() {
        var failure = false
        validation(aktivitetslogg) {
            onValidationFailed { failure = true }
            failureBlock()
            onSuccess { fail("Uventet kall") }
        }
        assertTrue(failure)
    }

    private fun Validation.successBlock() = valider { true }
    private fun Validation.failureBlock() = valider { false }
}
