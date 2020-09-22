package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingHendelseTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(10.NAV)
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        undersøke(tidslinje)
        assertEquals(12000, inspektør.totalUtbetaling())
    }

    @Test fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(10.NAV(3500))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        undersøke(tidslinje)
        assertEquals(21610, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(10.NAV(3500), 10.NAV(1200))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
        undersøke(tidslinje)
        assertEquals(21610 + 12000, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `selv om utbetaling blir begrenset til 6G får utbetaling for tidslinje med gradert sykdom gradert utbetaling`() {
        val tidslinje = tidslinjeOf(10.NAV(3500, 50.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar

        ).betal()
        undersøke(tidslinje)
        assertEquals(10810, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med gradert sykdom får gradert utbetaling`() {
        val tidslinje = tidslinjeOf(10.NAV(1200, 50.0))
        MaksimumUtbetaling(
            listOf(tidslinje),
                aktivitetslogg,
            1.januar

        ).betal()
        undersøke(tidslinje)
        assertEquals(6000, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje)
    }
}
