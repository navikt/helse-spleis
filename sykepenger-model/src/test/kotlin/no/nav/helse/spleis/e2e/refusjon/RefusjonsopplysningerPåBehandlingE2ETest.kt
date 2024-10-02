package no.nav.helse.spleis.e2e.refusjon

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerPåBehandlingE2ETest : AbstractDslTest() {

    @Test
    fun `ny vedtaksperiode`() {
        håndterSøknad(januar)

        assertEquals(Beløpstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }
}