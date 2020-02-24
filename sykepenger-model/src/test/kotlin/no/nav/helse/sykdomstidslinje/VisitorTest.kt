package no.nav.helse.sykdomstidslinje

import no.nav.helse.implisittDager
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.sykedager
import no.nav.helse.tournament.KonfliktskyDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VisitorTest {

    @Test
    fun flereSykdomstilfellerSlåttSammen() {
        val visitor = VisitorCounter()
        (3.sykedager + 1.implisittDager + 4.sykedager).accept(visitor)

        assertEquals(2, visitor.compositeCount)
        assertEquals(7, visitor.sykedagerCount)
        assertEquals(1, visitor.implisittDagerCount)
    }

    @Test
    fun enkeltSykdomstilfelle() {
        val visitor = VisitorCounter()
        3.sykedager.accept(visitor)

        assertEquals(2, visitor.compositeCount)
        assertEquals(3, visitor.sykedagerCount)
    }

    private class VisitorCounter : SykdomstidslinjeVisitor {
        var compositeCount = 0
        var sykedagerCount = 0
        var arbeidsdagerCount = 0
        var implisittDagerCount = 0

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            compositeCount += 1
        }

        override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            compositeCount += 1
        }

        override fun visitSykedag(sykedag: Sykedag.Sykmelding) {
            sykedagerCount += 1
        }

        override fun visitSykedag(sykedag: Sykedag.Søknad) {
            sykedagerCount += 1
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Inntektsmelding) {
            arbeidsdagerCount += 1
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) {
            arbeidsdagerCount += 1
        }

        override fun visitImplisittDag(implisittDag: ImplisittDag) {
            implisittDagerCount += 1
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            sykedagerCount += 1
        }
    }

    private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) = this.plus(other, ::ImplisittDag, KonfliktskyDagturnering)
}
