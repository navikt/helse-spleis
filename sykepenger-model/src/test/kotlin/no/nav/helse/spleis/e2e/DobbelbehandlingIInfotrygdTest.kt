package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class DobbelbehandlingIInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `avdekker overlapp dobbelbehandlinger i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterPåminnelse(1.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        val historie1 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar,  26.januar, 100.prosent, 1000.daglig)
        håndterUtbetalingshistorikk(1.vedtaksperiode, historie1)

        håndterSykmelding(Sykmeldingsperiode(3.februar, 26.februar, 100.prosent))
        håndterPåminnelse(2.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        val historie2 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 26.februar,  26.mars, 100.prosent, 1000.daglig)
        håndterUtbetalingshistorikk(2.vedtaksperiode, historie2)

        håndterSykmelding(Sykmeldingsperiode(1.mai, 30.mai, 100.prosent))
        håndterPåminnelse(3.vedtaksperiode, MOTTATT_SYKMELDING_FERDIG_GAP)
        val historie3 = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april,  1.mai, 100.prosent, 1000.daglig)
        håndterUtbetalingshistorikk(3.vedtaksperiode, historie3)

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `utbetalt i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 4.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(9.november(2020), 4.desember(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.desember(2020),  23.desember(2020), 100.prosent, 1000.daglig),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2021),  3.januar(2021), 100.prosent, 1000.daglig),
            Friperiode(24.desember(2020),  31.desember(2020)),
            ArbeidsgiverUtbetalingsperiode("456789123", 29.oktober(2020),  4.desember(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 29.oktober(2020), 1000.daglig, true)
            )
        )
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `utbetalt i infotrygd mens vi venter på inntektsmelding - oppdaget i AVVENTER_HISTORIKK`() {
        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 4.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(9.november(2020), 4.desember(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterInntektsmelding(listOf(9.november(2020) til 24.november(2020)))
        val historikk = arrayOf(ArbeidsgiverUtbetalingsperiode("456789123", 1.desember(2020),   4.desember(2020), 100.prosent, 1000.daglig))
        val inntektshistorikk = listOf(
            Inntektsopplysning("456789123", 1.desember(2020), 1000.daglig, true)
        )
        håndterYtelser(1.vedtaksperiode, *historikk, inntektshistorikk = inntektshistorikk)
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `utbetalt i infotrygd mens vi venter på inntektsmelding - oppdaget ved påminnelse`() {
        håndterSykmelding(Sykmeldingsperiode(9.november(2020), 4.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(9.november(2020), 4.desember(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        håndterUtbetalingshistorikk(1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode("456789123", 1.desember(2020),   4.desember(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning("456789123", 1.desember(2020), 1000.daglig, true)
            )
        )
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }
}
