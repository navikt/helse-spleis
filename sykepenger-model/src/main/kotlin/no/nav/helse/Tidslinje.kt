package no.nav.helse

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import java.time.LocalDate
import java.util.SortedMap
import no.nav.helse.hendelser.somPeriode

data class Tidslinjedag<T>(val dato: LocalDate, val verdi: T?)

abstract class Tidslinje<T, SELF: Tidslinje<T, SELF>> private constructor(
    private val dager: SortedMap<LocalDate, T>,
): Collection<Tidslinjedag<T>> by dager.somTidslinjedager {

    private val periode = if (dager.isEmpty()) null else dager.firstKey() til dager.lastKey()

    constructor(vararg perioder: Pair<Periode, T>): this(perioder.toList().fraPerioder)

    operator fun get(dato: LocalDate) = dager[dato]

    operator fun plus(nyTidslinje: SELF): SELF {
        val resultat = this.dager.toMutableMap()
        nyTidslinje.dager.forEach { (dato, nyVerdi) ->
            when (val eksisterendeVerdi = this.dager[dato]) {
                null -> resultat[dato] = nyVerdi
                else -> resultat[dato] = pluss(eksisterendeVerdi, nyVerdi)
            }
        }
        return opprett(*resultat.somArray)
    }

    override fun equals(other: Any?): Boolean {
        val andre = other as? Tidslinje<T, SELF> ?: return false
        if (dager.keys != andre.dager.keys) return false
        dager.forEach { (dato, dag) -> if (!erLike(dag,andre[dato]!!)) return false }
        return true
    }

    override fun toString() = gruppér().entries.joinToString { (periode, verdi) -> "$periode: $verdi" }

    protected open fun pluss(eksisterendeVerdi: T, nyVerdi: T): T = nyVerdi
    protected open fun erLike(a: T, b: T): Boolean = a == b
    protected abstract fun opprett(vararg perioder: Pair<Periode, T>): SELF

    internal fun subset(periode: Periode): SELF {
        if (this.periode == null || !this.periode.overlapperMed(periode)) return opprett()
        return opprett(*dager.subMap(periode.start, periode.endInclusive.nesteDag).somArray)
    }
    internal fun fraOgMed(dato: LocalDate) = opprett(*dager.tailMap(dato).somArray)
    internal fun tilOgMed(dato: LocalDate) = opprett(*dager.headMap(dato.nesteDag).somArray)

    internal fun gruppér(): Map<Periode, T> {
        val grupperte = mutableMapOf<Periode, T>()
        var aktiv: Pair<Periode, T>? = null
        dager.forEach { (dato, nyVerdi) ->
            if (aktiv == null) {
                aktiv = dato.somPeriode() to nyVerdi
                return@forEach
            }
            val (aktivPeriode, aktivVerdi ) = aktiv
            if (aktivPeriode.endInclusive.nesteDag == dato && erLike(aktivVerdi, nyVerdi)) {
                aktiv = aktivPeriode.oppdaterTom(dato) to aktivVerdi
            } else {
                grupperte[aktivPeriode] = aktivVerdi
                aktiv = dato.somPeriode() to nyVerdi
            }
        }
        aktiv?.let { (aktivPeriode, aktivVerdi) -> grupperte[aktivPeriode] = aktivVerdi }
        return grupperte.toMap()
    }

    override operator fun iterator(): Iterator<Tidslinjedag<T>> {
        if (periode == null) return emptyList<Tidslinjedag<T>>().iterator()
        return object : Iterator<Tidslinjedag<T>> {
            private val periodeIterator = periode.iterator()
            override fun hasNext() = periodeIterator.hasNext()
            override fun next() = periodeIterator.next().let { Tidslinjedag(it, get(it)) }
        }
    }

    companion object {
        private val <T>Map<LocalDate, T>.somTidslinjedager get() = entries.map { (dato, verdi) -> Tidslinjedag(dato, verdi) }
        private val <T>Map<LocalDate, T>.somArray get() = entries.map { (dato, verdi) -> dato.somPeriode() to verdi }.toTypedArray()

        private val <T>List<Pair<Periode, T>>.fraPerioder: SortedMap<LocalDate, T> get() {
            val dager = mutableMapOf<LocalDate, T>()
            this.forEach { (periode, verdi) ->
                periode.forEach { dato ->
                    require(dager[dato] == null) { "Datoen $dato er oppgitt fler ganger i samme tidslinje." }
                    dager[dato] = verdi
                }
            }
            return dager.toSortedMap()
        }
    }
}
