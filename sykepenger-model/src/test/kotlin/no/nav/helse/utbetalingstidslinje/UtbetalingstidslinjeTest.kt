package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        private val UNG_PERSON_FNR_2018 = Alder("12020052345")
    }

    @Test
    fun `samlet periode`() {
        assertEquals(1.januar til 1.januar, Utbetalingstidslinje.periode(listOf(tidslinjeOf(1.NAV))))
        assertEquals(1.desember(2017) til 7.mars, Utbetalingstidslinje.periode(listOf(
            tidslinjeOf(7.NAV),
            tidslinjeOf(7.NAV, startDato = 1.mars),
            tidslinjeOf(7.NAV, startDato = 1.desember(2017)),
        )))
    }

    @Test
    fun `betalte dager`() {
        tidslinjeOf(4.AP, 1.FRI, 2.HELG, 4.NAV, 1.AVV, 2.FRI, 5.ARB).also {
            assertTrue(it.harBetalt(1.januar))
            assertFalse(it.harBetalt(5.januar))
            assertTrue(it.harBetalt(6.januar))
            assertTrue(it.harBetalt(7.januar))
            assertTrue(it.harBetalt(12.januar))
            assertFalse(it.harBetalt(15.januar))
            assertTrue(it.harBetalt(1.januar til 7.januar))
            assertFalse(it.harBetalt(13.januar til 20.januar))
        }
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

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje)
    }
}
