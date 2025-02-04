package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.økonomi.Inntekt

class Inntektstidslinje(
    private val inntektsendringer: Beløpstidslinje,
    private val fastsattÅrsinntekt: Inntekt?,
) {
    internal operator fun get(dato: LocalDate): Inntekt {
        val dag = inntektsendringer[dato]
        if (dag is Beløpsdag) return dag.beløp // Direktetreff
        return fastsattÅrsinntekt ?: error(
            "Vi har en vedtaksperiode som hverken har fastsatt inntekt på skjæringstidspunktet eller inntektsendringer underveis.\n" +
                "Finner ingen inntekt for dato $dato."
        )
    }
}
