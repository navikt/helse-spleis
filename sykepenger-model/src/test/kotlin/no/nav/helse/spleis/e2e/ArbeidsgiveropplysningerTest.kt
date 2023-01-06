package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.PersonObserver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.Splarbeidsbros::class)
internal class ArbeidsgiveropplysningerTest: AbstractEndToEndTest() {

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() {
        nyPeriode(1.januar til 31.januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender ikke ut event TrengerArbeidsgiveropplysninger med toggle disabled`() = Toggle.Splarbeidsbros.disable {
        nyPeriode(1.januar til 31.januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal ikke be om arbeidsgiverperiode når det er mindre en 16 dagers gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(PersonObserver.Inntektsforslag(listOf(
                november(2017),
                desember(2017),
                januar(2018)
            ))),
            PersonObserver.Refusjon
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `skal ikke be om arbeidsgiveropplysninger ved forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal be om arbeidsgiverperiode ved 16 dagers gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(17.februar til 17.mars)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    )
                )
            ),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(17.februar til 4.mars))
        )
        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender med begge sykmeldingsperiodene når vi har en kort periode som forlenges av en lang`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(17.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedSykmeldingsperioder = listOf(
            1.januar til 16.januar,
            17.januar til 31.januar
        )
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender ikke med begge sykmeldingsperiodene når vi har et gap større enn 16 dager mellom dem`() {
        nyPeriode(1.januar til 31.januar)
        nyPeriode(17.februar til 17.mars)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedSykmeldingsperioder = listOf(17.februar til 17.mars)
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }


    @Test
    fun `sender med riktig sykmeldingsperioder og forslag til arbeidsgiverperiode når arbeidsperioden er stykket opp i flere korte perioder`() {
        nyPeriode(1.januar til 7.januar)
        nyPeriode(9.januar til 14.januar)
        nyPeriode(16.januar til 21.januar)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        oktober(2017),
                        november(2017),
                        desember(2017)
                    )
                )
            ),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(
                1.januar til 7.januar,
                9.januar til 14.januar,
                16.januar til 18.januar
            ))
        )

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        val expectedSykmeldingsperioder = listOf(
            1.januar til 7.januar,
            9.januar til 14. januar,
            16.januar til 21.januar
        )
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `ber ikke om inntekt når vi allerede har inntekt på skjæringstidspunktet -- med arbeidsgiverperiode`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a2)
        nyPeriode(1.mars til 31.mars, a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(20000.månedlig),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `ber ikke om inntekt og AGP når vi har inntekt på skjæringstidspunkt og det er mindre enn 16 dagers gap`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 10.februar, orgnummer = a2)
        nyPeriode(11.februar til 28.februar, a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val actualForespurteOpplysninger = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(20000.månedlig),
            PersonObserver.Refusjon
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `sender med riktig beregningsmåneder når første fraværsdag hos én arbeidsgivers er i en annen måned enn skjæringstidspunktet`() {
        nyPeriode(31.januar til 28.februar, a1)
        nyPeriode(1.februar til 28.februar, a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespurtOpplysning = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(PersonObserver.Inntektsforslag(
                listOf(
                    oktober(2017),
                    november(2017),
                    desember(2017)
                )
            )),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.februar til 16.februar))
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `ber om inntekt for a2 når søknad for a2 kommer inn etter fattet vedtak for a1`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.januar til 31.januar, a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespurtOpplysning = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(PersonObserver.Inntektsforslag(
                listOf(
                    oktober(2017),
                    november(2017),
                    desember(2017)
                )
            )),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.januar til 16.januar))
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `sender med FastsattInntekt når vi allerede har en inntektsmelding lagt til grunn på skjæringstidspunktet`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a2)
        nyPeriode(1.mars til 31.mars, a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespurtOpplysning = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(20000.månedlig),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `sender med FastsattInntekt når vi allerede har inntekt fra skatt lagt til grunn på skjæringstidspunktet`() {
        nyeVedtakMedUlikFom(mapOf(
            a1 to (31.desember(2017) til 31.januar),
            a2 to (1.januar til 31.januar)
        ))
        forlengVedtak(1.februar, 28.februar, orgnummer = a1)
        nyPeriode(1.mars til 31.mars, a2)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val actualForespurtOpplysning = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(20000.månedlig),
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `tenke på når vi skal be om arbeidsgiverperiode når en kort periode forlenges `() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(17.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        assertForventetFeil(
            forklaring = "vi skal ikke sende ut en vanlig forespørsel for første periode (siden den er kort) og burde be om AGP i forlengelse",
            nå = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt(
                        PersonObserver.Inntektsforslag(
                            listOf(
                                oktober(2017),
                                november(2017),
                                desember(2017)
                            )
                        )
                    ),
                    PersonObserver.Refusjon
                )
                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            },
            ønsket = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt(
                        PersonObserver.Inntektsforslag(
                            listOf(
                                oktober(2017),
                                november(2017),
                                desember(2017)
                            )
                        )
                    ),
                    PersonObserver.Refusjon,
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.januar til 16.januar))
                )

                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            }
        )




        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `tenke på når vi skal be om arbeidsgiverperiode når en kort periode har et lite gap til ny periode`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(20.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        assertForventetFeil(
            forklaring = "vi skal ikke sende ut en vanlig forespørsel for første periode (siden den er kort) og burde be om AGP i forlengelse",
            nå = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt(
                        PersonObserver.Inntektsforslag(
                            listOf(
                                oktober(2017),
                                november(2017),
                                desember(2017)
                            )
                        )
                    ),
                    PersonObserver.Refusjon
                )
                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            },
            ønsket = {
                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.Inntekt(
                        PersonObserver.Inntektsforslag(
                            listOf(
                                oktober(2017),
                                november(2017),
                                desember(2017)
                            )
                        )
                    ),
                    PersonObserver.Refusjon,
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.januar til 16.januar))
                )

                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            }
        )




        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    private fun nyeVedtakMedUlikFom(sykefraværHosArbeidsgiver: Map<String, Periode>, inntekt: Inntekt = 20000.månedlig) {
        val ag1Periode = sykefraværHosArbeidsgiver[a1]!!
        val ag2Periode = sykefraværHosArbeidsgiver[a2]!!
        nyPeriode(ag1Periode.start til ag1Periode.endInclusive, a1)
        nyPeriode(ag2Periode.start til ag2Periode.endInclusive, a2)

        håndterInntektsmelding(listOf(ag1Periode.start til ag1Periode.start.plusDays(15)), orgnummer = a1, beregnetInntekt = inntekt)
        håndterInntektsmelding(listOf(ag2Periode.start til ag2Periode.endInclusive), orgnummer = a2, beregnetInntekt = inntekt)

        val sykepengegrunnlag = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
        )

        val sammenligningsgrunnlag = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(sykepengegrunnlag, emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }
}