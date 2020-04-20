package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.NyDag.NyArbeidsdag
import no.nav.helse.sykdomstidslinje.NyDag.NyUkjentDag
import no.nav.helse.sykdomstidslinje.SykdomstidlinjeEkspanderingTest.TestSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SykdomstidlinjeEkspanderingTest {
    @Test
    internal fun `ekspanderer tom tidslinje`() {
        val actual = NySykdomstidslinje().merge(1.januar to 5.januar)

        assertEquals(Periode(1.januar, 5.januar), actual.periode())
        assertTrue(actual[3.januar] is NyArbeidsdag)
    }

    @Test
    internal fun `ekspanderer med tidslinje`() {
        val actual = (1.januar to 5.januar).merge(NySykdomstidslinje())

        assertEquals(Periode(1.januar, 5.januar), actual.periode())
        assertTrue(actual[3.januar] is NyArbeidsdag)
    }

    @Test
    internal fun `ekspanderer tom tidslinje med tom tidslinje`() {
        val actual = NySykdomstidslinje().merge(NySykdomstidslinje())

        assertNull(actual.periode())
    }

    @Test
    internal fun `legge sammen to perioder uten overlapp`() {
        val actual = (1.januar to 5.januar).merge(15.januar to 19.januar)

        assertEquals(Periode(1.januar, 19.januar), actual.periode())
        assertTrue(actual[3.januar] is NyArbeidsdag)
        assertTrue(actual[17.januar] is NyArbeidsdag)
        assertTrue(actual[10.januar] is NyUkjentDag)
    }

    @Test
    internal fun `kan subsette en tidslinje`() {
        val original = (1.januar to 5.januar).merge(15.januar to 19.januar)
        Periode(3.januar, 17.januar).also {
            assertEquals(it, original.subset(it).periode())
        }

        Periode(3.januar, 10.januar).also {
            assertEquals(it, original.subset(it).periode())
        }

        Periode(6.januar, 14.januar).also {
            assertEquals(it, original.subset(it).periode())
        }
    }

    @Test
    internal fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar to 5.januar).merge(15.januar to 19.januar)
        assertSize(19, actual)
        assertSize(15, actual.subset(Periode(3.januar, 17.januar)))
        assertSize(9, actual.subset(Periode(6.januar, 14.januar)))
    }

    private fun assertSize(expected: Int, sykdomstidslinje: NySykdomstidslinje) {
        var count = 0
        sykdomstidslinje.forEach { _ -> count++ }
        assertEquals(expected, count)

        count = 0
        for (dag: NyDag in sykdomstidslinje) {
             count++
        }
        assertEquals(expected, count)
    }

    internal class TestSykdomstidslinje(private val førsteDato: LocalDate, private val sisteDato: LocalDate) {
        internal fun asNySykdomstidslinje() = NySykdomstidslinje.arbeidsdager(førsteDato, sisteDato)

        fun merge(annen: TestSykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)
        fun merge(annen: NySykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)
    }

    private infix fun LocalDate.to(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato)
}

internal fun NySykdomstidslinje.merge(testTidslinje: TestSykdomstidslinje): NySykdomstidslinje = this.merge(testTidslinje.asNySykdomstidslinje())
