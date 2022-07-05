package no.nav.helse.sykdomstidslinje

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi

internal typealias BesteStrategy = (Dag, Dag) -> Dag

internal sealed class Dag(
    protected val dato: LocalDate,
    protected val kilde: SykdomstidslinjeHendelse.Hendelseskilde
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
                is SykHelgedag,
                is Arbeidsgiverdag,
                is ArbeidsgiverHelgedag -> venstre
                is Feriedag,
                is Permisjonsdag -> when (høyre) {
                    is Sykedag,
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

    internal fun problem(other: Dag, melding: String = "Kan ikke velge mellom ${name()} fra $kilde og ${other.name()} fra ${other.kilde}."): Dag =
        ProblemDag(dato, kilde, other.kilde, melding)

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as Dag)

    protected open fun equals(other: Dag) = this.dato == other.dato && this.kilde == other.kilde

    override fun hashCode() = dato.hashCode() * 37 + kilde.hashCode() * 41 + this::class.hashCode()

    override fun toString() = "${this::class.java.simpleName} ($dato) $kilde"

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

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
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

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
    }

    internal class Sykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
    }

    internal class ForeldetSykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
    }

    internal class SykHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
    }

    internal class Permisjonsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class ProblemDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        private val other: SykdomstidslinjeHendelse.Hendelseskilde?,
        private val melding: String
    ) : Dag(dato, kilde) {

        internal constructor(dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) : this(dato, kilde, null, melding)

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde, other, melding)
    }
}

private val helgedager = listOf(SATURDAY, SUNDAY)
internal fun LocalDate.erHelg() = this.dayOfWeek in helgedager

// antall ukedager mellom start og endInclusive, ikke medregnet endInclusive i seg selv
internal fun ClosedRange<LocalDate>.ukedager(): Int {
    val epochStart = start.toEpochDay()
    val epochEnd = endInclusive.toEpochDay()
    if (epochStart >= epochEnd) return 0
    val dagerMellom = (epochEnd - epochStart).toInt()
    val heleHelger = (dagerMellom + start.dayOfWeek.value - 1) / 7 * 2
    val justerFørsteHelg = if (start.dayOfWeek == SUNDAY) 1 else 0
    val justerSisteHelg = if (endInclusive.dayOfWeek == SUNDAY) 1 else 0
    return dagerMellom - heleHelger + justerFørsteHelg - justerSisteHelg
}

internal fun LocalDate.erRettFør(other: LocalDate): Boolean =
    this < other && when (this.dayOfWeek) {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY -> this.plusDays(1) == other
        FRIDAY -> other in this.plusDays(1)..this.plusDays(3)
        SATURDAY -> other in this.plusDays(1)..this.plusDays(2)
        else -> false
    }
