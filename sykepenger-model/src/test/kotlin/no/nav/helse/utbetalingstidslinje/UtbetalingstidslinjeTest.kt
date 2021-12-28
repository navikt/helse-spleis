package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    @Test
    fun `avviser med flere begrunnelser`() {
        tidslinjeOf(5.NAVv2).also {
            Utbetalingstidslinje.avvis(listOf(it), mapOf(1.januar to listOf(Begrunnelse.MinimumSykdomsgrad)))
            Utbetalingstidslinje.avvis(listOf(it), mapOf(1.januar to listOf(Begrunnelse.EtterDødsdato)))
            Utbetalingstidslinje.avvis(listOf(it), mapOf(1.januar to listOf(Begrunnelse.ManglerMedlemskap)))
            val dag = it[1.januar]
            assertTrue(dag is AvvistDag)
            assertEquals(3, (dag as AvvistDag).begrunnelser.size)
        }
    }

    @Test
    fun `samlet periode`() {
        assertEquals(1.januar til 1.januar, Utbetalingstidslinje.periode(listOf(tidslinjeOf(1.NAVv2))))
        assertEquals(1.desember(2017) til 7.mars, Utbetalingstidslinje.periode(listOf(
            tidslinjeOf(7.NAVv2),
            tidslinjeOf(7.NAVv2, startDato = 1.mars),
            tidslinjeOf(7.NAVv2, startDato = 1.desember(2017)),
        )))
    }

    @Test
    fun `betalte dager`() {
        tidslinjeOf(4.AP, 1.FRI, 6.NAVv2, 1.AVV, 2.FRI, 5.ARB).also {
            assertTrue(it.harBetalt(1.januar))
            assertFalse(it.harBetalt(5.januar))
            assertTrue(it.harBetalt(6.januar))
            assertTrue(it.harBetalt(7.januar))
            assertTrue(it.harBetalt(12.januar))
            assertFalse(it.harBetalt(15.januar))
            assertTrue(it.harBetalt(1.januar til 7.januar))
            assertFalse(it.harBetalt(13.januar til 20.januar))
        }
    }


    @Test
    fun `fri i helg mappes til ukjentdag`() {
        val tidslinje = tidslinjeOf(4.NAVv2, 3.FRI, 7.NAVv2)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[5.januar] is Dag.Feriedag)
            assertTrue(it[6.januar] is Dag.UkjentDag)
            assertTrue(it[8.januar] is Dag.Sykedag)
            assertTrue(it[13.januar] is Dag.SykHelgedag)
        }
    }

    @Test
    fun `mapper utbetalingstidslinje til sykdomstidslinje`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAVv2, 4.ARB)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[31.januar] is Dag.Sykedag)
            assertTrue(it[1.februar] is Dag.Arbeidsdag)
            assertTrue(it[4.februar] is Dag.Arbeidsdag)
        }
    }

    @Test
    fun `konverterer feriedager, avviste dager og foreldet dager`() {
        val tidslinje = tidslinjeOf(7.NAVv2, 5.FOR, 2.FRI, 5.AVV, 7.FRI)
        Utbetalingstidslinje.konverter(tidslinje).also {
            assertTrue(it[1.januar] is Dag.Sykedag)
            assertTrue(it[6.januar] is Dag.SykHelgedag)
            assertTrue(it[8.januar] is Dag.ForeldetSykedag)
            assertTrue(it[15.januar] is Dag.Sykedag)
            assertTrue(it[22.januar] is Dag.Feriedag)
        }
    }

    @Test
    fun `sammenhengende perioder brytes opp av arbeidsdager`() {
        val tidslinje = tidslinjeOf(5.NAVv2, 1.ARB, 5.NAVv2)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `sammenhengende perioder brytes opp av ukjent dag`() {
        val tidslinje = medInfotrygdtidslinje(tidslinjeOf(11.NAVv2), tidslinjeOf(1.NAVv2, startDato = 6.januar))
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(2, result.size)
        assertEquals(1.januar til 5.januar, result.first().periode())
        assertEquals(7.januar til 11.januar, result.last().periode())
    }

    @Test
    fun `fjerner ledende fridager`() {
        val tidslinje = tidslinjeOf(6.FRI, 5.NAVv2)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(7.januar til 11.januar, result.first().periode())
    }

    @Test
    fun `helg blir ikke sett på som en periode`() {
        val tidslinje = tidslinjeOf(5.ARB, 2.NAVv2, 5.ARB)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `foreldet perioder tas med`() {
        val tidslinje = tidslinjeOf(5.NAVv2, 5.FOR)
        val result = tidslinje.sammenhengendeUtbetalingsperioder()
        assertEquals(1, result.size)
        assertEquals(1.januar til 10.januar, result.first().periode())
    }

    private fun medInfotrygdtidslinje(tidslinje: Utbetalingstidslinje, other: Utbetalingstidslinje) =
        tidslinje.plus(other) { actual, challenger ->
            when (challenger) {
                is NavDag, is NavHelgDag -> UkjentDag(actual.dato, actual.økonomi)
                else -> actual
            }
        }
}
