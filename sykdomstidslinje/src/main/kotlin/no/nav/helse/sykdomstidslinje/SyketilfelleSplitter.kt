package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.math.BigDecimal

internal class SyketilfelleSplitter(private val dagsats: BigDecimal) : SykdomstidslinjeVisitor {

    private var state: BetalingsState = Arbeidsgiverperiode()
    private val betalingslinjer = mutableListOf<Betalingslinje>()

    fun results(): List<Betalingslinje> {
        require(state != Ugyldig)
        return betalingslinjer.toList()
    }

    private fun state(state:BetalingsState) {
        this.state.leaving()
        this.state = state
        this.state.entering()
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) = state.visitArbeidsdag(this, arbeidsdag)
    override fun visitImplisittDag(implisittDag: ImplisittDag) = state.visitImplisittDag(this, implisittDag)
    override fun visitFeriedag(feriedag: Feriedag) = state.visitFeriedag(this, feriedag)
    override fun visitSykedag(sykedag: Sykedag) = state.visitSykedag(this, sykedag)
    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) =
        state.visitEgenmeldingsdag(this, egenmeldingsdag)

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) = state.visitSykHelgedag(this, sykHelgedag)
    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) = state.visitUtenlandsdag(this, utenlandsdag)
    override fun visitUbestemt(ubestemtdag: Ubestemtdag) = state.visitUbestemt(this, ubestemtdag)
    override fun visitStudiedag(studiedag: Studiedag) = state.visitStudiedag(this, studiedag)
    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) = state.visitPermisjonsdag(this, permisjonsdag)

    abstract class BetalingsState {
        internal val nyttSyketilfelleGrense = 16

        open fun visitArbeidsdag(splitter: SyketilfelleSplitter, arbeidsdag: Arbeidsdag) {}
        open fun visitImplisittDag(splitter: SyketilfelleSplitter, implisittDag: ImplisittDag) {}
        open fun visitFeriedag(splitter: SyketilfelleSplitter, feriedag: Feriedag) {}
        open fun visitSykedag(splitter: SyketilfelleSplitter, sykedag: Sykedag) {}
        open fun visitEgenmeldingsdag(splitter: SyketilfelleSplitter, egenmeldingsdag: Egenmeldingsdag) {}
        open fun visitSykHelgedag(splitter: SyketilfelleSplitter, sykHelgedag: SykHelgedag) {}
        fun visitUtenlandsdag(splitter: SyketilfelleSplitter, utenlandsdag: Utenlandsdag) { splitter.state = Ugyldig }
        fun visitUbestemt(splitter: SyketilfelleSplitter, ubestemtdag: Ubestemtdag) { splitter.state = Ugyldig }
        fun visitStudiedag(splitter: SyketilfelleSplitter, studiedag: Studiedag) { splitter.state = Ugyldig }
        fun visitPermisjonsdag(splitter: SyketilfelleSplitter, permisjonsdag: Permisjonsdag) { splitter.state = Ugyldig }

        open fun entering() {}
        open fun leaving() {}
    }

    private class Arbeidsgiverperiode : BetalingsState() {
        private var sykedager = 0
        private var ikkeSykedager = 0

        override fun visitArbeidsdag(splitter: SyketilfelleSplitter, arbeidsdag: Arbeidsdag) = tellIkkeSykedag()
        override fun visitImplisittDag(splitter: SyketilfelleSplitter, implisittDag: ImplisittDag) = tellIkkeSykedag()
        override fun visitFeriedag(splitter: SyketilfelleSplitter, feriedag: Feriedag) = tellIkkeSykedag()
        override fun visitSykedag(splitter: SyketilfelleSplitter, sykedag: Sykedag) = tellSykedag(splitter)
        override fun visitEgenmeldingsdag(splitter: SyketilfelleSplitter, egenmeldingsdag: Egenmeldingsdag) = tellSykedag(splitter)
        override fun visitSykHelgedag(splitter: SyketilfelleSplitter, sykHelgedag: SykHelgedag) = tellSykedag(splitter)

        private fun tellIkkeSykedag() {
            ikkeSykedager += 1
            if (ikkeSykedager > nyttSyketilfelleGrense) sykedager = 0
        }

        private fun tellSykedag(splitter: SyketilfelleSplitter) {
            sykedager += 1
            ikkeSykedager = 0
            if (sykedager >= nyttSyketilfelleGrense) {
                splitter.state = InitialStateBetaling()
            }
        }
    }

    private class InitialStateBetaling(private var ikkeSykedager:Int = 0) : BetalingsState() {
        override fun visitSykedag(splitter: SyketilfelleSplitter, sykedag: Sykedag) =
            opprettBetalingslinje(sykedag, splitter)

        override fun visitEgenmeldingsdag(splitter: SyketilfelleSplitter, egenmeldingsdag: Egenmeldingsdag) =
            opprettBetalingslinje(egenmeldingsdag, splitter)

        override fun visitSykHelgedag(splitter: SyketilfelleSplitter, sykHelgedag: SykHelgedag) =
            opprettBetalingslinje(sykHelgedag, splitter)

        override fun visitArbeidsdag(splitter: SyketilfelleSplitter, arbeidsdag: Arbeidsdag) {
            tellIkkeSykedag(splitter)
        }

        override fun visitImplisittDag(splitter: SyketilfelleSplitter, implisittDag: ImplisittDag) {
            tellIkkeSykedag(splitter)
        }

        override fun visitFeriedag(splitter: SyketilfelleSplitter, feriedag: Feriedag) {
            tellIkkeSykedag(splitter)
        }

        private fun tellIkkeSykedag(splitter: SyketilfelleSplitter) {
            ikkeSykedager += 1
            if (ikkeSykedager > nyttSyketilfelleGrense) splitter.state(Ugyldig)
        }

        private fun opprettBetalingslinje(sykedag: Dag, splitter: SyketilfelleSplitter) {
            Betalingslinje(sykedag.dagen, splitter.dagsats).apply { splitter.betalingslinjer.add(this) }
            splitter.state(SykBetaling)
        }
    }
    private object SykBetaling : BetalingsState() {

        override fun visitSykedag(splitter: SyketilfelleSplitter, sykedag: Sykedag) {
            splitter.betalingslinjer.last().tom = sykedag.dagen
        }

        override fun visitArbeidsdag(splitter: SyketilfelleSplitter, arbeidsdag: Arbeidsdag) {
            splitter.state(InitialStateBetaling(1))
        }

        override fun visitImplisittDag(splitter: SyketilfelleSplitter, implisittDag: ImplisittDag) {
            splitter.state(InitialStateBetaling(1))
        }

        override fun visitFeriedag(splitter: SyketilfelleSplitter, feriedag: Feriedag) {
            splitter.state(InitialStateBetaling(1))
        }

        override fun visitEgenmeldingsdag(splitter: SyketilfelleSplitter, egenmeldingsdag: Egenmeldingsdag) {
            splitter.betalingslinjer.last().tom = egenmeldingsdag.dagen
        }

        override fun visitSykHelgedag(splitter: SyketilfelleSplitter, sykHelgedag: SykHelgedag) {
            splitter.betalingslinjer.last().tom = sykHelgedag.dagen
        }
    }

    private object Ugyldig : BetalingsState()


}

