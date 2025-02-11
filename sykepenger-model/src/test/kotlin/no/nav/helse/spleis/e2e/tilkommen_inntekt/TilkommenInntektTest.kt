package no.nav.helse.spleis.e2e.tilkommen_inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.InntektFraNyttArbeidsforhold
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.K
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("TODO: TilkommenV3")
internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `korrigerende søknad uten tilkommen inntekt burde fjerne tilkommen inntekt fra vilkårsgrunnlaget`() {
        a1 {
            nyttVedtak(januar)

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1231, 1431, subset = 1.februar til 28.februar)

            // Korrigerende søknad som angrer den tilkomne inntekten
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = emptyList())
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 28.februar)
        }
    }

    @Test
    fun `forlengelse uten tilkommet inntekt - etter periode med tilkommet inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterYtelser(3.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1231, 1431, subset = 1.februar til 28.februar)
            assertUtbetalingsbeløp(3.vedtaksperiode, 1431, 1431, subset = 1.mars til 31.mars)

            a2 {
                assertBeløpstidslinje(inspektør(1.vedtaksperiode).inntektsendringer, februar, 181.daglig)
                assertEquals(1, inspektør.vedtaksperiodeTeller)
            }
        }
    }

    @Test
    fun `syk fra ghost etter periode med tilkommet inntekt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                skatteinntekter = listOf(a1 to INNTEKT, a2 to 10000.månedlig),
                arbeidsforhold = listOf(
                    Triple(a1, LocalDate.EPOCH, null),
                    Triple(a2, 1.januar, null)
                )
            )
            assertVarsler(listOf(Varselkode.RV_VV_1, Varselkode.RV_VV_2), 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(fom = 1.februar, tom = 28.februar, orgnummer = a3, råttBeløp = 4000)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()
        }

        a2 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = INNTEKT)

            assertForventetFeil(
                forklaring = "søknad for februar uten tilkommet inntekt fjerner inntektene fra vilkårsgrunnlaget",
                nå = {},
                ønsket = { fail("""\_(ツ)_/¯""") }
            )
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()

            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 16.februar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1430, 1431, subset = 17.februar til 28.februar)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_8, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            assertUtbetalingsbeløp(1.vedtaksperiode, 462, 1431, subset = 17.februar til 28.februar)
        }
    }

    @Test
    fun `tilkommen inntekt midt i en førstegangsbehandling`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 20.januar,
                        tom = 31.januar,
                        orgnummer = a2,
                        råttBeløp = 40000
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5, 1.vedtaksperiode.filter())
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
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 1.februar,
                        tom = 28.februar,
                        orgnummer = "a2",
                        råttBeløp = 10000
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val dagsatsFørstegangs = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(1431.daglig, dagsatsFørstegangs)
            håndterYtelser(2.vedtaksperiode)
            val dagsatsForlengelse = inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(931.daglig, dagsatsForlengelse)
        }
    }

    @Test
    fun `bruker korrigerer inn tilkommen inntekt på forlengelse`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 1.februar,
                        tom = 28.februar,
                        orgnummer = "a2",
                        råttBeløp = 10000
                    )
                )
            )
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
            assertUtbetalingsbeløp(2.vedtaksperiode, 931, 1431, subset = 1.februar til 28.februar)
        }
    }

    @Test
    fun `bruker korrigerer inn tilkommen inntekt slik at det revurderes`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 1.januar,
                        tom = 31.januar,
                        orgnummer = "a2",
                        råttBeløp = 10000
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_SIMULERING_REVURDERING)
            assertVarsel(Varselkode.RV_SV_5, 1.vedtaksperiode.filter())
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
            }
            assertUtbetalingsbeløp(1.vedtaksperiode, 997, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `inntekt fra søknad på førstegangsbehandling med fom lik skjæringstidspunktet`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                orgnummer = a1,
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 1.januar,
                        tom = 31.januar,
                        orgnummer = a2,
                        råttBeløp = 10000
                    )
                )
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsler(listOf(Varselkode.RV_SV_5, Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `inntekt fra søknad på men allerede ghost på skjæringstidspunktet`() {
        a1 {
            val inntekt = 20000.månedlig
            listOf(a1).nyeVedtak(
                januar,
                inntekt = inntekt,
                ghosts = listOf(a2)
            )
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                orgnummer = a1,
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        råttBeløp = 20000
                    )
                )
            )
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntekt)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `inntekt fra søknad på men syk med IM på skjæringstidspunktet`() {
        a1 {
            val inntekt = 20000.månedlig
            listOf(a1, a2).nyeVedtak(januar, inntekt = inntekt)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                orgnummer = a1,
                inntekterFraNyeArbeidsforhold = listOf(
                    InntektFraNyttArbeidsforhold(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        råttBeløp = 20000
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntekt)
                assertInntektsgrunnlag(a2, inntekt)
            }
        }
    }

    @Test
    fun `markering av en arbeidsgiver og tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar)
            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("EnArbeidsgiver"))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(1.februar, 28.februar, "a3", 100)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(21.januar, 21.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("FlereArbeidsgivere"))
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
            ghosts = listOf(a2)
        )
        a1 {
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())

            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("FlereArbeidsgivere"))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(1.februar, 28.februar, "a3", 100)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(21.januar, 21.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("FlereArbeidsgivere"))
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            assertTilkommenInntektTag(2.vedtaksperiode)
            assertTrue(tags(2.vedtaksperiode).contains("FlereArbeidsgivere"))
        }
    }

    private fun tags(vedtaksperiode: UUID) = observatør.utkastTilVedtakEventer.last { it.vedtaksperiodeId == vedtaksperiode }.tags
    private fun assertIkkeTilkommenInntektTag(vedtaksperiode: UUID) = assertFalse(tags(vedtaksperiode).contains("TilkommenInntekt"))
    private fun assertTilkommenInntektTag(vedtaksperiode: UUID) = assertTrue(tags(vedtaksperiode).contains("TilkommenInntekt"))
}
