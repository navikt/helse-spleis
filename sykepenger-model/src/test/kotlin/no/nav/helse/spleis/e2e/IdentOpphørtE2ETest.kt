package no.nav.helse.spleis.e2e

import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.AbstractDslTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class IdentOpphørtE2ETest : AbstractDslTest() {

    @Test
    fun `endrer fødselsnummer ved ident opphørt`() {
        val nyttFnr = Personidentifikator("12345678911")
        val nyAktørId = "3219876543219"
        håndterIdentOpphørt(nyttFnr, nyAktørId)
        assertEquals(nyttFnr, inspiser(personInspektør).personidentifikator)
    }
}