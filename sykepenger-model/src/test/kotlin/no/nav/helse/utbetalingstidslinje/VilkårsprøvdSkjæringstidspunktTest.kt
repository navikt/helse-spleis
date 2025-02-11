package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp
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
}
