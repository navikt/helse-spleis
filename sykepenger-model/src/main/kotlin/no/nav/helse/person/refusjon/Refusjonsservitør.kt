package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.SortedMap
import java.util.TreeMap
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje

internal class Refusjonsservitør(
    private val refusjonstidslinjer: SortedMap<LocalDate, Beløpstidslinje> = TreeMap()
) {
    private val refusjonsrester = refusjonstidslinjer.toMutableMap()
    internal operator fun get(dato: LocalDate) = refusjonsrester[dato]

    private fun leggTil(dato: LocalDate, beløpstidslinje: Beløpstidslinje) {
        refusjonstidslinjer[dato] = refusjonstidslinjer.getOrDefault(dato, Beløpstidslinje()) + beløpstidslinje
        refusjonsrester[dato] = refusjonsrester.getOrDefault(dato, Beløpstidslinje()) + beløpstidslinje
    }

    // Serverer refusjonstidslinjer til perioder
    internal fun servér(startdato: LocalDate, periode: Periode): Beløpstidslinje {
        val søkevindu = startdato til periode.endInclusive
        val aktuelle = refusjonstidslinjer.filterKeys { it in søkevindu }
        val refusjonstidslinje = aktuelle.values.fold(Beløpstidslinje()) { sammensatt, ny -> sammensatt + ny }.strekkFrem(periode.endInclusive).subset(periode)
        aktuelle.keys.forEach { dato ->
            if (refusjonsrester.containsKey(dato)) refusjonsrester[dato] = refusjonsrester.getValue(dato) - periode
        }
        return refusjonstidslinje
    }

    // Serverer våre rester til en annen servitør
    internal fun servér(other: Refusjonsservitør, aktivitetslogg: IAktivitetslogg) {
        this.refusjonsrester.filterValues { it.isNotEmpty() }.forEach { (dato, beløpstidslinje) ->
            aktivitetslogg.info("Refusjonsservitøren har rester for ${dato.format(formatter)} etter servering: ${beløpstidslinje.perioderMedBeløp.joinToString()}")
            other.leggTil(dato, beløpstidslinje)
        }
    }

    internal companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        internal fun fra(refusjonstidslinjer: Map<LocalDate, Beløpstidslinje>) =
            Refusjonsservitør(refusjonstidslinjer.filterValues { it.isNotEmpty() }.toSortedMap())

        internal fun fra(refusjonstidslinje: Beløpstidslinje): Refusjonsservitør {
            if (refusjonstidslinje.isEmpty()) return Refusjonsservitør()
            return fra(mapOf(refusjonstidslinje.first().dato to refusjonstidslinje))
        }

        internal fun fra(refusjonstidslinje: Beløpstidslinje, startdatoer: Collection<LocalDate>): Refusjonsservitør {
            if (refusjonstidslinje.isEmpty() || startdatoer.isEmpty()) return Refusjonsservitør()
            val sorterteStartdatoer = startdatoer.toSortedSet()
            val sisteBit = sorterteStartdatoer.last() to refusjonstidslinje.fraOgMed(sorterteStartdatoer.last())
            val refusjonsopplysningerPerStartdato = sorterteStartdatoer.zipWithNext { nåværende, neste ->
                nåværende to refusjonstidslinje.subset(nåværende til neste.forrigeDag)
            }.toMap() + sisteBit
            return fra(refusjonsopplysningerPerStartdato)
        }
    }
}