package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NyOgFremtidigSøknadOutOfOrder : AbstractEndToEndTest() {

    @Test
    fun `Håndterer perioder som kommer inn i feil rekkefølge`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        inspektør.arbeidsgiver.accept(visitor = object : ArbeidsgiverVisitor {
            override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
                assertTrue(vedtaksperioder.isNotEmpty())
                assertEquals(vedtaksperioder, vedtaksperioder.sorted())
            }
        })

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `Håndterer ny sykmelding som ligger tidligere i tid`() {
        håndterSykmelding(Sykmeldingsperiode(23.mars(2020), 29.mars(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(30.mars(2020), 2.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.april(2020), 20.april(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(19.mars(2020), 22.mars(2020), 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `Kaster fortsatt ut hvis perioden er utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(20.januar til 4.februar))

        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar, 100.prosent))

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
    }

    @Test
    fun `når inntektsmeldingen ikke treffer vedtaksperioden bak`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `når det er gap mellom periodene ved innlesing av ny søknad`() {
        håndterSykmelding(Sykmeldingsperiode(15.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
    }

    @Test
    fun `Sykmelding i omvendt rekkefølge kaster ut etterfølgende som ikke er avsluttet — uten replay`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 5.januar, 100.prosent))

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, TIL_INFOTRYGD)
    }
}
