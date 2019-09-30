package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Arbeidsdag
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykHelgedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VisitorTest {
    companion object {
        private val uke1Mandag = LocalDate.of(2019, 9, 23)
        private val uke1Onsdag = LocalDate.of(2019, 9, 25)

        private val uke1Fredag = LocalDate.of(2019, 9, 27)
        private val uke2Mandag = LocalDate.of(2019, 9, 30)

        private val rapporteringshendelse = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
    }

    @Test
    fun flereSykdomstilfellerSlåttSammen() {
        val visitor = VisitorCounter()
        (Sykdomstidslinje.sykedager(uke1Mandag, uke1Onsdag, rapporteringshendelse) +
                Sykdomstidslinje.sykedager(uke1Fredag, uke2Mandag, rapporteringshendelse)).accept(visitor)

        assertEquals(2, visitor.compositeCount)
        assertEquals(7, visitor.sykedagerCount)
        assertEquals(1, visitor.arbeidsdagerCount)
    }

    @Test
    fun enkeltSykdomstilfelle() {
        val visitor = VisitorCounter()
        Sykdomstidslinje.sykedager(uke1Mandag, uke1Onsdag, rapporteringshendelse).accept(visitor)

        assertEquals(2, visitor.compositeCount)
        assertEquals(3, visitor.sykedagerCount)
    }

    private class VisitorCounter : SykdomstidslinjeVisitor {
        var compositeCount = 0
        var sykedagerCount = 0
        var arbeidsdagerCount = 0

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

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag) {
            sykedagerCount += 1
        }
    }
}
