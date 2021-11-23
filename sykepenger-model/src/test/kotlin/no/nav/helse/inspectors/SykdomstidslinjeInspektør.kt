package no.nav.helse.inspectors

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import kotlin.reflect.KClass

internal val Sykdomstidslinje.inspektør get() = SykdomstidslinjeInspektør(this)

internal class SykdomstidslinjeInspektør(tidslinje: Sykdomstidslinje) : SykdomstidslinjeVisitor {
    internal val dager = mutableMapOf<LocalDate, Dag>()
    internal val kilder = mutableMapOf<LocalDate, Hendelseskilde>()
    internal val grader = mutableMapOf<LocalDate, Int>()
    internal val problemdagmeldinger = mutableMapOf<LocalDate, String>()
    internal val låstePerioder = mutableListOf<Periode>()
    internal val dagteller = mutableMapOf<KClass<out Dag>, Int>()

    init {
        tidslinje.accept(this)
    }

    internal operator fun get(dato: LocalDate) = dager[dato]
        ?: throw IllegalArgumentException("No dag for $dato")

    internal val size get() = dager.size

    private fun set(dag: Dag, dato: LocalDate, kilde: Hendelseskilde) {
        dager[dato] = dag
        kilder[dato] = kilde
        dagteller.compute(dag::class) { _, value -> 1 + (value ?: 0) }
    }

    private fun set(dag: Dag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        økonomi.medAvrundetData {
            grad, _ -> this.grader[dato] = grad
            set(dag, dato, kilde)
        }
    }

    private fun set(dag: Dag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {
        problemdagmeldinger[dato] = melding
        set(dag, dato, kilde)
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        this.låstePerioder.addAll(låstePerioder)
    }

    override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(
        dag: Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) =
        set(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(
        dag: ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) =
        set(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) =
        set(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) =
        set(dag, dato, økonomi, kilde)

    override fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) =
        set(dag, dato, økonomi, kilde)

    override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
        set(dag, dato, kilde, melding)
}
