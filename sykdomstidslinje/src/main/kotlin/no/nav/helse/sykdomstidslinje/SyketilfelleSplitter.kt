package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import kotlin.math.min

data class Syketilfelle(
    val tidslinje: Sykdomstidslinje,
    val arbeidsgiverperiode: Sykdomstidslinje?,
    val dagerEtterArbeidsgiverperiode: Sykdomstidslinje?
)

private class SyketilfelleDraft(
    var dager: MutableList<Dag> = mutableListOf(),
    var arbeidsgiverperiode: MutableList<Dag> = mutableListOf(),
    var dagerEtterArbeidsgiverperiode: MutableList<Dag> = mutableListOf()
)

internal class SyketilfelleSplitter : SykdomstidslinjeVisitor {
    private var state: SykedagerTellerTilstand = Starttilstand()
    private var friskeDager = 0
    private var sykedager = 0
    private val syketilfeller = mutableListOf<Syketilfelle>()

    private var draft: SyketilfelleDraft = SyketilfelleDraft()

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        draft.dager.add(arbeidsdag)
        state.visitArbeidsdag(arbeidsdag, draft)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        draft.dager.add(implisittDag)
        state.visitImplisittdag(implisittDag, draft)
    }

    override fun visitFeriedag(feriedag: Feriedag) {
        draft.dager.add(feriedag)
        state.visitFeriedag(feriedag, draft)
    }

    override fun visitSykedag(sykedag: Sykedag) {
        draft.dager.add(sykedag)
        state.visitSykedag(sykedag, draft)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        draft.dager.add(egenmeldingsdag)
        state.visitEgenmeldingsdag(egenmeldingsdag, draft)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        draft.dager.add(sykHelgedag)
        state.visitSykHelgedag(sykHelgedag, draft)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) =
        throw RuntimeException("Det er ikke mulig å beregne arbeidsgiverperioder for tidslinjer med ubestemte dager, som f.eks ${ubestemtdag.dagen}")

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) =
        throw RuntimeException("Det er ikke mulig å beregne arbeidsgiverperioder for tidslinjer med permisjonsdager, som f.eks ${permisjonsdag.dagen}")

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
        pushResultat()
    }

    fun pushResultat() {
        syketilfeller.add(
            Syketilfelle(
                tidslinje = CompositeSykdomstidslinje(draft.dager).trim(),
                arbeidsgiverperiode = CompositeSykdomstidslinje(draft.arbeidsgiverperiode),
                dagerEtterArbeidsgiverperiode = CompositeSykdomstidslinje(draft.dagerEtterArbeidsgiverperiode).trim()
            )
        )
        friskeDager = 0
        sykedager = 0
        draft = SyketilfelleDraft()
    }

    private interface SykedagerTellerTilstand {
        fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {}
        fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {}
        fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {}
        fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {}
        fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {}
        fun visitEgenmeldingsdag(dag: Egenmeldingsdag, draft: SyketilfelleDraft) {}
    }

    private inner class Starttilstand : SykedagerTellerTilstand {

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag, draft)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag, draft)
        }
    }

    private inner class InnenforArbeidsgiverperiodeSyk(antallEkstraDager: Int = 0) : SykedagerTellerTilstand {

        init {
            sykedager += antallEkstraDager
        }

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            friskeDager = 0
            sykedager++

            if (draft.arbeidsgiverperiode.size < 16) {
                draft.arbeidsgiverperiode.add(dag)
            }

            if (sykedager >= 16) {
                state = UtenforArbeidsgiverperiodeSyk()
                if (sykedager > 16) {
                    state.visitSykedag(dag, draft)
                }
            }
        }

        override fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitSykHelgedag(dag, draft)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag, draft)
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            if (!dag.erHelg()) {
                state = InnenforArbeidsgiverperiodeFrisk()
                state.visitImplisittdag(dag, draft)
            } else {
                state = InnenforArbeidsperiodeFriPåfølgendeSykdom()
                state.visitImplisittdag(dag, draft)
            }
        }

        override fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitFeriedag(dag, draft)
        }
    }

    private inner class InnenforArbeidsgiverperiodeFrisk(antallFriskeDager: Int = 0) : SykedagerTellerTilstand {

        init {
            friskeDager += antallFriskeDager
        }

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag, draft)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag, draft)
        }

        override fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {
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

    private inner class InnenforArbeidsperiodeFriPåfølgendeSykdom : SykedagerTellerTilstand {
        //var antallFridagerFriPåfølgendeSykdom = 0
        val fridager: MutableList<Dag> = mutableListOf()

        override fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {
            fridager.add(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            state = InnenforArbeidsgiverperiodeFrisk(fridager.size)
            state.visitArbeidsdag(dag, draft)
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            if (!dag.erHelg()) {
                state = InnenforArbeidsgiverperiodeFrisk(fridager.size)
                state.visitImplisittdag(dag, draft)
            } else {
                fridager.add(dag)
            }
        }

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {

            val splitt = 16 - draft.arbeidsgiverperiode.size
            val fridagerIArbeidsgiverperioder = fridager.subList(0, min(splitt, fridager.size))
            val fridagerUtenforArbeidsgiverperioden = fridager.subList(min(splitt, fridager.size), fridager.size)

            draft.arbeidsgiverperiode.addAll(fridagerIArbeidsgiverperioder)
            draft.dagerEtterArbeidsgiverperiode.addAll(fridagerUtenforArbeidsgiverperioden)

            state = InnenforArbeidsgiverperiodeSyk(fridager.size)
            state.visitSykedag(dag, draft)
        }

        override fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {
            fridager.add(dag)
        }
    }

    private inner class UtenforArbeidsgiverperiodeSyk : SykedagerTellerTilstand {

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            friskeDager = 0
            draft.dagerEtterArbeidsgiverperiode.add(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitSykHelgedag(dag, draft)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag, draft)
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk()
            } else {
                state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            }
            state.visitImplisittdag(dag, draft)
        }

        override fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom()
            state.visitFeriedag(dag, draft)
        }
    }

    private inner class UtenforArbeidsgiverperiodeFrisk : SykedagerTellerTilstand {

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitFeriedag(dag: Feriedag, draft: SyketilfelleDraft) {
            tellFriskeDager()
        }

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag, draft)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitEgenmeldingsdag(dag, draft)
        }

        override fun visitSykHelgedag(dag: SykHelgedag, draft: SyketilfelleDraft) {
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

    private inner class UtenforArbeidsperiodeFriPåfølgendeSykdom : SykedagerTellerTilstand {

        override fun visitArbeidsdag(dag: Arbeidsdag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsgiverperiodeFrisk()
            state.visitArbeidsdag(dag, draft)
        }

        override fun visitImplisittdag(dag: ImplisittDag, draft: SyketilfelleDraft) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk()
                state.visitImplisittdag(dag, draft)
            }
        }

        override fun visitSykedag(dag: Sykedag, draft: SyketilfelleDraft) {
            state = UtenforArbeidsgiverperiodeSyk()
            state.visitSykedag(dag, draft)
        }
    }

    fun results(): List<Syketilfelle> = syketilfeller
}

