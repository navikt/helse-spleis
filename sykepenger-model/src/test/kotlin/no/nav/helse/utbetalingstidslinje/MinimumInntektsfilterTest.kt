package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MinimumInntektsfilterTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "05015112345"
    }

    @BeforeEach internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    internal fun `sjekker ikke fridager`() {
        val tidslinje = tidslinjeOf(1.FRI, 5.NAV)
        MinimumInntektsfilter(
            Alder(UNG_PERSON_FNR_2018),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).filter()
        assertTrue(aktivitetslogg.hasOnlyInfoAndNeeds())
    }

    @Test internal fun `ung person som oppfyller minstelønnskravet får ingen avviste dager`() {
        val tidslinje = tidslinjeOf(5.NAV)
        MinimumInntektsfilter(
            Alder(UNG_PERSON_FNR_2018),
            listOf(tidslinje),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).filter()
        undersøke(tidslinje)
        assertEquals(5, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(0, inspektør.avvistDagTeller)
    }

    @Test internal fun `dager under minstelønnskravet blir avvist`() {
        val tidslinje = tidslinjeOf(5.NAV(1200), 10.NAV(12))
        MinimumInntektsfilter(Alder(
            UNG_PERSON_FNR_2018),
            listOf(tidslinje),
            Periode(1.januar, 5.januar),
            aktivitetslogg
        ).filter()
        undersøke(tidslinje)
        assertEquals(15, inspektør.size)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertTrue(aktivitetslogg.hasMessages())
        assertFalse(aktivitetslogg.hasWarnings()) // Even though days rejected, the days were not in the periode
    }

    @Test internal fun `dager under minstelønnskravet blir avvist i flere tidslinjer`() {
        val tidslinje1 = tidslinjeOf(5.NAV, 10.NAV(12))
        val tidslinje2 = tidslinjeOf(5.UTELATE, 5.ARB, 10.NAV(12))
        MinimumInntektsfilter(
            Alder(UNG_PERSON_FNR_2018),
            listOf(tidslinje1, tidslinje2),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).filter()

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
        val tidslinje1 = tidslinjeOf(10.NAV(150))
        val tidslinje2 = tidslinjeOf(1.NAV(5), 9.NAV(150))
        MinimumInntektsfilter(
            Alder(UNG_PERSON_FNR_2018),
            listOf(tidslinje1, tidslinje2),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).filter()

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
        val tidslinje1 = tidslinjeOf(10.NAV(150), 10.NAV(400))
        val tidslinje2 =
            tidslinjeOf(1.NAV(5), 14.NAV(150), 5.NAV(400))
        MinimumInntektsfilter(
            Alder(PERSON_67_ÅR_FNR_2018),
            listOf(tidslinje1, tidslinje2),
            Periode(1.januar, 31.desember),
            aktivitetslogg
        ).filter()

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
