package no.nav.helse.utbetalingstidslinje

import java.math.MathContext
import no.nav.helse.desember
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
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
                val actual = it[dato].økonomi.inspektør.totalGrad.toDouble().toBigDecimal(mc)
                assertEquals(expected, actual)
            }
        }

        // totalgrad = 83.333333
        2.januar.also { dato ->
            val mc = MathContext(15)
            val expected = (250 / 3.0).toBigDecimal(mc)
            result.forEach {
                val actual = it[dato].økonomi.inspektør.totalGrad.toDouble().toBigDecimal(mc)
                assertEquals(expected, actual)
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
            val expected = 50.prosent
            val actual = result[0][dato].økonomi.inspektør.totalGrad
            assertEquals(expected, actual)
        }
    }
}
