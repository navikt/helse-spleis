package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_1
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest : AbstractEndToEndTest() {

    private val INNTEKT_FLERE_AG = 20000.månedlig

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmelding`() {
        nyPeriode(1.januar til 31.januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal ikke be om arbeidsgiverperiode når det er mindre en 16 dagers gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

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
            PersonObserver.Refusjon(forslag = emptyList())
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
    fun `skal ikke be om arbeidsgiveropplysninger ved forlengelse selv når inntektsmeldingen ikke har kommet enda`() {
        nyPeriode(1.januar til 31.januar)
        nyPeriode(1.februar til 28.februar)

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
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(17.februar til 4.mars))
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender med skjæringstidspunkt i eventet`() {
        nyPeriode(1.januar til 31.januar)
        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        assertEquals(1.januar, trengerArbeidsgiveropplysningerEvent.skjæringstidspunkt)
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

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
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
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode(
                forslag = listOf(
                    1.januar til 7.januar,
                    9.januar til 14.januar,
                    16.januar til 18.januar
                )
            )
        )

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        val expectedSykmeldingsperioder = listOf(
            1.januar til 7.januar,
            9.januar til 14.januar,
            16.januar til 21.januar
        )
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `ber ikke om inntekt når vi allerede har inntekt på skjæringstidspunktet -- med arbeidsgiverperiode`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a2)
        nyPeriode(1.mars til 31.mars, a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val inntektsmeldingId = inspektør(a1).hendelseIder(1.vedtaksperiode.id(a1)).last()
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
            PersonObserver.Refusjon(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT_FLERE_AG))),
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
        val inntektsmeldingId = inspektør(a1).hendelseIder(1.vedtaksperiode.id(a1)).last()

        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
            PersonObserver.Refusjon(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT_FLERE_AG)))
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `sender med riktig beregningsmåneder når første fraværsdag hos én arbeidsgivers er i en annen måned enn skjæringstidspunktet`() {
        nyPeriode(31.januar til 28.februar, a1)
        nyPeriode(1.februar til 28.februar, a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespurtOpplysning =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

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
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.februar til 16.februar))
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `ber om inntekt for a2 når søknad for a2 kommer inn etter fattet vedtak for a1`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.januar til 31.januar, a2)

        assertForventetFeil(
            forklaring = "Støtter ikke å legge til ny arbeidsgiver i eksisterende vilkårsgrunnlag",
            nå = {
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD, orgnummer = a2)
            },
            ønsket = {
                fail("""\_(ツ)_/¯""")
            }
        )
    }

    @Test
    fun `sender med FastsattInntekt når vi allerede har en inntektsmelding lagt til grunn på skjæringstidspunktet`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, orgnummer = a2)
        nyPeriode(1.mars til 31.mars, a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a1)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val inntektsmeldingId = inspektør(a1).hendelseIder(1.vedtaksperiode.id(a1)).last()
        val actualForespurtOpplysning =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
            PersonObserver.Refusjon(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT_FLERE_AG))),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `sender med FastsattInntekt når vi allerede har inntekt fra skatt lagt til grunn på skjæringstidspunktet`() {
        nyeVedtakMedUlikFom(
            mapOf(
                a1 to (31.desember(2017) til 31.januar),
                a2 to (1.januar til 31.januar)
            )
        )
        forlengVedtak(1.februar, 28.februar, orgnummer = a1)
        nyPeriode(1.mars til 31.mars, a2)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val inntektsmeldingId = inspektør(a2).hendelseIder(1.vedtaksperiode.id(a2)).last()

        val actualForespurtOpplysning =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = listOf(
        PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
            PersonObserver.Refusjon(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT_FLERE_AG))),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `be om arbeidsgiverperiode ved forlengelse av en kort periode`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(17.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
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
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.januar til 16.januar))
        )

        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `be om arbeidsgiverperiode når en kort periode har et lite gap til ny periode`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(20.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

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
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.januar til 16.januar))
        )

        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `blir syk fra ghost`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }, emptyList()),
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH),
            ), orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        val arbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder
        assertEquals(2, arbeidsgiveropplysningerEvents.size)
        val trengerArbeidsgiveropplysningerEvent = arbeidsgiveropplysningerEvents.last()

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.FastsattInntekt(INNTEKT),
            PersonObserver.Refusjon(emptyList()),
            PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.februar til 16.februar))
        )
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a1))
        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a2))
        assertVarsel(RV_RE_1, AktivitetsloggFilter.arbeidsgiver(a2))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `skal kun sende med refusjonsopplysninger som overlapper med, eller er etter, vedtaksperioden som ikke trenger inntektsopplysninger fra arbeidsgiver`() {
        gapHosÉnArbeidsgiver(Inntektsmelding.Refusjon(
            beløp = INNTEKT_FLERE_AG,
            opphørsdato = null,
            endringerIRefusjon = listOf(
                Inntektsmelding.Refusjon.EndringIRefusjon(18000.månedlig, 10.februar),
                Inntektsmelding.Refusjon.EndringIRefusjon(17000.månedlig, 1.mars),
                Inntektsmelding.Refusjon.EndringIRefusjon(16000.månedlig, 10.mars),
                Inntektsmelding.Refusjon.EndringIRefusjon(15000.månedlig, 1.april)
            )
        ))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        assertForventetFeil(
            forklaring = "perioden i mars (2.vedtaksperiode for a2) har en ny arbeidsgiverperiode og skal vente på opplysninger fra AG",
            nå = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

                val arbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder
                assertEquals(3, arbeidsgiveropplysningerEvents.size)
                val trengerArbeidsgiveropplysningerEvent = arbeidsgiveropplysningerEvents.last()
                val inntektsmeldingId = inspektør(a2).hendelseIder(1.vedtaksperiode.id(a2)).last()

                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
                    PersonObserver.Refusjon(
                        listOf(
                            Refusjonsopplysning(inntektsmeldingId, 1.mars, 9.mars, 17000.månedlig),
                            Refusjonsopplysning(inntektsmeldingId, 10.mars, 31.mars, 16000.månedlig),
                            Refusjonsopplysning(inntektsmeldingId, 1.april, null, 15000.månedlig)
                        )
                    ),
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
                )

                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            }
        )
    }

    @Test
    fun `skal sende med riktig refusjonsopplysninger ved ingen refusjon`() {
        gapHosÉnArbeidsgiver(
            Inntektsmelding.Refusjon(
                beløp = INNTEKT_FLERE_AG,
                opphørsdato = 15.mars
        ))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        assertForventetFeil(
            forklaring = "perioden i mars (2.vedtaksperiode for a2) har en ny arbeidsgiverperiode og skal vente på opplysninger fra AG",
            nå = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

                val trengerArbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder
                assertEquals(3, trengerArbeidsgiveropplysningerEvents.size)

                val trengerArbeidsgiveropplysningerEvent = trengerArbeidsgiveropplysningerEvents.last()
                val inntektsmeldingId = inspektør(a2).hendelseIder(1.vedtaksperiode.id(a2)).last()

                val expectedForespurteOpplysninger = listOf(
                    PersonObserver.FastsattInntekt(INNTEKT_FLERE_AG),
                    PersonObserver.Refusjon(
                        listOf(
                            Refusjonsopplysning(inntektsmeldingId, 1.januar, 15.mars, INNTEKT_FLERE_AG),
                            Refusjonsopplysning(inntektsmeldingId, 16.mars, null, INGEN)
                        )
                    ),
                    PersonObserver.Arbeidsgiverperiode(forslag = listOf(1.mars til 16.mars))
                )

                assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
            }
        )
    }

    @Test
    fun `Skal ikke be om arbeidsgiveropplysninger for perioder innenfor arbeidsgiverperioden`() {
        nyPeriode(1.januar til 10.januar)
        assertTrue(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.isEmpty())
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Skal ikke sende ut forespørsel for en periode som allerede har mottatt inntektsmelding`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nyPeriode(1.januar til 31.januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal ikke sende ut forespørsel for en periode som allerede har mottatt inntektsmelding -- selv om håndteringen feiler`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar), harOpphørAvNaturalytelser = true)
        nyPeriode(1.januar til 31.januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal sende ut forespørsel for en periode dersom inntektsmeldingReplay ikke bærer noen frukter`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nyPeriode(1.februar til 28.februar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal sende egenmeldingsdager fra søknad i forespørsel`() = Toggle.Egenmelding.enable {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 31.januar))
        håndterSøknad(Sykdom(5.januar, 31.januar, 100.prosent), egenmeldinger = listOf(
            Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 4.januar)
        ))

        val expectedEgenmeldinger = listOf(1.januar til 4.januar)
        assertEquals(expectedEgenmeldinger, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().egenmeldingsperioder)
    }

    private fun gapHosÉnArbeidsgiver(refusjon: Inntektsmelding.Refusjon) {
        nyPeriode(1.januar til 31.januar, a1)
        nyPeriode(1.januar til 31.januar, a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            orgnummer = a1,
            beregnetInntekt = INNTEKT_FLERE_AG
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = refusjon,
            orgnummer = a2,
            beregnetInntekt = INNTEKT_FLERE_AG
        )

        fraVilkårsprøvingTilGodkjent(INNTEKT_FLERE_AG)

        forlengVedtak(1.februar, 28.februar, orgnummer = a1)
        nyPeriode(1.mars til 31.mars, a2)
    }

    private fun nyeVedtakMedUlikFom(
        sykefraværHosArbeidsgiver: Map<String, Periode>,
        inntekt: Inntekt = INNTEKT_FLERE_AG
    ) {
        val ag1Periode = sykefraværHosArbeidsgiver[a1]!!
        val ag2Periode = sykefraværHosArbeidsgiver[a2]!!
        nyPeriode(ag1Periode.start til ag1Periode.endInclusive, a1)
        nyPeriode(ag2Periode.start til ag2Periode.endInclusive, a2)

        håndterInntektsmelding(
            listOf(ag1Periode.start til ag1Periode.start.plusDays(15)),
            orgnummer = a1,
            beregnetInntekt = inntekt
        )
        håndterInntektsmelding(
            listOf(ag2Periode.start til ag2Periode.start.plusDays(15)),
            orgnummer = a2,
            beregnetInntekt = inntekt
        )

        fraVilkårsprøvingTilGodkjent(inntekt)
    }

    private fun fraVilkårsprøvingTilGodkjent(inntekt: Inntekt) {
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