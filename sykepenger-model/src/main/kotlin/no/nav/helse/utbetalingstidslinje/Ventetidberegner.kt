package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Ventetidberegner.Ventetidtelling.Companion.MAKSIMALT_ANTALL_OPPHOLDSDAGER

internal class Ventetidberegner {

    fun result(sykdomstidslinje: Sykdomstidslinje): List<Ventetidsavklaring> {
        val ventetider = mutableListOf<Ventetidsavklaring>()
        var aktivVentetid: Ventetidtelling? = null
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                is Dag.SykHelgedag,
                is Dag.Sykedag -> {
                    if (aktivVentetid == null) {
                        aktivVentetid = Ventetidtelling.ny(dag.dato)
                    } else {
                        aktivVentetid = aktivVentetid.utvid(dag.dato)
                    }
                }

                is Dag.UkjentDag -> {
                    if (aktivVentetid?.ferdigAvklart == true) {
                        if (aktivVentetid.oppholdsdager.count() < MAKSIMALT_ANTALL_OPPHOLDSDAGER) {
                            aktivVentetid = aktivVentetid.opphold(dag.dato)
                        } else {
                            ventetider.add(aktivVentetid.somAvklaring())
                            aktivVentetid = null
                        }
                    } else if (aktivVentetid?.ferdigAvklart == false) {
                        if (aktivVentetid.oppholdsdager.isEmpty() && dag.dato.erHelg()) {
                            aktivVentetid = aktivVentetid.utvid(dag.dato)
                        } else {
                            ventetider.add(aktivVentetid.somAvklaring())
                            aktivVentetid = null
                        }
                    }
                }

                is Dag.Arbeidsdag,
                is Dag.FriskHelgedag -> {
                    if (aktivVentetid?.ferdigAvklart == true) {
                        if (aktivVentetid.oppholdsdager.count() < MAKSIMALT_ANTALL_OPPHOLDSDAGER) {
                            aktivVentetid = aktivVentetid.opphold(dag.dato)
                        } else {
                            ventetider.add(aktivVentetid.somAvklaring())
                            aktivVentetid = null
                        }
                    } else if (aktivVentetid?.ferdigAvklart == false) {
                        ventetider.add(aktivVentetid.somAvklaring())
                        aktivVentetid = null
                    }
                }

                is Dag.AndreYtelser,
                is Dag.ArbeidIkkeGjenopptattDag,
                is Dag.ArbeidsgiverHelgedag,
                is Dag.Arbeidsgiverdag,
                is Dag.Feriedag,
                is Dag.ForeldetSykedag,
                is Dag.Permisjonsdag,
                is Dag.ProblemDag -> error("forventer ikke dag av type ${dag::class.simpleName} i ventetidsberegning")
            }
        }
        return ventetider.toList() + listOfNotNull(aktivVentetid?.somAvklaring())
    }

    private data class Ventetidtelling(
        val omsluttendePeriode: Periode,
        val dager: Set<LocalDate>,
        val oppholdsdager: Set<LocalDate>
    ) {
        val ventetid = dager.take(MAKSIMALT_ANTALL_VENTETIDSDAGER)
        val ferdigAvklart = dager.size >= MAKSIMALT_ANTALL_VENTETIDSDAGER && dager.drop(MAKSIMALT_ANTALL_VENTETIDSDAGER).any { !it.erHelg() }

        fun utvid(dato: LocalDate) = copy(
            omsluttendePeriode = omsluttendePeriode.oppdaterTom(dato),
            dager = this.dager + dato,
            oppholdsdager = emptySet()
        )
        fun opphold(dato: LocalDate) = copy(
            omsluttendePeriode = omsluttendePeriode.oppdaterTom(dato),
            oppholdsdager = this.oppholdsdager + dato
        )

        companion object {
            const val MAKSIMALT_ANTALL_VENTETIDSDAGER = 16
            const val MAKSIMALT_ANTALL_OPPHOLDSDAGER = 15

            fun ny(dato: LocalDate) =
                Ventetidtelling(
                    omsluttendePeriode = dato.somPeriode(),
                    dager = setOf(dato),
                    oppholdsdager = emptySet()
                )
        }
    }

    private fun Ventetidtelling.somAvklaring() =
        Ventetidsavklaring(
            omsluttendePeriode = omsluttendePeriode,
            periode = ventetid.omsluttendePeriode,
            ferdigAvklart = ferdigAvklart
        )

}

data class Ventetidsavklaring(
    val omsluttendePeriode: Periode,
    val periode: Periode?,
    val ferdigAvklart: Boolean
)
