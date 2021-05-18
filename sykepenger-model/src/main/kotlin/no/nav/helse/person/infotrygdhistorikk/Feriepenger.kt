package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.Year

class Feriepenger(
    val orgnummer: String,
    val beløp: Double,
    val fom: LocalDate,
    val tom: LocalDate
) {
    internal companion object {
        internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilArbeidsgiver(orgnummer: String, opptjeningsår: Year) =
            filter { it.orgnummer == orgnummer }.filter { Year.from(it.fom) == opptjeningsår }.sumByDouble { it.beløp }
    }
}
