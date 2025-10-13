package no.nav.helse.etterlevelse

import no.nav.helse.etterlevelse.Tidslinjedag.Companion.dager
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UtbetalingstidslinjeBuilderTest {

    @Test
    fun `tar med fridager på slutten av en sykdomsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        Assertions.assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                mapOf("fom" to 1.februar, "tom" to 2.februar, "dagtype" to "FRIDAG", "grad" to 0)
            ),
            tidslinjedager
        )
    }

    @Test
    fun `tar ikke med fridager i oppholdsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 10.ARB, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        Assertions.assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
            ),
            tidslinjedager
        )
    }
}
