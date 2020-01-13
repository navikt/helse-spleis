package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.AP
import no.nav.helse.fixtures.HELG
import no.nav.helse.fixtures.NAV
import no.nav.helse.fixtures.tidslinjeOf
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingslinjeBuilderTest {

    private lateinit var linjer: List<Utbetalingslinje>

    @Test
    internal fun `ingen utbetaling for arbeidsgiverperiode`() {
        opprettUtbetalingslinjer(10.AP)

        assertEquals(emptyList<Utbetalingslinje>(), linjer)
    }

    @Test
    internal fun `helgedager er ikke med i utbetalingen`() {
        opprettUtbetalingslinjer(16.AP, 3.NAV, 1.HELG)

        assertEquals(1, linjer.size)
        assertEquals(17.januar, linjer.first().fom)
        assertEquals(19.januar, linjer.first().tom)
    }

    @Test
    internal fun `a`() {
        opprettUtbetalingslinjer(3.NAV(1200.0), 2.NAV(1500.0), 2.HELG, 2.NAV(1500.0))

        assertEquals(2, linjer.size)

        assertEquals(1.januar, linjer.first().fom)
        assertEquals(3.januar, linjer.first().tom)
        assertEquals(1200, linjer.first().dagsats)

        assertEquals(4.januar, linjer.last().fom)
        assertEquals(9.januar, linjer.last().tom)
        assertEquals(1500, linjer.last().dagsats)
    }

    private fun opprettUtbetalingslinjer(vararg dager: Triple<Int, Utbetalingstidslinje.(Double, LocalDate) -> Unit, Double>) {
        val tidslinje = tidslinjeOf(*dager)
        MaksimumDagsats(emptyMap(), listOf(tidslinje)).beregn()
        linjer = UtbetalingslinjeBuilder(tidslinje).result()
    }

}
