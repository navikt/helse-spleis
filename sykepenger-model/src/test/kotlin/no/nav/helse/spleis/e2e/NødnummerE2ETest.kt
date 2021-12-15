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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NødnummerE2ETest : AbstractEndToEndTest() {
    private companion object {
        private const val nødnummer = "973626108"
    }

    @Test
    fun `slår ikke ut på tidligere inntektsopplysning med nødnummer`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.februar til 16.februar))
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = listOf(Inntektsopplysning(nødnummer, 1.januar, 500.daglig, false)))
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `slår ut på inntektsopplysning med nødnummer`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), førsteFraværsdag = 1.februar)
        håndterYtelser(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 17.januar, 18.januar, 100.prosent, 500.daglig), inntektshistorikk = listOf(Inntektsopplysning(nødnummer, 1.januar, 500.daglig, false)))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `slår ut på forlengelse med inntektsopplysning med nødnummer`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(nødnummer, 17.januar, 31.januar, 100.prosent, 500.daglig), inntektshistorikk = listOf(Inntektsopplysning(nødnummer, 17.januar, 500.daglig, false)))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }
}
