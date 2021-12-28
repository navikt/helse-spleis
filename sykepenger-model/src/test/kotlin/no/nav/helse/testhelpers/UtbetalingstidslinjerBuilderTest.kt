package no.nav.helse.testhelpers

import no.nav.helse.inspectors.inspektør
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjerBuilderTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `lager navdager i ukerdager og navhelg i helg`() {
        val tidslinje = tidslinjeOf(14.NAV)
        assertRekkefølge(tidslinje)
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(10, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `utelate dager`() {
        val tidslinje = tidslinjeOf(1.NAV, 6.UTELATE, 1.NAV)
        assertEquals(2, tidslinje.size)
        assertEquals(2, tidslinje.inspektør.navDagTeller)
        assertEquals(1.januar, tidslinje.first().dato)
        assertEquals(8.januar, tidslinje.last().dato)
    }

    @Test
    fun `sammenblanding`() {
        val tidslinje = tidslinjeOf(7.NAV, 5.ARB, 2.FRI, 5.AVV, 2.HELG, 14.NAVDAGER)
        assertRekkefølge(tidslinje)
        assertEquals(8, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(19, tidslinje.inspektør.navDagTeller)
        assertEquals(5, tidslinje.inspektør.arbeidsdagTeller)
        assertEquals(2, tidslinje.inspektør.fridagTeller)
        assertEquals(5, tidslinje.inspektør.avvistDagTeller)
    }

    @Test
    fun `lager gitt antall navdager, men tar med helg også`() {
        val tidslinje = tidslinjeOf(14.NAVDAGER)
        assertRekkefølge(tidslinje)
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(14, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `starte på fredag`() {
        val tidslinje = tidslinjeOf(14.NAVDAGER, startDato = 5.januar)
        assertRekkefølge(tidslinje)
        assertEquals(6, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(14, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `starte på lørdag`() {
        val tidslinje = tidslinjeOf(14.NAVDAGER, startDato = 6.januar)
        assertRekkefølge(tidslinje)
        assertEquals(6, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(14, tidslinje.inspektør.navDagTeller)
    }

    private fun assertRekkefølge(tidslinje: Utbetalingstidslinje) {
        tidslinje.zipWithNext { left, right -> assertEquals(left.dato.plusDays(1L), right.dato)}
    }
}
