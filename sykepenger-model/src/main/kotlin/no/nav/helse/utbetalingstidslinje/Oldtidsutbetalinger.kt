package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.erRettFør
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

private const val ALLE_ARBEIDSGIVERE = "UKJENT"

internal class Oldtidsutbetalinger {
    private val tidslinjer = mutableMapOf<String, MutableList<Utbetalingstidslinje>>()

    private var tilstand: Tilstand = FinnSiste()
    private var førsteSykepengedagISistePeriode: LocalDate? = null
    private var sisteSykepengedagISistePeriode: LocalDate? = null

    interface UtbetalingerForArbeidsgiver {
        fun erRettFør(periode: Periode): Boolean

        //Hvis historikken ikke er tilstøtende, så forventer vi å få en inntektsmelding
        fun arbeidsgiverperiodeErBetalt(periode: Periode): Boolean
        fun førsteUtbetalingsdag(periode: Periode): LocalDate
    }

    internal fun utbetalingerInkludert(arbeidsgiver: Arbeidsgiver) = object : UtbetalingerForArbeidsgiver {
        override fun erRettFør(periode: Periode) =
            tidslinje(arbeidsgiver).sisteUtbetalingsperiodeFør(periode.start)
                ?.endInclusive
                ?.erRettFør(periode.start)
                ?: false

        override fun arbeidsgiverperiodeErBetalt(periode: Periode) = erRettFør(periode)

        override fun førsteUtbetalingsdag(periode: Periode): LocalDate {
            require(erRettFør(periode)) { "Periode er ikke tilstøtende" }
            return requireNotNull(tidslinje(arbeidsgiver).sisteUtbetalingsperiodeFør(periode.start)).start
        }
    }

    internal fun personTidslinje(periode: Periode) = tidslinjer.values
        .flatten()
        .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
        .kutt(periode.endInclusive)

    private fun tidslinje(arbeidsgiver: Arbeidsgiver) =
        (tidslinjer.getOrDefault(ALLE_ARBEIDSGIVERE, mutableListOf()) +
            tidslinjer.getOrDefault(arbeidsgiver.organisasjonsnummer(), mutableListOf()))
            .fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)

    internal fun add(orgnummer: String = ALLE_ARBEIDSGIVERE, tidslinje: Utbetalingstidslinje) {
        tidslinjer.getOrPut(orgnummer) { mutableListOf() }.add(tidslinje)
    }

    private fun Utbetalingstidslinje.sisteUtbetalingsperiodeFør(dato: LocalDate): Periode? {
        tilstand = FinnSiste()
        førsteSykepengedagISistePeriode = null
        sisteSykepengedagISistePeriode = null
        val tidslinje = kutt(dato.minusDays(1)).reverse()
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

    private inner class FinnSiste : Tilstand {
        override fun utbetaling(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteSykepengedagISistePeriode = dag.dato
            sisteSykepengedagISistePeriode = dag.dato
            tilstand = FinnFørste()
        }

        override fun fri(dag: Utbetalingstidslinje.Utbetalingsdag) {
            sisteSykepengedagISistePeriode = dag.dato
            tilstand = FinnFørste()
        }

        override fun gap(dag: Utbetalingstidslinje.Utbetalingsdag) {
            tilstand = Avslutt()
        }
    }

    private inner class FinnFørste : Tilstand {
        override fun utbetaling(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteSykepengedagISistePeriode = dag.dato
        }

        override fun gap(dag: Utbetalingstidslinje.Utbetalingsdag) {
            tilstand = Avslutt()
        }
    }

    private inner class Avslutt : Tilstand
}
