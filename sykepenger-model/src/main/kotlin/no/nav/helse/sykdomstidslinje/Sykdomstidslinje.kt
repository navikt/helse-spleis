package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.contains
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Dag.Companion.default
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.sykdomstidslinje.Dag.Companion.sykmeldingSkrevet
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.Forlengelsestrategi
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors.toMap

internal class Sykdomstidslinje private constructor(
    private val dager: SortedMap<LocalDate, Dag>,
    periode: Periode? = null,
    private val låstePerioder: MutableList<Periode> = mutableListOf()
) : Iterable<Dag> {

    // Støtte for at perioden er lengre enn vi har dager for (Map-et er sparse)
    private val periode: Periode? = periode ?: if (dager.size > 0) Periode(dager.firstKey(), dager.lastKey()) else null

    internal constructor(dager: Map<LocalDate, Dag> = emptyMap()) : this(dager.toSortedMap())

    internal constructor(original: Sykdomstidslinje, spanningPeriode: Periode) :
        this(original.dager, original.periode?.merge(spanningPeriode), original.låstePerioder)

    internal fun periode() = periode
    internal fun førsteDag() = periode!!.start
    internal fun sisteDag() = periode!!.endInclusive

    internal fun overlapperMed(other: Sykdomstidslinje) =
        when {
            this.periode == null && other.periode == null -> true
            this.periode == null || other.periode == null -> false
            else -> this.periode.overlapperMed(other.periode)
        }

    internal fun arbeidsgiverperioder(kuttdato: LocalDate, strategi: Forlengelsestrategi): List<Arbeidsgiverperiode> {
        val builder = ArbeidsgiverperiodeBuilder()
        builder.forlengelsestrategi(strategi)
        fremTilOgMed(kuttdato).accept(builder)
        return builder.result()
    }

    internal fun merge(other: Sykdomstidslinje, beste: BesteStrategy = default): Sykdomstidslinje {
        val nyeDager = dager.toMap(mutableMapOf<LocalDate, Dag>())
        other.dager.filter { it.key !in låstePerioder }.forEach { (dato, dag) -> nyeDager.merge(dato, dag, beste) }
        return Sykdomstidslinje(
            nyeDager.toSortedMap(),
            this.periode?.merge(other.periode) ?: other.periode,
            this.låstePerioder.toMutableList()
        )
    }

    private class ProblemDagVisitor(val problemmeldinger: MutableList<String>) : SykdomstidslinjeVisitor {
        override fun visitDag(
            dag: ProblemDag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) {
            problemmeldinger.add(melding)
        }
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        val problemmeldinger = mutableListOf<String>()
        val visitor = ProblemDagVisitor(problemmeldinger)
        dager.values.filter { it::class == ProblemDag::class }
            .forEach { it.accept(visitor) }

        return problemmeldinger
            .distinct()
            .onEach {
                aktivitetslogg.info("Sykdomstidslinjen inneholder ustøttet dag. Problem oppstått fordi: %s", it)
                aktivitetslogg.error("Sykdomstidslinjen inneholder ustøttet dag.")
            }
            .isEmpty()
    }

    internal operator fun plus(other: Sykdomstidslinje) = this.merge(other)
    internal operator fun get(dato: LocalDate): Dag = dager[dato] ?: UkjentDag(dato, INGEN)
    internal fun subset(periode: Periode) =
        if (this.periode == null || !periode.overlapperMed(this.periode)) Sykdomstidslinje()
        else Sykdomstidslinje(dager.filter { it.key in periode }.toSortedMap(), this.periode?.subset(periode))

    /**
     * Uten å utvide tidslinjen
     */
    internal fun fremTilOgMed(dato: LocalDate) =
        if (periode == null || dato < førsteDag()) Sykdomstidslinje() else subset(førsteDag() til dato)

    private fun fjernDagerFørSisteOppholdsdagFør(dato: LocalDate) = sisteOppholdsdag(før = dato)?.let { sisteOppholdsdag -> fraOgMed(sisteOppholdsdag) } ?: this

    private fun kuttEtterSisteSykedag(): Sykdomstidslinje = periode
        ?.findLast { erEnSykedag(this[it]) }
        ?.let { this.subset(Periode(dager.firstKey(), it)) } ?: Sykdomstidslinje()

    private fun fraOgMed(dato: LocalDate) =
        Sykdomstidslinje(dager.tailMap(dato).toMap())

    internal fun trim(periode: Periode) =
        Sykdomstidslinje(dager.filterNot { it.key in periode })

    internal fun forsøkUtvidelse(periode: Periode) =
        Sykdomstidslinje(dager.toSortedMap(), periode.merge(this.periode), låstePerioder.toMutableList()).takeIf { it.periode != this.periode }

    internal fun erLåst(periode: Periode) = låstePerioder.contains(periode)

    internal fun lås(periode: Periode) = this.also {
        requireNotNull(this.periode)
        require(periode in this.periode) { "$periode er ikke i ${this.periode}" }
        låstePerioder.add(periode)
    }

    internal fun låsOpp(periode: Periode) = this.also {
        låstePerioder.removeIf { it == periode } || throw IllegalArgumentException("Kan ikke låse opp periode $periode")
    }

    override operator fun iterator() = object : Iterator<Dag> {
        private val periodeIterator = periode?.iterator()
        override fun hasNext() = periodeIterator?.hasNext() ?: false
        override fun next() = this@Sykdomstidslinje[periodeIterator?.next() ?: throw NoSuchElementException()]
    }

    internal fun sisteSkjæringstidspunktTidligereEnn(kuttdato: LocalDate) =
        fremTilOgMed(kuttdato)
            .kuttEtterSisteSykedag()
            .sisteSkjæringstidspunkt()

    internal fun sisteSkjæringstidspunkt(): LocalDate? {
        val sisteOppholdsdag = sisteOppholdsdag()
        if (sisteOppholdsdag != null && sisteDag() > sisteOppholdsdag) return fraOgMed(sisteOppholdsdag).finnSkjæringstidspunkt()
        return kuttEtterSisteSykedag().finnSkjæringstidspunkt()
    }

    private fun finnSkjæringstidspunkt(): LocalDate? {
        val sisteOppholdsdag = sisteOppholdsdag() ?: return førsteSykedag()
        return førsteSykedagEtter(sisteOppholdsdag)
    }

    internal fun skjæringstidspunkter(): List<LocalDate> {
        val skjæringstidspunkter = mutableListOf<LocalDate>()
        var kuttdato = periode?.endInclusive ?: return skjæringstidspunkter
        do {
            val skjæringstidspunkt = sisteSkjæringstidspunktTidligereEnn(kuttdato)?.also {
                kuttdato = it.minusDays(1)
                skjæringstidspunkter.add(it)
            }
        } while (skjæringstidspunkt != null)
        return skjæringstidspunkter
    }

    internal fun førsteSykedagEtter(dato: LocalDate) =
        periode?.firstOrNull { it >= dato && erEnSykedag(this[it]) }

    private fun sisteSykedagEtter(dato: LocalDate) =
        periode?.lastOrNull { it >= dato && erEnSykedag(this[it]) }

    internal fun sisteIkkeOppholdsdag(dato: LocalDate): LocalDate? {
        return periode?.lastOrNull { it >= dato && !erOppholdsdag(it) }
    }

    internal fun harDagUtenSøknad(periode: Periode) = subset(periode).any { it.kommerFra(Sykmelding::class) }

    internal fun erRettFør(other: Sykdomstidslinje): Boolean {
        return this.sisteDag().erRettFør(other.førsteDag()) && !this.erSisteDagArbeidsdag() && !other.erFørsteDagArbeidsdag()
    }

    private fun erFørsteDagArbeidsdag() = this.dager.keys.firstOrNull()?.let(::erArbeidsdag) ?: true
    private fun erSisteDagArbeidsdag() = this.dager.keys.lastOrNull()?.let(::erArbeidsdag) ?: true

    internal fun harSykedager() = any { it is Sykedag || it is SykHelgedag || it is ForeldetSykedag }

    internal fun harProblemdager() = any { it is ProblemDag }

    internal fun kunFeriedagerFør(kuttdato: LocalDate) =
        this.dager
            .filter { erFeriedag(it.key) }
            .filter { it.key < kuttdato }
            .takeUnless { it.isEmpty() }
            ?.let { Sykdomstidslinje(it) }

    private fun sisteOppholdsdag() = periode?.lastOrNull { erOppholdsdag(it) }
    private fun sisteOppholdsdag(før: LocalDate) = periode?.filter { erOppholdsdag(it) }?.lastOrNull { it.isBefore(før) }

    private fun erOppholdsdag(dato: LocalDate): Boolean {
        if (erArbeidsdag(dato)) return true
        if (this[dato] !is UkjentDag) return false
        return !erGyldigHelgegap(dato)
    }

    private fun erArbeidsdag(dato: LocalDate) =
        this[dato] is Arbeidsdag || this[dato] is FriskHelgedag

    private fun erFeriedag(dato: LocalDate) =
        this[dato] is Feriedag

    private fun erGyldigHelgegap(dato: LocalDate): Boolean {
        if (!dato.erHelg()) return false
        val fredag = this[dato.minusDays(if (dato.dayOfWeek == DayOfWeek.SATURDAY) 1 else 2)]
        val mandag = this[dato.plusDays(if (dato.dayOfWeek == DayOfWeek.SATURDAY) 2 else 1)]
        return (erEnSykedag(fredag) || fredag is Feriedag) && (erEnSykedag(mandag) || mandag is Feriedag)
    }

    private fun førsteSykedag() = dager.entries.firstOrNull { erEnSykedag(it.value) }?.key

    private fun erEnSykedag(it: Dag) =
        it is Sykedag || it is SykHelgedag || it is Arbeidsgiverdag || it is ArbeidsgiverHelgedag || it is ForeldetSykedag

    internal fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitSykdomstidslinje(this, låstePerioder)
        periode?.forEach { this[it].accept(visitor) }
        visitor.postVisitSykdomstidslinje(this, låstePerioder)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Sykdomstidslinje) return false
        return dager == other.dager && periode == other.periode && låstePerioder == other.låstePerioder
    }

    override fun hashCode() = Objects.hash(dager, periode, låstePerioder)

    override fun toString() = toShortString()

    internal fun toShortString(): String {
        return periode?.joinToString(separator = "") {
            (if (it.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
                when (this[it]) {
                    is Sykedag -> "S"
                    is Arbeidsdag -> "A"
                    is UkjentDag -> "?"
                    is ProblemDag -> "X"
                    is SykHelgedag -> "H"
                    is Arbeidsgiverdag -> "U"
                    is ArbeidsgiverHelgedag -> "G"
                    is Feriedag -> "F"
                    is Permisjonsdag -> "P"
                    is FriskHelgedag -> "R"
                    is ForeldetSykedag -> "K"
                    is AvslåttDag -> "☠"
                }
        }?.trim() ?: "Tom tidslinje"
    }

    internal fun sykmeldingSkrevet(): LocalDateTime = dager.sykmeldingSkrevet()

    internal companion object {

        internal fun sisteRelevanteSkjæringstidspunktForPerioden(periode: Periode, tidslinjer: List<Sykdomstidslinje>) = tidslinjer
            .merge(sammenhengendeSykdom)
            .fjernDagerFørSisteOppholdsdagFør(periode.start)
            .sisteSkjæringstidspunktTidligereEnn(periode.endInclusive)

        internal fun skjæringstidspunkter(tidslinjer: List<Sykdomstidslinje>) = tidslinjer
            .merge(sammenhengendeSykdom)
            .skjæringstidspunkter()

        internal fun arbeidsdager(periode: Periode, kilde: Hendelseskilde) =
            Sykdomstidslinje(periode.associateWith { if (it.erHelg()) FriskHelgedag(it, kilde) else Arbeidsdag(it, kilde) })

        internal fun arbeidsdager(førsteDato: LocalDate, sisteDato: LocalDate, kilde: Hendelseskilde) =
            arbeidsdager(Periode(førsteDato, sisteDato), kilde)

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad).let { økonomi ->
                                    if (it.erHelg()) SykHelgedag(it, økonomi, kilde) else Sykedag(it, økonomi, kilde)
                                }

                            }
                        )
                    ))

        internal fun ukjent(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap<LocalDate, LocalDate, Dag>(
                            { it },
                            { UkjentDag(it, kilde) }
                        )
                    ))

        internal fun sykedager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            avskjæringsdato: LocalDate,
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad).let { økonomi ->
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
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad).let { økonomi ->
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
            grad: Prosentdel,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(
                        toMap(
                            { it },
                            {
                                Økonomi.sykdomsgrad(grad).let { økonomi ->
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

        internal fun permisjonsdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: Hendelseskilde
        ) =
            Sykdomstidslinje(
                førsteDato.datesUntil(sisteDato.plusDays(1))
                    .collect(toMap<LocalDate, LocalDate, Dag>({ it }, { Permisjonsdag(it, kilde) }))
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

        internal fun avslåttdager(
            førsteDato: LocalDate,
            sisteDato: LocalDate,
            kilde: Hendelseskilde
        ) = Sykdomstidslinje(
            førsteDato.datesUntil(sisteDato.plusDays(1))
                .collect(toMap<LocalDate, LocalDate, Dag>({ it }, { AvslåttDag(it, kilde) }))
        )

        internal fun ulikFerieinformasjon(sykdomstidslinje: Sykdomstidslinje, ferieperiode: Periode) =
            ferieperiode.any { sykdomstidslinje[it] !is Feriedag }
    }
}

internal fun List<Sykdomstidslinje>.merge(beste: BesteStrategy = default): Sykdomstidslinje =
    if (this.isEmpty()) Sykdomstidslinje()
    else reduce { result, tidslinje -> result.merge(tidslinje, beste) }
