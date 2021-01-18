package no.nav.helse.spleis.e2e.korrigering

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class KorrigertSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `Arbeidsdag i søknad nr 2 kaster ut perioden`() {
        Toggles.KorrigertSøknadToggle.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent), Arbeid(31.januar, 31.januar))
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
        }
    }
}
