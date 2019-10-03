package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykHelgedag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykedag

class UtbetalingsTidslinje(private val dagsats: Double): SykdomstidslinjeVisitor{

    private val utbetalingsDager = mutableListOf<UtbetalingsDag>()

    private var state: State = ArbeidsgiverPeriode()
    private val dagerIArbeidsgiverPeriode = 16
    private var dagNummer = 0

    fun totalSum() = UtbetalingsDag.sum(utbetalingsDager)

    override fun visitSykedag(sykedag: Sykedag){
        state.visitSykedag(sykedag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        state.visitSykHelgedag(sykHelgedag)
    }


    private class UtbetalingsDag(private val dagsats: Double, private val dag: Dag){

        companion object{
            internal fun sum(utbetalingsDager: List<UtbetalingsDag>) = utbetalingsDager.sumByDouble { it.dagsats }
        }

    }

    private abstract inner class State : SykdomstidslinjeVisitor{
        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            utbetalingsDager.add(UtbetalingsDag(0.0, dag = sykHelgedag))
            byttTilstand()
        }

        protected fun byttTilstand(){
            dagNummer += 1
            if(dagNummer > dagerIArbeidsgiverPeriode) state = TrygdenYterPeriode()
        }
    }

    private inner class ArbeidsgiverPeriode: State(){

        override fun visitSykedag(sykedag: Sykedag){
            utbetalingsDager.add(UtbetalingsDag(0.0, dag = sykedag))
            byttTilstand()
        }
    }

    private inner class TrygdenYterPeriode(): State(){
        override fun visitSykedag(sykedag: Sykedag){
            utbetalingsDager.add(UtbetalingsDag(dagsats, dag = sykedag))
        }
    }

}
