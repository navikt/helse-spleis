package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest: AbstractEndToEndTest() {

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() = Toggle.Splarbeidsbros.enable {
        nyPeriode(1.januar til 31.januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender ikke ut event TrengerArbeidsgiveropplysninger med toggle disabled`() = Toggle.Splarbeidsbros.disable {
        nyPeriode(1.januar til 31.januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal ikke be om arbeidsgiverperiode når det er mindre en 16 dagers gap`() = Toggle.Splarbeidsbros.enable {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `skal ikke be om arbeidsgiveropplysninger ved forlengelse`() = Toggle.Splarbeidsbros.enable {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal be om arbeidsgiverperiodeopplysninger ved 16 dagers gap`() = Toggle.Splarbeidsbros.enable {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(17.februar til 17.mars)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(17.februar til 4.mars))
        )
        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender med riktig forslag til arbeidsgiverperiodeo når arbeidsperioden er stykket opp i flere korte perioder`() = Toggle.Splarbeidsbros.enable {
        nyPeriode(1.januar til 7.januar)
        nyPeriode(9.januar til 14.januar)
        nyPeriode(16.januar til 21.januar)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(
                1.januar til 7.januar,
                9.januar til 14.januar,
                16.januar til 18.januar
            ))
        )
        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `ber ikke om inntekt når vi allerede har inntekt på skjæringstidspunktet -- med arbeidsgiverperiode`() = Toggle.Splarbeidsbros.enable {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a2)
        nyPeriode(1.mars til 31.mars, a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertForventetFeil(
            forklaring = "Skal ikke be om inntekt når det ikke trengs",
            ønsket = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Refusjon,
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
                )
                assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
            },
            nå = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt,
                    PersonObserver.Refusjon,
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
                )
                assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
            }
        )
    }

    @Test
    fun `ber ikke om inntekt og AGP når vi har inntekt på skjæringstidspunkt og det er mindre enn 16 dagers gap`() = Toggle.Splarbeidsbros.enable {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 10.februar, orgnummer = a2)
        nyPeriode(11.februar til 28.februar, a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        assertForventetFeil(
            forklaring = "Skal ikke be om inntekt eller AGP når det ikke trengs",
            ønsket = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Refusjon,
                )
                assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
            },
            nå = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt,
                    PersonObserver.Refusjon,
                )
                assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
            }
        )
    }
}