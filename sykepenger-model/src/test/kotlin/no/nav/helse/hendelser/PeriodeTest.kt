package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.etter
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class PeriodeTest {
    @Test
    internal fun `overlapper med periode`() {
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(20.juni, 10.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(20.juni, 1.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(2.juni, 7.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(1.juli, 12.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(10.juli, 12.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(30.juni, 11.juli)))
        assertTrue(Periode(1.juli, 10.juli).overlapperMed(Periode(1.juli, 10.juli)))
    }

    @Test
    internal fun `overlapper ikke med periode`() {
        assertFalse(Periode(1.juli, 10.juli).overlapperMed(Periode(1.april, 10.april)))
        assertFalse(Periode(1.juli, 10.juli).overlapperMed(Periode(1.april, 30.juni)))
        assertFalse(Periode(1.juli, 10.juli).overlapperMed(Periode(11.juli, 11.juli)))
        assertFalse(Periode(1.juli, 10.juli).overlapperMed(Periode(1.august, 10.august)))
    }

    @Test
    internal fun `er utenfor en annen periode`() {
        val other = Periode(3.juli, 4.juli)
        assertFalse(Periode(3.juli, 4.juli).utenfor(other))
        assertTrue(Periode(2.juli, 4.juli).utenfor(other))
        assertTrue(Periode(1.juli, 2.juli).utenfor(other))
        assertTrue(Periode(3.juli, 5.juli).utenfor(other))
        assertTrue(Periode(5.juli, 6.juli).utenfor(other))
    }

    @Test
    internal fun `fom kan være lik tom, men ikke før tom`() {
        assertDoesNotThrow { Periode(1.juli, 1.juli) }
        assertThrows<IllegalArgumentException> {
            Periode(2.juli, 1.juli)
        }
    }

    @Test
    internal fun `periode på eller etter dato`() {
        assertFalse(listOf(Periode(1.juli, 2.juli)).etter(3.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).etter(2.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).etter(1.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).etter(30.juni))
    }

    @Test
    internal fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar to 5.januar).merge(15.januar to 19.januar)
        assertSize(19, actual)
    }

    private fun assertSize(expected: Int, periode: Periode) {
        var count = 0
        periode.forEach { _ -> count++ }
        assertEquals(expected, count)

        count = 0
        for (date: LocalDate in periode) {
            count++
        }
        assertEquals(expected, count)
    }

    private infix fun LocalDate.to(other: LocalDate) = Periode(this, other)

}
