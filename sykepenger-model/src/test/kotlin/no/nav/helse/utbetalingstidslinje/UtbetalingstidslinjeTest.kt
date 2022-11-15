package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FOR
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UKJ
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    @Test
    fun `avviser perioder med flere begrunnelser`() {
        val periode = 1.januar til 5.januar
        tidslinjeOf(5.NAV).also {
            Utbetalingstidslinje.avvis(listOf(it), listOf(periode), listOf(Begrunnelse.MinimumSykdomsgrad))
            Utbetalingstidslinje.avvis(listOf(it), listOf(periode), listOf(Begrunnelse.EtterDødsdato))
            Utbetalingstidslinje.avvis(listOf(it), listOf(periode), listOf(Begrunnelse.ManglerMedlemskap))
            periode.forEach { dato ->
                val dag = it[dato] as AvvistDag
                assertEquals(3, dag.begrunnelser.size)
            }
        }
    }

    @Test
    fun `samlet periode`() {
        assertEquals(1.januar til 1.januar, Utbetalingstidslinje.periode(listOf(tidslinjeOf(1.NAV))))
        assertEquals(1.desember(2017) til 7.mars, Utbetalingstidslinje.periode(listOf(
            tidslinjeOf(7.NAV),
            tidslinjeOf(7.NAV, startDato = 1.mars),
            tidslinjeOf(7.NAV, startDato = 1.desember(2017)),
        )))
    }

    @Test
    fun `sammenhengende perioder brytes opp av arbeidsdager`() {
        val tidslinje = tidslinjeOf(5.NAV, 1.ARB, 5.NAV)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `sammenhengende perioder brytes opp av ukjent dag`() {
        val tidslinje = tidslinjeOf(5.NAV, 1.UKJ, 5.NAV)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `fjerner ledende fridager`() {
        val tidslinje = tidslinjeOf(6.FRI, 5.NAV)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(7.januar til 11.januar, result.first().periode())
    }

    @Test
    fun `helg blir ikke sett på som en periode`() {
        val tidslinje = tidslinjeOf(5.ARB, 2.NAV, 5.ARB)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `foreldet perioder tas med`() {
        val tidslinje = tidslinjeOf(5.NAV, 5.FOR)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(1.januar til 10.januar, result.first().periode())
    }
}
