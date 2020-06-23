package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek.*
import java.time.LocalDate

internal typealias BesteStrategy = (Dag, Dag) -> Dag

internal sealed class Dag(
    protected val dato: LocalDate,
    protected val kilde: SykdomstidslinjeHendelse.Hendelseskilde
) {
    private fun name() = javaClass.canonicalName.split('.').last()

    companion object {
        internal val default: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (venstre == høyre) venstre else ProblemDag(
                høyre.dato, høyre.kilde,
                "Kan ikke velge mellom ${venstre.name()} fra ${venstre.kilde} og ${høyre.name()} fra ${høyre.kilde}."
            )
        }

        internal val noOverlap: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            ProblemDag(
                høyre.dato,
                høyre.kilde,
                "Støtter ikke overlappende perioder (${venstre.kilde} og ${høyre.kilde})"
            )
        }
    }

    internal fun kommerFra(hendelse: Melding) = kilde.erAvType(hendelse)

    internal fun problem(other: Dag): Dag =
        ProblemDag(dato, kilde, "Kan ikke velge mellom ${name()} fra $kilde og ${other.name()} fra ${other.kilde}.")

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as Dag)

    protected open fun equals(other: Dag) = this.dato == other.dato && this.kilde == other.kilde

    override fun hashCode() = dato.hashCode() * 37 + this::class.hashCode()

    internal open fun accept(visitor: SykdomstidslinjeVisitor) {}

    internal class UkjentDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class Arbeidsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class Arbeidsgiverdag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            økonomi.accept(visitor, this, dato, kilde)
    }

    internal class Feriedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class FriskHelgedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class ArbeidsgiverHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            økonomi.accept(visitor, this, dato, kilde)
    }

    internal class Sykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            økonomi.accept(visitor, this, dato, kilde)
    }

    internal class ForeldetSykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            økonomi.accept(visitor, this, dato, kilde)
    }

    internal class SykHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            økonomi.accept(visitor, this, dato, kilde)
    }

    internal class Permisjonsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class Studiedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class Utenlandsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class ProblemDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        private val melding: String
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde, melding)

        override fun toString(): String {
            return "Problemdag(${kilde.meldingsreferanseId()})"
        }
    }

}

private val helgedager = listOf(SATURDAY, SUNDAY)
internal fun LocalDate.erHelg() = this.dayOfWeek in helgedager

internal fun LocalDate.tilstøterKronologisk(other: LocalDate): Boolean =
    this.isBefore(other) && when (this.dayOfWeek) {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY -> this.plusDays(1) == other
        FRIDAY -> other in this.plusDays(1)..this.plusDays(3)
        SATURDAY -> other in this.plusDays(1)..this.plusDays(2)
        else -> false
    }
