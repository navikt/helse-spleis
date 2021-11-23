package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.contains
import no.nav.helse.hendelser.til
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.serde.DateRanges
import no.nav.helse.serde.PersonData
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde.Companion.INGEN
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class Utbetalingstidslinje private constructor(
    private val utbetalingsdager: MutableList<Utbetalingsdag>
) : MutableList<Utbetalingsdag> by utbetalingsdager {

    private val førsteDato get() = utbetalingsdager.first().dato
    private val sisteDato get() = utbetalingsdager.last().dato

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal fun periode(tidslinjer: List<Utbetalingstidslinje>) = tidslinjer
            .filter { it.utbetalingsdager.isNotEmpty() }
            .map { it.periode() }
            .reduce(Periode::merge)

        internal fun konverter(utbetalingstidslinje: Utbetalingstidslinje) =
            utbetalingstidslinje
                .mapNotNull {
                    when {
                        !it.dato.erHelg() && it.erSykedag() -> Dag.Sykedag(it.dato, it.økonomi.medGrad(), INGEN)
                        !it.dato.erHelg() && it is Fridag -> Dag.Feriedag(it.dato, INGEN)
                        it.dato.erHelg() && it.erSykedag() -> Dag.SykHelgedag(it.dato, it.økonomi.medGrad(), INGEN)
                        it is Arbeidsdag -> Dag.Arbeidsdag(it.dato, INGEN)
                        it is ForeldetDag -> Dag.ForeldetSykedag(it.dato, it.økonomi.medGrad(), INGEN)
                        else -> null
                    }?.let { sykedag -> it.dato to sykedag }
                }
                .toMap()
                .let(::Sykdomstidslinje)

        private fun Økonomi.medGrad() = Økonomi.sykdomsgrad(medData { grad, _, _, _, _, _, _, _, _ -> grad }.prosent)

        private fun Utbetalingsdag.erSykedag() =
            this is NavDag || this is NavHelgDag || this is ArbeidsgiverperiodeDag || this is AvvistDag

        fun avvis(tidslinjer: List<Utbetalingstidslinje>, avvistPeriode: Periode, periode: Periode, begrunnelser: List<Begrunnelse>) =
            avvis(tidslinjer, listOf(avvistPeriode), periode, begrunnelser)

        fun avvis(tidslinjer: List<Utbetalingstidslinje>, begrunnelserForSkjæringstidspunkt: Map<LocalDate, List<Begrunnelse>>) {
            tidslinjer.forEach { it.avvis(begrunnelserForSkjæringstidspunkt) }
        }

        fun avvis(tidslinjer: List<Utbetalingstidslinje>, avvistePerioder: List<Periode>, periode: Periode, begrunnelser: List<Begrunnelse>): Boolean {
            tidslinjer.apply {
                forEach { it.avvis(avvistePerioder, begrunnelser) }
                return flatMap { tidslinje -> tidslinje.subset(periode) }.any { it is AvvistDag}
            }
        }

        fun avvis(tidslinjer: List<Utbetalingstidslinje>, avvistePerioder: List<Periode>, periode: Periode, finnBegrunnelse: (LocalDate) -> Begrunnelse): Boolean {
            tidslinjer.apply {
                forEach { it.avvis(avvistePerioder, finnBegrunnelse) }
                return flatMap { tidslinje -> tidslinje.subset(periode) }.any { it is AvvistDag}
            }
        }
    }

    internal fun er6GBegrenset(): Boolean {
        return utbetalingsdager.any {
            it.økonomi.er6GBegrenset()
        }
    }

    internal fun accept(visitor: UtbetalingsdagVisitor) {
        visitor.preVisit(this)
        utbetalingsdager.forEach { it.accept(visitor) }
        visitor.postVisit(this)
    }

    private fun avvis(avvisteDatoer: List<Periode>, begrunnelser: List<Begrunnelse>) {
        utbetalingsdager.replaceAll {
            if (it.dato !in avvisteDatoer) it
            else it.avvis(begrunnelser) ?: it
        }
    }

    private fun avvis(avvisteDatoer: List<Periode>, finnBegrunnelse: (LocalDate) -> Begrunnelse) {
        utbetalingsdager.replaceAll {
            if (it.dato !in avvisteDatoer) it
            else it.avvis(finnBegrunnelse) ?: it
        }
    }

    private fun avvis(begrunnelserForSkjæringstidspunkt: Map<LocalDate, List<Begrunnelse>>) {
        utbetalingsdager.forEachIndexed { index, utbetalingsdag ->
            utbetalingsdag.økonomi.medData { _, _, _, skjæringstidspunkt, _, _, _, _, _ ->
                begrunnelserForSkjæringstidspunkt[skjæringstidspunkt]?.also { begrunnelser ->
                    utbetalingsdager[index] = utbetalingsdag.avvis(begrunnelser) ?: utbetalingsdag
                }
            }
        }
    }

    internal fun addArbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(ArbeidsgiverperiodeDag(dato, økonomi))
    }

    internal fun addNAVdag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(NavDag(dato, økonomi))
    }

    internal fun addArbeidsdag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(Arbeidsdag(dato, økonomi))
    }

    internal fun addFridag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(Fridag(dato, økonomi))
    }

    internal fun addHelg(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(NavHelgDag(dato, økonomi))
    }

    private fun addUkjentDag(dato: LocalDate) =
        Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dato).let { økonomi ->
            if (dato.erHelg()) addFridag(dato, økonomi) else addUkjentDag(dato, økonomi)
        }

    private fun addUkjentDag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(UkjentDag(dato, økonomi))
    }

    internal fun addAvvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelser: List<Begrunnelse>) {
        utbetalingsdager.add(AvvistDag(dato, økonomi, begrunnelser))
    }

    internal fun addForeldetDag(dato: LocalDate, økonomi: Økonomi) {
        utbetalingsdager.add(ForeldetDag(dato, økonomi))
    }

    internal operator fun plus(other: Utbetalingstidslinje): Utbetalingstidslinje {
        return this.plus(other) { venstre, høyre -> maxOf(venstre, høyre) }
    }

    internal fun reverse(): Utbetalingstidslinje {
        return Utbetalingstidslinje(utbetalingsdager.asReversed())
    }

    internal fun harBetalt(dato: LocalDate): Boolean {
        return this[dato].erSykedag()
    }

    internal fun harBetalt(periode: Periode) =
        periode.any { harBetalt(it) }

    internal fun kunArbeidsgiverdager() =
        this.utbetalingsdager.all {
            it is ArbeidsgiverperiodeDag ||
                it is Arbeidsdag ||
                it is NavHelgDag ||
                it is Fridag
        }

    internal fun kunFridager() =
        this.utbetalingsdager.all {
            it is NavHelgDag ||
                it is Fridag
        }

    internal fun harUtbetalinger() = sykepengeperiode() != null

    internal fun harBrukerutbetalinger() = any { it.økonomi.harPersonbeløp() }

    internal fun plus(
        other: Utbetalingstidslinje,
        plusstrategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ): Utbetalingstidslinje {
        if (other.isEmpty()) return this
        if (this.isEmpty()) return other
        val tidligsteDato = this.tidligsteDato(other)
        val sisteDato = this.sisteDato(other)
        return this.utvide(tidligsteDato, sisteDato).binde(other.utvide(tidligsteDato, sisteDato), plusstrategy)
    }

    private fun binde(
        other: Utbetalingstidslinje,
        strategy: (Utbetalingsdag, Utbetalingsdag) -> Utbetalingsdag
    ) = Utbetalingstidslinje(
        this.utbetalingsdager.zip(other.utbetalingsdager, strategy).toMutableList()
    ).trim()

    private fun trim(): Utbetalingstidslinje {
        val første = firstOrNull { it !is UkjentDag }?.dato ?: return Utbetalingstidslinje()
        return subset(første til last { it !is UkjentDag }.dato)
    }

    internal fun trimLedendeFridager() = Utbetalingstidslinje(dropWhile { it is Fridag }.toMutableList())

    private fun utvide(tidligsteDato: LocalDate, sisteDato: LocalDate) =
        Utbetalingstidslinje().apply {
            val original = this@Utbetalingstidslinje
            tidligsteDato.datesUntil(original.førsteDato)
                .forEach { this.addUkjentDag(it) }
            this.utbetalingsdager.addAll(original.utbetalingsdager)
            original.sisteDato.plusDays(1).datesUntil(sisteDato.plusDays(1))
                .forEach { this.addUkjentDag(it) }
        }

    private fun tidligsteDato(other: Utbetalingstidslinje) =
        minOf(this.førsteDato, other.førsteDato)

    private fun sisteDato(other: Utbetalingstidslinje) =
        maxOf(this.sisteDato, other.sisteDato)

    internal fun sisteUkedag() = utbetalingsdager.last { it !is NavHelgDag }.dato

    internal fun periode() = Periode(førsteDato, sisteDato)

    internal fun sykepengeperiode(): Periode? {
        val første = utbetalingsdager.firstOrNull { it is NavDag }?.dato ?: return null
        val siste = utbetalingsdager.last { it is NavDag }.dato
        return første til siste
    }

    internal fun subset(periode: Periode): Utbetalingstidslinje {
        if (isEmpty()) return Utbetalingstidslinje()
        if (periode == periode()) return this
        return Utbetalingstidslinje(utbetalingsdager.filter { it.dato in periode }.toMutableList())
    }

    internal fun kutt(sisteDato: LocalDate) = subset(LocalDate.MIN til sisteDato)

    internal operator fun get(dato: LocalDate) =
        if (isEmpty() || dato !in periode()) UkjentDag(
            dato,
            Økonomi.ikkeBetalt().inntekt(Inntekt.INGEN, skjæringstidspunkt = dato)
        )
        else utbetalingsdager.first { it.dato == dato }

    override fun toString(): String {
        return utbetalingsdager.joinToString(separator = "") {
            (if (it.dato.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
                when (it::class) {
                    NavDag::class -> "N"
                    NavHelgDag::class -> "H"
                    Arbeidsdag::class -> "A"
                    ArbeidsgiverperiodeDag::class -> "P"
                    Fridag::class -> "F"
                    AvvistDag::class -> "X"
                    UkjentDag::class -> "U"
                    ForeldetDag::class -> "O"
                    else -> "?"
                }
        }
    }

    fun toMap() = mutableMapOf<String, Any?>("dager" to DateRanges().apply { forEach { plus(it.dato, it.toMap()) } }.toList())

    fun tilUtbetalingsdager(fridagplog: (Utbetalingsdag) -> String): List<PersonObserver.Utbetalingsdag> {
        return map { utbetalingsdag ->
            PersonObserver.Utbetalingsdag(
                utbetalingsdag.dato,
                mapDagtype(utbetalingsdag, fridagplog),
                mapBegrunnelser(utbetalingsdag)
            )
        }
    }

    private fun mapBegrunnelser(utbetalingsdag: Utbetalingsdag) =
        if (utbetalingsdag is AvvistDag)
            utbetalingsdag.begrunnelser.map { begrunnelse -> begrunnelse.javaClass.simpleName }
        else null

    private fun mapDagtype(dag: Utbetalingsdag, fridagplog: (Utbetalingsdag) -> String) = when (dag) {
        is Arbeidsdag -> PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag.name
        is ArbeidsgiverperiodeDag -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag.name
        is NavDag -> PersonData.UtbetalingstidslinjeData.TypeData.NavDag.name
        is NavHelgDag -> PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag.name
        is UkjentDag -> PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag.name
        is AvvistDag -> PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag.name
        is ForeldetDag -> PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag.name
        is Fridag -> fridagplog(dag)
    }

    internal sealed class Utbetalingsdag(
        internal val dato: LocalDate,
        internal val økonomi: Økonomi
    ) :
        Comparable<Utbetalingsdag> {

        internal abstract val prioritet: Int

        override fun compareTo(other: Utbetalingsdag): Int {
            return this.prioritet.compareTo(other.prioritet)
        }

        override fun toString() = "${this.javaClass.simpleName} ($dato) ${økonomi.medData { grad, _ -> grad }} %"

        internal fun avvis(begrunnelser: List<Begrunnelse>) = begrunnelser
            .filter { it.skalAvvises(this) }
            .takeIf(List<*>::isNotEmpty)
            ?.let(::avvisDag)

        internal fun avvis(finnBegrunnelse: (LocalDate) -> Begrunnelse) =
            avvis(listOf(finnBegrunnelse(this.dato)))

        protected open fun avvisDag(begrunnelser: List<Begrunnelse>) = AvvistDag(dato, økonomi, begrunnelser)

        internal abstract fun accept(visitor: UtbetalingsdagVisitor)

        internal class ArbeidsgiverperiodeDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 30
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData =
                PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
        }

        abstract fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData
        internal open fun toMap() = mutableMapOf<String, Any?>(
            "type" to serialiseringstype()
        ).apply {
            putAll(økonomi.toMap())
        }

        internal class NavDag(
            dato: LocalDate,
            økonomi: Økonomi
        ) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 50

            companion object {
                internal val reflectedArbeidsgiverBeløp = { økonomi: Økonomi ->
                    økonomi.medAvrundetData { _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp!! }
                }

                internal val reflectedPersonBeløp = { økonomi: Økonomi ->
                    økonomi.medAvrundetData { _, _, _, _, _, personBeløp, _ -> personBeløp!! }
                }
            }

            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.NavDag
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
        }

        internal class NavHelgDag(dato: LocalDate, økonomi: Økonomi) :
            Utbetalingsdag(dato, økonomi) {
            override val prioritet = 40
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag
        }

        internal class Arbeidsdag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 20
            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag
        }

        internal class Fridag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 10
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.Fridag
        }

        internal class AvvistDag(
            dato: LocalDate,
            økonomi: Økonomi,
            internal val begrunnelser: List<Begrunnelse>
        ) : Utbetalingsdag(dato, økonomi) {
            init {
                økonomi.lås()
            }

            override val prioritet = 60
            override fun avvisDag(begrunnelser: List<Begrunnelse>) =
                AvvistDag(dato, økonomi, this.begrunnelser + begrunnelser)

            override fun accept(visitor: UtbetalingsdagVisitor) = økonomi.accept(visitor, this, dato)
            internal fun navDag(): Utbetalingsdag =
                if (EgenmeldingUtenforArbeidsgiverperiode in begrunnelser) this else NavDag(dato, økonomi.låsOpp())

            override internal fun toMap() = mutableMapOf<String, Any?>(
                "type" to PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag,
                "begrunnelser" to begrunnelser.map { PersonData.UtbetalingstidslinjeData.BegrunnelseData.fraBegrunnelse(it).name }
            ).apply {
                putAll(økonomi.toMap())
            }

            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag
        }

        internal class ForeldetDag(dato: LocalDate, økonomi: Økonomi) :
            Utbetalingsdag(dato, økonomi) {
            override val prioritet = 40 // Mellom ArbeidsgiverperiodeDag og NavDag
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag
        }

        internal class UkjentDag(dato: LocalDate, økonomi: Økonomi) : Utbetalingsdag(dato, økonomi) {
            override val prioritet = 0
            override fun accept(visitor: UtbetalingsdagVisitor) = visitor.visit(this, dato, økonomi)
            override fun serialiseringstype(): PersonData.UtbetalingstidslinjeData.TypeData = PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag
        }
    }
}

internal sealed class Begrunnelse(private val dagtyperSomAvvises: List<KClass<out Utbetalingsdag>> = listOf(NavDag::class)) {

    internal fun skalAvvises(utbetalingsdag: Utbetalingsdag) = utbetalingsdag is AvvistDag || utbetalingsdag::class in dagtyperSomAvvises

    object SykepengedagerOppbrukt : Begrunnelse()
    object SykepengedagerOppbruktOver67 : Begrunnelse()
    object MinimumInntekt : Begrunnelse()
    object MinimumInntektOver67 : Begrunnelse()
    object EgenmeldingUtenforArbeidsgiverperiode : Begrunnelse()
    object MinimumSykdomsgrad : Begrunnelse()
    object EtterDødsdato : Begrunnelse(listOf(NavDag::class, NavHelgDag::class))
    object Over70 : Begrunnelse(listOf(NavDag::class, NavHelgDag::class))
    object ManglerOpptjening : Begrunnelse()
    object ManglerMedlemskap : Begrunnelse()

}
