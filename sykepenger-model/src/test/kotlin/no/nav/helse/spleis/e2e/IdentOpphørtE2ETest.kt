package no.nav.helse.spleis.e2e

import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class IdentOpphørtE2ETest : AbstractDslTest() {

    @Test
    fun `endrer fødselsnummer ved ident opphørt`() {
        val nyttFnr = Personidentifikator("12345678911")
        a1 {
            håndterIdentOpphørt(nyttFnr)
        }
        assertEquals(nyttFnr, inspiser(personInspektør).personidentifikator)
    }
}