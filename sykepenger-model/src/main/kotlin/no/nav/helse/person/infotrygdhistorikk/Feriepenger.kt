package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.Year

class Feriepenger(
    val orgnummer: String,
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate
) {
    internal companion object {
        internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilPerson(opptjeningsår: Year) =
            filter { it.orgnummer == "0" }.filter { Year.from(it.fom) == opptjeningsår }.sumBy { it.beløp }

        internal fun Iterable<Feriepenger>.utbetalteFeriepengerTilArbeidsgiver(orgnummer: String, opptjeningsår: Year) =
            filter { it.orgnummer == orgnummer }.filter { Year.from(it.fom) == opptjeningsår }.sumBy { it.beløp }
    }
}
