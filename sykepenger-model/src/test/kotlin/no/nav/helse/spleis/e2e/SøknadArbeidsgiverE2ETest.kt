package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SøknadArbeidsgiverE2ETest : AbstractEndToEndTest() {

    @Test
    fun `avslutter søknad utenfor arbeidsgiverperioden dersom det kun er helg`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `forkastede problemdager skal ikke skape problem ved utregning av arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Papirsykmelding(27.desember(2017), 31.desember(2017)))
        assertTrue(hendelselogg.hasErrorsOrWorse()) // perioden blir forkastet pga papirsykmelding
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `hensyntar forkastet historikk for å unngå å lage dårlig stemning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.januar til 20.januar)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `hensyntar forkastet historikk for å unngå å lage dårlig stemning - med gap til forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.januar til 20.januar)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `hensyntar forkastet historikk for å unngå å lage dårlig stemning - med mye gap til forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.januar til 20.januar)
        håndterSykmelding(Sykmeldingsperiode(7.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(7.februar, 15.februar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter søknad innenfor arbeidsgiverperioden fordi arbeid er gjenopptatt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Arbeid(17.januar, 20.januar))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter ikke søknad innenfor arbeidsgiverperioden dersom ferie er utenfor`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(17.januar, 20.januar))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det før IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.januar, INNTEKT, true)))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.januar, INNTEKT, true)))
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM - flere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(2.februar, 2.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.februar, 6.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 6.februar)
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER.toString(), 17.januar, INNTEKT, true)))
        håndterSøknad(Sykdom(6.februar, 6.februar, 100.prosent))
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_UFERDIG)
    }

    @Test
    fun `avslutter korte perioder med gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
    }

    @Test
    fun `avslutter korte perioder med gap med arbeidsgiversøknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korteperioder med gap med arbeidsgiversøknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med gap med søknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK)
    }


    @Test
    fun `avslutter korte perioder med gap med søknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med søknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med søknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_SØKNAD_UFERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_UFERDIG_FORLENGELSE, AVVENTER_SØKNAD_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerende søknad med gjenopptatt arbeid slik at hele perioden er innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), Arbeid(17.januar, 18.januar))
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertError(1.vedtaksperiode, "Mottatt flere søknader for perioden - siste søknad inneholder arbeidsdag")
    }

}
