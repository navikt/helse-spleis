package no.nav.helse.spleis.e2e.tilkommen_inntekt

import java.time.LocalDate
import java.time.Month
import java.util.UUID
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.K
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `korrigerende søknad uten tilkommen inntekt burde fjerne tilkommen inntekt fra vilkårsgrunnlaget`() {
        a1 {
            nyttVedtak(januar)

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a2, beløp = 4.K.månedlig)))
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1246, 1431, subset = 1.februar til 28.februar)

            // Korrigerende søknad som angrer den tilkomne inntekten
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = emptyList())
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 28.februar)
        }
    }

    @Test
    fun `tilkommen inntekt midt i en førstegangsbehandling`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 20.januar,
                        tom = 31.januar,
                        orgnummer = a2,
                        beløp = 40.K.månedlig
                    )
                )
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 40.K.månedlig)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)

            assertForventetFeil(
                forklaring = "burde vi støtte at en tilkommen inntekt kommer midt i en førstegangssøknad?",
                nå = {
                    assertEquals(
                        utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp,
                        utbetalingstidslinje[22.januar].økonomi.inspektør.arbeidsgiverbeløp
                    )
                    assertFalse(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.any { it.inspektør.orgnummer == a2 } == true)
                },
                ønsket = {
                    assertNotEquals(
                        utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp,
                        utbetalingstidslinje[22.januar].økonomi.inspektør.arbeidsgiverbeløp
                    )
                    assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.any { it.inspektør.orgnummer == a2 } == true)
                }
            )
        }
    }

    @Test
    fun `til godkjenning og så kommer forlengelse-søknad med tilkommen inntekt`() {
        a1 {
            tilGodkjenning(januar, beregnetInntekt = 31000.00.månedlig)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 1.februar,
                        tom = 28.februar,
                        orgnummer = "a2",
                        beløp = 10000.månedlig
                    )
                )
            )
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val dagsatsFørstegangs =
                inspektør.utbetaling(0).utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(1431.daglig, dagsatsFørstegangs)
            håndterYtelser(2.vedtaksperiode)
            val dagsatsForlengelse =
                inspektør.sisteUtbetaling().utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(969.daglig, dagsatsForlengelse)
            // bruker har en tilkommen inntekt på 10K, slik at inntektstapet i perioden er 31K - 10K = 21K.
            // utbetalingen på forlengelsen justeres derfor ned med denne brøken 21/31
        }
    }

    @Test
    fun `bruker korrigerer inn tilkommen inntekt på forlengelse`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 1.februar,
                        tom = 28.februar,
                        orgnummer = "a2",
                        beløp = 10000.månedlig
                    )
                )
            )
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
            assertUtbetalingsbeløp(2.vedtaksperiode, 969, 1431, subset = 1.februar til 28.februar)
        }
    }

    @Test
    fun `bruker korrigerer inn tilkommen inntekt slik at det revurderes`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 1.januar,
                        tom = 31.januar,
                        orgnummer = "a2",
                        beløp = 10000.månedlig
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_SIMULERING_REVURDERING)
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 969, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `inntekt fra søknad på førstegangsbehandling med fom lik skjæringstidspunktet`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                orgnummer = a1,
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 1.januar,
                        tom = 31.januar,
                        orgnummer = a2,
                        beløp = 10000.månedlig
                    )
                )
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                    arbeidsgivere = listOf(a1 to INNTEKT, a2 to 10000.månedlig),
                    skjæringstidspunkt = 1.januar,
                ),
                arbeidsforhold = listOf(
                    Arbeidsforhold(
                        a1,
                        LocalDate.EPOCH,
                        null,
                        Arbeidsforholdtype.ORDINÆRT
                    ),
                    Arbeidsforhold(
                        a2,
                        1.januar,
                        null,
                        Arbeidsforholdtype.ORDINÆRT
                    )
                )
            )
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmelding)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
            }
        }
    }

    @Test
    fun `inntekt fra søknad på men allerede ghost på skjæringstidspunktet`() {
        a1 {
            val inntekt = 20000.månedlig
            val inntekter = listOf(a1 to inntekt, a2 to inntekt)
            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
            listOf(a1).nyeVedtak(
                januar,
                inntekt = inntekt,
                sykepengegrunnlagSkatt = lagStandardSykepengegrunnlag(inntekter, 1.januar),
                arbeidsforhold = arbeidsforhold
            )
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                orgnummer = a1,
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        beløp = 20000.månedlig
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmelding)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
            }
        }
    }

    @Test
    fun `inntekt fra søknad på men syk med IM på skjæringstidspunktet`() {
        a1 {
            val inntekt = 20000.månedlig
            val inntekter = listOf(a1 to inntekt, a2 to inntekt)
            val arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
            listOf(a1, a2).nyeVedtak(
                januar,
                inntekt = inntekt,
                sykepengegrunnlagSkatt = lagStandardSykepengegrunnlag(inntekter, 1.januar),
                arbeidsforhold = arbeidsforhold
            )
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                orgnummer = a1,
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        beløp = 20000.månedlig
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmelding)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is Inntektsmelding)
            }
        }
    }

    @Test
    fun `markering av en arbeidsgiver og tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar)
            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("EnArbeidsgiver"))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(1.februar, 28.februar,"a3", 100.daglig)))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(21.januar, 21.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("EnArbeidsgiver"))
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
        }
    }

    @Test
    fun `markering av flere arbeidsgivere og tilkommen inntekt`() {
        listOf(a1).nyeVedtak(
            periode = januar,
            inntekt = 20000.månedlig,
            sykepengegrunnlagSkatt = lagStandardSykepengegrunnlag(listOf(a1 to 20000.månedlig, a2 to 20000.månedlig), 1.januar),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        a1 {
            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("FlereArbeidsgivere"))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(1.februar, 28.februar,"a3", 100.daglig)))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(21.januar, 21.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
        }
    }

    @Test
    fun `fellesskapets tverrfaglige test på mandag 14 oktober tyvetyvefire`() {
        a1 {
            nyttVedtak(1.januar(2025) til 31.januar(2025), beregnetInntekt = 90000.månedlig)
            håndterSøknad(Sykdom(1.februar(2025), 28.februar(2025), 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(1.februar(2025), 28.februar(2025), "a2", 1867.daglig)))
            håndterYtelser(2.vedtaksperiode)
            inspektør.sisteUtbetaling().utbetalingstidslinje.forEach {
                if (it is Utbetalingsdag.NavDag && it.dato.month == Month.FEBRUARY) {
                    assertEquals(995.daglig, it.økonomi.arbeidsgiverbeløp)
                    assertForventetFeil(
                        forklaring = "Morten har figuren i excalidraw med mellomutregningene",
                        nå = {
                            assertEquals(100.prosent, it.økonomi.totalGrad)
                        },
                        ønsket = {
                            assertEquals(35.prosent, it.økonomi.totalGrad)
                        }
                    )
                }
            }
        }
    }

    private fun tags(vedtaksperiode: UUID) = observatør.utkastTilVedtakEventer.last { it.vedtaksperiodeId == vedtaksperiode }.tags
    private fun assertIkkeTilkommenInntektTag(vedtaksperiode: UUID) = assertFalse(tags(vedtaksperiode).contains("TilkommenInntekt"))
    private fun assertTilkommenInntektTag(vedtaksperiode: UUID) = assertTrue(tags(vedtaksperiode).contains("TilkommenInntekt"))
}