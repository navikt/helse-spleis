package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
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
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).beregn()
        undersøke(tidslinje)
        assertEquals(12000, inspektør.totalUtbetaling())
    }

    @Test fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00))
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).beregn()
        undersøke(tidslinje)
        assertEquals(21610, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00), 10.NAV(1200.00))
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(11.januar, 31.desember),
            aktivitetslogg

        ).beregn()
        undersøke(tidslinje)
        assertEquals(21610 + 12000, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test fun `selv om utbetaling blir begrenset til 6G får utbetaling for tidslinje med gradert sykdom gradert utbetaling`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00, 50.0))
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg

        ).beregn()
        undersøke(tidslinje)
        assertEquals(10810, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med gradert sykdom får gradert utbetaling`() {
        val tidslinje = tidslinjeOf(10.NAV(1200.00, 50.0))
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg

        ).beregn()
        undersøke(tidslinje)
        assertEquals(6000, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }
}
