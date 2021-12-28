package no.nav.helse.utbetalingstidslinje

import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAVv2
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingHendelseTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(12.NAVv2)
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        assertEquals(12000.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(12.NAVv2(3500.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        assertEquals(21610.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(12.NAVv2(3500.0), 14.NAVv2(1200.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        assertEquals(21610.0 + 12000.0, tidslinje.inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `selv om utbetaling blir begrenset til 6G får utbetaling for tidslinje med gradert sykdom gradert utbetaling`() {
        val tidslinje = tidslinjeOf(12.NAVv2(3500.0, 50.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar

        ).betal()
        assertEquals(10810.0, tidslinje.inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med gradert sykdom får gradert utbetaling`() {
        val tidslinje = tidslinjeOf(12.NAVv2(1200.0, 50.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar

        ).betal()
        assertEquals(6000.0, tidslinje.inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }
}
