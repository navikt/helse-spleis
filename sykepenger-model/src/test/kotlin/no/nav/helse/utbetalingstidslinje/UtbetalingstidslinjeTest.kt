package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        private val UNG_PERSON_FNR_2018 = Alder("12020052345")
    }

    @Test internal fun `avviste dager blir konvertert til Navdager`() {
        val tidslinje = tidslinjeOf(10.NAV(12.0), 5.NAV(1200.0))
        MinimumInntektsfilter(UNG_PERSON_FNR_2018, listOf(tidslinje)).filter()
        undersøke(tidslinje)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(5, inspektør.navDagTeller)

        undersøke(tidslinje.klonOgKonverterAvvistDager())
        assertEquals(0, inspektør.avvistDagTeller)
        assertEquals(15, inspektør.navDagTeller)
    }

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }

}
