package no.nav.helse.testhelpers

import no.nav.helse.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjerBuilderTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `lager navdager i ukerdager og navhelg i helg`() {
        val tidslinje = tidslinjeOf(14.NAV)
        assertEquals(4, tidslinje.inspektør.navHelgDagTeller)
        assertEquals(10, tidslinje.inspektør.navDagTeller)
    }
}
