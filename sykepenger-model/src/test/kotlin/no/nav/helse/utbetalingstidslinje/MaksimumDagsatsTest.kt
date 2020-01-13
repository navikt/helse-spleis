package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.NAV
import no.nav.helse.fixtures.UtbetalingstidslinjeInspektør
import no.nav.helse.fixtures.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MaksimumDagsatsTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test fun `når inntekt er under 6G blir utbetaling lik inntekt`() {
        val tidslinje = tidslinjeOf(10.NAV)
        MaksimumDagsats(emptyMap(), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(12000, inspektør.totalUtbetaling())
    }

    @Test fun `når inntekt er over 6G blir utbetaling lik 6G`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00))
        MaksimumDagsats(emptyMap(), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(21610, inspektør.totalUtbetaling())
    }

    @Test fun `utbetaling for tidslinje med ulike daginntekter blir kalkulert per dag`() {
        val tidslinje = tidslinjeOf(10.NAV(3500.00), 10.NAV(1200.00))
        MaksimumDagsats(emptyMap(), listOf(tidslinje)).beregn()
        undersøke(tidslinje)
        assertEquals(21610 + 12000, inspektør.totalUtbetaling())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }
}
