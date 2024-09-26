package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.`3G`
import no.nav.helse.Grunnbeløp.Companion.`4G`
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PensjonsgivendeInntektTest {

    @Test
    fun `fastsatt årsinntekt under 6g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(2016, 500000.årlig),
            PensjonsgivendeInntekt(2015, 450000.årlig),
            PensjonsgivendeInntekt(2014, 380000.årlig),
        )

        assertEquals(478906.årlig, SelvstendigNæringsdrivende(inntekter).fastsattÅrsinntekt(16.juni))
    }

    @Test
    fun `fastsatt årsinntekt over 6g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(2016, 670000.årlig),
            PensjonsgivendeInntekt(2015, 590000.årlig),
            PensjonsgivendeInntekt(2014, 490000.årlig),
        )

        assertEquals(589138.årlig, SelvstendigNæringsdrivende(inntekter).fastsattÅrsinntekt(12.mai))
    }

    @Test
    fun `fastsatt årsinntekt over 12g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(2016, 1500000.årlig),
            PensjonsgivendeInntekt(2015, 0.årlig),
            PensjonsgivendeInntekt(2014, 0.årlig),
        )

        val skjæringstidspunkt = 12.mai
        val expected = (`2G`.beløp(skjæringstidspunkt) + `2G`.beløp(skjæringstidspunkt) / 3).årlig.toInt().årlig
        assertEquals(expected, SelvstendigNæringsdrivende(inntekter).fastsattÅrsinntekt(skjæringstidspunkt))
    }
}

private class SelvstendigNæringsdrivende(private val inntekter: List<PensjonsgivendeInntekt>) {
    fun fastsattÅrsinntekt(skjæringstidspunkt: LocalDate): Inntekt {
        return inntekter
            .map { it.omregnetÅrsinntekt(skjæringstidspunkt) }
            .summer()
            .årlig.toInt()
            .årlig
    }
}

private class PensjonsgivendeInntekt(
    private val år: Int,
    private val beløp: Inntekt
) {

    internal fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate): Inntekt {
        val grense = `2G`.beløp(skjæringstidspunkt)
        val utgangspunkt = beløp * (`1G`.beløp(skjæringstidspunkt) ratio `3G`.snitt(år))
        val aktuelt = minOf(grense, utgangspunkt)
        val ekstra = (utgangspunkt.coerceAtMost(`4G`.beløp(skjæringstidspunkt)) - aktuelt) / 3
        return aktuelt + ekstra
    }
}