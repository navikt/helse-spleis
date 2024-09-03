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
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `tilkommen inntekt midt i en førstegangsbehandling`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                tilkomneInntekter = listOf(TilkommenInntekt(fom = 20.januar, tom = 31.januar, orgnummer = a2, beløp = 40.K.månedlig))
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 40.K.månedlig)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)

            assertForventetFeil(
                forklaring = "burde vi støtte at en tilkommen inntekt kommer midt i en førstegangssøknad?",
                nå = {
                    assertEquals(utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp, utbetalingstidslinje[22.januar].økonomi.inspektør.arbeidsgiverbeløp)
                    assertFalse(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.any { it.inspektør.orgnummer == a2 } == true)
                },
                ønsket = {
                    assertNotEquals(utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp, utbetalingstidslinje[22.januar].økonomi.inspektør.arbeidsgiverbeløp)
                    assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.any { it.inspektør.orgnummer == a2 } == true)
                }
            )
        }
    }

    @Test
    fun `oppdaterer sykepengegrunnlag med inntekter fra søknaden`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = null, orgnummer = "a2", beløp = 10000.månedlig)))
            assertVarsel(Varselkode.RV_SV_5)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                val inntektA1 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]
                val inntektA2 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]
                assertEquals(1.januar til LocalDate.MAX, inntektA1!!.inspektør.gjelder)
                assertEquals(31000.månedlig, inntektA1.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA1.inspektør.inntektsopplysning is Inntektsmelding)
                assertEquals(1.februar til LocalDate.MAX, inntektA2!!.inspektør.gjelder)
                assertEquals(10000.månedlig, inntektA2.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA2.inspektør.inntektsopplysning is InntektFraSøknad)
            }
        }
    }

    @Test
    fun `til godkjenning og så kommer forlengelse-søknad med tilkommen inntekt`() {
        a1{
            tilGodkjenning(januar, beregnetInntekt = 31000.00.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = null, orgnummer = "a2", beløp = 10000.månedlig)))
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val dagsatsFørstegangs = inspektør.utbetalinger.single().inspektør.utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(1431.daglig, dagsatsFørstegangs)
            håndterYtelser(2.vedtaksperiode)
            val dagsatsForlengelse = inspektør.utbetalinger.last().inspektør.utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(969.daglig, dagsatsForlengelse)
            // bruker har en tilkommen inntekt på 10K, slik at inntektstapet i perioden er 31K - 10K = 21K.
            // utbetalingen på forlengelsen justeres derfor ned med denne brøken 21/31
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
                        tom = null,
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
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmelding)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
            }
        }
    }
}