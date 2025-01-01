package no.nav.helse.spleis.e2e.tilkommen_inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.testhelpers.assertInstanceOf
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
    fun `korrigerende søknad uten tilkommen inntekt burde fjerne tilkommen inntekt fra vilkårsgrunnlaget`() {
        a1 {
            nyttVedtak(januar)

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000)))
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1231, 1431, subset = 1.februar til 28.februar)

            // Korrigerende søknad som angrer den tilkomne inntekten
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = emptyList())
            håndterYtelser(2.vedtaksperiode)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 28.februar)
        }
    }

    @Test
    fun `forlengelse uten tilkommet inntekt - etter periode med tilkommet inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000)))
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

            inspektør.vilkårsgrunnlag(3.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                assertEquals(1, tilkommendeInntekter.size)
                assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
            }
        }
    }

    @Test
    fun `overstyrer tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000))
            )
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1231, 1431, subset = 1.februar til 28.februar)
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                assertEquals(1, tilkommendeInntekter.size)
                assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
                assertEquals(200.daglig, tilkommendeInntekter.single().beløpstidslinje[1.februar].beløp)
            }
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        a2,
                        250.daglig,
                        forklaring = "forklaring",
                        gjelder = 1.februar til 28.februar
                    )
                )
            )
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1181, 1431, subset = 1.februar til 28.februar)
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                assertEquals(1, tilkommendeInntekter.size)
                assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
                assertEquals(250.daglig, tilkommendeInntekter.single().beløpstidslinje[1.februar].beløp)
            }
        }
    }

    @Test
    fun `overstyrer seneste tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a2, råttBeløp = 4000))
            )
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()
            håndterSøknad(
                Sykdom(1.mars, 31.mars, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(fom = 1.mars, tom = 31.mars, orgnummer = a2, råttBeløp = 8000)
                )
            )
            assertVarsel(Varselkode.RV_SV_5, 3.vedtaksperiode.filter())
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode, true)
            håndterUtbetalt()
            inspektør.vilkårsgrunnlag(3.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                assertEquals(1, tilkommendeInntekter.size)
                assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
                assertEquals(200.daglig, tilkommendeInntekter.single().beløpstidslinje[1.februar].beløp)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.mars])
                assertEquals(363.daglig, tilkommendeInntekter.single().beløpstidslinje[1.mars].beløp)
            }
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        a2,
                        400.daglig,
                        forklaring = "forklaring",
                        gjelder = 1.mars til 31.mars
                    )
                )
            )
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
            inspektør.vilkårsgrunnlag(3.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                assertEquals(1, tilkommendeInntekter.size)
                assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
                assertEquals(200.daglig, tilkommendeInntekter.single().beløpstidslinje[1.februar].beløp)
                assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.mars])
                assertEquals(400.daglig, tilkommendeInntekter.single().beløpstidslinje[1.mars].beløp)
            }
        }
    }

    @Test
    fun `overstyre inn tilkommen inntekt fra blanke ark`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        a2,
                        400.daglig,
                        forklaring = "forklaring",
                        gjelder = 5.januar til 31.januar
                    )
                )
            )
            assertForventetFeil(
                forklaring = "Tilkommen inntekt kan dukke opp om informasjonen er tilgjengelig for saxbehandler senere enn når søknaden blir sendt",
                nå = {
                    inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                        assertEquals(0, tilkommendeInntekter.size)
                    }
                },
                ønsket = {
                    inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                        assertEquals(1, tilkommendeInntekter.size)
                        assertEquals(a2, tilkommendeInntekter.single().orgnummer)
                        assertEquals(400.daglig, tilkommendeInntekter.single().beløpstidslinje[5.januar].beløp)
                    }
                }
            )
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

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = 28.februar, orgnummer = a3, råttBeløp = 4000)))
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
                nå = {
                    inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                        assertEquals(0, tilkommendeInntekter.size)
                    }
                },
                ønsket = {
                    inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inntektsgrunnlag.inspektør.tilkommendeInntekter.also { tilkommendeInntekter ->
                        assertEquals(1, tilkommendeInntekter.size)
                        assertEquals(a3, tilkommendeInntekter.single().orgnummer)
                        assertInstanceOf<Beløpsdag>(tilkommendeInntekter.single().beløpstidslinje[1.februar])
                    }
                }
            )
        }

        a1 {
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
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
            val dagsatsFørstegangs =
                inspektør.utbetaling(0).utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
            assertEquals(1431.daglig, dagsatsFørstegangs)
            håndterYtelser(2.vedtaksperiode)
            val dagsatsForlengelse =
                inspektør.sisteUtbetaling().utbetalingstidslinje.inspektør.navdager.last().økonomi.inspektør.arbeidsgiverbeløp
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
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
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmeldinginntekt)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        råttBeløp = 20000
                    )
                )
            )
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmeldinginntekt)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
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
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 15.februar,
                        tom = 28.februar,
                        orgnummer = a2,
                        råttBeløp = 20000
                    )
                )
            )
            assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmeldinginntekt)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is Inntektsmeldinginntekt)
            }
        }
    }

    @Test
    fun `markering av en arbeidsgiver og tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar)
            assertIkkeTilkommenInntektTag(1.vedtaksperiode)
            assertTrue(tags(1.vedtaksperiode).contains("EnArbeidsgiver"))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(1.februar, 28.februar, "a3", 100)))
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

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(1.februar, 28.februar, "a3", 100)))
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
