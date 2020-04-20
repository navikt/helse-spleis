package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.SykdomstidlinjeEkspanderingTest.TestSykdomstidslinje
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SykdomstidlinjeEkspanderingTest {
    @Test
    internal fun `ekspanderer tom tidslinje`() {
        val actual = NySykdomstidlinje().append(1.januar to 5.januar)

        assertEquals(Periode(1.januar, 5.januar), actual.periode())
        assertTrue(actual[3.januar] is NyArbeidsdag)
    }

    internal class TestSykdomstidslinje(private val førsteDato: LocalDate, private val sisteDato: LocalDate) {
        internal fun asNySykdomstidslinje() = NySykdomstidlinje.arbeidsdager(førsteDato, sisteDato)
    }

    private infix fun LocalDate.to(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato)
}

internal fun NySykdomstidlinje.append(testTidslinje: TestSykdomstidslinje): NySykdomstidlinje = this.merge(testTidslinje.asNySykdomstidslinje())
