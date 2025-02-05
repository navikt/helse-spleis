package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.BeløpstidslinjeDsl.Arbeidsgiver
import no.nav.helse.dsl.BeløpstidslinjeDsl.hele
import no.nav.helse.dsl.BeløpstidslinjeDsl.oppgir
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkårsprøvdSkjæringstidspunktTest {

    @Test
    fun ghosttidslinjer() {
        val vilkårsgrunnlag = VilkårsprøvdSkjæringstidspunkt(
            `6G` = Grunnbeløp.`6G`.beløp(1.januar),
            inntekter = listOf(
                VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt("a1", fastsattÅrsinntekt = 1000.daglig),
                VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt("a2", fastsattÅrsinntekt = 1000.daglig)
            ),
            tilkommendeInntekter = emptyList(),
            deaktiverteArbeidsforhold = emptyList()
        )

        vilkårsgrunnlag.medGhostOgTilkommenInntekt(
            mapOf(
                "a1" to listOf(tidslinjeOf(16.AP, 8.NAV)),
                "a2" to listOf(tidslinjeOf(20.UTELATE, 8.AP))
            )
        ).also { result ->
            assertEquals(2, result.size)
            assertEquals(28, result["a1"]?.size)
            assertEquals(28, result["a2"]?.size)
        }
    }

    @Test
    fun `nye inntekter underveis`() {
        val vilkårsgrunnlag = VilkårsprøvdSkjæringstidspunkt(
            `6G` = Grunnbeløp.`6G`.beløp(1.januar),
            inntekter = listOf(
                VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt("a1", fastsattÅrsinntekt = 1000.daglig)
            ),
            tilkommendeInntekter = listOf(
                VilkårsprøvdSkjæringstidspunkt.NyInntektUnderveis("a2", Arbeidsgiver oppgir 500.daglig hele januar)
            ),
            deaktiverteArbeidsforhold = emptyList()
        )

        vilkårsgrunnlag.medGhostOgTilkommenInntekt(
            mapOf(
                "a1" to listOf(tidslinjeOf(16.AP, 8.NAV))
            )
        ).also { result ->
            assertEquals(2, result.size)
            assertEquals(24, result["a1"]?.size)
            assertEquals(24, result["a2"]?.size)
            assertEquals(1.januar til 24.januar, result["a2"]?.periode())
        }
    }
}
