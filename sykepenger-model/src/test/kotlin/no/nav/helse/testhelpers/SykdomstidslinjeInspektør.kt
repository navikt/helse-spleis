package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.økonomi.Grad
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SykdomstidslinjeInspektør(tidslinje: NySykdomstidslinje) : NySykdomstidslinjeVisitor {
    internal val dager = mutableMapOf<LocalDate, NyDag>()
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

    private fun set(dag: NyDag, dato: LocalDate, kilde: Hendelseskilde) {
        dager[dato] = dag
        kilder[dato] = kilde
    }

    private fun set(dag: NyDag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) {
        grader[dato] = grad
        set(dag, dato, kilde)
    }

    private fun set(dag: NyDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {
        problemdagmeldinger[dato] = melding
        set(dag, dato, kilde)
    }

    override fun preVisitNySykdomstidslinje(
        tidslinje: NySykdomstidslinje,
        låstePerioder: List<Periode>,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {
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

    override fun visitDag(dag: NyArbeidsgiverHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: NySykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: NyForeldetSykedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

    override fun visitDag(dag: NySykHelgedag, dato: LocalDate, grad: Grad, kilde: Hendelseskilde) =
        set(dag, dato, grad, kilde)

//    override fun visitDag(dag: NyStudiedag, dato: LocalDate, kilde: Hendelseskilde) =
//        set(dag, dato, kilde)
//
//    override fun visitDag(dag: NyPermisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
//        set(dag, dato, kilde)
//
//    override fun visitDag(dag: NyUtenlandsdag, dato: LocalDate, kilde: Hendelseskilde) =
//        set(dag, dato, kilde)

    override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) =
        set(dag, dato, kilde, melding)
}
