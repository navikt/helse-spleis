package no.nav.helse.person

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Test

internal class HistorieEnArbeidsgiverTest : HistorieTest() {

    @Test
    fun `spleis - infotrygd - gap, ny agp - infotrygd (kort) - spleis`() {
        historie(
            refusjon(1.februar, 28.februar),
            refusjon(5.april, 10.april),
        )
        historie.add(AG1, tidslinjeOf(16.AP, 15.NAV))
        historie.add(AG1, sykedager(11.april, 30.april))
        val utbetalingstidslinje = beregn(AG1, 11.april til 30.april, 1.januar, 5.april)
        assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.mars til 4.april, Arbeidsdag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 5.april til 10.april, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 11.april til 30.april, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 1.februar til 28.februar, 1.januar)
        assertAlleDager(utbetalingstidslinje, 1.januar til 31.januar, UkjentDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - kort gap - spleis`() {
        historie(refusjon(17.januar, 31.januar))
        historie.add(AG1, sykedager(1.januar, 16.januar))
        historie.add(AG1, sykedager(2.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 2.februar til 28.februar, 1.januar, 1.februar)
        assertAlleDager(utbetalingstidslinje, 2.februar til 17.februar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 18.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(
            refusjon(17.januar, 31.januar),
            refusjon(1.mars, 31.mars)
        )
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.april, 30.april))
        val utbetalingstidslinje = beregn(AG1, 1.april til 30.april, 1.januar)
        assertAlleDager(utbetalingstidslinje, 1.januar til 31.januar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.januar til 31.januar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.april til 30.april, NavDag::class, NavHelgDag::class)
    }
}
