package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.*
import java.time.LocalDate

internal class FørsteFraværsdagVisitor : SykdomstidslinjeVisitor {
    private var førsteFraværsdagTilstand: FørsteFraværsdagTilstand = TrengerFørsteFraværsdag
    private var førsteFraværsdag: LocalDate? = null

    fun førsteFraværsdag() =
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
        fun fraværsdag(førsteFraværsdagVisitor: FørsteFraværsdagVisitor, dagen: LocalDate) {}

        fun ikkeFraværsdag(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {}

        fun ugyldigDag(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {
            førsteFraværsdagVisitor.state(UgyldigTilstand)
        }

        fun leaving(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {}
        fun entering(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {}
    }

    private object TrengerFørsteFraværsdag: FørsteFraværsdagTilstand {
        override fun fraværsdag(førsteFraværsdagVisitor: FørsteFraværsdagVisitor, dagen: LocalDate) {
            førsteFraværsdagVisitor.state(HarFørsteFraværsdag(dagen))
        }
    }

    private class HarFørsteFraværsdag(private val førsteFraværsdag: LocalDate): FørsteFraværsdagTilstand {
        override fun entering(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {
            førsteFraværsdagVisitor.førsteFraværsdag = førsteFraværsdag
        }

        override fun ikkeFraværsdag(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {
            førsteFraværsdagVisitor.state(TrengerFørsteFraværsdag)
        }

        override fun leaving(førsteFraværsdagVisitor: FørsteFraværsdagVisitor) {
            førsteFraværsdagVisitor.førsteFraværsdag = null
        }
    }

    private object UgyldigTilstand: FørsteFraværsdagTilstand
}
