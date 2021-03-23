package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StatslønnWarningTest : AbstractEndToEndTest() {

    @Test
    fun `Ikke warning ved statslønn når det ikke er overgang`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNav = 18.februar)
        håndterYtelser(vedtaksperiodeId = 1.vedtaksperiode, statslønn = true)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(vedtaksperiodeId = 1.vedtaksperiode, statslønn = true)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTrue(inspektør.personLogg.warn().isEmpty())
    }

    @Test
    fun `Warning ved statslønn`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNav = 18.februar)
        val historikk = arrayOf(Utbetalingsperiode(ORGNUMMER, 1.desember(2017) til 31.desember(2017), 100.prosent, 15000.daglig))
        håndterYtelser(1.vedtaksperiode, *historikk, statslønn = true)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertTrue(
            inspektør.personLogg.warn().toString()
                .contains("Det er lagt inn statslønn i Infotrygd, undersøk at utbetalingen blir riktig.")
        ) {
            inspektør.personLogg.toString()
        }
    }

}
