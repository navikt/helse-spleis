package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.økonomi.Inntekt

class Inntektstidslinje(
    beløpstidslinje: Beløpstidslinje,
    skjæringstidspunkt: LocalDate,
    private val fastsattÅrsinntekt: Inntekt?,
    private val gjelderTilOgMed: LocalDate = LocalDate.MAX
) {
    private val beløpstidslinje = beløpstidslinje.fraOgMed(skjæringstidspunkt.nesteDag)

    internal operator fun get(dato: LocalDate): Inntekt? {
        if (dato > gjelderTilOgMed) return null // Arbeidsgiveren har opphørt/deaktivert
        val dag = beløpstidslinje[dato]
        if (dag is Beløpsdag) return dag.beløp // Direktetreff
        return fastsattÅrsinntekt // Gir et beløp for arbeidsgivere på skjæringstidspunktet, mens andre (les tilkommen) ikke får noe beløp (null)
    }
}
