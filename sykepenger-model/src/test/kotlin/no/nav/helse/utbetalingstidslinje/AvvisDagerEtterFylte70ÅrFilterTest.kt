package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.NAVv2
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvvisDagerEtterFylte70ÅrFilterTest {
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    companion object {
        private val FYLLER_70_FØRSTE_FEBRUAR = Alder("01024812345".somFødselsnummer())
        private val FYLLER_70_SØNDAG_FJERDE_FEBRUAR = Alder("04024812345".somFødselsnummer())
    }

    @Test
    fun `ingen utbetaling hvis søker fyller 70 år før perioden`() {
        undersøke(FYLLER_70_FØRSTE_FEBRUAR, 2.februar til 28.februar)

        assertEquals(0, inspektør.navDagTeller)
        assertEquals(27, inspektør.avvistDagTeller)
    }

    @Test
    fun `full utbetaling hvis søker er under 70 år i hele perioden`() {
        undersøke(FYLLER_70_FØRSTE_FEBRUAR, 1.januar til 31.januar)

        assertEquals(0, inspektør.avvistDagTeller)
        assertEquals(23, inspektør.navDagTeller)
        assertEquals(8, inspektør.navHelgDagTeller)
    }

    @Test
    fun `utbetaling til dagen før 70-årsdagen`() {
        undersøke(FYLLER_70_FØRSTE_FEBRUAR, 31.januar til 1.februar)

        assertEquals(1, inspektør.navDagTeller)
        assertEquals(1, inspektør.avvistDagTeller)
    }

    @Test
    fun `ingen utbetaling hvis søker fyller 70 år første dag i perioden`() {
        undersøke(FYLLER_70_FØRSTE_FEBRUAR, 1.februar til 28.februar)

        assertEquals(0, inspektør.navDagTeller)
        assertEquals(28, inspektør.avvistDagTeller)
    }

    @Test
    fun `utbetaling på fredagen hvis søker fyller 70 søndagen etter`() {
        undersøke(FYLLER_70_SØNDAG_FJERDE_FEBRUAR, 1.februar til 28.februar)
        assertEquals(2, inspektør.navDagTeller)
        assertEquals(1, inspektør.navHelgDagTeller)
        assertEquals(25, inspektør.avvistDagTeller)
    }

    private fun undersøke(alder: Alder, periode: Periode) {
        aktivitetslogg = Aktivitetslogg()
        val tidslinjer = listOf(tidslinjeOf(9001.NAVv2, startDato = periode.start).kutt(periode.endInclusive))
        AvvisDagerEtterFylte70ÅrFilter(tidslinjer, periode, alder, aktivitetslogg).filter()
        inspektør = tidslinjer.first().inspektør
    }
}
