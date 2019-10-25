package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import kotlin.math.min

data class Syketilfelle(
    val arbeidsgiverperiode: Sykdomstidslinje?,
    val dagerEtterArbeidsgiverperiode: Sykdomstidslinje?
){
    val tidslinje
    get() = when {
        dagerEtterArbeidsgiverperiode != null -> arbeidsgiverperiode?.plus(dagerEtterArbeidsgiverperiode) ?: dagerEtterArbeidsgiverperiode
        else -> arbeidsgiverperiode
    }
}

private class SyketilfelleDraft(
    var arbeidsgiverperiode: MutableList<Dag> = mutableListOf(),
    var dagerEtterArbeidsgiverperiode: MutableList<Dag> = mutableListOf()
)

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
        fun syketilfelle() =
            if ((draft.dagerEtterArbeidsgiverperiode.isNotEmpty() || draft.arbeidsgiverperiode.isNotEmpty())){
                Syketilfelle(
                    arbeidsgiverperiode = CompositeSykdomstidslinje(draft.arbeidsgiverperiode),
                    dagerEtterArbeidsgiverperiode = CompositeSykdomstidslinje(draft.dagerEtterArbeidsgiverperiode).trim())
            }else null


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

            if (draft.arbeidsgiverperiode.size < 16) {
                draft.arbeidsgiverperiode.add(dag)
            }

            if (draft.arbeidsgiverperiode.size >= 16) {
                state = UtenforArbeidsgiverperiodeSyk(draft)
                if (draft.dagerEtterArbeidsgiverperiode.isNotEmpty()) {
                    state.visitSykedag(dag)
                }
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
            if (!dag.erHelg()) {
                state = InnenforArbeidsgiverperiodeFrisk(draft, 0)
                state.visitImplisittdag(dag)
            } else {
                state = InnenforArbeidsperiodeFriPåfølgendeSykdom(draft)
                state.visitImplisittdag(dag)
            }
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
            if (friskeDager >= 16) {
                pushResultat()
                state = Starttilstand()
            }
        }
    }

    private inner class InnenforArbeidsperiodeFriPåfølgendeSykdom(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        //var antallFridagerFriPåfølgendeSykdom = 0
        val fridager: MutableList<Dag> = mutableListOf()

        override fun visitFeriedag(dag: Feriedag) {
            fridager.add(dag)
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
            }
        }

        override fun visitSykedag(dag: Sykedag) {

            val splitt = 16 - draft.arbeidsgiverperiode.size
            val fridagerIArbeidsgiverperioder = fridager.subList(0, min(splitt, fridager.size))
            val fridagerUtenforArbeidsgiverperioden = fridager.subList(min(splitt, fridager.size), fridager.size)

            draft.arbeidsgiverperiode.addAll(fridagerIArbeidsgiverperioder)
            draft.dagerEtterArbeidsgiverperiode.addAll(fridagerUtenforArbeidsgiverperioden)

            state = InnenforArbeidsgiverperiodeSyk(draft)
            state.visitSykedag(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            fridager.add(dag)
        }
    }

    private inner class UtenforArbeidsgiverperiodeSyk(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        override fun visitSykedag(dag: Sykedag) {
            draft.dagerEtterArbeidsgiverperiode.add(dag)
        }

        override fun visitSykHelgedag(dag: SykHelgedag) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            state.visitSykHelgedag(dag)
        }

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = UtenforArbeidsgiverperiodeFrisk(draft)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk(draft)
            } else {
                state = UtenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            }
            state.visitImplisittdag(dag)
        }

        override fun visitFeriedag(dag: Feriedag) {
            state = UtenforArbeidsperiodeFriPåfølgendeSykdom(draft)
            state.visitFeriedag(dag)
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
            if (friskeDager >= 16) {
                pushResultat()
                state = Starttilstand()
            }
        }
    }

    private inner class UtenforArbeidsperiodeFriPåfølgendeSykdom(draft: SyketilfelleDraft) : SykedagerTellerTilstand(draft) {

        override fun visitArbeidsdag(dag: Arbeidsdag) {
            state = UtenforArbeidsgiverperiodeFrisk(draft)
            state.visitArbeidsdag(dag)
        }

        override fun visitImplisittdag(dag: ImplisittDag) {
            if (!dag.erHelg()) {
                state = UtenforArbeidsgiverperiodeFrisk(draft)
                state.visitImplisittdag(dag)
            }
        }

        override fun visitSykedag(dag: Sykedag) {
            state = UtenforArbeidsgiverperiodeSyk(draft)
            state.visitSykedag(dag)
        }
    }

    fun results(): List<Syketilfelle> = syketilfeller
}

