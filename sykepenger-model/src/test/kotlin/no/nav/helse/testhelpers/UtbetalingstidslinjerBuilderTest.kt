package no.nav.helse.testhelpers

import no.nav.helse.hendelser.til
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
    fun ukedager() {
        assertEquals(4.januar, 1.januar + 3.ukedager)
        assertEquals(5.januar, 2.januar + 3.ukedager)
        assertEquals(8.januar, 3.januar + 3.ukedager)
        assertEquals(8.januar, 5.januar + 1.ukedager)
        assertEquals(8.januar, 6.januar + 0.ukedager)
        assertEquals(9.januar, 6.januar + 1.ukedager)
        assertEquals(8.januar, 7.januar + 0.ukedager)
        assertEquals(9.januar, 7.januar + 1.ukedager)
        assertEquals(15.januar, 5.januar + 6.ukedager)
        assertEquals(28.desember, 16.januar + 248.ukedager)
        assertEquals(19.januar, 1.januar + 14.ukedager)
        assertEquals(22.januar, 2.januar + 14.ukedager)
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
        assertEquals(1.januar til 18.januar, tidslinje.periode())
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(14, tidslinje.inspektør.navDagTeller)
    }

    @Test
    fun `starte på fredag`() {
        val tidslinje = tidslinjeOf(14.NAVDAGER, startDato = 5.januar)
        assertRekkefølge(tidslinje)
        assertEquals(5.januar til 24.januar,tidslinje.periode())
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

    @Test
    fun `kun fredag`() {
        val tidslinje = tidslinjeOf(1.NAVDAGER, startDato = 5.januar)
        assertEquals(5.januar til 5.januar, tidslinje.periode())
    }

    private fun assertRekkefølge(tidslinje: Utbetalingstidslinje) {
        tidslinje.zipWithNext { left, right -> assertEquals(left.dato.plusDays(1L), right.dato)}
    }
}
