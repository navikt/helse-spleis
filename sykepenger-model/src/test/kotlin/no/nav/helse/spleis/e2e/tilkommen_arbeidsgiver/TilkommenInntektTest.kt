package no.nav.helse.spleis.e2e.tilkommen_arbeidsgiver

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.daglig
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

            // TODO: Spleis får et signal om at det er kommet nye inntekter på personen
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING, reberegning = true)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode(a2, 1.januar, 31.januar, 1000.daglig)))
            assertUtbetalingsbeløp(1.vedtaksperiode, 842, 1431, subset = 17.januar til 31.januar)
        }
    }
}
