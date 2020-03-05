package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykdomsgraderTest {

    @Test
    fun `hente ut total sykdomsgrad for dato`() {
        val sykdomsgrader = Sykdomsgrader(listOf(tidslinjeOf(10.NAV(1200.0, 50.0))))
        assertEquals(50.0, sykdomsgrader[1.januar])
    }

    @Test
    fun `ved manglende dag, blir total sykdomsgrad 0`() {
        val sykdomsgrader = Sykdomsgrader(listOf(tidslinjeOf(5.NAV(1200.0, 50.0), 1.ARB)))
        assertEquals(50.0, sykdomsgrader[1.januar])
        assertEquals(Double.NaN, sykdomsgrader[6.januar])
    }
}
