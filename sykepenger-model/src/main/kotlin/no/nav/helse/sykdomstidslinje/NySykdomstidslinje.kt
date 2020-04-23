package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.contains
import no.nav.helse.person.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NyDag.Companion.default
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.dag.erHelg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors.toMap

internal class NySykdomstidslinje private constructor(
    private val dager: SortedMap<LocalDate, NyDag>,
    private val periode: Periode? = periode(dager),
    private val låstePerioder: MutableList<Periode> = mutableListOf(),
    private val id: UUID = UUID.randomUUID(),
    private val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Iterable<NyDag> {

    internal constructor(dager: Map<LocalDate, NyDag> = emptyMap()) : this(
        dager.toSortedMap()
    )

    internal fun periode() = periode

    internal fun merge(annen: NySykdomstidslinje, beste: BesteStrategy = default): NySykdomstidslinje {
        val dager = mutableMapOf<LocalDate, NyDag>()
        this.dager.toMap(dager)
        annen.dager.filter { it.key !in låstePerioder }.forEach { (dato, dag) -> dager.merge(dato, dag, beste) }
        return NySykdomstidslinje(
            dager.toSortedMap(),
            this.periode?.merge(annen.periode) ?: annen.periode,
            this.låstePerioder
        )
    }

    operator fun get(dato: LocalDate): NyDag = dager[dato] ?: NyUkjentDag(dato, INGEN)

    internal fun subset(periode: Periode) =
        NySykdomstidslinje(dager.filter { it.key in periode }.toSortedMap(), periode)

    /**
     * Without padding of days
     */
    internal fun kutt(kuttDatoInclusive: LocalDate) =
        when {
            periode == null -> this
            kuttDatoInclusive < periode.start -> NySykdomstidslinje()
            kuttDatoInclusive > periode.endInclusive -> this
            else -> subset(Periode(periode.start, kuttDatoInclusive))
        }

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

    internal fun accept(visitor: NySykdomstidslinjeVisitor) {
        visitor.preVisitNySykdomstidslinje(this, id, tidsstempel)
        periode?.forEach { this[it].accept(visitor) }
        visitor.postVisitNySykdomstidslinje(this, id, tidsstempel)
    }

    internal companion object {

        private fun periode(dager: SortedMap<LocalDate, NyDag>) =
            if (dager.size > 0) Periode(dager.firstKey(), dager.lastKey()) else null

        internal fun arbeidsdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NyFriskHelgedag(it, kilde) else NyArbeidsdag(it, kilde) })
                    )
            )

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number = 100.0,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NySykHelgedag(it, grad, kilde) else NySykedag(it, grad, kilde) })
                    )
            )

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            avskjæringsdato: LocalDate,
            grad: Number = 100.0,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NySykHelgedag(it, grad, kilde) else sykedag(it, avskjæringsdato, grad, kilde) })
                    )
            )

        private fun sykedag(
            dato: LocalDate,
            avskjæringsdato: LocalDate,
            grad: Number,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = if (dato < avskjæringsdato) NyKunArbeidsgiverdag(dato, grad, kilde) else NySykedag(dato, grad, kilde)

        internal fun kunArbeidsgiverSykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number = 100.0,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            { if (it.erHelg()) NyKunArbeidsgiverdag(it, grad, kilde) else NySykedag(it, grad, kilde) })
                    )
            )

        internal fun arbeidsgiverdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Number = 100.0,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, NyDag>(
                            { it },
                            {
                                if (it.erHelg()) NyArbeidsgiverHelgedag(it, grad, kilde)
                                else NyArbeidsgiverdag(it, grad, kilde)
                            })
                    )
            )

        internal fun feriedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, NyDag>({ it }, { NyFeriedag(it, kilde) }))
            )

        internal fun problemdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) =
            NySykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, NyDag>({ it }, { ProblemDag(it, kilde) }))
            )
    }

    override operator fun iterator() = object : Iterator<NyDag> {
        private val periodeIterator = periode?.iterator()

        override fun hasNext() = periodeIterator?.hasNext() ?: false

        override fun next() =
            periodeIterator?.let { this@NySykdomstidslinje[it.next()] }
                ?: throw NoSuchElementException()
    }
}

internal fun List<NySykdomstidslinje>.merge(beste: BesteStrategy = default): NySykdomstidslinje =
    if (this.isEmpty()) NySykdomstidslinje()
    else reduce { result, tidslinje -> result.merge(tidslinje, beste) }
