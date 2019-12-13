package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.time.LocalDate

internal class UtgangspunktForBeregningAvYtelseVisitor : SykdomstidslinjeVisitor {
    private var førsteFraværsdagTilstand: FørsteFraværsdagTilstand = TrengerFørsteFraværsdag
    private var førsteFraværsdag: LocalDate? = null

    fun utgangspunktForBeregningAvYtelse() =
        førsteFraværsdag ?: throw IllegalStateException("Første fraværsdag er null")

    override fun visitSykedag(sykedag: Sykedag) {
        førsteFraværsdagTilstand.sykedag(this, sykedag.dagen)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        førsteFraværsdagTilstand.egenmeldingsdag(this, egenmeldingsdag.dagen)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        førsteFraværsdagTilstand.sykHelgedag(this, sykHelgedag.dagen)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        førsteFraværsdagTilstand.arbeidsdag(this)
    }

    override fun visitFeriedag(feriedag: Feriedag) {
        førsteFraværsdagTilstand.feriedag(this)
    }

    override fun visitPermisjonsdag(permisjonsdag: Permisjonsdag) {
        førsteFraværsdagTilstand.permisjonsdag(this)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        førsteFraværsdagTilstand.implisittdag(this)
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
        førsteFraværsdagTilstand.utenlandsdag(this)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
        førsteFraværsdagTilstand.ubestemtdag(this)
    }

    override fun visitStudiedag(studiedag: Studiedag) {
        førsteFraværsdagTilstand.studiedag(this)
    }

    private fun state(nyTilstand: FørsteFraværsdagTilstand) {
        førsteFraværsdagTilstand.leaving(this)
        førsteFraværsdagTilstand = nyTilstand
        førsteFraværsdagTilstand.entering(this)
    }

    private interface FørsteFraværsdagTilstand {
        fun sykedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {}
        fun egenmeldingsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {}
        fun sykHelgedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {}
        fun implisittdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun arbeidsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun feriedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun permisjonsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun studiedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(UgyldigTilstand)
        }
        fun utenlandsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(UgyldigTilstand)
        }
        fun ubestemtdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(UgyldigTilstand)
        }

        fun leaving(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun entering(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
    }

    private object TrengerFørsteFraværsdag: FørsteFraværsdagTilstand {
        override fun sykedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }

        override fun egenmeldingsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }

        override fun sykHelgedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }
    }

    private class HarFørsteFraværsdag(private val førsteFraværsdag: LocalDate): FørsteFraværsdagTilstand {
        override fun entering(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.førsteFraværsdag = førsteFraværsdag
        }

        override fun arbeidsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(TrengerFørsteFraværsdag)
        }

        override fun implisittdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(MuligOpphold(førsteFraværsdag))
        }

        override fun leaving(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.førsteFraværsdag = null
        }
    }

    private class MuligOpphold(private val kanskjeFørsteFraværsdag: LocalDate): FørsteFraværsdagTilstand {
        override fun arbeidsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(TrengerFørsteFraværsdag)
        }

        override fun feriedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(kanskjeFørsteFraværsdag))
        }

        override fun sykedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }

        override fun egenmeldingsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }

        override fun sykHelgedag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }
    }

    private object UgyldigTilstand: FørsteFraværsdagTilstand
}
