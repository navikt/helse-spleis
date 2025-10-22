package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal class Ventetidberegner {

    fun result(sykdomstidslinje: Sykdomstidslinje): List<Ventetidsavklaring> {
        val ventetider = mutableListOf<Ventetidsavklaring>()
        var aktivVentetid: Ventetidtelling? = null
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.SykHelgedag,
                is Dag.Sykedag -> {
                    if (aktivVentetid == null) {
                        aktivVentetid = Ventetidtelling(setOf(dag.dato), oppholdsdager = emptySet())
                    } else {
                        aktivVentetid = aktivVentetid.utvid(dag.dato)
                    }
                }

                is Dag.UkjentDag -> {
                    if (dag.dato.erHelg()) {
                        aktivVentetid = aktivVentetid?.utvid(dag.dato)
                    }
                }

                is Dag.AndreYtelser,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.Arbeidsdag,
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.Feriedag,
                is Dag.ForeldetSykedag,
                is Dag.FriskHelgedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag -> error("forventer ikke dag av type ${dag::class.simpleName} i ventetidsberegning")
            }
        }
        return ventetider.toList() + listOfNotNull(aktivVentetid?.somAvklaring())
    }

    private data class Ventetidtelling(
        val dager: Set<LocalDate>,
        val oppholdsdager: Set<LocalDate>
    ) {
        init {
            check(dager.isNotEmpty()) { "kan ikke ha tomme dager" }
        }

        val ventetid = dager.take(MAKSIMALT_ANTALL_VENTETIDSDAGER)
        val ferdigAvklart = dager.size >= MAKSIMALT_ANTALL_VENTETIDSDAGER && dager.drop(MAKSIMALT_ANTALL_VENTETIDSDAGER).any { !it.erHelg() }

        fun utvid(dato: LocalDate) = copy(dager = this.dager + dato)

        companion object {
            const val MAKSIMALT_ANTALL_VENTETIDSDAGER = 16
        }
    }

    private fun Ventetidtelling.somAvklaring() =
        Ventetidsavklaring(
            periode = ventetid.omsluttendePeriode!!,
            ferdigAvklart = ferdigAvklart
        )

}

data class Ventetidsavklaring(
    val periode: Periode,
    val ferdigAvklart: Boolean
)
