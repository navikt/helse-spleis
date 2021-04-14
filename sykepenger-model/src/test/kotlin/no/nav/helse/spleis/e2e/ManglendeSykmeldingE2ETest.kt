package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/*
    TESTPLAN
    ==============================================================================
                                             |   Søknad   |   Søknad arbeidsgiver
    Uten perioder fra før                    |     x      |
    ^-- med mottatt IM                       |     x      |
    Med uferdig gap foran                    |     x      |
    Med uferdig forlengelse foran            |     x      |
    Med ferdig gap foran                     |     x      |
    Med ferdig forlengelse foran med IM      |     x      |
    Med ferdig forlengelse foran  uten IM    |     x      |
    Forlengelse med refusjon opphørt         |     x      |
    Overlapper med forkastet                 |            |
    Med perioder etter (out of order)        |     -      |
    ^-- med Inntektsmelding                  |            |
    Med uferdig gap etter                    |            |
    Med uferdig forlengelse etter            |            |
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
    fun `uten perioder fra før uten inntektsmelding`() {
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTrue(observatør.bedtOmInntektsmeldingReplay(1.vedtaksperiode))
    }

    @Test
    fun `uten perioder fra før med inntektsmelding`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `uten perioder fra før med refusjon opphørt`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(18.januar, INNTEKT, emptyList()))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `uferdig gap foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode))
    }

    @Test
    @Disabled("AvventerInntektsmeldingUferdigGap ber ikke om replay av Inntektsmelding")
    fun `uferdig gap foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 25.januar)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP, AVVENTER_UFERDIG_GAP)
    }

    @Test
    fun `ferdig gap foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 16.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 16.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(
            3.januar til 16.januar,
            18.januar til 19.januar
        ), førsteFraværsdag = 18.januar)
        håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ferdig gap foran med inntektsmelding`() {
        nyttVedtak(3.januar, 20.januar, 100.prosent)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTrue(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode))
    }

    @Test
    fun `uferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    @Disabled("Periode 2 (med søknad) ber ikke om replay. Avventer avklaring. Periode 2 vil uansett gå videre når periode 1 ferdigbehandles.")
    fun `uferdig forlengelse (kort) foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 16.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `uferdig forlengelse foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `forkastet uferdig forlengelse foran med refusjon opphørt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(25.januar, INNTEKT, emptyList()))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `uferdig forlengelse foran med refusjon opphørt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(25.januar, INNTEKT, emptyList()))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `ferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE)
    }

    @Test
    fun `ferdig forlengelse foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(3.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(
            3.januar til 10.januar,
            11.januar til 18.januar
        ))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ferdig forlengelse foran med refusjon opphørt`() {
        nyttVedtak(3.januar, 20.januar)
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(21.januar, INNTEKT, emptyList()))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }
}
