package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Sykdomsgrader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SpennBuilderTest {

    private lateinit var linjer: List<Utbetalingslinje>

    @Test
    internal fun `konverter enkel Utbetalingstidslinje til Utbetalingslinjer`() {
        opprett(5.NAV, 2.HELG, 3.NAV)

        assertEquals(1, linjer.size)
        assertLinje(0, 1.januar, 10.januar)
    }

    @Test
    internal fun `helg ved start og slutt i perioden utelates ikke`() {
        opprett(2.HELG(1200.0), 5.NAV(1200.0), 2.HELG(1200.0))

        assertEquals(1, linjer.size)
        assertLinje(0, 1.januar, 9.januar, 1200, 100.0)
    }

    @Test
    internal fun `gap-dag som første og siste dag i perioden`() {
        opprett(1.ARB, 3.NAV, 1.ARB)

        assertEquals(1, linjer.size)
        assertLinje(0, 2.januar, 4.januar)
    }

    @Test
    internal fun `grad endres i løpet av helgen`() {
        opprett(5.NAV(1500.0), 1.HELG(1500.0), 1.HELG(1500.0, 80.0), 5.NAV(1500.0, 80.0))

        assertEquals(2, linjer.size)
        assertLinje(0, 1.januar, 6.januar, 1500, 100.0)
        assertLinje(1, 7.januar, 12.januar, (1500 * 0.8).toInt(), 80.0)
    }

    @Test
    internal fun `gap i vedtaksperiode`() {
        assertNyLinjeVedGap(1.ARB)
        assertNyLinjeVedGap(1.FRI)
        assertNyLinjeVedGap(1.AVV)
        assertNyLinjeVedGap(1.FOR)
    }

    @Test
    internal fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag`() {
        opprett(2.NAV, 1.AP, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(1, linjer.size)
        assertLinje(0, 4.januar, 10.januar)
    }

    @Test
    internal fun `Endring i sats`() {
        opprett(3.NAV(1200.0), 2.NAV(1500.0), 2.HELG, 2.NAV(1500.0))

        assertEquals(2, linjer.size)
        assertLinje(0, 1.januar, 3.januar, 1200)
        assertLinje(1, 4.januar, 9.januar, 1500)
    }

    @Test
    internal fun `Endring i utbetaling pga grad`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1500.0, 60.0), 2.HELG(1500.0, 60.0), 2.NAV(1500.0, 60.0))

        assertEquals(2, linjer.size)
        assertLinje(0, 1.januar, 3.januar, 1500, 100.0)
        assertLinje(1, 4.januar, 9.januar, (1500 * 0.6).toInt(), 60.0)
    }

    @Test
    internal fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1875.0, 80.0), 2.HELG(1500.0, 80.0), 2.NAV(1500.0, 80.0))

        assertEquals(3, linjer.size)
        assertLinje(0, 1.januar, 3.januar, 1500, 100.0, 1)
        assertLinje(1, 4.januar, 5.januar, 1500, 80.0, 2, 1)
        assertLinje(2, 6.januar, 9.januar, (1500 * 0.8).toInt(), 80.0, 3, 2)
    }

    @Test
    internal fun `Endring i sykdomsgrad`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1500.0, 80.0), 2.HELG(1500.0, 80.0), 2.NAV(1500.0, 80.0))

        assertEquals(2, linjer.size)
        assertLinje(0, 1.januar, 3.januar, 1500, 100.0)
        assertLinje(1, 4.januar, 9.januar, (1500 * 0.8).toInt(), 80.0)
    }

    private fun assertLinje(
        index: Int,
        fom: LocalDate,
        tom: LocalDate,
        sats: Int = linjer[index].dagsats,
        grad: Double = linjer[index].grad,
        delytelseId: Int = linjer[index]["delytelseId"],
        refDelytelseId: Int? = linjer[index]["refDelytelseId"]
    ) {
        assertEquals(fom, linjer[index].fom)
        assertEquals(tom, linjer[index].tom)
        assertEquals(grad, linjer[index].grad)
        assertEquals(sats, linjer[index].dagsats)
        assertEquals(delytelseId, linjer[index]["delytelseId"])
        assertEquals(refDelytelseId, linjer[index]["refDelytelseId"] ?: null)
    }

    private fun assertNyLinjeVedGap(gapDay: Utbetalingsdager) {
        opprett(2.NAV, gapDay, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(2, linjer.size)
        assertEquals(1.januar, linjer.first().fom)
        assertEquals(2.januar, linjer.first().tom)
        assertEquals(4.januar, linjer.last().fom)
        assertEquals(10.januar, linjer.last().tom)
    }

    private fun opprett(vararg dager: Utbetalingsdager, sisteDato: LocalDate? = null) {
        val tidslinje = tidslinjeOf(*dager)
        MaksimumUtbetaling(
            Sykdomsgrader(listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 1.mars),
            Aktivitetslogg()
        ).beregn()
        linjer = SpennBuilder(tidslinje, sisteDato ?: tidslinje.sisteDato()).result()
    }
}
