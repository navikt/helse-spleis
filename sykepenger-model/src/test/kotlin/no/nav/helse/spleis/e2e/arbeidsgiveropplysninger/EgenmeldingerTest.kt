package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EgenmeldingerTest : AbstractEndToEndTest() {

    @Test
    fun `Egenmeldinger etterfulgt av ferie i snuten - bestemmende fraværsdag er første egenmeldingsdag`() {
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(11.januar, 12.januar), egenmeldinger = listOf(1.januar til 10.januar))
        assertForventetFeil(
            forklaring = "Vi burde begynne å sende en bestemmende fraværsdag som hensyntar egenmeldingsdager",
            nå = {
                assertEquals(13.januar, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().førsteFraværsdager.first().førsteFraværsdag)
            },
            ønsket = {
                assertEquals(1.januar, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().førsteFraværsdager.first().førsteFraværsdag)
            })
    }

    @Test
    fun `Ferie i snuten - bestemmende fraværsdag er første sykedag i vedtaksperioden`() {
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(11.januar, 12.januar))
        assertEquals(13.januar, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().førsteFraværsdager.first().førsteFraværsdag)
    }

    @Test
    fun `Egenmeldinger etterfulgt av arbeid i snuten - bestemmende fraværsdag er første sykedag i vedtaksperioden`() {
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent), egenmeldinger = listOf(1.januar til 10.januar))
        håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent))

        assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_INNTEKTSMELDING)
        assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 10.januar, 15.januar til 21.januar), førsteFraværsdag = 10.februar)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(15.januar, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().førsteFraværsdager.first().førsteFraværsdag)

    }
}