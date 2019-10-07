package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.*

class UtbetalingsTidslinje(private val dagsats: Double) : SykdomstidslinjeVisitor {

    private val utbetalingsDager = mutableListOf<UtbetalingsDag>()

    private var state: State = ArbeidsgiverPeriode()
    private val dagerIArbeidsgiverPeriode = 16
    private var arbeidsgiverDagNummer = 0

    fun totalSum() = UtbetalingsDag.sum(utbetalingsDager)

    fun erAvklart() = utbetalingsDager.all(UtbetalingsDag::erAvklart)

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

    private fun tellArbeidsgiverDager() {
        arbeidsgiverDagNummer += 1
        if (arbeidsgiverDagNummer >= dagerIArbeidsgiverPeriode) state = TrygdenYterPeriode()
    }

    private interface UtbetalingsDag {
        companion object {
            internal fun sum(utbetalingsDager: List<UtbetalingsDag>) = utbetalingsDager.sumByDouble { it.dagsats() }
        }

        fun dagsats(): Double
        fun erAvklart(): Boolean
    }

    private class UavklartUtbetalingsdag(private val dag: Dag): UtbetalingsDag {
        override fun dagsats() = 0.0
        override fun erAvklart() = false
    }

    private class AvklartUtbetalingsdag(private val dagsats: Double, private val dag: Dag): UtbetalingsDag {
        override fun dagsats() = dagsats
        override fun erAvklart() = true
    }

    private interface State : SykdomstidslinjeVisitor

    private inner class ArbeidsgiverPeriode : State {
        override fun visitSykedag(sykedag: Sykedag) {
            utbetalingsDager.add(AvklartUtbetalingsdag(0.0, dag = sykedag))
            tellArbeidsgiverDager()
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsDager.add(AvklartUtbetalingsdag(0.0, dag = sykHelgedag))
            tellArbeidsgiverDager()
        }

        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
            utbetalingsDager.add(AvklartUtbetalingsdag(0.0, dag = egenmeldingsdag))
            tellArbeidsgiverDager()
        }

        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
            utbetalingsDager.add(UavklartUtbetalingsdag(dag = utenlandsdag))
        }
    }

    private inner class TrygdenYterPeriode : State {
        override fun visitSykedag(sykedag: Sykedag) {
            utbetalingsDager.add(AvklartUtbetalingsdag(dagsats, dag = sykedag))
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsDager.add(AvklartUtbetalingsdag(0.0, dag = sykHelgedag))
        }

        override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
            utbetalingsDager.add(UavklartUtbetalingsdag(dag = utenlandsdag))
        }
    }

}
