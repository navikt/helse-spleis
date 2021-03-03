package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjeShortenTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    @Test fun `forkorte enkel utbetalingstidslinje`() {
        undersøke(tidslinjeOf(10.NAV).subset(3.januar til 10.januar))
        assertEquals(8, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
    }

    @Test fun `ignorere forkort dato før utbetalingstidslinje startdato`() {
        undersøke(tidslinjeOf(10.NAV).subset(1.januar.minusDays(5) til 10.januar))
        assertEquals(10, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
    }

    @Test fun `forkort dato etter utbetalingstidslinje sluttdato`() {
        undersøke(tidslinjeOf(10.NAV).subset(11.januar til 12.januar))
        assertEquals(0, inspektør.size)
    }

    @Test fun `forkorte dato på sluttdato`() {
        undersøke(tidslinjeOf(10.NAV).subset(10.januar til 10.januar))
        assertEquals(1, inspektør.size)
        assertEquals(1, inspektør.navDagTeller)
    }

    @Test fun `forkorte dato på startdato`() {
        undersøke(tidslinjeOf(10.NAV).subset(1.januar til 10.januar))
        assertEquals(10, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
    }

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje)
    }
}
