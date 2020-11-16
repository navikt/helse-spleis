package no.nav.helse.person

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
    fun `fri i helg mappes til ukjentdag`() {
        val tidslinje = tidslinjeOf(4.NAV, 3.FRI, 5.NAV, 2.HELG)
        Historie.Historikkbøtte.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[5.januar] is Dag.Feriedag)
            assertTrue(it[6.januar] is Dag.UkjentDag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[13.januar] is Dag.SykHelgedag)
        }
    }

    @Test
    fun `mapper utbetalingstidslinje til sykdomstidslinje`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV, 4.ARB)
        Historie.Historikkbøtte.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[31.januar] is Dag.Sykedag)
            assertTrue(it[1.februar] is Dag.Arbeidsdag)
            assertTrue(it[4.februar] is Dag.Arbeidsdag)
        }
    }

    @Test
    fun `konverterer feriedager, avviste dager og foreldet dager`() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 5.FOR, 2.FRI, 5.AVV, 7.FRI)
        Historie.Historikkbøtte.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[8.januar] is Dag.ForeldetSykedag)
            assertTrue(it[15.januar] is Dag.Sykedag)
            assertTrue(it[22.januar] is Dag.Feriedag)
        }
    }

    @Test
    fun `utbetalingstidslinje for orgnr`() {
        val bøtte = Historie.Historikkbøtte()
        bøtte.add(tidslinje = tidslinjeOf(7.FRI))
        bøtte.add(AG1, tidslinjeOf(5.NAV, 2.HELG, startDato = 8.januar))
        bøtte.add(AG2, tidslinjeOf(5.NAV, 2.HELG, startDato = 15.januar))
        bøtte.utbetalingstidslinje(AG1).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(14.januar, it.sisteDato())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is NavDag)
        }
        bøtte.utbetalingstidslinje(AG2).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(21.januar, it.sisteDato())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is UkjentDag)
            assertTrue(it[15.januar] is NavDag)
            assertTrue(it[21.januar] is NavHelgDag)
        }
        bøtte.utbetalingstidslinje().also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(21.januar, it.sisteDato())
            assertTrue(it[1.januar] is Fridag)
            assertTrue(it[8.januar] is NavDag)
            assertTrue(it[15.januar] is NavDag)
            assertTrue(it[21.januar] is NavHelgDag)
        }
    }

    @Test
    fun `samlet utbetalingstidslinje`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.utbetalingstidslinje(1.februar til 28.februar).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(28.februar, it.sisteDato())
        }
        historie.utbetalingstidslinje(1.januar til 21.januar).also {
            assertEquals(1.januar, it.førsteDato())
            assertEquals(21.januar, it.sisteDato())
        }
    }
}
