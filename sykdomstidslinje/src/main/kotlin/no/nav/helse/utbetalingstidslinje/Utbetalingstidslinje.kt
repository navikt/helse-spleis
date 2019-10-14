package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*

class Utbetalingstidslinje(private val dagsats: Double) : SykdomstidslinjeVisitor {

    private val utbetalingsdager = mutableListOf<Utbetalingsdag>()

    private var state: State = Arbeidsgiverperiode()
    private val dagerIArbeidsgiverperiode = 16
    private var antallArbeidsgiverdager = 0

    fun totalSum() = Utbetalingsdag.sum(utbetalingsdager)

    fun erAvklart() = utbetalingsdager.all(Utbetalingsdag::erAvklart)

    override fun toString() = utbetalingsdager.joinToString(separator = "\n") { it.toString() }

    override fun visitSykedag(sykedag: Sykedag) {
        state.visitSykedag(sykedag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        state.visitSykHelgedag(sykHelgedag)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        state.visitEgenmeldingsdag(egenmeldingsdag)
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
        state.visitUtenlandsdag(utenlandsdag)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
        state.visitUbestemt(ubestemtdag)
    }

    private fun tellArbeidsgiverdager() {
        antallArbeidsgiverdager += 1
        if (antallArbeidsgiverdager >= dagerIArbeidsgiverperiode) state = Trygdeperiode()
    }

    private interface Utbetalingsdag {
        companion object {
            internal fun sum(utbetalingsdager: List<Utbetalingsdag>) = utbetalingsdager.sumByDouble { it.dagsats() }
        }

        fun dagsats(): Double
        fun erAvklart(): Boolean
    }

    private class UavklartUtbetalingsdag(private val dag: Dag) : Utbetalingsdag {
        override fun dagsats() = 0.0
        override fun erAvklart() = false
        override fun toString(): String = "Uavklart utbetalingsdag\t$dag"
    }

    private class AvklartUtbetalingsdag(private val dagsats: Double, private val dag: Dag) : Utbetalingsdag {
        override fun dagsats() = dagsats
        override fun erAvklart() = true
        override fun toString(): String = "Avklart utbetalingsdag\t$dag"
    }

    private interface State : SykdomstidslinjeVisitor

    private inner class Arbeidsgiverperiode : State {
        override fun visitSykedag(sykedag: Sykedag) {
            utbetalingsdager.add(AvklartUtbetalingsdag(0.0, dag = sykedag))
            tellArbeidsgiverdager()
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsdager.add(AvklartUtbetalingsdag(0.0, dag = sykHelgedag))
            tellArbeidsgiverdager()
        }

        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
            utbetalingsdager.add(AvklartUtbetalingsdag(0.0, dag = egenmeldingsdag))
            tellArbeidsgiverdager()
        }

        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
            utbetalingsdager.add(UavklartUtbetalingsdag(dag = utenlandsdag))
        }

        override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
            utbetalingsdager.add(UavklartUtbetalingsdag(dag = ubestemtdag))
        }
    }

    private inner class Trygdeperiode : State {
        override fun visitSykedag(sykedag: Sykedag) {
            utbetalingsdager.add(AvklartUtbetalingsdag(dagsats, dag = sykedag))
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsdager.add(AvklartUtbetalingsdag(0.0, dag = sykHelgedag))
        }

        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
            utbetalingsdager.add(UavklartUtbetalingsdag(dag = utenlandsdag))
        }

        override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
            utbetalingsdager.add(UavklartUtbetalingsdag(dag = ubestemtdag))
        }
    }

}
