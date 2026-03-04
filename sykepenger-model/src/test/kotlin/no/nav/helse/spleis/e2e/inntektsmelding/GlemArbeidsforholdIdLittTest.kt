package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
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
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `flere inntektsmeldinger med arbeidsforholdId satt blir til RV_IM_4 når de replayes`() {
        a1 {
            håndterSøknad(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), arbeidsforholdId = "123")
            håndterInntektsmelding(listOf(1.januar til 12.januar, 13.januar til 16.januar), arbeidsforholdId = "124")
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("trengerReplay"))
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_VILKÅRSPRØVING)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_IM_4)
        }
    }
}
