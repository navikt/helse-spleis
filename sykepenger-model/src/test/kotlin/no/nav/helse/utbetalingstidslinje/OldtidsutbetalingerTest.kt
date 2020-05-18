package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OldtidsutbetalingerTest {
    private companion object {
        private const val ORGNUMMER = "12345678"
        private val PERSON = Person("aktørId", "fnr")
        private val ARBEIDSGIVER = Arbeidsgiver(PERSON, ORGNUMMER)
    }

    @Test
    fun `sjekk tilstøtende for en enkelt periode`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.addUtbetaling(ORGNUMMER, tidslinjeOf(19.NAV))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(1.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `sammenhengende ferie mellom to perioder gir tilstøtende`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.addUtbetaling(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.addFerie(tidslinje = tidslinjeOf(1.UTELATE, 18.FRI))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(1.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `periode er ikke tilstøtende til kun feriedager`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.addFerie(tidslinje = tidslinjeOf(19.FRI))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
        assertThrows<IllegalArgumentException> { oldtid.førsteUtbetalingsdag(ARBEIDSGIVER) }
    }

    @Test
    fun `ikke tilstøtende når det finnes et gap`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.addUtbetaling(ORGNUMMER, tidslinjeOf(18.NAV))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
        assertThrows<IllegalArgumentException> { oldtid.førsteUtbetalingsdag(ARBEIDSGIVER) }
    }

    @Test
    fun `tom utbetalingstidslinje er aldri tilstøtende`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
    }
}
