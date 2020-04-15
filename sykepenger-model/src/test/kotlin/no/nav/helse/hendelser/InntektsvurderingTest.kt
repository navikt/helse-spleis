package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class InntektsvurderingTest {
    private companion object {
        private const val INNTEKT = 1000.0
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    internal fun `ugyldige verdier`() {
        assertTrue(undersøke(inntektsvurdering(emptyMap()), 0.0))
        assertTrue(undersøke(inntektsvurdering(mapOf(YearMonth.now() to listOf(0.0))), 0.0))
    }

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val inntektsvurdering = inntektsvurdering()
        assertTrue(undersøke(inntektsvurdering, 1250.01))
        assertTrue(undersøke(inntektsvurdering, 749.99))
        assertFalse(undersøke(inntektsvurdering, 1000.00))
        assertFalse(undersøke(inntektsvurdering, 1250.00))
        assertFalse(undersøke(inntektsvurdering, 750.00))
    }

    private fun undersøke(inntektsvurdering: Inntektsvurdering, beregnetInntekt: Double): Boolean {
        aktivitetslogg = Aktivitetslogg()
        return inntektsvurdering.valider(aktivitetslogg, beregnetInntekt.toBigDecimal()).hasErrors()
    }

    private fun inntektsvurdering(
        inntektsmåneder: Map<YearMonth, List<Double>> = (1..12).map {
            YearMonth.of(2017, it) to INNTEKT
        }.groupBy({ it.first }) { it.second }
    ) = Inntektsvurdering(inntektsmåneder)
}
