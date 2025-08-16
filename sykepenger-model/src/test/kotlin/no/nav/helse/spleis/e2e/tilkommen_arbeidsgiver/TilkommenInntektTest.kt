package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `tilkommen inntekt legges til etter perioden er utbetalt`() = Toggle.TilkommenInntektV4.enable {
        a1 {
            håndterSøknad(januar, inntekterFraNyeArbeidsforhold = true)
            håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag()
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertVarsler(1.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            // Her legger saksbehandler til inntekter basert på informasjon i søknaden

            håndterInntektsendringer(inntektsendringFom = 20.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 20.januar, 31.januar, 1000.daglig)))
            assertVarsler(1.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`, Varselkode.RV_UT_23)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 19.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 431, 1431, subset = 20.januar til 31.januar)
        }
    }

    @Test
    fun `tilkommen inntekt på førstegangsbehandling`() = Toggle.TilkommenInntektV4.enable {
        a1 {
            håndterSøknad(januar, inntekterFraNyeArbeidsforhold = true)
            håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag()
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(1.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            // Her legger saksbehandler til inntekter basert på informasjon i søknaden

            håndterInntektsendringer(inntektsendringFom = 1.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, 1000.daglig)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 431, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `tjener masse penger som tilkommen`() = Toggle.TilkommenInntektV4.enable {
        a1 {
            håndterSøknad(januar, inntekterFraNyeArbeidsforhold = true)
            håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag()
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(1.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            // Her legger saksbehandler til inntekter basert på informasjon i søknaden

            håndterInntektsendringer(inntektsendringFom = 1.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, 10000.daglig)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 17.januar til 31.januar)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_VV_4, Varselkode.RV_SV_5)
        }
    }

    @Test
    fun `blir først lagt til grunn som ghost, men viser seg å være tilkommen inntekt`() {
        a1 {
            nyttVedtak(januar, ghosts = listOf(a2))
            assertUtbetalingsbeløp(1.vedtaksperiode, 1080, 1431, subset = 17.januar til 31.januar)
            assertInntektsgrunnlag(1.januar, 2) {
                assertSykepengegrunnlag(561804.årlig) // 6G for januar 2018
                assertInntektsgrunnlag(a1, INNTEKT, deaktivert = false)
                assertInntektsgrunnlag(a2, INNTEKT, deaktivert = false, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }

            håndterOverstyrArbeidsforhold(1.januar, OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, deaktivert = true, forklaring = "Dette er tilkommen inntekt"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            assertInntektsgrunnlag(1.januar, 2) {
                assertSykepengegrunnlag(372000.årlig)
                assertInntektsgrunnlag(a1, INNTEKT, deaktivert = false)
                assertInntektsgrunnlag(a2, INNTEKT, deaktivert = true, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }

            // Her legger saksbehandler til inntekter basert på informasjon i søknaden
            // Ettersom a2 nå ikke er en del av sykepengegrunnlaget blir utbetalingen annerledes selv om inntekten i a2 er helt lik som da den var ghost
            håndterInntektsendringer(inntektsendringFom = 1.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, INNTEKT)))
            håndterSimulering(1.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 17.januar til 31.januar)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_2, Varselkode.RV_VV_4)
        }
    }
}
