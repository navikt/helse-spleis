package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.time.LocalDate

internal class UtgangspunktForBeregningAvYtelseVisitor : SykdomstidslinjeVisitor {
    private var førsteFraværsdagTilstand: FørsteFraværsdagTilstand = TrengerFørsteFraværsdag
    private var førsteFraværsdag: LocalDate? = null

    fun utgangspunktForBeregningAvYtelse() =
        førsteFraværsdag ?: throw IllegalStateException("Første fraværsdag er null")

    override fun visitSykedag(sykedag: Sykedag) {
        førsteFraværsdagTilstand.fraværsdag(this, sykedag.dagen)
    }

    override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag) {
        førsteFraværsdagTilstand.fraværsdag(this, egenmeldingsdag.dagen)
    }

    override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
        førsteFraværsdagTilstand.fraværsdag(this, sykHelgedag.dagen)
    }

    override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
        førsteFraværsdagTilstand.ikkeFraværsdag(this)
    }

    override fun visitImplisittDag(implisittDag: ImplisittDag) {
        førsteFraværsdagTilstand.ikkeFraværsdag(this)
    }

    override fun visitUtenlandsdag(utenlandsdag: Utenlandsdag) {
        førsteFraværsdagTilstand.ugyldigDag(this)
    }

    override fun visitUbestemt(ubestemtdag: Ubestemtdag) {
        førsteFraværsdagTilstand.ugyldigDag(this)
    }

    override fun visitStudiedag(studiedag: Studiedag) {
        førsteFraværsdagTilstand.ugyldigDag(this)
    }

    private fun state(nyTilstand: FørsteFraværsdagTilstand) {
        førsteFraværsdagTilstand.leaving(this)
        førsteFraværsdagTilstand = nyTilstand
        førsteFraværsdagTilstand.entering(this)
    }

    private interface FørsteFraværsdagTilstand {
        fun fraværsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {}

        fun ikkeFraværsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}

        fun ugyldigDag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(UgyldigTilstand)
        }

        fun leaving(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
        fun entering(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {}
    }

    private object TrengerFørsteFraværsdag: FørsteFraværsdagTilstand {
        override fun fraværsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor, dagen: LocalDate) {
            utgangspunktForBeregningAvYtelseVisitor.state(HarFørsteFraværsdag(dagen))
        }
    }

    private class HarFørsteFraværsdag(private val førsteFraværsdag: LocalDate): FørsteFraværsdagTilstand {
        override fun entering(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.førsteFraværsdag = førsteFraværsdag
        }

        override fun ikkeFraværsdag(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.state(TrengerFørsteFraværsdag)
        }

        override fun leaving(utgangspunktForBeregningAvYtelseVisitor: UtgangspunktForBeregningAvYtelseVisitor) {
            utgangspunktForBeregningAvYtelseVisitor.førsteFraværsdag = null
        }
    }

    private object UgyldigTilstand: FørsteFraværsdagTilstand
}
