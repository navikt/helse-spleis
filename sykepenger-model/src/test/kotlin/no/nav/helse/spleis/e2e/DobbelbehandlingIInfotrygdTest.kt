package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DobbelbehandlingIInfotrygdTest : AbstractEndToEndTest() {

    @Test
    fun `avdekker overlapp dobbelbehandlinger i Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        val historie1 = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar, 26.januar, 100.prosent, 1000.daglig)
        )
        val inntektshistorikk1 = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            *historie1,
            inntektshistorikk = inntektshistorikk1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )

        håndterSykmelding(Sykmeldingsperiode(3.februar, 26.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 26.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            *historie1,
            inntektshistorikk = inntektshistorikk1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        val historie2 = historie1 + arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 26.februar, 26.mars, 100.prosent, 1000.daglig)
        )
        val inntektshistorikk2 = inntektshistorikk1 + Inntektsopplysning(ORGNUMMER, 26.februar, INNTEKT, true)
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            *historie2,
            inntektshistorikk = inntektshistorikk2,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )

        håndterSykmelding(Sykmeldingsperiode(1.mai, 30.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 30.mai, 100.prosent))
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            *historie2,
            inntektshistorikk = inntektshistorikk2,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterPåminnelse(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        val inntektshistorikk3 = inntektshistorikk2 + Inntektsopplysning(ORGNUMMER, 1.mai, INNTEKT, true)
        val historie3 = historie2 + arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.april, 1.mai, 100.prosent, 1000.daglig)
        )
        håndterUtbetalingshistorikk(
            3.vedtaksperiode,
            *historie3,
            inntektshistorikk = inntektshistorikk3,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TIL_INFOTRYGD
        )
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
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        håndterUtbetalingshistorikk(1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode("456789123", 1.desember(2020),   4.desember(2020), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning("456789123", 1.desember(2020), 1000.daglig, true)
            )
        )
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }
}
