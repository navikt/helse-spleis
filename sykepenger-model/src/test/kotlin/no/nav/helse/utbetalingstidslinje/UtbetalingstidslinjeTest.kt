package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        private val UNG_PERSON_FNR_2018 = Alder("12020052345")
    }

    @Test
    fun `avviste dager blir konvertert til Navdager med opprinnelig inntekt`() {
        val tidslinje = tidslinjeOf(10.NAV(12), 5.NAV(1200))
        MinimumInntektsfilter(
            UNG_PERSON_FNR_2018,
            listOf(tidslinje),
            Periode(1.januar, 15.januar),
            Aktivitetslogg()
        ).filter()
        undersøke(tidslinje)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(5, inspektør.navDagTeller)

        undersøke(tidslinje.klonOgKonverterAvvistDager())
        assertEquals(0, inspektør.avvistDagTeller)
        assertEquals(15, inspektør.navDagTeller)
        assertEquals(120.0 + 6000.0, inspektør.totalInntekt())

    }

    @Test
    fun `siste sykepengeperiode uten opphold`() {
        tidslinjeOf(10.NAV, 1.AP, 10.NAV).also { tidslinje ->
            val periode = tidslinje.sisteSykepengeperiode()
            assertEquals(1.januar, periode?.start)
            assertEquals(21.januar, periode?.endInclusive)
        }
        tidslinjeOf(10.NAV, 1.FRI, 10.NAV).also { tidslinje ->
            val periode = tidslinje.sisteSykepengeperiode()
            assertEquals(12.januar, periode?.start)
            assertEquals(21.januar, periode?.endInclusive)
        }
        tidslinjeOf(10.NAV, 1.ARB, 10.NAV).also { tidslinje ->
            val periode = tidslinje.sisteSykepengeperiode()
            assertEquals(12.januar, periode?.start)
            assertEquals(21.januar, periode?.endInclusive)
        }
        tidslinjeOf(10.NAV, 1.AVV, 10.NAV).also { tidslinje ->
            val periode = tidslinje.sisteSykepengeperiode()
            assertEquals(12.januar, periode?.start)
            assertEquals(21.januar, periode?.endInclusive)
        }
        tidslinjeOf(10.NAV, 1.FOR, 10.NAV).also { tidslinje ->
            val periode = tidslinje.sisteSykepengeperiode()
            assertEquals(12.januar, periode?.start)
            assertEquals(21.januar, periode?.endInclusive)
        }
    }

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje)
    }
}
