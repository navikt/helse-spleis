package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForeldetSøknadE2ETest: AbstractEndToEndTest() {
    @Test
    fun `forledet søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar,  31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        assertForventetFeil(
            forklaring = "Det opprettes søknad uavhengig fra sykmeldingsperioder",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
            },
            ønsket = {
                assertEquals(0, inspektør.vedtaksperiodeTeller)
            }
        )
    }
}
