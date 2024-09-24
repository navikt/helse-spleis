package no.nav.helse.spleis.e2e.tilkommen_inntekt

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.InntektFraSøknad
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
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
            val tilkommenInntekt = inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.arbeidsgiverInntektsopplysninger.first { it.gjelder(a2) }.inspektør.inntektsopplysning
            assertTrue(tilkommenInntekt is InntektFraSøknad)
            assertEquals(4.K.månedlig, tilkommenInntekt.fastsattÅrsinntekt())

            // Korrigerende søknad som angrer den tilkomne inntekten
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = emptyList())

            assertForventetFeil(
                forklaring = "Den korrigerende søknaden skal fjerne tilkommen inntekt fra vilkårsgrunnlaget",
                ønsket = {
                    assertNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.arbeidsgiverInntektsopplysninger.firstOrNull { it.gjelder(a2) })
                },
                nå = {
                    val korrigertTilkommenInntekt = inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.arbeidsgiverInntektsopplysninger.first { it.gjelder(a2) }.inspektør.inntektsopplysning
                    assertTrue(korrigertTilkommenInntekt is InntektFraSøknad)
                    assertEquals(4.K.månedlig, korrigertTilkommenInntekt.fastsattÅrsinntekt())
                }
            )
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
    fun `oppdaterer sykepengegrunnlag med inntekter fra søknaden`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
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
            assertVarsel(Varselkode.RV_SV_5)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                val inntektA1 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]
                val inntektA2 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]
                assertEquals(1.januar til LocalDate.MAX, inntektA1!!.inspektør.gjelder)
                assertEquals(31000.månedlig, inntektA1.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA1.inspektør.inntektsopplysning is Inntektsmelding)
                assertEquals(1.februar til 28.februar, inntektA2!!.inspektør.gjelder)
                assertEquals(10000.månedlig, inntektA2.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA2.inspektør.inntektsopplysning is InntektFraSøknad)
            }
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
                inspektør.utbetalinger.single().inspektør.utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(1431.daglig, dagsatsFørstegangs)
            håndterYtelser(2.vedtaksperiode)
            val dagsatsForlengelse =
                inspektør.utbetalinger.last().inspektør.utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
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
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
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
                        fom = 1.februar,
                        tom = 28.februar,
                        orgnummer = "a2",
                        beløp = 10000.månedlig
                    )
                )
            )
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
            assertVarsel(Varselkode.RV_SV_5)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
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
                    Vilkårsgrunnlag.Arbeidsforhold(
                        a1,
                        LocalDate.EPOCH,
                        null,
                        Arbeidsforholdtype.ORDINÆRT
                    ),
                    Vilkårsgrunnlag.Arbeidsforhold(
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
}