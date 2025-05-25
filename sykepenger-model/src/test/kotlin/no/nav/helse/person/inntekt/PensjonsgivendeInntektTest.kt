package no.nav.helse.person.inntekt

import java.time.Year
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt.PensjonsgivendeInntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PensjonsgivendeInntektTest {

    @Test
    fun `fastsatt årsinntekt under 6g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(Year.of(2016), 500000.årlig),
            PensjonsgivendeInntekt(Year.of(2015), 450000.årlig),
            PensjonsgivendeInntekt(Year.of(2014), 380000.årlig),
        )

        val anvendtGrunnbeløp = `1G`.beløp(16.juni)
        val selvstendigGrunnlag = SelvstendigFaktaavklartInntekt.beregnInntektsgrunnlag(inntekter, anvendtGrunnbeløp)
        assertEquals(478906.årlig, selvstendigGrunnlag)
    }

    @Test
    fun `fastsatt årsinntekt over 6g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(Year.of(2016), 670000.årlig),
            PensjonsgivendeInntekt(Year.of(2015), 590000.årlig),
            PensjonsgivendeInntekt(Year.of(2014), 490000.årlig),
        )

        val anvendtGrunnbeløp = `1G`.beløp(12.mai)
        val selvstendigGrunnlag = SelvstendigFaktaavklartInntekt.beregnInntektsgrunnlag(inntekter, anvendtGrunnbeløp)
        assertEquals(589138.årlig, selvstendigGrunnlag)
    }

    @Test
    fun `fastsatt årsinntekt over 12g`() {
        val inntekter = listOf(
            PensjonsgivendeInntekt(Year.of(2016), 1500000.årlig),
            PensjonsgivendeInntekt(Year.of(2015), 0.årlig),
            PensjonsgivendeInntekt(Year.of(2014), 0.årlig),
        )

        val skjæringstidspunkt = 12.mai
        val anvendtGrunnbeløp = `1G`.beløp(skjæringstidspunkt)
        val expected = (`2G`.beløp(skjæringstidspunkt) + `2G`.beløp(skjæringstidspunkt) / 3).årlig.toInt().årlig
        val selvstendigGrunnlag = SelvstendigFaktaavklartInntekt.beregnInntektsgrunnlag(inntekter, anvendtGrunnbeløp)
        assertEquals(expected, selvstendigGrunnlag)
    }
}
