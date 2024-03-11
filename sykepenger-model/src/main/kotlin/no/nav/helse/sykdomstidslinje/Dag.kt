package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.memento.DagMemento
import no.nav.helse.memento.DagMemento.AndreYtelserMemento.YtelseMemento
import no.nav.helse.memento.HendelseskildeMemento
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi

internal typealias BesteStrategy = (Dag, Dag) -> Dag

internal sealed class Dag(
    protected val dato: LocalDate,
    protected val kilde: Hendelseskilde
) {
    private fun name() = javaClass.canonicalName.split('.').last()

    companion object {
        internal val default: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (venstre == høyre) venstre else høyre.problem(venstre)
        }

        internal val override: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            høyre
        }

        internal val sammenhengendeSykdom: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            when (venstre) {
                is Sykedag,
                is SykedagNav,
                is SykHelgedag,
                is Arbeidsgiverdag,
                is ArbeidsgiverHelgedag -> venstre
                is Feriedag,
                is Permisjonsdag -> when (høyre) {
                    is Sykedag,
                    is SykedagNav,
                    is SykHelgedag,
                    is Arbeidsgiverdag,
                    is ArbeidsgiverHelgedag -> høyre
                    else -> venstre
                }
                else -> høyre
            }
        }

        internal val noOverlap: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            høyre.problem(venstre, "Støtter ikke overlappende perioder (${venstre.kilde} og ${høyre.kilde})")
        }

        internal val replace: BesteStrategy = { venstre: Dag, høyre: Dag ->
            if (høyre is UkjentDag) venstre
            else høyre
        }
    }

    internal fun kommerFra(hendelse: Melding) = kilde.erAvType(hendelse)
    internal fun kommerFra(hendelse: String) = kilde.erAvType(hendelse)

    internal fun erHelg() = dato.erHelg()

    internal fun problem(other: Dag, melding: String = "Kan ikke velge mellom ${name()} fra $kilde og ${other.name()} fra ${other.kilde}."): Dag =
        ProblemDag(dato, kilde, other.kilde, melding)

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as Dag)

    protected open fun equals(other: Dag) = this.dato == other.dato && this.kilde == other.kilde

    override fun hashCode() = dato.hashCode() * 37 + kilde.hashCode() * 41 + this::class.hashCode()

    override fun toString() = "${this::class.java.simpleName} ($dato) $kilde"

    internal open fun accept(visitor: SykdomstidslinjeDagVisitor) {}

    internal class UkjentDag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)

        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.UkjentDagMemento(dato, kilde)
    }

    internal class Arbeidsdag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ArbeidsdagMemento(dato, kilde)
    }

    internal class Arbeidsgiverdag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ArbeidsgiverdagMemento(dato, kilde, økonomi.memento())
    }

    internal class Feriedag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.FeriedagMemento(dato, kilde)
    }

    internal class ArbeidIkkeGjenopptattDag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {
        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ArbeidIkkeGjenopptattDagMemento(dato, kilde)
    }

    internal class FriskHelgedag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)

        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.FriskHelgedagMemento(dato, kilde)
    }

    internal class ArbeidsgiverHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ArbeidsgiverHelgedagMemento(dato, kilde, økonomi.memento())
    }

    internal class Sykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.SykedagMemento(dato, kilde, økonomi.memento())
    }

    internal class ForeldetSykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ForeldetSykedagMemento(dato, kilde, økonomi.memento())
    }

    internal class SykHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.SykHelgedagMemento(dato, kilde, økonomi.memento())
    }

    internal class SykedagNav(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {
        override fun accept(visitor: SykdomstidslinjeDagVisitor) = visitor.visitDag(this, dato, økonomi, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.SykedagNavMemento(dato, kilde, økonomi.memento())
    }

    internal class Permisjonsdag(
        dato: LocalDate,
        kilde: Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.PermisjonsdagMemento(dato, kilde)
    }

    internal class ProblemDag(
        dato: LocalDate,
        kilde: Hendelseskilde,
        private val other: Hendelseskilde,
        private val melding: String
    ) : Dag(dato, kilde) {

        internal constructor(dato: LocalDate, kilde: Hendelseskilde, melding: String) : this(dato, kilde, kilde, melding)

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde, other, melding)
        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) = DagMemento.ProblemDagMemento(dato, kilde, other.memento(), melding)
    }

    internal class AndreYtelser(
        dato: LocalDate,
        kilde: Hendelseskilde,
        private val ytelse: AnnenYtelse,
    ) : Dag(dato, kilde) {
        enum class AnnenYtelse {
            Foreldrepenger, AAP, Omsorgspenger, Pleiepenger, Svangerskapspenger, Opplæringspenger, Dagpenger;

            fun memento() = when (this) {
                Foreldrepenger -> YtelseMemento.Foreldrepenger
                AAP -> YtelseMemento.AAP
                Omsorgspenger -> YtelseMemento.Omsorgspenger
                Pleiepenger -> YtelseMemento.Pleiepenger
                Svangerskapspenger -> YtelseMemento.Svangerskapspenger
                Opplæringspenger -> YtelseMemento.Opplæringspenger
                Dagpenger -> YtelseMemento.Dagpenger
            }
        }

        override fun accept(visitor: SykdomstidslinjeDagVisitor) =
            visitor.visitDag(this, dato, kilde, ytelse)

        override fun memento(dato: LocalDate, kilde: HendelseskildeMemento) =
            DagMemento.AndreYtelserMemento(dato, kilde, ytelse.memento())
    }

    internal fun memento() = memento(dato, kilde.memento())
    protected abstract fun memento(dato: LocalDate, kilde: HendelseskildeMemento): DagMemento
}

internal interface SykdomstidslinjeDagVisitor {
    fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {}

    fun visitDag(dag: Dag.ArbeidIkkeGjenopptattDag, dato: LocalDate, kilde: Hendelseskilde) {}

    fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Dag.SykedagNav,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {}

    fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: Hendelseskilde,
        other: Hendelseskilde?,
        melding: String
    ) {
    }
    fun visitDag(
        dag: Dag.AndreYtelser,
        dato: LocalDate,
        kilde: Hendelseskilde,
        ytelse: Dag.AndreYtelser.AnnenYtelse
    ) {
    }
}