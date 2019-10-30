package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*

private val ARBEIDSGIVERPERIODE = 16

internal class SyketilfelleSplitter : SykdomstidslinjeVisitor {
    private var state: SykedagerTellerTilstand = Starttilstand()
    private val syketilfeller = mutableListOf<Syketilfelle>()


    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        state.visitArbeidsdag(arbeidsdag)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        state.visitImplisittdag(implisittDag)
    }

    override fun visitFeriedag(feriedag: Feriedag) {
        state.visitFeriedag(feriedag)
    }

    override fun visitSykedag(sykedag: Sykedag) {
        state.visitSykedag(sykedag)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        state.visitEgenmeldingsdag(egenmeldingsdag)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        state.visitSykHelgedag(sykHelgedag)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) =
        throw RuntimeException("Det er ikke mulig å beregne arbeidsgiverperioder for tidslinjer med ubestemte dager, som f.eks ${ubestemtdag.dagen}")

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) =
        throw RuntimeException("Det er ikke mulig å beregne arbeidsgiverperioder for tidslinjer med permisjonsdager, som f.eks ${permisjonsdag.dagen}")

    override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
        pushResultat()
    }

    fun pushResultat() = state.syketilfelle()?.apply { syketilfeller.add(this) }

    private abstract class SykedagerTellerTilstand(val draft: SyketilfelleDraft) {

        open fun visitArbeidsdag(dag: Arbeidsdag) {}
        open fun visitImplisittdag(dag: ImplisittDag) {}
        open fun visitSykedag(dag: Sykedag) {}
        open fun visitSykHelgedag(dag: SykHelgedag) {}
        open fun visitFeriedag(dag: Feriedag) {}
        open fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {}
        internal fun syketilfelle() = draft.syketilfelle()
    }

    private class SyketilfelleDraft(
        private var arbeidsgiverperiode: MutableSet<Dag> = mutableSetOf(),
        private var dagerEtterArbeidsgiverperiode: MutableSet<Dag> = mutableSetOf()
    ) {

        internal fun addDag(dag: Dag) {
            if (dag !in (arbeidsgiverperiode + dagerEtterArbeidsgiverperiode)) {
                if (erArbeidsgiverperiodeBruktOpp()) {
                    dagerEtterArbeidsgiverperiode.add(dag)
                } else {
                    arbeidsgiverperiode.add(dag)
                }
            }
        }

        fun syketilfelle() =
            if (dagerEtterArbeidsgiverperiode.isNotEmpty() || arbeidsgiverperiode.isNotEmpty()) {
                Syketilfelle(
                    arbeidsgiverperiode = CompositeSykdomstidslinje(arbeidsgiverperiode.toList()),
                    dagerEtterArbeidsgiverperiode = CompositeSykdomstidslinje(dagerEtterArbeidsgiverperiode.toList())
                )
            } else null

        fun erArbeidsgiverperiodeBruktOpp() = arbeidsgiverperiode.size >= ARBEIDSGIVERPERIODE

        fun copy() = SyketilfelleDraft(
            arbeidsgiverperiode = arbeidsgiverperiode.copy(),
            dagerEtterArbeidsgiverperiode = dagerEtterArbeidsgiverperiode.copy()
        )

        fun Set<Dag>.copy() = this.mapTo(destination = mutableSetOf(), transform = { it })
    }


    private inner class Starttilstand : SykedagerTellerTilstand(draft = SyketilfelleDraft()) {

        override fun visitSykedag(dag: Sykedag) {
            state = InnenforArbeidsgiverperiodeSyk(draft)
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = InnenforArbeidsgiverperiodeSyk(draft)
            state.visitEgenmeldingsdag(dag)
        }
    }

    private inner class InnenforArbeidsgiverperiodeSyk(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        override fun visitSykedag(dag: Sykedag) {
            draft.addDag(dag)

            if (draft.erArbeidsgiverperiodeBruktOpp()) {
                state = UtenforArbeidsgiverperiodeSyk(draft)
                state.visitSykedag(dag)
            }
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            state.visitSykHelgedag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = InnenforArbeidsgiverperiodeFrisk(draft, 0)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            state = if (!dag.erHelg()) {
                InnenforArbeidsgiverperiodeFrisk(draft, 0)
            } else {
                InnenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            }

            state.visitImplisittdag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            state = InnenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            state.visitFeriedag(dag)
        }
    }

    private inner class InnenforArbeidsgiverperiodeFrisk(draft: SyketilfelleDraft, var friskeDager: Int) : SykedagerTellerTilstand(draft) {

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
            state = InnenforArbeidsgiverperiodeSyk(draft)
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = InnenforArbeidsgiverperiodeSyk(draft)
            state.visitEgenmeldingsdag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            tellFriskeDager()
        }

        private fun tellFriskeDager() {
            friskeDager++
            if (friskeDager >= ARBEIDSGIVERPERIODE) {
                pushResultat()
                state = Starttilstand()
            }
        }
    }

    private inner class InnenforArbeidsperiodeFriPåfølgendeSykdom(draft: SyketilfelleDraft) :
        SykedagerTellerTilstand(draft) {

        val draftForSykEtterFri = draft.copy()

        val fridager: MutableList<Dag> = mutableListOf()

        override fun visitFeriedag(dag: Feriedag) {
            fridager.add(dag)
            draftForSykEtterFri.addDag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = InnenforArbeidsgiverperiodeFrisk(draft, fridager.size)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = InnenforArbeidsgiverperiodeFrisk(draft, fridager.size)
                state.visitImplisittdag(dag)
            } else {
                fridager.add(dag)
                draftForSykEtterFri.addDag(dag)
            }
        }

        override fun visitSykedag(dag: Sykedag) {
            state = InnenforArbeidsgiverperiodeSyk(draftForSykEtterFri)
            state.visitSykedag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            fridager.add(dag)
            draftForSykEtterFri.addDag(dag)
        }
    }

    private inner class UtenforArbeidsgiverperiodeSyk(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        override fun visitSykedag(dag: Sykedag) {
            draft.addDag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            draft.addDag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = UtenforArbeidsgiverperiodeFrisk(draft)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk(draft)
                state.visitImplisittdag(dag)
            } else {
                draft.addDag(dag)
            }
        }

        override fun visitFeriedag(dag: Feriedag) {
           draft.addDag(dag)
        }
    }

    private inner class UtenforArbeidsgiverperiodeFrisk(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        var friskeDager = 0

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
            state = UtenforArbeidsgiverperiodeSyk(draft)
            state.visitSykedag(dag)
        }

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag) {
            state = UtenforArbeidsgiverperiodeSyk(draft)
            state.visitEgenmeldingsdag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            tellFriskeDager()
        }

        private fun tellFriskeDager() {
            friskeDager++
            if (friskeDager >= ARBEIDSGIVERPERIODE) {
                pushResultat()
                state = Starttilstand()
            }
        }
    }
    fun results(): List<Syketilfelle> = syketilfeller
}

