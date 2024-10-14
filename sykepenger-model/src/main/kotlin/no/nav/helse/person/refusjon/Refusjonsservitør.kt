package no.nav.helse.person.refusjon

import java.time.LocalDate
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje

internal class Refusjonsservitør private constructor(
    refusjonstidslinje: Beløpstidslinje,
    førsteFraværsdager: Iterable<LocalDate>
) {
    private val sorterteFørsteFraværsdager = førsteFraværsdager.toSortedSet()
    private val sisteBit = sorterteFørsteFraværsdager.last() to refusjonstidslinje.fraOgMed(sorterteFørsteFraværsdager.last())

    private val refusjonsopplysningerPerFørsteFraværsdag = sorterteFørsteFraværsdager.zipWithNext { nåværende, neste ->
        nåværende to refusjonstidslinje.subset(nåværende til neste.forrigeDag)
    }.toMap() + sisteBit

    private val refusjonsrester = refusjonsopplysningerPerFørsteFraværsdag.toMutableMap()

    internal fun servér(førsteFraværsdag: LocalDate, periode: Periode): Beløpstidslinje {
        val beløpstidslinje = refusjonsopplysningerPerFørsteFraværsdag[førsteFraværsdag] ?: return Beløpstidslinje()
        refusjonsrester[førsteFraværsdag] = refusjonsrester.getValue(førsteFraværsdag) - periode
        return beløpstidslinje.strekkFrem(periode.endInclusive).subset(periode)
    }

    internal fun donérRester(hendelse: IAktivitetslogg) {
        val rester = refusjonsrester.values.filter { it.isNotEmpty() }
        if (rester.isEmpty()) return
        hendelse.info("Refusjonsservitøren har rester etter servering: ${rester.map { it.first().dato til it.last().dato }.joinToString() }")
    }

    internal companion object {
        internal fun fra(refusjonstidslinje: Beløpstidslinje, førsteFraværsdager: Collection<LocalDate>): Refusjonsservitør? {
            if (refusjonstidslinje.isEmpty() || førsteFraværsdager.isEmpty()) return null
            return Refusjonsservitør(refusjonstidslinje, førsteFraværsdager)
        }
    }
}
