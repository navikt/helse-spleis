package no.nav.helse.spleis.e2e.tilkommen_inntekt

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TilkommenInntektTredjeRakettTest : AbstractDslTest() {

    @Test
    fun `Oppretter vedtaksperiode for tilkommen inntekt og legger til inntekt som inntektsendring på behandlingsendring`() = Toggle.Companion.TilkommenInntektV3.enable {
        a1 {
            nyttVedtak(januar)
            assertUgyldigSituasjon("peker på søknaden") { // TODO: tenk litt på dokumentsporing, vi har lovet flex at på sis-topicet ALDRI har en søknad hører til flere vedtaksperioder
                håndterSøknad(
                    februar,
                    tilkomneInntekter = listOf(Søknad.TilkommenInntekt(1.februar, 28.februar, a2, 1000)),
                )
            }
            assertEquals(2, inspektør.vedtaksperiodeTeller)
            assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK)
        }
        a2 {
            assertEquals(1, inspektør.vedtaksperiodeTeller)
            assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
        }
    }
}
