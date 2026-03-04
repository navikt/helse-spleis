package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import org.junit.jupiter.api.Test

/**
 * vi skal, i en kort periode, droppe alle inntektsmeldinger som har arbeidsforholdId,
 * men ta de inn igjen på replay
 */
internal class GlemArbeidsforholdIdLittTest : AbstractDslTest() {
    @Test
    fun `inntektsmelding med arbeidsforholdId blir ignorert`() {
        a1 {
            håndterSøknad(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), arbeidsforholdId = "123")
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("trengerReplay"))
            //TODO: også bør vi verifisere at vi likevel takler å håndtere en inntektsmelding med arbeidsforholdId, i en replay-kontekst
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
        }
    }
}
