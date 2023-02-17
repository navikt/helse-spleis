package no.nav.helse.utbetalingstidslinje

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


internal class HistorieEnArbeidsgiverTest : HistorieTest() {

    @Test
    fun `spleis - infotrygd - gap, ny agp - infotrygd (kort) - spleis`() {
        historie(
            utbetaling(1.februar, 28.februar),
            utbetaling(5.april, 10.april),
        )
        addTidligereUtbetaling(AG1, tidslinjeOf(16.AP, 15.NAV))
        addSykdomshistorikk(AG1, sykedager(11.april, 30.april))
        val utbetalingstidslinje = beregn(AG1, 1.januar, 5.april)
        assertAlleDager(utbetalingstidslinje, 1.januar til 16.januar, ArbeidsgiverperiodeDag::class)
        assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.mars til 4.april, Arbeidsdag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 5.april til 10.april, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 11.april til 30.april, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - spleis`() {
        historie(utbetaling(1.januar, 31.januar))
        addSykdomshistorikk(AG1, sykedager(1.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertAlleDager(utbetalingstidslinje, 1.januar til 31.januar, UkjentDag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `agp infotrygd - kort gap - spleis`() {
        historie(utbetaling(17.januar, 31.januar))
        addSykdomshistorikk(AG1, sykedager(1.januar, 16.januar))
        addSykdomshistorikk(AG1, sykedager(2.februar, 28.februar))
        val utbetalingstidslinje = beregn(AG1, 1.januar, 2.februar)
        assertAlleDager(utbetalingstidslinje, 2.februar til 17.februar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 18.februar til 28.februar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(
            utbetaling(17.januar, 31.januar),
            utbetaling(1.mars, 31.mars)
        )
        addTidligereUtbetaling(AG1, navdager(1.februar, 28.februar))
        addSykdomshistorikk(AG1, sykedager(1.april, 30.april))
        val utbetalingstidslinje = beregn(AG1, 17.januar)
        assertAlleDager(utbetalingstidslinje, 17.januar til 31.januar, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.februar til 28.februar, NavDag::class, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 1.mars til 31.mars, UkjentDag::class, Fridag::class)
        assertAlleDager(utbetalingstidslinje, 1.april til 30.april, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `infotrygd - ferie infotrygd - spleis`() {
        historie(
            utbetaling(1.januar, 31.januar),
            ferie(1.februar, 28.februar)
        )
        addSykdomshistorikk(AG1, sykedager(1.mars, 31.mars))
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(1.mars til 31.mars, utbetalingstidslinje.periode())
        assertAlleDager(utbetalingstidslinje, 1.mars til 31.mars, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `beregner ikke ny arbeidsgiverperiode etter brukerutbetaling`() {
        historie(
            utbetaling(1.januar, 30.april),
            utbetaling(1.mai, 30.mai)
        )
        addSykdomshistorikk(AG1, sykedager(1.juni, 30.juni))
        beregn(AG1, 1.januar, 1.juni).also {
            assertAlleDager(it, 1.juni til 30.juni, NavDag::class, NavHelgDag::class)
            assertEquals(1.juni til 30.juni, it.periode())
        }
    }

    @Test
    fun `infotrygd legger inn ferie i etterkant`() {
        historie(
            ferie(8.januar, 14.januar)
        )
        addSykdomshistorikk(AG1, 7.S + 7.F + 7.S)
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(21, utbetalingstidslinje.size)
        assertEquals(1.januar til 21.januar, utbetalingstidslinje.periode())
    }

    @Test
    fun `ferie midt i refusjon i infotrygd`() {
        historie(
            utbetaling(1.januar, 3.januar),
            ferie(4.januar, 6.januar),
            utbetaling(7.januar, 10.januar)
        )
        resetSeed(11.januar)
        addSykdomshistorikk(AG1, 4.S + 5.S + 2.S)
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(11, utbetalingstidslinje.size)
        assertAlleDager(utbetalingstidslinje, 11.januar til 21.januar, NavDag::class, NavHelgDag::class)
        assertEquals(11.januar til 21.januar, utbetalingstidslinje.periode())
    }

    @Test
    fun `ferie i spleis overlapper med utbetaling i infotrygd`() {
        historie(
            utbetaling(8.januar, 12.januar)
        )
        addSykdomshistorikk(AG1, 12.F)
        val utbetalingstidslinje = beregn(AG1, 8.januar)
        assertEquals(7, utbetalingstidslinje.size)
        assertAlleDager(utbetalingstidslinje, 1.januar til 7.januar, Fridag::class)
        assertEquals(1.januar til 7.januar, utbetalingstidslinje.periode())
    }

    @Test
    fun `overlapper helt med infotrygd`() {
        historie(
            ferie(1.januar, 7.januar),
            utbetaling(8.januar, 12.januar)
        )
        resetSeed(8.januar)
        addSykdomshistorikk(AG1, 5.S)
        val utbetalingstidslinje = beregn(AG1, 8.januar)
        assertEquals(0, utbetalingstidslinje.size)
    }

    @Test
    fun `fri etter infotrygdfri`() {
        historie(
            utbetaling(1.januar, 7.januar),
            ferie(8.januar, 14.januar)
        )
        resetSeed(15.januar)
        addSykdomshistorikk(AG1, 5.F)
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(5, utbetalingstidslinje.size)
        assertAlleDager(utbetalingstidslinje, 15.januar til 19.januar, Fridag::class)
    }

    @Test
    fun `navdag etter infotrygdfri`() {
        historie(
            utbetaling(1.januar, 7.januar),
            ferie(8.januar, 14.januar)
        )
        resetSeed(15.januar)
        addSykdomshistorikk(AG1, 5.S)
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(5, utbetalingstidslinje.size)
        assertAlleDager(utbetalingstidslinje, 15.januar til 19.januar, NavDag::class)
    }

    @Test
    fun `tidslinje med ekstra NAVdager etter en tidslinje som trekkes fra resulterer i en tidslinje med bare de siste NAVdagene`() {
        historie(
            ferie(1.januar, 7.januar),
            utbetaling(8.januar, 12.januar)
        )
        resetSeed(15.januar)
        addSykdomshistorikk(AG1, 7.S)
        val utbetalingstidslinje = beregn(AG1, 8.januar)
        assertEquals(7, utbetalingstidslinje.size)
        assertAlleDager(utbetalingstidslinje, 13.januar til 14.januar, NavHelgDag::class)
        assertAlleDager(utbetalingstidslinje, 15.januar til 19.januar, NavDag::class)
    }

    @Test
    fun `beholder ikke infotrygdferie selv om den avsluttes på en søndag`() {
        historie(
            utbetaling(1.januar, 7.januar),
            ferie(8.januar, 14.januar),
        )
        resetSeed(15.januar)
        addSykdomshistorikk(AG1, 7.F)
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertAlleDager(utbetalingstidslinje, 15.januar til 21.januar, Fridag::class)
        assertEquals(15.januar til 21.januar, utbetalingstidslinje.periode())
        assertEquals(7, utbetalingstidslinje.size)
    }

    @Test
    fun `fjerner overlapp`() {
        historie(
            utbetaling(1.januar, 7.januar)
        )
        addSykdomshistorikk(AG1, sykedager(1.januar, 7.januar))
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(0, utbetalingstidslinje.size)
    }

    @Test
    fun `ledende infotrygddager utfyller tidslinje`() {
        historie(
            ferie(8.januar, 9.januar)
        )
        addSykdomshistorikk(AG1, sykedager(1.januar, 14.januar))
        val utbetalingstidslinje = beregn(AG1, 1.januar)
        assertEquals(1.januar til 14.januar, utbetalingstidslinje.periode())
        assertEquals(14, utbetalingstidslinje.size)
    }
}
