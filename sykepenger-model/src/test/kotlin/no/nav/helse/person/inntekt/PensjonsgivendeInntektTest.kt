package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Grunnbeløp
import no.nav.helse.juni
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

internal class PensjonsgivendeInntektTest {

    @Test
    fun `sykepengegrunnlag under 6g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(2016, 500000.årlig),
            PensjonsgivendeInntekt(2015, 450000.årlig),
            PensjonsgivendeInntekt(2014, 380000.årlig),
        )

        assertEquals(478906.årlig, SelvstendigNæringsdrivende(inntekter).fastsattÅrsinntekt(16.juni))
    }
}

private class SelvstendigNæringsdrivende(private val inntekter: List<PensjonsgivendeInntekt>) {
    internal fun fastsattÅrsinntekt(skjæringstidspunkt: LocalDate): Inntekt {
        return inntekter
            .map { it.omregnetÅrsinntekt(skjæringstidspunkt) }
            .summer()
            .reflection { årlig, _, _, _ -> årlig.roundToInt() }
            .årlig
    }
}

private class PensjonsgivendeInntekt(
    private val år: Int,
    private val beløp: Inntekt
) {

    internal fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate) =
        beløp * (Grunnbeløp.`1G`.beløp(skjæringstidspunkt) ratio Grunnbeløp.`3G`.snitt(år))
}