package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingFilterHendelseTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(12.NAV).betal()
        assertEquals(12000.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(12.NAV(3500.0)).betal()
        assertEquals(21610.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(12.NAV(3500.0), 14.NAV(1200.0)).betal()
        assertEquals(21610.0 + 12000.0, tidslinje.inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.harAktiviteter())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `selv om utbetaling blir begrenset til 6G får utbetaling for tidslinje med gradert sykdom gradert utbetaling`() {
        val tidslinje = tidslinjeOf(12.NAV(3500.0, 50.0)).betal()
        assertEquals(10800.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test
    fun `utbetaling for tidslinje med gradert sykdom får gradert utbetaling`() {
        val tidslinje = tidslinjeOf(12.NAV(1200.0, 50.0)).betal()
        assertEquals(6000.0, tidslinje.inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.harAktiviteter())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    private fun Utbetalingstidslinje.betal() =
        MaksimumUtbetalingFilter().betal(listOf(this), this.periode(), aktivitetslogg, EmptyLog).single()
}
