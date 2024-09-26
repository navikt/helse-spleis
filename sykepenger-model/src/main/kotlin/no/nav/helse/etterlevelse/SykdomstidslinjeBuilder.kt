package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Økonomi

internal class SykdomstidslinjeBuilder(sykdomstidslinje: Sykdomstidslinje) {
    private val navdager = sykdomstidslinje.mapNotNull { dag ->
        when (dag) {
            is Dag.AndreYtelser -> visit(dag.dato, when(dag.ytelse) {
                Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> "FORELDREPENGER"
                Dag.AndreYtelser.AnnenYtelse.AAP -> "ARBEIDSAVKLARINGSPENGER"
                Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> "OMSORGSPENGER"
                Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> "PLEIEPENGER"
                Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> "SVANGERSKAPSPENGER"
                Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> "OPPLÆRINGSPENGER"
                Dag.AndreYtelser.AnnenYtelse.Dagpenger -> "DAGPENGER"
            }, null)
            is Dag.Feriedag -> visit(dag.dato, "FERIEDAG", null)
            is Dag.Permisjonsdag -> visit(dag.dato, "PERMISJONSDAG", null)
            is Dag.SykHelgedag -> visit(dag.dato, "SYKEDAG", dag.økonomi)
            is Dag.Sykedag -> visit(dag.dato, "SYKEDAG", dag.økonomi)
            is Dag.ArbeidIkkeGjenopptattDag,
            is Dag.Arbeidsdag,
            is Dag.ArbeidsgiverHelgedag,
            is Dag.Arbeidsgiverdag,
            is Dag.ForeldetSykedag,
            is Dag.FriskHelgedag,
            is Dag.ProblemDag,
            is Dag.SykedagNav,
            is Dag.UkjentDag -> null // gjør ingenting med disse
        }
    }

    fun dager() = navdager.toList()

    private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?): Tidslinjedag {
        val grad = økonomi?.brukAvrundetGrad { grad-> grad }
        return Tidslinjedag(dato, dagtype, grad)
    }
}