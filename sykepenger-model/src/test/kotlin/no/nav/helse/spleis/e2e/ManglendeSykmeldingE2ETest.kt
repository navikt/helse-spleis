package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManglendeSykmeldingE2ETest : AbstractEndToEndTest() {

    @BeforeAll
    fun setup() {
        Toggles.OppretteVedtaksperioderVedSøknad.enable()
    }

    @AfterAll
    fun teardown() {
        Toggles.OppretteVedtaksperioderVedSøknad.pop()
    }

    @Test
    fun `kan ikke sende inn en gammel søknad`() {
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent), sendtTilNav = 1.september)
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `overlapper med tidligere utbetalt periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        forlengPeriode(1.april, 30.april)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode, false) // <- TIL_INFOTRYGD
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(4, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `overlapper med forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(
            Sykdom(1.januar, 2.januar, 100.prosent), andreInntektskilder = listOf(
                Søknad.Inntektskilde(false, "SELVSTENDIG_NÆRINGSDRIVENDE") // <-- TIL_INFOTRYGD
            )
        )
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
    fun `søknad etter forkastet med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(8.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(
                Søknad.Inntektskilde(false, "SELVSTENDIG_NÆRINGSDRIVENDE") // <-- TIL_INFOTRYGD
            )
        )
        håndterSøknad(Sykdom(8.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(8.februar til 23.februar))
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `søknad etter forkastet med inntektsmelding (med forlengelse av kort arbeidsgiverperiode) - strekkes ikke tilbake`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar, 13.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(14.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(14.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(
                Søknad.Inntektskilde(false, "SELVSTENDIG_NÆRINGSDRIVENDE") // <-- TIL_INFOTRYGD
            )
        )
        håndterSøknad(Sykdom(14.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(8.februar til 22.februar))
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
        assertEquals(14.februar til 28.februar, inspektør.periode(4.vedtaksperiode))
    }

    @Test
    fun `søknad etter forkastet med inntektsmelding (med gap til kort arbeidsgiverperiode) - strekkes ikke tilbake`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar, 12.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(14.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(14.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(
                Søknad.Inntektskilde(false, "SELVSTENDIG_NÆRINGSDRIVENDE") // <-- TIL_INFOTRYGD
            )
        )
        håndterSøknad(Sykdom(14.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(8.februar til 12.februar, 14.februar til 23.februar), 14.februar)
        assertTilstander(4.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
        assertEquals(14.februar til 28.februar, inspektør.periode(4.vedtaksperiode))
    }

    @Test
    fun `søknad ag - overlapper med periode - eldst først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 3.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 4.januar, 100.prosent))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertTrue(hendelselogg.hasWarningsOrWorse())
        assertEquals(1.januar til 3.januar, inspektør.periode(2.vedtaksperiode))
    }

    @Test
    fun `søknad ag - overlapper med periode - eldst sist`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 3.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 4.januar, 100.prosent))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 3.januar, 100.prosent))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertTrue(hendelselogg.hasWarningsOrWorse())
        assertEquals(3.januar til 4.januar, inspektør.periode(2.vedtaksperiode))
    }

    @Test
    fun `uten perioder fra før uten inntektsmelding`() {
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTrue(observatør.bedtOmInntektsmeldingReplay(1.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `søknad ag - uten perioder fra før uten inntektsmelding`() {
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 4.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `uten perioder fra før med inntektsmelding`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode(ORGNUMMER))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `uten perioder fra før med refusjon opphørt`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Refusjon(INNTEKT, 18.januar, emptyList()))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode(ORGNUMMER))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `uferdig gap foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `søknad ag - uferdig gap foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(6.januar, 7.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `uferdig gap foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 25.januar)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode(ORGNUMMER))) { "Periode 2 vil be om replay av inntektsmelding så snart perioden foran er ferdigbehandlet" }
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `ferdig gap foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 16.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 16.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(
            listOf(
                3.januar til 16.januar,
                18.januar til 19.januar
            ), førsteFraværsdag = 18.januar
        )
        håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode(ORGNUMMER))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ferdig gap foran med inntektsmelding`() {
        nyttVedtak(3.januar, 20.januar, 100.prosent)
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTrue(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode(ORGNUMMER)))
    }

    @Test
    fun `uferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `søknad ag - uferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(5.januar, 6.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `uferdig forlengelse (kort) foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 16.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        assertFalse(observatør.bedtOmInntektsmeldingReplay(2.vedtaksperiode(ORGNUMMER))) { "Periode 2 vil be om replay av inntektsmelding så snart perioden foran er ferdigbehandlet" }
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
        val inntektsmeldingId = håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Refusjon(INNTEKT, 25.januar, emptyList()))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode(ORGNUMMER))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `uferdig forlengelse foran med refusjon opphørt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 24.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Refusjon(INNTEKT, 25.januar, emptyList()))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `ferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE)
    }

    @Test
    fun `søknad ag - ferdig forlengelse foran uten inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(11.januar, 14.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `ferdig forlengelse foran med inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(
                3.januar til 10.januar,
                11.januar til 18.januar
            )
        )
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ferdig forlengelse foran med refusjon opphørt`() {
        nyttVedtak(3.januar, 20.januar)
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Refusjon(INNTEKT, 21.januar, emptyList()))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `ferdig gap foran med refusjon opphørt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), refusjon = Refusjon(INNTEKT, 21.januar, emptyList()))
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
    fun `søknad ag - nyere ferdig gap som blir forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 31.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(15.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `nyere ferdig gap som blir uferdig gap`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `søknad ag - nyere ferdig gap som forblir ferdig gap`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 4.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
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
    fun `søknad ag - nyere uferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(19.januar, 24.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 18.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 2.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(4.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
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
    fun `søknad ag - lager sammenhengende med uferdig`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 12.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
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
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(10.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 9.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        )
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `søknad ag - nyere ferdig forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(10.februar, 15.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(10.februar, 15.februar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(3.januar, 9.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        )
        assertTilstander(3.vedtaksperiode, START, AVSLUTTET_UTEN_UTBETALING)
    }
}
