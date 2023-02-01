package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
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
}
