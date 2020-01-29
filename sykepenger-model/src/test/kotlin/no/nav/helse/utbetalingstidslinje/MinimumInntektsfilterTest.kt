package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.UtbetalingstidslinjeInspektør
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MinimumInntektsfilterTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "05015112345"
    }

    @Test internal fun `ung person som oppfyller minstelønnskravet får ingen avviste dager`() {
        val tidslinje = tidslinjeOf(5.NAV)
        MinimumInntektsfilter(Alder(UNG_PERSON_FNR_2018), listOf(tidslinje)).filter()
        undersøke(tidslinje)
        assertEquals(5, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(0, inspektør.avvistDagTeller)
    }

    @Test internal fun `dager under minstelønnskravet blir avvist`() {
        val tidslinje = tidslinjeOf(5.NAV(1200.0), 10.NAV(12.0))
        MinimumInntektsfilter(Alder(UNG_PERSON_FNR_2018), listOf(tidslinje)).filter()
        undersøke(tidslinje)
        assertEquals(15, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
    }

    @Test internal fun `dager under minstelønnskravet blir avvist i flere tidslinjer`() {
        val tidslinje1 = tidslinjeOf(5.NAV, 10.NAV(12.0))
        val tidslinje2 = tidslinjeOf(5.UTELATE, 5.ARB, 10.NAV(12.0))
        MinimumInntektsfilter(Alder(UNG_PERSON_FNR_2018), listOf(tidslinje1, tidslinje2)).filter()

        undersøke(tidslinje1)
        assertEquals(15, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)

        undersøke(tidslinje2)
        assertEquals(15, inspektør.size)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(5, inspektør.arbeidsdagTeller)
    }

    @Test internal fun `total inntekt per dag avgjør minstelønnskravet`() {
        val tidslinje1 = tidslinjeOf(10.NAV(150.0))
        val tidslinje2 = tidslinjeOf(1.NAV(5.0), 9.NAV(150.0))
        MinimumInntektsfilter(Alder(UNG_PERSON_FNR_2018), listOf(tidslinje1, tidslinje2)).filter()

        undersøke(tidslinje1)
        assertEquals(10, inspektør.size)
        assertEquals(9, inspektør.navDagTeller)
        assertEquals(1, inspektør.avvistDagTeller)

        undersøke(tidslinje2)
        assertEquals(10, inspektør.size)
        assertEquals(9, inspektør.navDagTeller)
        assertEquals(1, inspektør.avvistDagTeller)
    }

    @Test internal fun `total inntekt per dag avgjør minstelønnskravet for person som er 67 år`() {
        val tidslinje1 = tidslinjeOf(10.NAV(150.0), 10.NAV(400.0))
        val tidslinje2 =
            tidslinjeOf(1.NAV(5.0), 14.NAV(150.0), 5.NAV(400.0))
        MinimumInntektsfilter(Alder(PERSON_67_ÅR_FNR_2018), listOf(tidslinje1, tidslinje2)).filter()

        undersøke(tidslinje1)
        assertEquals(20, inspektør.size)
        assertEquals(4 + 5, inspektør.navDagTeller)
        assertEquals(11, inspektør.avvistDagTeller)

        undersøke(tidslinje2)
        assertEquals(20, inspektør.size)
        assertEquals(4 + 5, inspektør.navDagTeller)
        assertEquals(11, inspektør.avvistDagTeller)
    }

    private fun undersøke(tidslinje: Utbetalingstidslinje) {
        inspektør = UtbetalingstidslinjeInspektør(tidslinje).result()
    }

}
