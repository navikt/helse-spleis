package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvbruttSøknadE2ETest : AbstractDslTest() {

    @Test
    fun `avbrutt søknad på ukjent arbeidsgiver`() {
        a1 {
            håndterAvbruttSøknad(januar)
            assertEquals(emptyList<Nothing>(), inspektør.sykmeldingsperioder())
        }
    }
}
