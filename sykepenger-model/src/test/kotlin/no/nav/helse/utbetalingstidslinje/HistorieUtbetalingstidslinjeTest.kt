package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HistorieUtbetalingstidslinjeTest : HistorieTest() {

    @Test
    fun `konvertering av utbetalingstidslinje erstatter ikke dager fra sykdomshistorikk`() {
        val bøtte = Historie.Historikkbøtte(true)
        bøtte.add(AG1, sykedager(8.januar, 12.januar))
        bøtte.add(AG1, arbeidsdager(5.januar, 9.januar))
        bøtte.add(AG1, navdager(10.januar, 11.januar))
        bøtte.sykdomstidslinje(AG1).also {
            assertTrue(it[5.januar] is Dag.Arbeidsdag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[9.januar] is Dag.Sykedag)
            assertTrue(it[12.januar] is Dag.Sykedag)
        }
    }

    @Test
    fun `sykdomstidslinje overskriver konvertert  utbetalingstidslinje`() {
        val bøtte = Historie.Historikkbøtte(true)
        bøtte.add(AG1, arbeidsdager(5.januar, 9.januar))
        bøtte.add(AG1, navdager(10.januar, 11.januar))
        bøtte.add(AG1, sykedager(8.januar, 12.januar))
        bøtte.sykdomstidslinje(AG1).also {
            assertTrue(it[5.januar] is Dag.Arbeidsdag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[9.januar] is Dag.Sykedag)
            assertTrue(it[12.januar] is Dag.Sykedag)
        }
    }

    @Test
    fun `utbetalingstidslinje for orgnr`() {
        val bøtte = Historie.Historikkbøtte()
        bøtte.add(tidslinje = tidslinjeOf(7.FRI))
        bøtte.add(AG1, tidslinjeOf(5.NAV, 2.HELG, startDato = 8.januar))
        bøtte.add(AG2, tidslinjeOf(5.NAV, 2.HELG, startDato = 15.januar))
        bøtte.utbetalingstidslinje(AG1).also {
            assertEquals(1.januar til 14.januar, it.periode())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is NavDag)
        }
        bøtte.utbetalingstidslinje(AG2).also {
            assertEquals(1.januar til 21.januar, it.periode())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is UkjentDag)
            assertTrue(it[15.januar] is NavDag)
            assertTrue(it[21.januar] is NavHelgDag)
        }
        bøtte.utbetalingstidslinje().also {
            assertEquals(1.januar til 21.januar, it.periode())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is NavDag)
            assertTrue(it[15.januar] is NavDag)
            assertTrue(it[21.januar] is NavHelgDag)
        }
    }
}
