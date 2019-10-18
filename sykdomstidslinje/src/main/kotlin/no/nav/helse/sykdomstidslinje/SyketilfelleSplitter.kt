package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal class SyketilfelleSplitter : SykdomstidslinjeVisitor {
    var state: SykedagerTellerTilstand = Starttilstand()
    var friskeDager = 0
    private var sykedager = 0
    private var syketilfelle = mutableListOf<Dag>()
    private val syketilfeller = mutableListOf<Sykdomstidslinje>()

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        syketilfelle.add(arbeidsdag)
        state.visitArbeidsdag(arbeidsdag)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        syketilfelle.add(implisittDag)
        state.visitImplisittdag(implisittDag)
    }

    override fun visitFeriedag(feriedag: Feriedag) {
        syketilfelle.add(feriedag)
        state.visitFeriedag(feriedag)
    }

    override fun visitSykedag(sykedag: Sykedag) {
        syketilfelle.add(sykedag)
        state.visitSykedag(sykedag)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        syketilfelle.add(egenmeldingsdag)
        state.visitEgenmeldingsdag(egenmeldingsdag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        syketilfelle.add(sykHelgedag)
        state.visitSykHelgedag(sykHelgedag)
    }

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
        pushResultat()
    }

    fun pushResultat() {
        syketilfeller.add(CompositeSykdomstidslinje(syketilfelle).trim())
        friskeDager = 0
        syketilfelle = mutableListOf()
    }

    internal interface SykedagerTellerTilstand {
        fun visitArbeidsdag(dag: Arbeidsdag) {}
        fun visitImplisittdag(dag: ImplisittDag) {}
        fun visitSykedag(dag: Sykedag) {}
        fun visitSykHelgedag(dag: SykHelgedag) {}
        fun visitFeriedag(dag: Feriedag) {}
        fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {}
    }

    internal inner class Starttilstand : SykedagerTellerTilstand {

        override fun visitSykedag(dag: Sykedag) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag)
        }
    }

    internal inner class InnenforArbeidsgiverperiodeSyk(antallEkstraDager: Int = 0) : SykedagerTellerTilstand {

        init {
            sykedager += antallEkstraDager
        }

        override fun visitSykedag(dag: Sykedag) {
            friskeDager = 0
            sykedager++
            if (sykedager >= 16) {
                state = UtenforArbeidsgiverperiodeSyk()
                state.visitSykedag(dag)
            }
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitSykHelgedag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = InnenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            state = InnenforArbeidsgiverperiodeFrisk()
            state.visitImplisittdag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitFeriedag(dag)
        }
    }

    internal inner class InnenforArbeidsgiverperiodeFrisk(antallFriskeDager: Int = 0) : SykedagerTellerTilstand {

        init {
            friskeDager += antallFriskeDager
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            tellFriskeDager()
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            tellFriskeDager()
        }

        override fun visitFeriedag(dag: Feriedag) {
            tellFriskeDager()
        }

        override fun visitSykedag(dag: Sykedag) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            tellFriskeDager()
        }

        private fun tellFriskeDager() {
            friskeDager++
            if (friskeDager >= 16) {
                state = Starttilstand()
                pushResultat()
            }
        }
    }

    internal inner class InnenforArbeidsperiodeFriPåfølgendeSykdom : SykedagerTellerTilstand {
        var antallFridagerFriPåfølgendeSykdom = 0
        override fun visitFeriedag(dag: Feriedag) {
            antallFridagerFriPåfølgendeSykdom++
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = InnenforArbeidsgiverperiodeFrisk(antallFridagerFriPåfølgendeSykdom)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = InnenforArbeidsgiverperiodeFrisk(antallFridagerFriPåfølgendeSykdom)
                state.visitImplisittdag(dag)
            } else {
                antallFridagerFriPåfølgendeSykdom++
            }
        }

        override fun visitSykedag(dag: Sykedag) {
            state = InnenforArbeidsgiverperiodeSyk(antallFridagerFriPåfølgendeSykdom)
            state.visitSykedag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            antallFridagerFriPåfølgendeSykdom++
        }
    }

    internal inner class UtenforArbeidsgiverperiodeSyk : SykedagerTellerTilstand {

        override fun visitSykedag(dag: Sykedag) {
            friskeDager = 0
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitSykHelgedag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = UtenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk()
            } else {
                state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            }
            state.visitImplisittdag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitFeriedag(dag)
        }
    }

    internal inner class UtenforArbeidsgiverperiodeFrisk : SykedagerTellerTilstand {

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            tellFriskeDager()
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            tellFriskeDager()
        }

        override fun visitFeriedag(dag: Feriedag) {
            tellFriskeDager()
        }

        override fun visitSykedag(dag: Sykedag) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            tellFriskeDager()
        }

        private fun tellFriskeDager() {
            friskeDager++
            if (friskeDager > 16) {
                state = Starttilstand()
                pushResultat()
            }
        }
    }

    internal inner class UtenforArbeidsperiodeFriPåfølgendeSykdom : SykedagerTellerTilstand {

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = UtenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk()
                state.visitImplisittdag(dag)
            }
        }

        override fun visitSykedag(dag: Sykedag) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag)
        }
    }

    fun results() = syketilfeller
}

