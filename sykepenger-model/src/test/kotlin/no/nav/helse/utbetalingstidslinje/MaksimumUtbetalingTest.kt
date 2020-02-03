package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MaksimumUtbetalingTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
    }

    @Test fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(10.NAV)
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogger
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
            aktivitetslogger
        ).beregn()
        undersøke(tidslinje)
        assertEquals(21610, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogger.hasWarnings())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00), 10.NAV(1200.00))
        MaksimumUtbetaling(Sykdomsgrader(
            listOf(tidslinje)),
            listOf(tidslinje),
            Periode(11.januar, 31.desember),
            aktivitetslogger

        ).beregn()
        undersøke(tidslinje)
        assertEquals(21610 + 12000, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogger.hasMessages())
        assertFalse(aktivitetslogger.hasWarnings())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }
}
