package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SykdomsgradfilterTest {

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    internal fun `sykdomsgrad over 20%`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 50.0)))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `alle dager fom første dag med total sykdomsgrad under 20% skal avvises`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0)))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertTrue(aktivitetslogg.hasWarnings())
    }

    @Test
    internal fun `kun dager i perioden blir avvist ved for lav sykdomsgrad`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 10.NAV(1200, 19.0)))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(5, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hasWarnings())
    }

    private fun undersøke(tidslinjer: List<Utbetalingstidslinje>, periode: Periode) {
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        aktivitetslogg = Aktivitetslogg()
        Sykdomsgradfilter(sykdomsgrader, tidslinjer, periode, aktivitetslogg).filter()
        inspektør = UtbetalingstidslinjeInspektør(tidslinjer.first())
            .result()
    }
}
