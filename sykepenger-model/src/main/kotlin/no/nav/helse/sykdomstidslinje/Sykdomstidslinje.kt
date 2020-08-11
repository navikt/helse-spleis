package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.contains
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Dag.Companion.default
import no.nav.helse.sykdomstidslinje.Dag.Companion.override
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.*
import java.util.stream.Collectors.toMap

internal class Sykdomstidslinje private constructor(
    private val dager: SortedMap<LocalDate, Dag>,
    periode: Periode? = null,
    private val låstePerioder: MutableList<Periode> = mutableListOf()
) : Iterable<Dag> {

    private val periode: Periode?

    init {
        this.periode = periode ?: periode(dager)
    }

    internal constructor(dager: Map<LocalDate, Dag> = emptyMap()) : this(
        dager.toSortedMap()
    )

    private constructor(dager: List<Pair<LocalDate, Dag>>) : this(
        dager.fold(mutableMapOf<LocalDate, Dag>()) { acc, (date, dag) ->
            acc.apply { put(date, dag) }
        }
    )

    internal fun length() = count() // Added to limit number of changes when removing old sykdomstidlinje
    internal fun periode() = periode
    internal fun førsteDag() = periode!!.start
    internal fun sisteDag() = periode!!.endInclusive

    internal fun overlapperMed(other: Sykdomstidslinje) =
        when {
            this.count() == 0 && other.count() == 0 -> true
            this.count() == 0 || other.count() == 0 -> false
            else -> this.overlapp(other)
        }

    private fun overlapp(other: Sykdomstidslinje): Boolean {
        requireNotNull(periode, { "Kan ikke undersøke overlapping med tom sykdomstidslinje" })
        requireNotNull(other.periode, { "Kan ikke undersøke overlapping med tom sykdomstidslinje" })

        return periode.overlapperMed(other.periode)
    }

    internal fun merge(other: Sykdomstidslinje, beste: BesteStrategy = default): Sykdomstidslinje {
        val dager = mutableMapOf<LocalDate, Dag>()
        this.dager.toMap(dager)
        other.dager.filter { it.key !in låstePerioder }.forEach { (dato, dag) -> dager.merge(dato, dag, beste) }
        return Sykdomstidslinje(
            dager.toSortedMap(),
            this.periode?.merge(other.periode) ?: other.periode,
            this.låstePerioder.toMutableList()
        )
    }

    private class ProblemDagVisitor(internal val problemmeldinger: MutableList<String>) : SykdomstidslinjeVisitor {
        override fun visitDag(
            dag: ProblemDag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) {
            problemmeldinger.add(melding)
        }
    }

    internal fun valider() = dager.values.filterIsInstance<ProblemDag>().none()

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val problemmeldinger = mutableListOf<String>()
        val visitor = ProblemDagVisitor(problemmeldinger)
        dager.values.filter { it::class == ProblemDag::class }
            .forEach { it.accept(visitor) }

        return problemmeldinger
            .distinct()
            .onEach {
                aktivitetslogg.error(
                    "Sykdomstidslinjen inneholder ustøttet dag. Problem oppstått fordi: %s",
                    it
                )
            }
            .isEmpty()
    }

    internal operator fun plus(other: Sykdomstidslinje) = this.merge(other)
    internal operator fun get(dato: LocalDate): Dag = dager[dato] ?: UkjentDag(dato, INGEN)
    internal fun subset(periode: Periode) =
        Sykdomstidslinje(dager.filter { it.key in periode }.toSortedMap(), periode)

    /**
     * Without padding of days
     */
    internal fun kuttFremTilOgMed(kuttDatoInclusive: LocalDate) =
        Sykdomstidslinje(dager.headMap(kuttDatoInclusive.plusDays(1)))

    internal fun kuttFraOgMed(kuttDatoInclusive: LocalDate) =
        Sykdomstidslinje(dager.tailMap(kuttDatoInclusive))

    internal fun trim(periode: Periode) =
        Sykdomstidslinje(dager.filterNot { it.key in periode })

    internal fun lås(periode: Periode) = this.also {
        requireNotNull(this.periode)
        require(periode in this.periode)
        låstePerioder.add(periode)
    }

    /**
     * Støtter kun å låse opp de perioder som tidligere har blitt låst
     */
    internal fun låsOpp(periode: Periode) = this.also {
        låstePerioder.removeIf { it == periode } || throw IllegalArgumentException("Kan ikke låse opp periode $periode")
    }

    internal fun førsteFraværsdag(): LocalDate? {
        return førsteSykedagDagEtterSisteIkkeSykedag() ?: førsteSykedag()
    }

    /**
     * Første fraværsdag i siste sammenhengende sykefravær i perioden
     */
    private fun førsteSykedagDagEtterSisteIkkeSykedag() =
        fjernDagerEtterSisteSykedag().let { tidslinje ->
            tidslinje.periode?.lastOrNull { this[it] is Arbeidsdag || this[it] is FriskHelgedag || this[it] is UkjentDag }
                ?.let { ikkeSykedag ->
                    tidslinje.dager.entries.firstOrNull {
                        it.key.isAfter(ikkeSykedag) && erEnSykedag(it.value)
                    }?.key
                }
        }

    internal fun førsteSykedagEtter(dato:LocalDate) = dager.entries.firstOrNull { it.key >= dato && erEnSykedag(it.value) }?.key

    private fun førsteSykedag() = dager.entries.firstOrNull { erEnSykedag(it.value) }?.key

    private fun fjernDagerEtterSisteSykedag(): Sykdomstidslinje = periode
        ?.findLast { erEnSykedag(this[it]) }
        ?.let { this.subset(Periode(dager.firstKey(), it)) } ?: Sykdomstidslinje()


    private fun erEnSykedag(it: Dag) =
        it is Sykedag || it is SykHelgedag || it is Arbeidsgiverdag || it is ArbeidsgiverHelgedag || it is ForeldetSykedag

    internal fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitSykdomstidslinje(this, låstePerioder)
        periode?.forEach { this[it].accept(visitor) }
        visitor.postVisitSykdomstidslinje(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Sykdomstidslinje) return false
        return dager == other.dager
            && låstePerioder == other.låstePerioder
    }

    override fun toString() = toShortString()

    internal fun toShortString(): String {
        return periode?.joinToString(separator = "") {
            (if (it.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
                when (this[it]::class) {
                    Sykedag::class -> "S"
                    Arbeidsdag::class -> "A"
                    UkjentDag::class -> "?"
                    ProblemDag::class -> "X"
                    SykHelgedag::class -> "H"
                    Arbeidsgiverdag::class -> "U"
                    ArbeidsgiverHelgedag::class -> "G"
                    Feriedag::class -> "F"
                    FriskHelgedag::class -> "R"
                    ForeldetSykedag::class -> "K"
                    else -> "*"
                }
        } ?: "Tom tidslinje"
    }

    internal companion object {

        private fun periode(dager: SortedMap<LocalDate, Dag>) =
            if (dager.size > 0) Periode(dager.firstKey(), dager.lastKey()) else null

        internal fun arbeidsdager(periode: Periode?, kilde: Hendelseskilde) =
            Sykdomstidslinje(
                periode?.map { it to if (it.erHelg()) FriskHelgedag(it, kilde) else Arbeidsdag(it, kilde) }
                    ?: emptyList<Pair<LocalDate, Dag>>()
            )

        internal fun arbeidsdager(førsteDato: LocalDate, sisteDato: LocalDate, kilde: Hendelseskilde) =
            arbeidsdager(Periode(førsteDato, sisteDato), kilde)

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, Dag>(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad.prosent).let { økonomi ->
                                    if (it.erHelg()) SykHelgedag(it, økonomi, kilde) else Sykedag(it, økonomi, kilde)
                                }

                            }
                        )
                    ))

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            avskjæringsdato: LocalDate,
            grad: Number,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, Dag>(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad.prosent).let { økonomi ->
                                    if (it.erHelg()) SykHelgedag(it, økonomi, kilde) else sykedag(
                                        it,
                                        avskjæringsdato,
                                        økonomi,
                                        kilde
                                    )
                                }
                            })
                    )
            )

        private fun sykedag(
            dato: LocalDate,
            avskjæringsdato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = if (dato < avskjæringsdato) ForeldetSykedag(dato, økonomi, kilde) else Sykedag(dato, økonomi, kilde)

        internal fun foreldetSykedag(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, Dag>(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad.prosent).let { økonomi ->
                                    if (it.erHelg()) SykHelgedag(it, økonomi, kilde) else ForeldetSykedag(
                                        it,
                                        økonomi,
                                        kilde
                                    )
                                }
                            })
                    )
            )

        internal fun arbeidsgiverdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, Dag>(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad.prosent).let { økonomi ->
                                    if (it.erHelg()) ArbeidsgiverHelgedag(it, økonomi, kilde)
                                    else Arbeidsgiverdag(it, økonomi, kilde)
                                }
                            })
                    )
            )

        internal fun feriedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, Dag>({ it }, { Feriedag(it, kilde) }))
            )

        internal fun problemdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, Dag>({ it }, { ProblemDag(it, kilde, melding) }))
            )
    }

    override operator fun iterator() = object : Iterator<Dag> {
        private val periodeIterator = periode?.iterator()

        override fun hasNext() = periodeIterator?.hasNext() ?: false

        override fun next() =
            periodeIterator?.let { this@Sykdomstidslinje[it.next()] }
                ?: throw NoSuchElementException()
    }

    fun starterFør(other: Sykdomstidslinje): Boolean {
        requireNotNull(periode)
        requireNotNull(other.periode)
        return periode.start < other.periode.start
    }

    fun harNyArbeidsgiverperiodeEtter(etter: LocalDate) =
        kuttFraOgMed(etter)
            .kunSykedager()
            .zipWithNext()
            .any { erNyArbeidsgiverperiode(it.first, it.second) }

    private fun erNyArbeidsgiverperiode(fra : LocalDate, til: LocalDate) =
        fra.until(til, DAYS) > 16

    private fun kunSykedager() =
        this.dager
            .filterValues { it.erSykedag() }
            .map { it.key }

    private fun Dag.erSykedag() = when (this) {
        is Sykedag,
        is SykHelgedag,
        is Arbeidsgiverdag,
        is ArbeidsgiverHelgedag -> true
        else -> false
    }
}

internal fun List<Sykdomstidslinje>.merge(beste: BesteStrategy = default): Sykdomstidslinje =
    if (this.isEmpty()) Sykdomstidslinje()
    else reduce { result, tidslinje -> result.merge(tidslinje, beste) }

internal fun List<Sykdomstidslinje>.join() = merge(override)
