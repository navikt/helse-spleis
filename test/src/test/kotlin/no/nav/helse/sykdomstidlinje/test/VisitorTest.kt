package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
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

        private val rapporteringshendelse = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
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

        override fun preVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            compositeCount += 1
        }

        override fun postVisitComposite(compositeSykdomstidslinje: CompositeSykdomstidslinje) {
            compositeCount += 1
        }

        override fun visitSykedag(sykedag: Sykedag) {
            sykedagerCount += 1
        }
    }
}
