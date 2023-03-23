package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DødsmeldingE2E : AbstractDslTest() {

    @Test
    fun `registrerer dødsdato`() {
        val dødsdato = 10.januar
        håndterDødsmelding(dødsdato)
        assertEquals(dødsdato, inspiser(personInspektør).dødsdato)
    }
}