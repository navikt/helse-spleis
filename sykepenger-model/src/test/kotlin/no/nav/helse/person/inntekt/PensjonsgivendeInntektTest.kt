package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.Grunnbeløp.Companion.`12G`
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
            .map { it.justertÅrsgrunnlag(skjæringstidspunkt) }
            .summer()
            .årlig.toInt()
            .årlig
    }
}

private class PensjonsgivendeInntekt(
    private val år: Int,
    private val beløp: Inntekt
) {

    fun justertÅrsgrunnlag(skjæringstidspunkt: LocalDate): Inntekt {
        val justering = `1G`.beløp(skjæringstidspunkt).årlig / `1G`.snitt(år).årlig
        val gJustertInntekt = beløp * justering
        val `6G` = `6G`.beløp(skjæringstidspunkt)

        val inntekterOppTil6g = minOf(`6G`, beløp * justering)
        val inntekterOppTil12g = minOf(`12G`.beløp(skjæringstidspunkt), gJustertInntekt)
        val inntekterOver6g = maxOf(INGEN, inntekterOppTil12g - `6G`)
        val enTredjedelAvInntekterOver6g = inntekterOver6g / 3

        return (inntekterOppTil6g + enTredjedelAvInntekterOver6g) / 3
    }
}
