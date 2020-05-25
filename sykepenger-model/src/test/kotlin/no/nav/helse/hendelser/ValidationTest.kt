package no.nav.helse.hendelser

import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
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
        validation(TestHendelse(aktivitetslogg)) {
            onError { fail("Uventet kall") }
            successBlock()
            onSuccess { success = true }
        }
        assertTrue(success)
    }

    @Test
    fun failure() {
        var failure = false
        validation(TestHendelse(aktivitetslogg)) {
            onError { failure = true }
            failureBlock()
            onSuccess { fail("Uventet kall") }
        }
        assertTrue(failure)
    }

    private fun Validation.successBlock() = valider("feilmelding") { true }
    private fun Validation.failureBlock() = valider("feilmelding") { false }

    private inner class TestHendelse(aktivitetslogg: Aktivitetslogg) : ArbeidstakerHendelse(aktivitetslogg) {

        override fun aktørId() = "aktørId"

        override fun fødselsnummer() = "fnr"

        override fun organisasjonsnummer() = "orgnr"
    }
}
