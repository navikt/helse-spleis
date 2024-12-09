package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.helse.dto.RefusjonsservitørDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje

internal class Refusjonsservitør(input: Map<LocalDate, Beløpstidslinje> = emptyMap()) {
    private val refusjonstidslinjer = input.filterValues { it.isNotEmpty() }.toSortedMap()
    private val refusjonsrester = refusjonstidslinjer.toMutableMap()
    internal operator fun get(dato: LocalDate) = refusjonsrester[dato]
    private fun leggTil(dato: LocalDate, beløpstidslinje: Beløpstidslinje) {
        refusjonstidslinjer[dato] = refusjonstidslinjer.getOrDefault(dato, Beløpstidslinje()) + beløpstidslinje
        refusjonsrester[dato] = refusjonsrester.getOrDefault(dato, Beløpstidslinje()) + beløpstidslinje
    }

    // jeg vil bare se hva som kommer etterpå
    internal fun dessertmeny(startdato: LocalDate, periode: Periode): Beløpstidslinje {
        val søkevindu = startdato til periode.endInclusive
        val aktuelle = refusjonstidslinjer.filterKeys { it in søkevindu }.values.map { it.fraOgMed(periode.endInclusive.nesteDag) }.filter { it.isNotEmpty() }
        val sisteKjenteDag = aktuelle.maxOfOrNull { it.last().dato } ?: return Beløpstidslinje()
        return aktuelle.map { it.fyll(sisteKjenteDag) }.fold(Beløpstidslinje(), Beløpstidslinje::plus)
    }

    // Serverer refusjonstidslinjer til perioder
    internal fun servér(startdato: LocalDate, periode: Periode): Beløpstidslinje {
        val søkevindu = startdato til periode.endInclusive
        val aktuelle = refusjonstidslinjer.filterKeys { it in søkevindu }
        val refusjonstidslinje = aktuelle.values.map { it.fyll(periode.endInclusive) }.fold(Beløpstidslinje(), Beløpstidslinje::plus)
        refusjonstidslinjer.keys.forEach { dato ->
            if (!refusjonsrester.containsKey(dato)) return@forEach
            val nyVerdi = refusjonsrester.getValue(dato) - periode
            if (nyVerdi.isEmpty()) refusjonsrester.remove(dato)
            else refusjonsrester[dato] = nyVerdi
        }
        return refusjonstidslinje
    }

    // Serverer våre rester til en annen servitør
    internal fun servér(other: Refusjonsservitør, aktivitetslogg: IAktivitetslogg) {
        this.refusjonsrester.filterValues { it.isNotEmpty() }.forEach { (dato, beløpstidslinje) ->
            val perioderMedBeløp = beløpstidslinje.filterIsInstance<Beløpsdag>().map { it.dato }.grupperSammenhengendePerioder()
            aktivitetslogg.info("Refusjonsservitøren har rester for ${dato.format(formatter)} etter servering: ${perioderMedBeløp.joinToString()}")
            other.leggTil(dato, beløpstidslinje)
        }
    }

    internal fun dto() = RefusjonsservitørDto(refusjonsrester.mapValues { (_, beløpstidslinje) -> beløpstidslinje.dto() })
    internal fun view() = RefusjonsservitørView(refusjonsrester.toMap())

    internal companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        internal fun fra(refusjonstidslinje: Beløpstidslinje): Refusjonsservitør {
            if (refusjonstidslinje.isEmpty()) return Refusjonsservitør()
            return Refusjonsservitør(mapOf(refusjonstidslinje.first().dato to refusjonstidslinje))
        }

        internal fun fra(refusjonstidslinje: Beløpstidslinje, startdatoer: Collection<LocalDate>): Refusjonsservitør {
            if (refusjonstidslinje.isEmpty() || startdatoer.isEmpty()) return Refusjonsservitør()
            val sorterteStartdatoer = startdatoer.toSortedSet()
            val sisteBit = sorterteStartdatoer.last() to refusjonstidslinje.fraOgMed(sorterteStartdatoer.last())
            val refusjonsopplysningerPerStartdato = sorterteStartdatoer.zipWithNext { nåværende, neste ->
                nåværende to refusjonstidslinje.subset(nåværende til neste.forrigeDag)
            }.toMap() + sisteBit
            return Refusjonsservitør(refusjonsopplysningerPerStartdato)
        }

        internal fun gjenopprett(dto: RefusjonsservitørDto) = Refusjonsservitør(dto.refusjonstidslinjer.mapValues { (_, beløpstidslinje) -> Beløpstidslinje.gjenopprett(beløpstidslinje) }.toSortedMap())
    }
}

data class RefusjonsservitørView(val refusjonstidslinjer: Map<LocalDate, Beløpstidslinje>)
