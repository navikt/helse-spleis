package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Forstår utbetalingsforpliktelser for en bestemt arbeidsgiver
 */

internal class ArbeidsgiverUtbetalingstidslinje {

    constructor() {
        utbetalingsdager = mutableListOf()
    }

    private constructor(utbetalingsdager: List<Utbetalingsdag>) {
        this.utbetalingsdager = utbetalingsdager.toMutableList()
    }

    private val utbetalingsdager: MutableList<Utbetalingsdag>
    private val utbetalingslinjer = mutableListOf<Utbetalingslinje>()
    private var helseState: HelseState = HelseState.IkkeSyk

    fun utbetalingslinjer(others: List<ArbeidsgiverUtbetalingstidslinje>, alderRegler: AlderRegler, førsteDag: LocalDate, sisteDag: LocalDate) =
        this
            .avgrens(others, alderRegler)
            .filterByMinimumInntekt(others, alderRegler)
            .reduserAvSykdomsgrad(others)
            .subset(førsteDag, sisteDag)
            .utbetalingslinjer()

    private fun avgrens(others: List<ArbeidsgiverUtbetalingstidslinje>, alderRegler: AlderRegler) : ArbeidsgiverUtbetalingstidslinje {
        val startDato =  utbetalingsdager.first().dato
        val sisteUtbetalingsdag = merge(others)
            .betalingsperiode(alderRegler)

        return this.subset(startDato, sisteUtbetalingsdag)
    }

    private fun betalingsperiode(alderRegler: AlderRegler): LocalDate {
        var betalteDager = 0
        var redusertYtelseDager = 0
        var sisteBetalteDag = utbetalingsdager.first().dato
        for (dag in utbetalingsdager) {
            if (dag is Utbetalingsdag.NavDag && dag.inntekt > 0) {
                if (alderRegler.navBurdeBetale(betalteDager, redusertYtelseDager, dag.dato)) {
                    sisteBetalteDag = dag.dato
                }

                if (alderRegler.harFylt67(dag.dato)) {
                    redusertYtelseDager++
                }

                betalteDager ++
            }
        }
        return sisteBetalteDag
    }

    private fun merge(others: List<ArbeidsgiverUtbetalingstidslinje>): ArbeidsgiverUtbetalingstidslinje {
        require(others.isEmpty()) { "Hello future programmer, you need to implement support for multiple employers" }
        return this
    }


    private fun filterByMinimumInntekt(
        others: List<ArbeidsgiverUtbetalingstidslinje>,
        alderRegler: AlderRegler
    ) = this

    private fun reduserAvSykdomsgrad(others: List<ArbeidsgiverUtbetalingstidslinje>) = this

    private fun subset(fom: LocalDate, tom: LocalDate) : ArbeidsgiverUtbetalingstidslinje {
        return ArbeidsgiverUtbetalingstidslinje(utbetalingsdager.filterNot { it.dato.isBefore(fom) || it.dato.isAfter(tom) })
    }

    private fun utbetalingslinjer(): List<Utbetalingslinje> {
        utbetalingsdager.forEach { it.accept(this, this.helseState) }
        return utbetalingslinjer
    }

    internal fun addArbeidsgiverperiodedag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.ArbeidsgiverperiodeDag(inntekt.roundToInt(), dato))
    }

    internal fun addNAVdag(inntekt: Double, dato: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.NavDag(inntekt.roundToInt(), dato))
    }

    internal fun addArbeidsdag(dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Arbeidsdag(dagen))
    }

    internal fun addFridag(dagen: LocalDate) {
        utbetalingsdager.add(Utbetalingsdag.Fridag(dagen))
    }


    private sealed class Utbetalingsdag(internal val inntekt: Int, internal val dato: LocalDate) {

        companion object {
            internal fun subset(liste: List<Utbetalingsdag>, fom: LocalDate, tom: LocalDate) = liste.filter { it.dato.isAfter(fom.minusDays(1)) && it.dato.isBefore(tom.plusDays(1)) }
        }

        abstract fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState)

        internal class ArbeidsgiverperiodeDag(inntekt: Int, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitArbeidsgiverperiodeDag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }

        internal class NavDag(inntekt: Int, dato: LocalDate) :
            Utbetalingsdag(inntekt, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitNAVDag(arbeidsgiverUtbetalingstidslinje, this)
            }

            fun utbetalingslinje() = Utbetalingslinje(dato, dato, inntekt)
            fun oppdater(last: Utbetalingslinje) {
                last.tom = dato
            }
        }

        internal class Arbeidsdag(dato: LocalDate) :
            Utbetalingsdag(0, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitArbeidsdag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }

        internal class Fridag(dato: LocalDate) :
            Utbetalingsdag(0, dato) {
            override fun accept(arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje, state: HelseState) {
                state.visitFridag(arbeidsgiverUtbetalingstidslinje, this)
            }
        }
    }

    private sealed class HelseState {

        open fun visitArbeidsgiverperiodeDag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            arbeidsgiverperiodeDag: Utbetalingsdag.ArbeidsgiverperiodeDag
        ) {
        }

        abstract fun visitNAVDag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            NavDag: Utbetalingsdag.NavDag
        )

        open fun visitArbeidsdag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            arbeidsdag: Utbetalingsdag.Arbeidsdag
        ) {
        }

        open fun visitFridag(
            arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
            fridag: Utbetalingsdag.Fridag
        ) {
        }

        internal object IkkeSyk : HelseState() {
            override fun visitNAVDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                navDag: Utbetalingsdag.NavDag
            ) {
                if (navDag.inntekt == 0) return
                arbeidsgiverUtbetalingstidslinje.utbetalingslinjer.add(navDag.utbetalingslinje())
                arbeidsgiverUtbetalingstidslinje.helseState = Syk
            }
        }

        internal object Syk : HelseState() {
            override fun visitNAVDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                navDag: Utbetalingsdag.NavDag
            ) {
                navDag.oppdater(arbeidsgiverUtbetalingstidslinje.utbetalingslinjer.last())
            }

            override fun visitArbeidsgiverperiodeDag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                arbeidsgiverperiodeDag: Utbetalingsdag.ArbeidsgiverperiodeDag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }

            override fun visitArbeidsdag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                arbeidsdag: Utbetalingsdag.Arbeidsdag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }

            override fun visitFridag(
                arbeidsgiverUtbetalingstidslinje: ArbeidsgiverUtbetalingstidslinje,
                fridag: Utbetalingsdag.Fridag
            ) {
                arbeidsgiverUtbetalingstidslinje.helseState = IkkeSyk
            }
        }
    }

}
