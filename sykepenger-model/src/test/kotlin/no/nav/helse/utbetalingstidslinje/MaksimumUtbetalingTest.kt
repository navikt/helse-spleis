package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaksimumUtbetalingTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(10.NAV)
        MaksimumUtbetaling(Sykdomsgrader(listOf(tidslinje)), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(12000, inspektør.totalUtbetaling())
    }

    @Test fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00))
        MaksimumUtbetaling(Sykdomsgrader(listOf(tidslinje)), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(21610, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00), 10.NAV(1200.00))
        MaksimumUtbetaling(Sykdomsgrader(listOf(tidslinje)), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(21610 + 12000, inspektør.totalUtbetaling())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }
}
