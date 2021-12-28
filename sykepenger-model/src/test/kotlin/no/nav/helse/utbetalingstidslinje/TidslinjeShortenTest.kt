package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.testhelpers.NAVv2
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TidslinjeShortenTest {
    @Test
    fun `forkorte enkel utbetalingstidslinje`() {
        val inspektør = tidslinjeOf(10.NAVv2).subset(3.januar til 10.januar).inspektør
        assertEquals(8, inspektør.size)
        assertEquals(6, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `ignorere forkort dato før utbetalingstidslinje startdato`() {
        val inspektør = tidslinjeOf(10.NAVv2).subset(1.januar.minusDays(5) til 10.januar).inspektør
        assertEquals(10, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `forkort dato etter utbetalingstidslinje sluttdato`() {
        val inspektør = tidslinjeOf(10.NAVv2).subset(11.januar til 12.januar).inspektør
        assertEquals(0, inspektør.size)
    }

    @Test
    fun `forkorte dato på sluttdato`() {
        val inspektør = tidslinjeOf(10.NAVv2).subset(10.januar til 10.januar).inspektør
        assertEquals(1, inspektør.size)
        assertEquals(1, inspektør.navDagTeller)
    }

    @Test
    fun `forkorte dato på startdato`() {
        val inspektør = tidslinjeOf(10.NAVv2).subset(1.januar til 10.januar).inspektør
        assertEquals(10, inspektør.size)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

}
