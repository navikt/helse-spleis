package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AvvisDagerEtterDødsdatofilterTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    fun `avviser ikke dager uten dødsdato`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, null, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(3, inspektør.navDagTeller)
    }

    @Test
    fun `helg avvises ikke`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 5.NAV))
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, 19.januar, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.avvistDagTeller)
        assertEquals(3, inspektør.navDagTeller)
    }

    @Test
    fun `avviser ikke dager på dødsdato`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 6.NAV))
        val periode = Periode(1.januar, 22.januar)
        undersøke(tidslinjer, periode.endInclusive, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(4, inspektør.navDagTeller)
    }

    @Test
    fun `avviser ikke dager før dødsdato`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 6.NAV))
        val periode = Periode(1.januar, 22.januar)
        undersøke(tidslinjer, 22.januar, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(4, inspektør.navDagTeller)
    }

    @Test
    fun `avviser etter dødsdato`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 7.NAV))
        val periode = Periode(1.januar, 23.januar)
        undersøke(tidslinjer, 19.januar, periode)
        assertEquals(3, inspektør.navDagTeller)
        assertEquals(2, inspektør.avvistDagTeller)
    }

    @Test
    fun `avviser ikke arbeidsgiverperiode`() {
        val tidslinjer = listOf(tidslinjeOf(16.AP, 7.NAV))
        val periode = Periode(1.januar, 23.januar)
        undersøke(tidslinjer, 1.januar, periode)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
    }

    @Test
    fun `avvis dager for begge tidslinjer`() {
        val tidslinjer = listOf(
            tidslinjeOf(16.AP, 5.NAV),
            tidslinjeOf(16.AP, 5.NAV)
        )
        val periode = Periode(1.januar, 21.januar)
        undersøke(tidslinjer, 1.januar, periode)
        assertEquals(3, tidslinjer.first().inspektør.avvistDagTeller)
        assertEquals(3, tidslinjer.last().inspektør.avvistDagTeller)
    }

    private fun undersøke(tidslinjer: List<Utbetalingstidslinje>, dødsdato: LocalDate?, periode: Periode) {
        aktivitetslogg = Aktivitetslogg()
        AvvisDagerEtterDødsdatofilter(dødsdato).filter(tidslinjer, periode, aktivitetslogg)
        inspektør = tidslinjer.first().inspektør
    }
}
