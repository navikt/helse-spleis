package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OldtidsutbetalingerEnArbeidsgiverTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "12345678"
        private val PERSON = Person(AKTØRID, UNG_PERSON_FNR_2018)
        private val ARBEIDSGIVER = Arbeidsgiver(PERSON, ORGNUMMER)
    }

    @Test
    fun `sjekk tilstøtende for en enkelt periode`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(19.NAV))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertTrue(oldtid.arbeidsgiverperiodeBetalt(ARBEIDSGIVER))
        assertEquals(1.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `sammenhengende ferie mellom to perioder gir tilstøtende`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(1.UTELATE, 18.FRI))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(1.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `ferie før første nav dag blir ignorert`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(18.FRI))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(19.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `ferie mellom nav dager gir ikke gap`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(1.UTELATE, 17.FRI))
        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(1.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `ferie med gap dag foran blir ignorert`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(2.UTELATE, 16.FRI))
        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))

        assertTrue(oldtid.tilstøtende(ARBEIDSGIVER))
        assertEquals(19.januar, oldtid.førsteUtbetalingsdag(ARBEIDSGIVER))
    }

    @Test
    fun `periode er ikke tilstøtende til kun feriedager`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(tidslinje = tidslinjeOf(19.FRI))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
        assertThrows<IllegalArgumentException> { oldtid.førsteUtbetalingsdag(ARBEIDSGIVER) }
    }

    @Test
    fun `ikke tilstøtende når det finnes et gap`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(18.NAV))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
        assertFalse(oldtid.arbeidsgiverperiodeBetalt(ARBEIDSGIVER))
        assertThrows<IllegalArgumentException> { oldtid.førsteUtbetalingsdag(ARBEIDSGIVER) }
    }

    @Test
    fun `tom utbetalingstidslinje er aldri tilstøtende`() {
        val oldtid = Oldtidsutbetalinger(Periode(22.januar, 31.januar))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
    }

    @Test
    fun `tilstøtende sjekker bare mot utbetalinger tidligere enn periodeTom`() {
        val oldtid = Oldtidsutbetalinger(Periode(1.januar, 12.januar))

        oldtid.add(ORGNUMMER, tidslinjeOf(12.UTELATE, 12.NAV))

        assertFalse(oldtid.tilstøtende(ARBEIDSGIVER))
    }
}
