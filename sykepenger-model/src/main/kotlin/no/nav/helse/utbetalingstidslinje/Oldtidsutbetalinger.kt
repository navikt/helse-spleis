package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.harTilstøtende
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

private const val ALLE_ARBEIDSGIVERE = "UKJENT"

internal class Oldtidsutbetalinger(
    private val periode: Periode
) {
    private val utbetalinger = mutableMapOf<String, MutableList<Utbetalingstidslinje>>()
    private val ferier = mutableMapOf<String, MutableList<Utbetalingstidslinje>>()

    private var tilstand: Tilstand = Initial()
    private var førsteSykepengedagISistePeriode: LocalDate? = null
    private var sisteSykepengedagISistePeriode: LocalDate? = null

    internal fun tilstøtende(arbeidsgiver: Arbeidsgiver): Boolean =
        tidslinje(arbeidsgiver).sistePeriode()
            ?.endInclusive
            ?.harTilstøtende(periode.start)
            ?: false

    internal fun førsteUtbetalingsdag(arbeidsgiver: Arbeidsgiver): LocalDate {
        require(tilstøtende(arbeidsgiver)) { "Periode er ikke tilstøtende" }
        return requireNotNull(tidslinje(arbeidsgiver).sistePeriode()).start
    }

    private fun tidslinje(arbeidsgiver: Arbeidsgiver) =
        (ferier.getOrDefault(ALLE_ARBEIDSGIVERE, mutableListOf()) +
            utbetalinger.getOrDefault(arbeidsgiver.organisasjonsnummer(), mutableListOf()))
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun addUtbetaling(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
        utbetalinger.getOrPut(orgnummer) { mutableListOf() }.add(tidslinje)
    }

    internal fun addFerie(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
        ferier.getOrPut(orgnummer) { mutableListOf() }.add(tidslinje)
    }

    private fun Utbetalingstidslinje.sistePeriode(): Periode? {
        tilstand = Initial()
        førsteSykepengedagISistePeriode = null
        sisteSykepengedagISistePeriode = null
        val tidslinje = kutt(periode.start.minusDays(1)).reverse()
        for (challenger in tidslinje) {
            when (challenger) {
                is NavDag, is ArbeidsgiverperiodeDag -> tilstand.utbetaling(challenger)
                is Fridag, is NavHelgDag -> tilstand.fri(challenger)
                else -> tilstand.gap(challenger)
            }
        }
        return førsteSykepengedagISistePeriode?.let { Periode(it, sisteSykepengedagISistePeriode!!) }
    }

    private interface Tilstand {
        fun utbetaling(dag: Utbetalingstidslinje.Utbetalingsdag) {}
        fun fri(dag: Utbetalingstidslinje.Utbetalingsdag) {}
        fun gap(dag: Utbetalingstidslinje.Utbetalingsdag) {}
    }

    private inner class Initial : Tilstand {
        override fun utbetaling(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteSykepengedagISistePeriode = dag.dato
            sisteSykepengedagISistePeriode = dag.dato
            tilstand = B()
        }

        override fun fri(dag: Utbetalingstidslinje.Utbetalingsdag) {
            sisteSykepengedagISistePeriode = dag.dato
            tilstand = B()
        }

        override fun gap(dag: Utbetalingstidslinje.Utbetalingsdag) {
            tilstand = Avslutt()
        }
    }

    private inner class B : Tilstand {
        override fun utbetaling(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteSykepengedagISistePeriode = dag.dato
        }

        override fun fri(dag: Utbetalingstidslinje.Utbetalingsdag) {
        }

        override fun gap(dag: Utbetalingstidslinje.Utbetalingsdag) {
            tilstand = Avslutt()
        }
    }

    private inner class Avslutt : Tilstand
}
