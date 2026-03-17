package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrInntektFlereArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `overstyr inntekt med flere AG -- happy case`() {
        tilGodkjenningFlereAG()
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
            }
            håndterOverstyrInntekt(1.januar, 19000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
                assertInntektsgrunnlag(a2, INNTEKT)
            }
        }
    }

    @Test
    fun `overstyr inntekt med flere AG -- kan overstyre perioden i AvventerBlokkerende`() {
        tilGodkjenningFlereAG()
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a2 {
            håndterOverstyrInntekt(1.januar, 19000.månedlig)
            assertIngenFunksjonelleFeil()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
            }
        }
    }

    @Test
    fun `skal ikke kunne overstyre en arbeidsgiver hvis en annen er utbetalt`() {
        tilGodkjenningFlereAG()
        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrInntekt(1.januar, 19000.månedlig)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, 19000.månedlig, forventetKorrigertInntekt = 19000.månedlig)
            }
        }
    }

    @Test
    fun `flere arbeidsgivere med ghost - overstyrer inntekt til arbeidsgiver med sykdom -- happy case`() {
        tilOverstyring()
        a1 {
            håndterOverstyrInntekt(1.januar, 29000.månedlig)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, 29000.månedlig, forventetKorrigertInntekt = 29000.månedlig)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        nullstillTilstandsendringer()
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(
                    1.vedtaksperiode,
                    AVVENTER_HISTORIKK,
                    AVVENTER_SIMULERING,
                    AVVENTER_GODKJENNING
            )
        }
    }

    @Test
    fun `overstyrer inntekt til under krav til minste inntekt`() {
        tilGodkjenningFlereAG(beregnetInntekt = 1959.månedlig)
        a1 {
            håndterOverstyrInntekt(1.januar, 1500.månedlig)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_SV_1, 1.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil()
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `overstyring av inntekt kan føre til brukerutbetaling`() {
        a1 { nyPeriode(januar) }
        a2 { nyPeriode(januar) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT / 4) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT / 4) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrInntekt(1.januar, 8000.månedlig)
            håndterYtelser(1.vedtaksperiode)

            inspektør.utbetaling(1).also { utbetaling ->
                assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
                utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(358, linje.beløp)
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                }
                assertEquals(1, utbetaling.personOppdrag.size)
                utbetaling.personOppdrag[0].inspektør.also { linje ->
                    assertEquals(12, linje.beløp)
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                }
            }
        }
        a2 {
            inspektør.utbetaling(1).also { utbetaling ->
                assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
                utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(358, linje.beløp)
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                }
                assertTrue(utbetaling.personOppdrag.isEmpty())
            }
        }
    }

    private fun tilGodkjenningFlereAG(beregnetInntekt: Inntekt = INNTEKT) {
        a1 { nyPeriode(januar) }
        a2 { nyPeriode(januar) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
    }

    private fun tilOverstyring(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) {
        a1 {
            nyPeriode(fom til tom)
            håndterArbeidsgiveropplysninger(listOf(fom til fom.plusDays(15)))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
    }
}
