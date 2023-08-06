package no.nav.helse.etterlevelse

import no.nav.helse.etterlevelse.Tidslinjedag.Companion.dager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
            dager.dager(10.januar..20.januar)
        )
    }

    @Test
    fun erRettFør() {
        assertTrue(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(2.januar))
        assertFalse(Tidslinjedag(1.januar, "dagtype", 100).erRettFør(3.januar))
    }
}
