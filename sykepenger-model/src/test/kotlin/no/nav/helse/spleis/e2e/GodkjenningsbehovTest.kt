package no.nav.helse.spleis.e2e

import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_GODKJENT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningsbehovTest : AbstractDslTest() {

    @Test
    fun `sender med inntektskilder i sykepengegrunnlaget i godkjenningsbehovet`() {
        a1 { nyPeriode(januar) }
        a2 { nyPeriode(januar) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(), nåtidspunkt = 10.februar(2025).atStartOfDay()) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            val inntektskilder = inntektskilder(godkjenningsbehov)
            assertEquals(listOf(Inntektskilde.Arbeidsgiver, Inntektskilde.AOrdningen), inntektskilder)
        }
    }

    @Test
    fun `sender med inntektskilde saksbehandler i sykepengegrunnlaget i godkjenningsbehovet ved skjønnsmessig fastsettelse -- AOrdning på orginal inntekt`() {
        a1 {
            nyPeriode(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(), nåtidspunkt = 10.februar(2025).atStartOfDay())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        a1 {
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            val inntektskilder = inntektskilder(godkjenningsbehov)
            assertEquals(listOf(Inntektskilde.Saksbehandler), inntektskilder)
        }
    }

    @Test
    fun `sender med inntektskilde saksbehandler i sykepengegrunnlaget i godkjenningsbehovet ved skjønnsmessig fastsettelse -- inntektsmelding på orginal inntekt`() {
        a1 { tilGodkjenning(januar) }
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        a1 {
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            val inntektskilder = inntektskilder(godkjenningsbehov)
            assertEquals(listOf(Inntektskilde.Saksbehandler), inntektskilder)
        }
    }

    @Test
    fun `sender med sykepengegrunnlag i godkjenningsbehovet`() {
        a1 {
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                tilGodkjenning(januar, beregnetInntekt = INNTEKT * 6)
            }
            assertEquals(Grunnbeløp.`6G`.beløp(1.januar).årlig, godkjenningsbehov.event.sykepengegrunnlagsfakta.sykepengegrunnlag)
        }
    }

    @Test
    fun `sender med feil vilkårsgrunnlagId i påminnet godkjenningsbehov om det har kommet nytt vilkårsgrunnlag med endring _senere_ enn perioden`() {
        a1 {
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                tilGodkjenning(januar)
            }
            val vilkårsgrunnlagId1 = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.view().inspektør.vilkårsgrunnlagId
            assertEquals(vilkårsgrunnlagId1, godkjenningsbehov.event.vilkårsgrunnlagId)
            nyPeriode(februar)
            nyPeriode(mars)
            nullstillTilstandsendringer()
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.mars,
                refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null)
            )
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING)
            val vilkårsgrunnlagId2 = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.view().inspektør.vilkårsgrunnlagId
            assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagId2)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(vilkårsgrunnlagId1, godkjenningsbehov.event.vilkårsgrunnlagId)
        }
    }

    @Test
    fun `sender med feil vilkårsgrunnlagId i første godkjenningsbehov om det har kommet nytt vilkårsgrunnlag med endring _senere_ enn perioden mens den står i avventer simulering`() {
        a1 {
            nyPeriode(januar)
            nyPeriode(februar)
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            val vilkårsgrunnlagId1 = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.view().inspektør.vilkårsgrunnlagId
            håndterYtelser(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.mars,
                refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null)
            )
            assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING)
            val vilkårsgrunnlagId2 = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.view().inspektør.vilkårsgrunnlagId
            assertEquals(vilkårsgrunnlagId1, vilkårsgrunnlagId2)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            assertEquals(vilkårsgrunnlagId1, godkjenningsbehov.event.vilkårsgrunnlagId)
        }
    }

    @Test
    fun `godkjenningsbehov som ikke kan avvises automatisk blir avvist av saksbehandler`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(17.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            nyPeriode(mars)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "Agp skal utbetales av NAV!!"
            )
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertFalse(godkjenningsbehov.event.kanAvvises)

            val utbetalingId = godkjenningsbehov.utbetalingId
            val utbetaling = inspektør.utbetalinger(1.vedtaksperiode).last()
            assertEquals(utbetalingId, utbetaling.inspektør.utbetalingId)

            assertEquals(IKKE_UTBETALT, utbetaling.inspektør.tilstand)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 1.mars, listOf(1.mars til 16.mars))

            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)

            assertEquals(IKKE_GODKJENT, inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.tilstand)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 17.januar, listOf(17.januar til 31.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 1.mars, listOf(1.mars til 16.mars))

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_IV_7, 2.vedtaksperiode.filter())
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `førstegangsbehandling som kan avvises`() {
        a1 {
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                tilGodkjenning(januar)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue(godkjenningsbehov.event.kanAvvises)
        }
    }

    @Test
    fun `omgjøring som kan avvises`() {
        a1 {
            nyPeriode(2.januar til 17.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue(godkjenningsbehov.event.kanAvvises)
        }
    }

    @Test
    fun `omgjøring som _ikke_ kan avvises`() {
        a1 {
            nyPeriode(2.januar til 17.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyPeriode(18.januar til 31.januar)
            håndterInntektsmelding(listOf(2.januar til 17.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            val godkjenningsbehov1 = enesteGodkjenningsbehovSomFølgeAv({2.vedtaksperiode}) {
                håndterSimulering(2.vedtaksperiode)
            }

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue(godkjenningsbehov1.event.kanAvvises)

            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov2 = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertFalse(godkjenningsbehov2.event.kanAvvises)

            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            val godkjenningsbehov3 = enesteGodkjenningsbehovSomFølgeAv({2.vedtaksperiode}) {
                håndterYtelser(2.vedtaksperiode)
            }

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertFalse(godkjenningsbehov3.event.kanAvvises)
        }
    }

    @Test
    fun `revurdering kan ikke avvises`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.05, emptyList())))
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertFalse(godkjenningsbehov.event.kanAvvises)
        }
    }

    @Test
    fun `kan avvise en out of order rett i forkant av en utbetalt periode`() {
        a1 {
            nyttVedtak(februar)
            nyPeriode(januar)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            val godkjenningsbehov1 = enesteGodkjenningsbehovSomFølgeAv({2.vedtaksperiode}) {
                håndterSimulering(2.vedtaksperiode)
            }

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue(godkjenningsbehov1.event.kanAvvises)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            håndterUtbetalingsgodkjenning(2.vedtaksperiode, godkjent = false)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.februar, listOf(1.februar til 16.februar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            val godkjenningsbehov2 = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterYtelser(1.vedtaksperiode)
            }

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertFalse(godkjenningsbehov2.event.kanAvvises)
        }
    }

    @Test
    fun `kan avvise en out of order selv om noe er utbetalt senere på annen agp`() {
        a1 {
            nyttVedtak(mars)
            nyPeriode(januar)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            val godkjenningsbehov1 = enesteGodkjenningsbehovSomFølgeAv({2.vedtaksperiode}) {
                håndterSimulering(2.vedtaksperiode)
            }

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue(godkjenningsbehov1.event.kanAvvises)

            håndterUtbetalingsgodkjenning(2.vedtaksperiode, godkjent = false)

            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            val godkjenningsbehov2 = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterYtelser(1.vedtaksperiode)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertFalse(godkjenningsbehov2.event.kanAvvises)

            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `markerer godkjenningsbehov som har brukt skatteinntekter istedenfor inntektsmelding med riktig tag`() {
        a1 {
            nyPeriode(januar)
            håndterPåminnelse(
                1.vedtaksperiode,
                AVVENTER_INNTEKTSMELDING,
                tilstandsendringstidspunkt = 10.november(2024).atStartOfDay(),
                nåtidspunkt = 10.februar(2025).atStartOfDay()
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertTrue("InntektFraAOrdningenLagtTilGrunn" in godkjenningsbehov.event.tags)
        }
    }

    @Test
    fun `markerer godkjenningsbehov som har brukt skatteinntekter istedenfor inntektsmelding med riktig tag for flere arbeidsgivere med ulik start`() {
        a1 { nyPeriode(januar) }
        a2 { nyPeriode(februar) }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
        }
        a2 {
            håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar)
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            val godkjenningsbehov = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertTrue("InntektFraAOrdningenLagtTilGrunn" in godkjenningsbehov.event.tags)
        }
    }

    private fun inntektskilder(godkjenningsbehov: Behovsoppsamler.Behovsdetaljer.Godkjenning) = when (val fakta = godkjenningsbehov.event.sykepengegrunnlagsfakta) {
        is EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel -> fakta.arbeidsgivere.map { Inntektskilde.valueOf(it.inntektskilde) }
        is EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn -> fakta.arbeidsgivere.map { Inntektskilde.Saksbehandler }
        is EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerFraInfotrygd,
        is EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel -> error("Denne testen tester ikke disse casene..")
    }
}
