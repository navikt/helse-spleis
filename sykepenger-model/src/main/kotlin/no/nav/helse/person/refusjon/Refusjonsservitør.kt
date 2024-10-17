package no.nav.helse.person.refusjon

import java.time.LocalDate
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje

internal class Refusjonsservitør private constructor(
    refusjonstidslinje: Beløpstidslinje,
    startdatoer: Iterable<LocalDate>
) {
    private val sorterteStartdatoer = startdatoer.toSortedSet()
    private val sisteBit = sorterteStartdatoer.last() to refusjonstidslinje.fraOgMed(sorterteStartdatoer.last())

    private val refusjonsopplysningerPerStartdato = sorterteStartdatoer.zipWithNext { nåværende, neste ->
        nåværende to refusjonstidslinje.subset(nåværende til neste.forrigeDag)
    }.toMap() + sisteBit

    private val refusjonsrester = refusjonsopplysningerPerStartdato.toMutableMap()

    internal fun servér(startdato: LocalDate, periode: Periode): Beløpstidslinje {
        val startdatoInnenforSøkevindu = startdatoInnenforSøkevindu(startdato til periode.endInclusive)
        val refusjonstidslinje = refusjonsopplysningerPerStartdato[startdatoInnenforSøkevindu] ?: return Beløpstidslinje()
        refusjonsrester[startdatoInnenforSøkevindu] = refusjonsrester.getValue(startdatoInnenforSøkevindu) - periode
        return refusjonstidslinje.strekkFrem(periode.endInclusive).subset(periode)
    }

    private fun startdatoInnenforSøkevindu(søkevindu: Periode) = refusjonsopplysningerPerStartdato.keys.filter { it in søkevindu }.also {
        check(it.size < 2) { "Det er flere refusjonsopplysninger lagret innenfor samme søkevindu, det blir en sklitakling fra siden" }
    }.firstOrNull()

    internal fun donérRester(hendelse: IAktivitetslogg) {
        val rester = refusjonsrester.values.filter { it.isNotEmpty() }
        if (rester.isEmpty()) return
        hendelse.info("Refusjonsservitøren har rester etter servering: ${rester.map { it.first().dato til it.last().dato }.joinToString() }")
    }

    internal companion object {
        internal fun fra(refusjonstidslinje: Beløpstidslinje, startdatoer: Collection<LocalDate>): Refusjonsservitør? {
            if (refusjonstidslinje.isEmpty() || startdatoer.isEmpty()) return null
            return Refusjonsservitør(refusjonstidslinje, startdatoer)
        }

        internal fun fra(refusjonstidslinje: Beløpstidslinje): Refusjonsservitør? {
            if (refusjonstidslinje.isEmpty()) return null
            return Refusjonsservitør(refusjonstidslinje, listOf(refusjonstidslinje.first().dato))
        }
    }
}
