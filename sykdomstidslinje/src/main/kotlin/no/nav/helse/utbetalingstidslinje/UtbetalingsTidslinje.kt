package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykHelgedag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykedag

class UtbetalingsTidslinje(private val dagsats: Double): SykdomstidslinjeVisitor{

    private val utbetalingsDager = mutableListOf<UtbetalingsDag>()

    private var state: State = ArbeidsgiverPeriode()
    private val dagerIArbeidsgiverPeriode = 16
    private var arbeidsgiverDagNummer = 0

    fun totalSum() = UtbetalingsDag.sum(utbetalingsDager)

    override fun visitSykedag(sykedag: Sykedag){
        state.visitSykedag(sykedag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        state.visitSykHelgedag(sykHelgedag)
    }

    private fun byttTilstand(){
        arbeidsgiverDagNummer += 1
        if(arbeidsgiverDagNummer > dagerIArbeidsgiverPeriode) state = TrygdenYterPeriode()
    }

    private class UtbetalingsDag(private val dagsats: Double, private val dag: Dag){

        companion object{
            internal fun sum(utbetalingsDager: List<UtbetalingsDag>) = utbetalingsDager.sumByDouble { it.dagsats }
        }

    }

    private interface State : SykdomstidslinjeVisitor

    private inner class ArbeidsgiverPeriode: State{
        override fun visitSykedag(sykedag: Sykedag){
            utbetalingsDager.add(UtbetalingsDag(0.0, dag = sykedag))
            byttTilstand()
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsDager.add(UtbetalingsDag(0.0, dag = sykHelgedag))
            byttTilstand()
        }
    }

    private inner class TrygdenYterPeriode: State{
        override fun visitSykedag(sykedag: Sykedag){
            utbetalingsDager.add(UtbetalingsDag(dagsats, dag = sykedag))
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsDager.add(UtbetalingsDag(0.0, dag = sykHelgedag))
        }

    }

}
