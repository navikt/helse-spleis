package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VisitorTest {
    private val sendtSøknad = Testhendelse(Uke(4).fredag.atTime(12, 0))

    @Test
    fun flereSykdomstilfellerSlåttSammen() {
        val visitor = VisitorCounter()
        (Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, sendtSøknad) +
                Sykdomstidslinje.sykedager(Uke(1).fredag, Uke(2).mandag, sendtSøknad)).accept(visitor)

        assertEquals(2, visitor.compositeCount)
        assertEquals(7, visitor.sykedagerCount)
        assertEquals(1, visitor.implisittDagerCount)
    }

    @Test
    fun enkeltSykdomstilfelle() {
        val visitor = VisitorCounter()
        Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, sendtSøknad).accept(visitor)

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

        override fun visitSykedag(sykedag: Sykedag) {
            sykedagerCount += 1
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag) {
            arbeidsdagerCount += 1
        }

        override fun visitImplisittDag(implisittDag: ImplisittDag) {
            implisittDagerCount += 1
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            sykedagerCount += 1
        }
    }
}
