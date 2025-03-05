package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `tilkommen inntekt på førstegangsbehandling`() = Toggle.TilkommenInntektV4.enable {
        a1 {
            håndterSøknad(januar, inntekterFraNyeArbeidsforhold = listOf(Søknad.InntektFraNyttArbeidsforhold(1.januar, 31.januar, a2, 23000))) // TODO: TilkommenV4 nå trenger vi ikke få inn beløp, eller annet. Kan bare være flagg for å legge på varsel
            håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag()
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(1.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            // Her legger saksbehandler til inntekter basert på informasjon i søknaden

            håndterNyeInntekter(fraOgMed = 1.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, 1000.daglig)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 842, 1431, subset = 17.januar til 31.januar)
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
            håndterNyeInntekter(fraOgMed = 1.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, INNTEKT)))
            håndterSimulering(1.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode, 715, 1431, subset = 17.januar til 31.januar)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_UT_23, Varselkode.RV_VV_2)
        }
    }

    private fun TestPerson.TestArbeidsgiver.håndterNyeInntekter(fraOgMed: LocalDate) {
        // TODO: Spleis får et signal om at det er kommet nye inntekter på personen
        // frem til det finnes et sånt signal, trigger vi reberegning manuelt
        val vedtaksperiode = inspektør.førsteVedtaksperiodeSomOverlapperEllerErEtter(fraOgMed)
        håndterPåminnelse(vedtaksperiode.id, vedtaksperiode.tilstand, reberegning = true)
    }
}
