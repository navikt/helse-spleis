package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UtbetalingstidslinjeTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        private val UNG_PERSON_FNR_2018 = Alder("12020052345")
    }

    @Test fun `avviste dager blir konvertert til Navdager med opprinnelig inntekt`() {
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

    @Test
    fun `minus`() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 5.AP, 2.HELG, 5.NAV, 2.HELG).minus(tidslinjeOf(10.NAV))
        assertEquals(11, tidslinje.size)
        assertEquals(11.januar, tidslinje.førsteDato())
        assertTrue(tidslinje[1.januar] is UkjentDag)
        assertTrue(tidslinje[10.januar] is UkjentDag)
        assertTrue(tidslinje[11.januar] is ArbeidsgiverperiodeDag)
        assertEquals(21.januar, tidslinje.sisteDato())
    }

    @Test
    fun `ulike dagtyper`() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 5.AP, 2.HELG, 5.NAV, 2.HELG).minus(tidslinjeOf(1.AVV, 4.NAV))
        assertEquals(16, tidslinje.size)
        assertEquals(6.januar, tidslinje.førsteDato())
        assertEquals(21.januar, tidslinje.sisteDato())
    }

    @Test
    fun `fjerne dager midt i`() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 5.AP, 2.HELG, 5.NAV, 2.HELG).minus(tidslinjeOf(5.NAV, startDato = 8.januar))
        assertEquals(21, tidslinje.size)
        assertEquals(1.januar, tidslinje.førsteDato())
        assertTrue(tidslinje[7.januar] is NavHelgDag)
        assertTrue(tidslinje[8.januar] is UkjentDag)
        assertTrue(tidslinje[12.januar] is UkjentDag)
        assertTrue(tidslinje[13.januar] is NavHelgDag)
        assertEquals(21.januar, tidslinje.sisteDato())
    }

    @Test
    fun `fjerne dager på slutten`() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 5.AP).minus(tidslinjeOf(5.NAV, startDato = 8.januar))
        assertEquals(12, tidslinje.size)
        assertEquals(1.januar, tidslinje.førsteDato())
        assertTrue(tidslinje[8.januar] is UkjentDag)
        assertTrue(tidslinje[12.januar] is UkjentDag)
        assertEquals(12.januar, tidslinje.sisteDato())
    }


    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje)
    }

}
