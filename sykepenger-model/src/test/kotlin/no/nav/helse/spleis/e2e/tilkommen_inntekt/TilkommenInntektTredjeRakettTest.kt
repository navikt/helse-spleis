package no.nav.helse.spleis.e2e.tilkommen_inntekt

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TilkommenInntektTredjeRakettTest : AbstractDslTest() {

    @Test
    fun `Oppretter vedtaksperiode for tilkommen inntekt og legger til inntekt som inntektsendring på behandlingsendring`() = Toggle.Companion.TilkommenInntektV3.enable {
        a1 {
            nyttVedtak(januar)
            assertUgyldigSituasjon("peker på søknaden") { // TODO: tenk litt på dokumentsporing, vi har lovet flex at på sis-topicet tilhører ALDRI én søknad flere vedtaksperioder
                håndterSøknad(
                    februar,
                    inntekterFraNyeArbeidsforhold = listOf(Søknad.InntektFraNyttArbeidsforhold(1.februar, 28.februar, a2, 1000)),
                )
            }
            assertEquals(2, inspektør.vedtaksperiodeTeller)
            assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK)
        }
        a2 {
            assertEquals(1, inspektør.vedtaksperiodeTeller)
            assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
            assertBeløpstidslinje(
                inspektør(1.vedtaksperiode).inntektsendringer,
                februar,
                50.daglig
            )
        }
        a1 {
            assertUgyldigSituasjon("peker på søknaden") {
                håndterYtelser(2.vedtaksperiode)
            }
            assertForventetFeil(
                "har ikke hensyntatt tilkommen inntekt på utbetalingstidslinjen ennå",
                nå = {
                    assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431)
                },
                ønsket = {
                    assertUtbetalingsbeløp(2.vedtaksperiode, 1381, 1431)
                }
            )

        }
    }
}
