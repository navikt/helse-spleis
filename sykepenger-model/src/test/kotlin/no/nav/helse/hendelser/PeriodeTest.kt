package no.nav.helse.hendelser

import no.nav.helse.fixtures.april
import no.nav.helse.fixtures.august
import no.nav.helse.fixtures.juli
import no.nav.helse.fixtures.juni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    internal fun `fom kan være lik tom, men ikke før tom`() {
        assertDoesNotThrow { Periode(1.juli, 1.juli) }
        assertThrows<IllegalArgumentException> {
            Periode(2.juli, 1.juli)
        }
    }
}
