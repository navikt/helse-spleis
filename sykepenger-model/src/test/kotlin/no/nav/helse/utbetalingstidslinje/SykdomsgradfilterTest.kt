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
    fun `sykdomsgrad over 20 prosent`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 50.0)))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `alle dager fom første dag med total sykdomsgrad under 20 prosent skal avvises`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 10.NAV))
        val periode = Periode(1.januar, 31.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(10, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `ikke warning når de avviste dagene er utenfor perioden`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 10.NAV))
        val periode = Periode(22.januar, 31.januar)
        undersøke(tidslinjer, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(10, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `ikke warning når de avviste dagene gjelder forrige arbeidsgiverperiode`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV(1200, 19.0), 20.ARB, 16.AP, 10.NAV))
        println(Utbetalingstidslinje.periode(tidslinjer))
        val periode = Periode(11.februar, 9.mars)
        undersøke(tidslinjer, periode)
        assertEquals(32, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(10, inspektør.navDagTeller)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `avvis dager for begge tidslinjer`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 5.NAV(1200, 19.0)),
            tidslinjeOf(16.AP, 5.NAV(1200, 19.0))
        )
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(5, UtbetalingstidslinjeInspektør(tidslinjer.first()).avvistDagTeller)
        assertEquals(5, UtbetalingstidslinjeInspektør(tidslinjer.last()).avvistDagTeller)
    }

    @Test
    fun `avviser utbetaling når samlet grad er under 20 prosent`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 4.NAV, 1.NAV(1200, 39)),
            tidslinjeOf(16.AP, 4.NAV, 1.FRI)
        )
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(4, tidslinjer.inspektør(0).navDagTeller)
        assertEquals(1, tidslinjer.inspektør(0).avvistDagTeller)
        assertEquals(4, tidslinjer.inspektør(1).navDagTeller)
        assertEquals(1, tidslinjer.inspektør(1).fridagTeller)
    }

    @Test
    fun `avviser ikke utbetaling når samlet grad er minst 20 prosent`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 4.NAV, 1.NAV(1200, 40)),
            tidslinjeOf(16.AP, 4.NAV, 1.FRI)
        )
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, periode)
        assertEquals(5, tidslinjer.inspektør(0).navDagTeller)
        assertEquals(0, tidslinjer.inspektør(0).avvistDagTeller)
        assertEquals(4, tidslinjer.inspektør(1).navDagTeller)
        assertEquals(1, tidslinjer.inspektør(1).fridagTeller)
    }

    private fun undersøke(tidslinjer: List<Utbetalingstidslinje>, periode: Periode) {
        aktivitetslogg = Aktivitetslogg()
        Sykdomsgradfilter(tidslinjer, periode, aktivitetslogg).filter()
        inspektør = tidslinjer.inspektør(0)
    }

    private fun List<Utbetalingstidslinje>.inspektør(index: Int) = UtbetalingstidslinjeInspektør(this[index])
}
