package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
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
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
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
import org.junit.jupiter.api.Assertions.assertFalse
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
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(1.januar, 31.januar, inntektsmeldingId = inntektsmeldingId)
        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT)))
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
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(1.januar, 31.januar, inntektsmeldingId = inntektsmeldingId)
        nyPeriode(17.februar til 17.mars)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT))),
            PersonObserver.Arbeidsgiverperiode
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
    fun `sender med riktig sykmeldingsperioder og forslag til arbeidsgiverperiode når arbeidsgiverperioden er stykket opp i flere korte perioder`() {
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
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode
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
            PersonObserver.Arbeidsgiverperiode
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
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode
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
            PersonObserver.Arbeidsgiverperiode
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
            PersonObserver.Arbeidsgiverperiode
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
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode
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
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode
        )

        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal ikke be om arbeidsgiverperiode når vi allerede har motatt inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 25.januar))
        håndterSøknad(Sykdom(20.januar, 25.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        assertForventetFeil("Skal ikke be om arbeidsgiverperiode når vi allerede har motatt inntektsmelding",
            nå = { assertTrue(trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger.contains(PersonObserver.Arbeidsgiverperiode)) },
            ønsket = { assertFalse(trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger.contains(PersonObserver.Arbeidsgiverperiode)) }
        )
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
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
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
            PersonObserver.Arbeidsgiverperiode
        )
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a1))
        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a2))
        assertVarsel(RV_RE_1, AktivitetsloggFilter.arbeidsgiver(a2))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
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
                    PersonObserver.Arbeidsgiverperiode
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
                    PersonObserver.Arbeidsgiverperiode
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
    fun `Skal sende egenmeldingsdager fra søknad i forespørsel`() {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 31.januar))
        håndterSøknad(Sykdom(5.januar, 31.januar, 100.prosent), egenmeldinger = listOf(
            Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 4.januar)
        ))

        val expectedEgenmeldinger = listOf(1.januar til 4.januar)
        assertEquals(expectedEgenmeldinger, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().egenmeldingsperioder)
    }

    @Test
    fun `Skal ikke sende med skjønnsfastsatt sykpengegrunnlag som inntektForrigeSkjæringstidspunkt` () {
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(1.januar, 31.januar, inntektsmeldingId = inntektsmeldingId)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT*2)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, INNTEKT))),

        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Skal sende med saksbehandlerinntekt som inntektForrigeSkjæringstidspunkt` () {
        nyttVedtak(1.januar, 31.januar)
        val id = håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(ORGNUMMER, 32000.månedlig, "", null, listOf(Triple(1.januar, null, 32000.månedlig)))
            )
        )
        håndterYtelser(1.vedtaksperiode)

        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.SAKSBEHANDLER, 32000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(Refusjonsopplysning(id, 1.januar, null, 32000.månedlig))),
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Skal ikke sende med inntektForrigeSkjæringstidspunkt fra annen arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.mars til 31.mars, orgnummer = a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        desember(2017),
                        januar(2018),
                        februar(2018)
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList()),
            PersonObserver.Arbeidsgiverperiode
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender kun med refusjonsopplysninger som overlapper med eller er nyere enn forespørselsperioden`() {
        nyPeriode(1.januar til 31.januar)
        val id = håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 1.april, endringerIRefusjon = listOf(
                Inntektsmelding.Refusjon.EndringIRefusjon(29000.månedlig, 20.januar),
                Inntektsmelding.Refusjon.EndringIRefusjon(28000.månedlig, 30.januar),
                Inntektsmelding.Refusjon.EndringIRefusjon(27000.månedlig, 20.februar)
            ))
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nyPeriode(15.februar til 28.februar)
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(
                Refusjonsopplysning(id, 30.januar, 19.februar, 28000.månedlig),
                Refusjonsopplysning(id, 20.februar, 1.april, 27000.månedlig),
                Refusjonsopplysning(id, 2.april, null, INGEN)
            )),
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender med riktige refusjonsopplysninger ved opphør av refusjon før perioden`() {
        nyPeriode(1.januar til 31.januar)
        val id = håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 10.februar, endringerIRefusjon = emptyList())
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nyPeriode(15.februar til 28.februar)
        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    PersonObserver.Inntektsdata(1.januar, PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING, 31000.0)
                )
            ),
            PersonObserver.Refusjon(forslag = listOf(
                Refusjonsopplysning(id, 11.februar, null, INGEN)
            ))
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender med tom liste med refusjonsopplysninger når vi mangler vilkårsgrunnlag på forrige skjæringstidspunkt`() {
        nyPeriode(1.januar til 31.januar)
        nyPeriode(15.februar til 28.februar)

        val expectedForespurteOpplysninger = listOf(
            PersonObserver.Inntekt(
                PersonObserver.Inntektsforslag(
                    listOf(
                        november(2017),
                        desember(2017),
                        januar(2018)
                    ),
                    null
                )
            ),
            PersonObserver.Refusjon(forslag = emptyList())
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order som er kant i kant`() {
        nyPeriode(1.februar til 28.februar)
        nyPeriode(1.januar til 31.januar)

       assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
       assertEquals(1, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order med gap`() {
        nyPeriode(1.februar til 28.februar)
        nyPeriode(1.januar til 30.januar)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order som ikke fører til en ny forespørsel`() {
        nyPeriode(1.februar til 28.februar)
        nyPeriode(25.januar til 31.januar)

        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)

        assertForventetFeil(
            forklaring = "Her burde februar-perioden sende ut en ny forespørsel",
            nå = {
                assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            },
            ønsket = {
                assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            }
        )
    }

    @Test
    fun `Skal ikke sende ut forespørsler dersom vi er innenfor arbeidsgiverperioden`() {
        nyPeriode(1.januar til 2.januar)
        nyPeriode(3.januar til 6.januar)

        håndterInntektsmelding(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
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
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
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