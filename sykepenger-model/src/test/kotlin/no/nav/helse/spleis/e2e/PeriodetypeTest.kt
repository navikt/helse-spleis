package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodetypeTest : AbstractEndToEndTest() {

    @Test
    fun `førstegangsbehadling frem til og med første periode med betaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.januar, 13.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(14.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 13.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(3.vedtaksperiode))
        assertForventetFeil(
            nå = { assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(4.vedtaksperiode)) },
            ønsket = { assertEquals(FORLENGELSE, inspektør.periodetype(4.vedtaksperiode)) }
        )
    }

    @Test
    fun `førstegangsbehadling etter gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype er overgang fra Infotrygd hvis foregående ble behandlet i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        assertEquals(OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
    }

    @Test
    fun `periodetype er forlengelse fra Infotrygd hvis førstegangsbehandlingen skjedde i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        håndterSykmelding(Sykmeldingsperiode(26.februar, 15.april, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(26.februar, 15.april, 100.prosent))
        assertEquals(OVERGANG_FRA_IT, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(INFOTRYGDFORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager pga lav sykdomsgrad`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar, 10.februar, 15.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 10.februar, 15.prosent))
        håndterSøknad(Sykdom(11.februar, 21.februar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(20.januar, 4.februar)),
            førsteFraværsdag = 20.januar
        )
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `første periode er kun arbeidsgiverperiode og helg - før utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `første periode er kun arbeidsgiverperiode og helg - etter utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling består kun av ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 21.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(11.januar, 21.januar))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
        assertForventetFeil(
            nå = {
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(3.vedtaksperiode))
            },
            ønsket = {
                assertEquals(FORLENGELSE, inspektør.periodetype(3.vedtaksperiode))
            }
        )
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling består kun av avviste dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 1000.månedlig
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertForventetFeil(
            nå = {
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
            },
            ønsket = {
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
                assertEquals(FORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling er avvist`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 1000.månedlig
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertForventetFeil(
            nå = {
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(2.vedtaksperiode))
            },
            ønsket = {
                assertEquals(FØRSTEGANGSBEHANDLING, inspektør.periodetype(1.vedtaksperiode))
                assertEquals(FORLENGELSE, inspektør.periodetype(2.vedtaksperiode))
            }
        )
    }

}
