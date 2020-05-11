package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.økonomi.Grad
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SykdomstidslinjeInspektør(tidslinje: Sykdomstidslinje) : SykdomstidslinjeVisitor {
    internal val dager = mutableMapOf<LocalDate, Dag>()
    internal val kilder = mutableMapOf<LocalDate, Hendelseskilde>()
    internal val grader = mutableMapOf<LocalDate, Grad>()
    internal val problemdagmeldinger = mutableMapOf<LocalDate, String>()
    internal lateinit var id: UUID
    internal lateinit var tidsstempel: LocalDateTime

    init {
        tidslinje.accept(this)
    }

    internal operator fun get(dato: LocalDate) = dager[dato]
        ?: throw IllegalArgumentException("No dag for ${dato}")

    internal val size get() = dager.size

    private fun set(dag: Dag, dato: LocalDate, kilde: Hendelseskilde) {
        dager[dato] = dag
        kilder[dato] = kilde
    }

    private fun set(dag: Dag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {
        grader[dato] = grad
        set(dag, dato, kilde)
    }

    private fun set(dag: Dag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {
        problemdagmeldinger[dato] = melding
        set(dag, dato, kilde)
    }

    override fun preVisitSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        låstePerioder: List<Periode>,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {
        this.id = id
        this.tidsstempel = tidsstempel
    }

    override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: Arbeidsgiverdag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: ArbeidsgiverHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: Sykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: ForeldetSykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: SykHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
        set(dag, dato, kilde, melding)
}
