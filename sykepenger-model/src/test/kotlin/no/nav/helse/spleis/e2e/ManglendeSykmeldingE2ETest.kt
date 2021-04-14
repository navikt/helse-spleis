package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/*
    TESTPLAN
    ==============================================================================
                                             |   Søknad   |   Søknad arbeidsgiver
    Uten perioder fra før                    |     x      |
    ^-- med mottatt IM                       |            |
    Med uferdig gap foran                    |     x      |
    Med uferdig forlengelse foran            |     x      |
    Med ferdig gap foran                     |            |
    Med ferdig forlengelse foran             |            |
    Forlengelse med refusjon opphørt         |            |
    Overlapper med forkastet                 |            |
    Med perioder etter (out of order)        |     -      |
    ^-- med Inntektsmelding                  |            |
    Med uferdig gap etter                    |     x      |
    Med uferdig forlengelse etter            |     x      |
    Med ferdig gap etter                     |            |
    Med ferdig forlengelse etter             |            |                          Sende inn kort AG-søknad. Sende inn Sykmelding. Sende inn Søknad foran kort AG-søknad.
*/

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManglendeSykmeldingE2ETest : AbstractEndToEndTest() {

    @BeforeAll
    fun setup() {
        Toggles.OppretteVedtaksperioderVedSøknad.enable()
    }

    @AfterAll
    fun teardown() {
        Toggles.OppretteVedtaksperioderVedSøknad.disable()
    }

    @Test
    fun `uten perioder fra før`() {
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTrue(observatør.bedtOmInntektsmeldingReplay(1.vedtaksperiode))
    }

    @Test
    fun `uferdig gap foran`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode))
    }

    @Test
    fun `uferdig forlengelse foran`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `ferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE)
    }
}
