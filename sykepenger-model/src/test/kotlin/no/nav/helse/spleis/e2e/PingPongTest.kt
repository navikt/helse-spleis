package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PingPongTest : AbstractEndToEndTest() {

    @Test
    fun `Forlengelser av infotrygd overgang har samme maksdato som forrige`() {
        val historikk1 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 20.november(2019),  29.mai(2020), 100.prosent, 1145.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk1, inntektshistorikk = inntekter)
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 9.juli(2020), 100.prosent))
        håndterSøknad(
            Sykdom(22.juni(2020), 9.juli(2020), 100.prosent)
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(110))

        val historikk2 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2020),  17.august(2020), 100.prosent, 1145.daglig)
        val inntekter2 = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), 1000.daglig, true)
        )
        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, historikk1, historikk2, inntektshistorikk = inntekter2)
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.sisteMaksdato(3.vedtaksperiode))
        assertEquals(inspektør.sisteMaksdato(1.vedtaksperiode), inspektør.sisteMaksdato(3.vedtaksperiode))
    }

    @Test
    fun `riktig skjæringstidspunkt ved spleis - infotrygd - spleis`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar,  28.februar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true))
        håndterUtbetalingshistorikk(2.vedtaksperiode, historie, inntektshistorikk = inntekter)
        håndterYtelser(2.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `kort periode - infotrygd - spleis --- inntekt kommer fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT - 100.månedlig, inspektør.vilkårsgrunnlag(3.vedtaksperiode)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `spleis - infotrygd - spleis --- inntekt kommer fra første periode`() {
        nyttVedtak(20.desember(2017), 16.januar)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 27.januar, 100.prosent))

        håndterInntektsmelding(listOf(20.desember(2017) til 5.januar))

        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(28.januar, 31.januar, 100.prosent))

        val historie = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar,  27.januar, 100.prosent, 1000.daglig)
        val inntekt = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT - 100.månedlig, true))

        håndterUtbetalingshistorikk(3.vedtaksperiode, historie, inntektshistorikk = inntekt)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(20.desember(2017), inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(INNTEKT, inspektør.vilkårsgrunnlag(3.vedtaksperiode)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `Kaster ut alt om vi oppdager at en senere periode er utbetalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        // Ingen søknad for første sykmelding - den sykmeldte sender den ikke inn eller vi er i et out of order-scenario

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))

        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(a1, 17.februar, 20.februar, 100.prosent, INNTEKT))

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD )
        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `går til Infortrygd dersom det finnes en utbetalt periode i infotrygdhistorikken som starter etter denne perioden`() {
        håndterSykmelding(Sykmeldingsperiode(12.juli(2021), 1.august(2021), 100.prosent))
        håndterSøknad(Sykdom(12.juli(2021), 1.august(2021), 100.prosent), Søknad.Søknadsperiode.Ferie(12.juli(2021), 1.august(2021)))

        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 2.august(2021), INNTEKT, true), Inntektsopplysning(ORGNUMMER, 22.juni(2021), INNTEKT, true))
        val utbetlinger = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 2.august(2021), 28.september(2021), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 22.juni(2021), 11.juli(2021), 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetlinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }
}
