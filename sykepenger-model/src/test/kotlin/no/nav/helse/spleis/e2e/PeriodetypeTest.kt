package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodetypeTest : AbstractEndToEndTest() {

    @Test
    fun `førstegangsbehadling frem til og med første periode med betaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
        håndterSykmelding(Sykmeldingsperiode(6.januar, 13.januar))
        håndterSykmelding(Sykmeldingsperiode(14.januar, 20.januar))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 13.januar, 100.prosent))
        håndterSøknad(Sykdom(14.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(3.vedtaksperiode))
        assertEquals(FORLENGELSE, periodetype(4.vedtaksperiode))
    }

    @Test
    fun `førstegangsbehadling etter gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar))

        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))

        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager pga lav sykdomsgrad`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar, 10.februar))
        håndterSykmelding(Sykmeldingsperiode(11.februar, 21.februar))
        håndterSøknad(Sykdom(20.januar, 10.februar, 15.prosent))
        håndterSøknad(Sykdom(11.februar, 21.februar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(20.januar, 4.februar)),
            førsteFraværsdag = 20.januar
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FORLENGELSE, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `første periode er kun arbeidsgiverperiode og helg - før utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `første periode er kun arbeidsgiverperiode og helg - etter utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling består kun av ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 21.januar))
        håndterSøknad(Sykdom(11.januar, 21.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(11.januar, 21.januar))

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
        assertForventetFeil(
            nå = {
                assertEquals(FØRSTEGANGSBEHANDLING, periodetype(3.vedtaksperiode))
            },
            ønsket = {
                assertEquals(FORLENGELSE, periodetype(3.vedtaksperiode))
            }
        )
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling består kun av avviste dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 1000.månedlig
        )
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
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FORLENGELSE, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `periodetype for forlengelse dersom førstegangsbehandling er avvist`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 1000.månedlig
        )
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
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(2.vedtaksperiode))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 20.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertEquals(FØRSTEGANGSBEHANDLING, periodetype(1.vedtaksperiode))
    }

    private fun periodetype(vedtaksperiodeIdInnhenter: IdInnhenter) =
        inspektør.arbeidsgiver.periodetype(inspektør.periode(vedtaksperiodeIdInnhenter))
}
