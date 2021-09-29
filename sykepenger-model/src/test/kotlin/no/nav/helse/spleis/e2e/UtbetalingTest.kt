package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingTest : AbstractEndToEndTest() {
    val ANNET_ORGNUMMER = "foo"

    @Test
    fun `Utbetaling enddret får rett organisasjonsnummer ved overlappende sykemelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ANNET_ORGNUMMER, 1.januar(2016), 31.januar(2016), 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ANNET_ORGNUMMER, 1.januar(2016), 1000.daglig, true)
            )
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)


        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent))
        assertEquals(ORGNUMMER, observatør.utbetaltEndretEventer.single().orgnummer())
    }
}
