package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import no.nav.helse.fixtures.tidslinjeOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TidslinjefusjonTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test internal fun `slå sammen tilstøtende betalingstider`() {
        undersøke(tidslinjeOf(10.NAV) + tidslinjeOf(10.UTELATE, 10.NAV))
        assertEquals(20, inspektør.size)
        assertEquals(20, inspektør.navDagTeller)
    }

    @Test internal fun `slå sammen betalingstidslinjer som ikke er tilstøtende`() {
        undersøke(tidslinjeOf(15.NAV) + tidslinjeOf(15.UTELATE, 3.UTELATE, 15.NAV))
        assertEquals(33, inspektør.size)
        assertEquals(30, inspektør.navDagTeller)
        assertEquals(3, inspektør.ukjentDagTeller)
    }

    @Test internal fun `slå sammen ikke-tilstøtende betalingstider med helger`() {
        undersøke(tidslinjeOf(15.NAV) + tidslinjeOf(15.UTELATE, 7.UTELATE, 15.NAV))
        assertEquals(37, inspektør.size)
        assertEquals(30, inspektør.navDagTeller)
        assertEquals(5, inspektør.ukjentDagTeller)
        assertEquals(2, inspektør.fridagTeller)
    }

    @Test internal fun `bli med i flere utbetalingstidslinjer`() {
        undersøke(tidslinjeOf(15.NAV) +
                tidslinjeOf(15.UTELATE, 7.UTELATE, 15.NAV) +
                tidslinjeOf(15.UTELATE, 22.UTELATE, 7.UTELATE, 15.NAV))
        assertEquals(59, inspektør.size)
        assertEquals(45, inspektør.navDagTeller)
        assertEquals(10, inspektør.ukjentDagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    // The following tests handle overlapping Utbetalingstidslinjer. This should only be for multiple arbeitsgivere

    @Test internal fun `NAV-utbetalinger har prioritet`() {
        undersøke(tidslinjeOf(15.ARB) + tidslinjeOf(15.NAV))
        assertEquals(15, inspektør.size)
        assertEquals(15, inspektør.navDagTeller)
        assertEquals(0, inspektør.arbeidsdagTeller)
    }

    @Test internal fun `NAV-utbetalinger for flere arbeidsgivere`() {
        undersøke(tidslinjeOf(15.NAV, 15.ARB) + tidslinjeOf(10.ARB, 10.NAV, 10.ARB, 10.NAV))
        assertEquals(40, inspektør.size)
        assertEquals(30, inspektør.navDagTeller)
        assertEquals(10, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.ukjentDagTeller)
    }

    @Test internal fun `dagens forrang`() {
        undersøke(
            tidslinjeOf(1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV) +
            tidslinjeOf(1.UTELATE, 1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV) +
            tidslinjeOf(2.UTELATE, 1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV) +
            tidslinjeOf(3.UTELATE, 1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV) +
            tidslinjeOf(4.UTELATE, 1.FRI, 1.ARB, 1.AP, 1.HELG, 1.NAV)
        )
        assertEquals(9, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(1, inspektør.navHelgDagTeller)
        assertEquals(1, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(1, inspektør.fridagTeller)
    }

    @Test internal fun `bygg historisk utbetalingstidslinje`() {
        undersøke(
            HistoriskUtbetaling.utbetalingstidslinje(listOf(
                HistoriskUtbetaling(123, 1.januar, 5.januar),
                HistoriskUtbetaling(123, 15.januar, 29.januar),
                HistoriskUtbetaling(456, 22.januar, 5.februar)
            ))
        )
        assertEquals(36, inspektør.size)
        assertEquals(21, inspektør.navDagTeller)
        assertEquals(6, inspektør.navHelgDagTeller)
    }

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }

}
