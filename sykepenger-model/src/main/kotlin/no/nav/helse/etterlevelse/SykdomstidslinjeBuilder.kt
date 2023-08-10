package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

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

    override fun visitDag(dag: Dag.AndreYtelser, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, ytelse: Dag.AndreYtelser.AnnenYtelse) {
        visit(dato, when(ytelse) {
            Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> "FORELDREPENGER"
            Dag.AndreYtelser.AnnenYtelse.AAP -> "ARBEIDSAVKLARINGSPENGER"
            Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> "OMSORGSPENGER"
            Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> "PLEIEPENGER"
            Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> "SVANGERSKAPSPENGER"
            Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> "OPPLÆRINGSPENGER"
            Dag.AndreYtelser.AnnenYtelse.Dagpenger -> "DAGPENGER"
        }, null)
    }

    private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?) {
        val grad = økonomi?.brukAvrundetGrad { grad-> grad }
        navdager.add(Tidslinjedag(dato, dagtype, grad))
    }
}