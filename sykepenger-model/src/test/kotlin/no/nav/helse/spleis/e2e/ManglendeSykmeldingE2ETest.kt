package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
    Overlapper med forkastet                 |     x      |
    Med perioder etter (out of order)        |     x      |
    ^-- med Inntektsmelding                  |     x      |
    Med uferdig gap etter                    |     x      |
    Med uferdig forlengelse etter            |     x      |
    Med ferdig gap etter                     |     x      |
    Med ferdig forlengelse etter             |     x      |                          Sende inn kort AG-søknad. Sende inn Sykmelding. Sende inn Søknad foran kort AG-søknad.
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
    fun `overlapper med forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 2.januar, 100.prosent), andreInntektskilder = listOf(
            Søknad.Inntektskilde(false, "SELVSTENDIG_NÆRINGSDRIVENDE") // <-- TIL_INFOTRYGD
        ))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `overlapper med periode - eldst først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        håndterSøknad(Sykdom(1.januar, 3.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertTrue(hendelselogg.hasWarningsOrWorse())
        assertEquals(1.januar til 3.januar, inspektør.periode(2.vedtaksperiode))
    }

    @Test
    fun `overlapper med periode - eldst sist`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        håndterSøknad(Sykdom(3.januar, 4.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        håndterSøknad(Sykdom(1.januar, 3.januar, 100.prosent))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertTrue(hendelselogg.hasWarningsOrWorse())
        assertEquals(3.januar til 4.januar, inspektør.periode(2.vedtaksperiode))
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
    fun `uferdig gap foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 25.januar)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode)) { "Periode 2 vil be om replay av inntektsmelding så snart perioden foran er ferdigbehandlet" }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
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
    fun `uferdig forlengelse (kort) foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 16.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode)) { "Periode 2 vil be om replay av inntektsmelding så snart perioden foran er ferdigbehandlet" }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
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
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(25.januar, INNTEKT, emptyList()))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
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

    @Test
    fun `ferdig gap foran med refusjon opphørt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Triple(21.januar, INNTEKT, emptyList()))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `nyere ferdig gap som blir forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `nyere ferdig gap som blir uferdig gap`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `nyere uferdig gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }


    @Test
    fun `nyere uferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.januar, 24.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `søknad lager sammenhengende med uferdig`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `søknad lager sammenhengende med ferdig`() {
        nyttVedtak(3.januar, 20.januar)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 24.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_HISTORIKK)
    }

    @Test
    fun `nyere ferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(10.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 9.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }
}
