package no.nav.helse.testhelpers

import no.nav.helse.person.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Grad
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SykdomstidslinjeInspektør(tidslinje: NySykdomstidslinje) : NySykdomstidslinjeVisitor {
    init {
        tidslinje.accept(this)
    }
    internal val dager = mutableMapOf<LocalDate, NyDag>()
    internal val kilder = mutableMapOf<LocalDate, Hendelseskilde>()
    internal val grader = mutableMapOf<LocalDate, Grad>()
    internal lateinit var id: UUID
    internal lateinit var tidsstempel: LocalDateTime

    internal operator fun get(dato: LocalDate) = dager[dato]
        ?: throw IllegalArgumentException("No dag for ${dato}")

    internal val size get() = dager.size

    private fun set(dag: NyDag, dato: LocalDate, kilde: Hendelseskilde) {
        dager[dato] = dag
        kilder[dato] = kilde
    }

    private fun set(dag: NyDag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {
        grader[dato] = grad
        set(dag, dato, kilde)
    }

    override fun preVisitNySykdomstidslinje(tidslinje: NySykdomstidslinje, id: UUID, tidsstempel: LocalDateTime) {
        this.id = id
        this.tidsstempel = tidsstempel
    }

    override fun visitDag(dag: NyUkjentDag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: NyArbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: NyArbeidsgiverdag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: NyFeriedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: NyFriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: NyArbeidsgiverHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: NySykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: NySykHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)

    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde) =
        set(dag, dato, kilde)
}
