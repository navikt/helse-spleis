package no.nav.helse.person.etterlevelse

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.*
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Tidslinjedag.Companion.dager
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SubsumsjonObserverTest {

    @Test
    fun tidslinjedager() {
        val dager = List(31) { index ->
            val dagen = (index + 1).januar
            if (dagen < 16.januar) Tidslinjedag(dagen, "NAVDAG", 100)
            else Tidslinjedag(dagen, "FERIEDAG", 100)
        }

        assertEquals(
            listOf(
                mapOf(
                    "fom" to 1.januar,
                    "tom" to 15.januar,
                    "dagtype" to "NAVDAG",
                    "grad" to 100
                ),
                mapOf(
                    "fom" to 16.januar,
                    "tom" to 31.januar,
                    "dagtype" to "FERIEDAG",
                    "grad" to 100
                )
            ),
            dager.dager()
        )
    }

    @Test
    fun `tidslinjedager blir cappet til periode`() {
        val dager = List(31) { index ->
            val dagen = (index + 1).januar
            if (dagen < 16.januar) Tidslinjedag(dagen, "NAVDAG", 100)
            else Tidslinjedag(dagen, "FERIEDAG", 100)
        }

        assertEquals(
            listOf(
                mapOf(
                    "fom" to 10.januar,
                    "tom" to 15.januar,
                    "dagtype" to "NAVDAG",
                    "grad" to 100
                ),
                mapOf(
                    "fom" to 16.januar,
                    "tom" to 20.januar,
                    "dagtype" to "FERIEDAG",
                    "grad" to 100
                )
            ),
            dager.dager(Periode(10.januar, 20.januar))
        )
    }

    @Test
    fun erRettFør() {
        assertTrue(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(2.januar))
        assertFalse(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(3.januar))
    }

    @Test
    fun `tar med fridager på slutten av en sykdomsperiode`() {
        val utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV, 2.FRI)
        val tidslinjedager = utbetalingstidslinje.subsumsjonsformat().dager()

        assertEquals(
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

        assertEquals(
            listOf(
                mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
            ),
            tidslinjedager
        )
    }
}
