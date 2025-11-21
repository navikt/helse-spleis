package no.nav.helse.spleis.e2e.andre_ytelser

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class GraderteAndreYtelserTest: AbstractDslTest() {

    @Test
    fun `pleiepenger sykt barn`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 520_000.årlig)
            assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
            håndterInntektsendringer(20.januar)
            håndterYtelser(1.vedtaksperiode, inntekterForBeregning = listOf(InntekterForBeregning.Inntektsperiode.AndelAvSykepengegrunnlag("PLEIEPENGER", 20.januar til 30.januar, 30.prosent)))
            assertForventetFeil(
                forklaring = "Det er jo ikke laget enda da, så ville vært litt sprøtt om det fungerte og",
                nå = {
                    assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 31.januar)
                     },
                ønsket = {
                    assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 17.januar til 19.januar)
                    assertUtbetalingsbeløp(1.vedtaksperiode, 1400, 2000, subset = 20.januar til 30.januar)
                    assertUtbetalingsbeløp(1.vedtaksperiode, 2000, 2000, subset = 31.januar til 31.januar)
                }
            )
        }
    }
}
