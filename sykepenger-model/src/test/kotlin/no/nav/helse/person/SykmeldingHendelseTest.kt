package no.nav.helse.person

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykmeldingHendelseTest : AbstractDslTest() {

    @Test
    fun `Sykmelding skaper Arbeidsgiver og Sykmeldingsperiode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            assertIngenFunksjonelleFeil()
            assertEquals(0, inspektør.vedtaksperiodeTeller)
            assertEquals(1, inspektør.sykmeldingsperioder().size)
        }
    }

    @Test
    fun `To søknader uten overlapp`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(6.januar, 10.januar))

            assertIngenFunksjonelleFeil()
            assertEquals(0, inspektør.vedtaksperiodeTeller)
            assertEquals(2, inspektør.sykmeldingsperioder().size)
        }
    }
}
