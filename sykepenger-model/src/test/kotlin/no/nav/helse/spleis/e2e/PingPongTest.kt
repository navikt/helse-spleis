package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PingPongTest : AbstractEndToEndTest() {

    @Test
    fun `Forlengelser av infotrygd overgang har samme maksdato som forrige`() {
        val historikk1 = Utbetalingsperiode(ORGNUMMER, 20.november(2019) til 29.mai(2020), 100.prosent, 1145.daglig)

        håndterSykmelding(Sykmeldingsperiode(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterSøknad(Sykdom(30.mai(2020), 19.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk1)
        håndterYtelser(
            1.vedtaksperiode, historikk1,
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    20.november(2019),
                    1000.daglig,
                    true
                )
            )
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)


        håndterSykmelding(Sykmeldingsperiode(22.juni(2020), 9.juli(2020), 100.prosent))
        håndterSøknad(
            Sykdom(22.juni(2020), 9.juli(2020), 100.prosent)
        )
        håndterYtelser(
            2.vedtaksperiode, historikk1,
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    20.november(2019),
                    1000.daglig,
                    true
                )
            )
        )
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(35))

        val historikk2 = Utbetalingsperiode(ORGNUMMER, 22.juni(2020) til 17.august(2020), 100.prosent, 1145.daglig)

        håndterSykmelding(Sykmeldingsperiode(18.august(2020), 2.september(2020), 100.prosent))
        håndterSøknad(Sykdom(18.august(2020), 2.september(2020), 100.prosent))
        oppfriskUtbetalingshistorikk(3.vedtaksperiode, historikk2)
        håndterYtelser(3.vedtaksperiode, historikk1, historikk2, inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 20.november(2019), 1000.daglig, true),
            Inntektsopplysning(ORGNUMMER, 22.juni(2020), 1000.daglig, true)
        ))
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
        håndterUtbetalt(3.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(30.oktober(2020), inspektør.maksdato(3.vedtaksperiode))
    }


    @Test
    fun `riktig skjæringstidspunkt ved spleis - infotrygd - spleis`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        oppfriskUtbetalingshistorikk(2.vedtaksperiode, Utbetalingsperiode(ORGNUMMER, 1.februar til 28.februar, 100.prosent, 1000.daglig), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        håndterYtelser(2.vedtaksperiode, Utbetalingsperiode(ORGNUMMER, 1.februar til 28.februar, 100.prosent, 1000.daglig), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }
}
