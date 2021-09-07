package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.reflection.serialiserØkonomi
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek.*
import java.time.LocalDate

internal typealias BesteStrategy = (Dag, Dag) -> Dag

internal sealed class Dag(
    protected val dato: LocalDate,
    protected val kilde: SykdomstidslinjeHendelse.Hendelseskilde
) {
    private fun name() = javaClass.canonicalName.split('.').last()

    internal fun serialiser(kildeMap: Map<String, Any>, melding: String? = null) =
        mutableMapOf<String, Any>().also { map ->
            map["type"] = this.toJsonType()
            map["kilde"] = kildeMap
            leggTilEventueltØkonomiMap(map)
            map.compute("melding") { _, _ -> melding }
        }
    protected open fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) {}

    internal fun toJsonType() = when (this) {
        is Sykedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG
        is UkjentDag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.UKJENT_DAG
        is Arbeidsdag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSDAG
        is Arbeidsgiverdag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG
        is Feriedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FERIEDAG
        is FriskHelgedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FRISK_HELGEDAG
        is ArbeidsgiverHelgedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG
        is ForeldetSykedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FORELDET_SYKEDAG
        is SykHelgedag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG
        is Permisjonsdag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PERMISJONSDAG
        is ProblemDag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PROBLEMDAG
        is AvslåttDag -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.AVSLÅTT_DAG
    }

    companion object {
        internal val default: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (venstre == høyre) venstre else ProblemDag(
                høyre.dato, høyre.kilde,
                "Kan ikke velge mellom ${venstre.name()} fra ${venstre.kilde} og ${høyre.name()} fra ${høyre.kilde}."
            )
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
                is Feriedag,
                is Permisjonsdag,
                is ArbeidsgiverHelgedag -> venstre
                else -> høyre
            }
        }

        internal val noOverlap: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            ProblemDag(
                høyre.dato, høyre.kilde,
                "Støtter ikke overlappende perioder (${venstre.kilde} og ${høyre.kilde})"
            )
        }

        internal val fyll: BesteStrategy = { venstre: Dag, _: Dag ->
            venstre
        }

        internal val replace: BesteStrategy = { venstre: Dag, høyre: Dag ->
            if (høyre is UkjentDag) venstre
            else høyre
        }

        /**
         * Fordi vi ikke har (eller trenger) turnering for arbeidsgiversøknader trenger vi en strategi for å
         * sørge for at arbeidsdager vinner over sykedager
         */
        internal val arbeidsdagerVinner: BesteStrategy = { venstre: Dag, høyre: Dag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (høyre is Arbeidsdag || høyre is FriskHelgedag) høyre
            else venstre
        }
    }

    internal fun kommerFra(hendelse: Melding) = kilde.erAvType(hendelse)

    internal fun problem(other: Dag): Dag =
        ProblemDag(dato, kilde, "Kan ikke velge mellom ${name()} fra $kilde og ${other.name()} fra ${other.kilde}.")

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as Dag)

    protected open fun equals(other: Dag) = this.dato == other.dato// && this.kilde == other.kilde

    override fun hashCode() = dato.hashCode() * 37 + this::class.hashCode()

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
        override fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) = map.putAll(serialiserØkonomi(økonomi))
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
        override fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) = map.putAll(serialiserØkonomi(økonomi))
    }

    internal class Sykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
        override fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) = map.putAll(serialiserØkonomi(økonomi))
    }

    internal class ForeldetSykedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
        override fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) = map.putAll(serialiserØkonomi(økonomi))
    }

    internal class SykHelgedag(
        dato: LocalDate,
        private val økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) = økonomi.accept(visitor, this, dato, kilde)
        override fun leggTilEventueltØkonomiMap(map: MutableMap<String, Any>) = map.putAll(serialiserØkonomi(økonomi))
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
        private val melding: String
    ) : Dag(dato, kilde) {

        override fun accept(visitor: SykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde, melding)

        override fun toString(): String {
            return "Problemdag(${kilde.meldingsreferanseId()})"
        }
    }

    internal class AvslåttDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : Dag(dato, kilde) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitDag(this, dato, kilde)
        }

        override fun toString() = "AvslåttDag(${kilde.meldingsreferanseId()})"
    }
}

private val helgedager = listOf(SATURDAY, SUNDAY)
internal fun LocalDate.erHelg() = this.dayOfWeek in helgedager

internal fun LocalDate.erRettFør(other: LocalDate): Boolean =
    this < other && when (this.dayOfWeek) {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY -> this.plusDays(1) == other
        FRIDAY -> other in this.plusDays(1)..this.plusDays(3)
        SATURDAY -> other in this.plusDays(1)..this.plusDays(2)
        else -> false
    }
