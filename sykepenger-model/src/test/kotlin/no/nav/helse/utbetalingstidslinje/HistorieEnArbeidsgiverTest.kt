package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `infotrygd - spleis - spleis - kort gap - spleis`() {
        historie(
            refusjon(17.januar, 20.januar),
            refusjon(21.januar, 31.januar)
        )
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, navdager(1.mars, 31.mars))
        historie.add(AG1, sykedager(1.januar, 16.januar))
        historie.add(AG1, sykedager(10.april, 30.april))
        val utbetalingstidslinje = beregn(AG1, 1.april til 30.april, 17.januar, 10.april)
        assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, UkjentDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 31.mars, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.april til 9.april, Arbeidsdag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 10.april til 30.april, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 1.februar til 28.februar, 1.januar)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.januar til 28.februar, 1.januar)
        assertAlleDager(utbetalingstidslinje, 1.januar til 31.januar, UkjentDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - kort gap - spleis`() {
        historie(refusjon(17.januar, 31.januar))
        historie.add(AG1, sykedager(1.januar, 16.januar))
        historie.add(AG1, sykedager(2.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 2.februar til 28.februar, 1.januar, 1.februar)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.januar til 16.januar, 1.januar)
        assertSkjæringstidspunkt(utbetalingstidslinje, 17.januar til 31.januar, null)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.februar til 1.februar, null)
        assertSkjæringstidspunkt(utbetalingstidslinje, 2.februar til 28.februar, 2.februar)
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
        assertSkjæringstidspunkt(utbetalingstidslinje, 17.januar til 31.januar, null)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.mars til 31.mars, null)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.februar til 28.februar, 17.januar)
        assertSkjæringstidspunkt(utbetalingstidslinje, 1.april til 30.april, 17.januar)
        assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.mars til 31.mars, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.april til 30.april, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `spleis - infotrygd - ferie infotrygd - spleis`() {
        historie(
            refusjon(1.januar, 31.januar),
            ferie(1.februar, 28.februar)
        )
        historie.add(AG1, navdager(1.desember(2017), 31.desember(2017)))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        val utbetalingstidslinje = beregn(AG1, 1.mars til 31.mars, 1.januar)
        assertEquals(1.desember(2017), utbetalingstidslinje.førsteDato())
        assertAlleDager(utbetalingstidslinje, 1.desember(2017) til 16.desember(2017), ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinje, 17.desember(2017) til 31.desember(2017), NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.januar til 28.februar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.mars til 31.mars, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `infotrygd - ferie infotrygd - spleis`() {
        historie(
            refusjon(1.januar, 31.januar),
            ferie(1.februar, 28.februar)
        )
        historie.add(AG1, sykedager(1.mars, 31.mars))
        val utbetalingstidslinje = beregn(AG1, 1.mars til 31.mars, 1.januar)
        assertEquals(1.mars, utbetalingstidslinje.førsteDato())
        assertAlleDager(utbetalingstidslinje, 1.mars til 31.mars, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `Historisk utbetaling til bruker skal ikke bli med i utbetalingstidslinje for arbeidsgiver`() {
        historie(bruker(1.januar, 31.januar))
        historie.add(AG1, tidslinjeOf(2.NAV, 2.HELG, 5.NAV, startDato = 1.mars))
        beregn(AG1, 1.mars til 9.mars, 1.mars).also {
            assertEquals(1.mars, it.førsteDato())
            assertEquals(9.mars, it.sisteDato())
        }
    }

    @Test
    fun `beregner ikke ny arbeidsgiverperiode etter brukerutbetaling`() {
        historie(
            refusjon(1.januar, 30.april),
            bruker(1.mai, 30.mai)
        )
        historie.add(AG1, sykedager(1.juni, 30.juni))
        beregn(AG1, 1.mars til 1.juni, 30.juni).also {
            assertAlleDager(it, 1.juni til 30.juni, NavDag::class, NavHelgDag::class)
            assertEquals(1.juni, it.førsteDato())
            assertEquals(30.juni, it.sisteDato())
        }
    }

    @Test
    fun `beregner historikkperiode`() {
        historieUtenSpleisbøtte(
            refusjon(1.januar, 31.januar, orgnr = AG1)
        )
        assertEquals(31.januar, historie.periodeTom(AG1))
    }
}
