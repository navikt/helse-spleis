package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi
import kotlin.math.roundToInt

internal class SykdomstidslinjeBuilder(sykdomstidslinje: Sykdomstidslinje) : SykdomstidslinjeVisitor {
    private val navdager = mutableListOf<Tidslinjedag>()

    init {
        sykdomstidslinje.accept(this)
    }

    fun dager() = navdager.toList()

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        visit(dato, "SYKEDAG", økonomi)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        visit(dato, "SYKEDAG", økonomi)
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        visit(dato, "FERIEDAG", null)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        visit(dato, "PERMISJONSDAG", null)
    }

    private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?) {
        val grad = økonomi?.medData { grad, _, _ -> grad }
        navdager.add(Tidslinjedag(dato, dagtype, grad?.roundToInt()))
    }
    companion object {
        internal fun Sykdomstidslinje.subsumsjonsformat(): List<Tidslinjedag> = SykdomstidslinjeBuilder(this).dager()

    }
}