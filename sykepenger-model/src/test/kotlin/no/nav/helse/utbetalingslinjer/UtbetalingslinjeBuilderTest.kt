package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Sykdomsgrader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    internal fun `Endring i utbetaling`() {
        opprettUtbetalingslinjer(3.NAV(1200.0), 2.NAV(1500.0), 2.HELG, 2.NAV(1500.0))

        assertEquals(2, linjer.size)

        assertEquals(1.januar, linjer.first().fom)
        assertEquals(3.januar, linjer.first().tom)
        assertEquals(1200, linjer.first().dagsats)

        assertEquals(4.januar, linjer.last().fom)
        assertEquals(9.januar, linjer.last().tom)
        assertEquals(1500, linjer.last().dagsats)
    }

    @Test
    internal fun `Endring i utbetaling pga grad`() {
        opprettUtbetalingslinjer(3.NAV(1500.0, 100.0), 2.NAV(1500.0, 60.0), 2.HELG, 2.NAV(1500.0, 60.0))

        assertEquals(2, linjer.size)

        assertEquals(1.januar, linjer.first().fom)
        assertEquals(3.januar, linjer.first().tom)
        assertEquals(1500, linjer.first().dagsats)
        assertEquals(100.0, linjer.first().grad)

        assertEquals(4.januar, linjer.last().fom)
        assertEquals(9.januar, linjer.last().tom)
        assertEquals((1500 * 0.6).toInt(), linjer.last().dagsats)
        assertEquals(60.0, linjer.last().grad)
    }

    @Test
    internal fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        opprettUtbetalingslinjer(3.NAV(1500.0, 100.0), 2.NAV(1875.0, 80.0), 2.HELG, 2.NAV(1500.0, 80.0))

        assertEquals(3, linjer.size)

        assertEquals(1.januar, linjer.first().fom)
        assertEquals(3.januar, linjer.first().tom)
        assertEquals(1500, linjer.first().dagsats)
        assertEquals(100.0, linjer.first().grad)

        assertEquals(4.januar, linjer[1].fom)
        assertEquals(5.januar, linjer[1].tom)
        assertEquals(1500, linjer[1].dagsats)
        assertEquals(80.0, linjer[1].grad)

        assertEquals(8.januar, linjer.last().fom)
        assertEquals(9.januar, linjer.last().tom)
        assertEquals((1500 * 0.8).toInt(), linjer.last().dagsats)
        assertEquals(80.0, linjer.last().grad)
    }

    private fun opprettUtbetalingslinjer(vararg dager: Utbetalingsdager) {
        val tidslinje = tidslinjeOf(*dager)
        MaksimumUtbetaling(
            Sykdomsgrader(listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 1.mars),
            Aktivitetslogg()
        ).beregn()
        linjer = UtbetalingslinjeBuilder(
            tidslinje,
            Periode(tidslinje.førsteDato(), tidslinje.sisteDato())
        ).result()
    }

}
