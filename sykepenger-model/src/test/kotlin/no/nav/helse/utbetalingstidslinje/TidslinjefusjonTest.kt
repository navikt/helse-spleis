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

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }

}
