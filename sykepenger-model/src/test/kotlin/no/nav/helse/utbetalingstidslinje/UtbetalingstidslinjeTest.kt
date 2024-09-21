package no.nav.helse.utbetalingstidslinje

import java.math.MathContext
import no.nav.helse.desember
import no.nav.helse.etterlevelse.Tidslinjedag.Companion.dager
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.februar
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    @Test
    fun subsetting() {
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).subset(1.januar til 5.januar).periode())
        assertEquals(4.januar.somPeriode(), tidslinjeOf(5.NAV).subset(4.januar.somPeriode()).periode())
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).subset(31.desember(2017) til 6.januar).periode())
        assertTrue(tidslinjeOf(5.NAV).subset(6.januar til 6.januar).isEmpty())
    }

    @Test
    fun fraOgMed() {
        assertEquals(3.januar til 5.januar, tidslinjeOf(5.NAV).fraOgMed(3.januar).periode())
        assertEquals(0, tidslinjeOf(5.NAV).fraOgMed(6.januar).size)
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).fraOgMed(31.desember(2017)).periode())
    }

    @Test
    fun fremTilOgMed() {
        assertEquals(1.januar til 2.januar, tidslinjeOf(5.NAV).fremTilOgMed(2.januar).periode())
        assertEquals(1.januar til 5.januar, tidslinjeOf(5.NAV).fremTilOgMed(6.januar).periode())
        assertEquals(0, tidslinjeOf(5.NAV).fremTilOgMed(31.desember(2017)).size)
    }

    @Test
    fun plus() {
        assertEquals(0, (tidslinjeOf() + tidslinjeOf()).size)
        assertEquals(1.januar til 5.januar, (tidslinjeOf() + tidslinjeOf(5.NAV)).periode())
        assertEquals(1.januar til 10.januar, (tidslinjeOf(3.NAV) + tidslinjeOf(5.NAV, startDato = 6.januar)).periode())
    }

    @Test
    fun `avviser perioder med flere begrunnelser`() {
        val periode = 1.januar til 5.januar
        tidslinjeOf(5.NAV).also {
            val første = Utbetalingstidslinje.avvis(listOf(it), listOf(periode), listOf(Begrunnelse.MinimumSykdomsgrad))
            val andre = Utbetalingstidslinje.avvis(første, listOf(periode), listOf(Begrunnelse.EtterDødsdato))
            val tredje = Utbetalingstidslinje.avvis(andre, listOf(periode), listOf(Begrunnelse.ManglerMedlemskap))
            periode.forEach { dato ->
                val dag = tredje.single()[dato] as AvvistDag
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
    fun `total sykdomsgrad`() {
        val ag1 = tidslinjeOf(5.NAV(dekningsgrunnlag = 1000, grad = 50))
        val ag2 = tidslinjeOf(1.FRI(dekningsgrunnlag = 2000), 4.NAV(dekningsgrunnlag = 2000, grad = 100))
        val tidslinjer = listOf(ag1, ag2)
        val result = Utbetalingsdag.totalSykdomsgrad(tidslinjer)

        // totalgrad = 16.6666666666...
        1.januar.also { dato ->
            val mc = MathContext(15)
            val expected = (100 / 6.0).toBigDecimal(mc)
            result.forEach {
                val actual = it[dato].økonomi.inspektør.totalGrad.toBigDecimal(mc)
                assertEquals(expected.toInt(), actual.toInt())
            }
        }

        // totalgrad = 83.333333
        2.januar.also { dato ->
            val mc = MathContext(15)
            val expected = (250 / 3.0).toBigDecimal(mc)
            result.forEach {
                val actual = it[dato].økonomi.inspektør.totalGrad.toBigDecimal(mc)
                assertEquals(expected.toInt(), actual.toInt())
            }
        }
    }
    @Test
    fun `total sykdomsgrad med ukjent dag`() {
        val ag1 = tidslinjeOf(1.NAV(dekningsgrunnlag = 1000, grad = 50))
        val ag2 = tidslinjeOf()
        val tidslinjer = listOf(ag1, ag2)
        val result = Utbetalingsdag.totalSykdomsgrad(tidslinjer)

        1.januar.also { dato ->
            val expected = 50
            val actual = result[0][dato].økonomi.inspektør.totalGrad
            assertEquals(expected, actual)
        }
    }


    @Test
    fun `tar med fridager på slutten av en sykdomsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                mapOf("fom" to 1.februar, "tom" to 2.februar, "dagtype" to "FRIDAG", "grad" to 0)
            ),
            tidslinjedager
        )
    }

    @Test
    fun `tar ikke med fridager i oppholdsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 10.ARB, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
            ),
            tidslinjedager
        )
    }
}
