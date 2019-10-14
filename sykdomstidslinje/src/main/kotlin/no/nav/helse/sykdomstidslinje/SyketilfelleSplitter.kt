package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

internal class SyketilfelleSplitter : SykdomstidslinjeVisitor {
    var state: SykedagerTellerTilstand = Starttilstand()
    var ikkeSykedager = 0
    private var syketilfelle = mutableListOf<Dag>()
    private val syketilfeller = mutableListOf<Sykdomstidslinje>()

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        syketilfelle.add(arbeidsdag)
        state.visitArbeidsdag(arbeidsdag)
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
        ikkeSykedager = 0
        syketilfelle = mutableListOf()
    }

    internal interface SykedagerTellerTilstand {
        fun visitArbeidsdag(dag: Arbeidsdag) {}
        fun visitSykedag(dag: Sykedag) {}
        fun visitSykHelgedag(dag: SykHelgedag) {}
        fun visitFeriedag(dag: Feriedag) {}
        fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {}
    }

    internal inner class Starttilstand : SykedagerTellerTilstand {
        override fun visitSykedag(dag: Sykedag) {
            state = TellerSykedager()
            state.visitSykedag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = TellerSykedager()
            state.visitSykHelgedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = TellerSykedager()
            state.visitEgenmeldingsdag(dag)
        }
    }

    internal inner class TellerSykedager : SykedagerTellerTilstand {
        override fun visitSykedag(dag: Sykedag) {
            ikkeSykedager = 0
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            ikkeSykedager = 0
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = TellerIkkeSykedager()
            state.visitArbeidsdag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            state = FeriePåfølgendeSykdom()
            state.visitFeriedag(dag)
        }
    }

    internal inner class TellerIkkeSykedager : SykedagerTellerTilstand {
        override fun visitArbeidsdag(dag: Arbeidsdag) {
            tellIkkeSykedager()
        }

        override fun visitSykedag(dag: Sykedag) {
            state = TellerSykedager()
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = TellerSykedager()
            state.visitEgenmeldingsdag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = TellerSykedager()
            state.visitSykHelgedag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            tellIkkeSykedager()
        }

        private fun tellIkkeSykedager() {
            ikkeSykedager++
            if (ikkeSykedager >= 16) {
                state = Starttilstand()
                pushResultat()
            }
        }
    }

    internal inner class FeriePåfølgendeSykdom : SykedagerTellerTilstand {
        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = TellerIkkeSykedager()
            state.visitArbeidsdag(dag)
        }

        override fun visitSykedag(dag: Sykedag) {
            state = TellerSykedager()
            state.visitSykedag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = TellerHelg()
            state.visitSykHelgedag(dag)
        }
    }

    internal inner class TellerHelg : SykedagerTellerTilstand {
        override fun visitArbeidsdag(dag: Arbeidsdag) {
            ikkeSykedager += 2
            state = TellerIkkeSykedager()
            state.visitArbeidsdag(dag)
        }

        override fun visitSykedag(dag: Sykedag) {
            state = TellerSykedager()
            state.visitSykedag(dag)
        }
    }

    fun results() = syketilfeller
}

