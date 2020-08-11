package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(19.NAV))

        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).arbeidsgiverperiodeErBetalt(periode))
        assertEquals(1.januar, oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode))
    }

    @Test
    fun `sammenhengende ferie mellom to perioder gir tilstøtende`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(1.UTELATE, 18.FRI))

        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertEquals(1.januar, oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode))
    }

    @Test
    fun `ferie før første Nav-dag blir ignorert`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(18.FRI))

        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertEquals(19.januar, oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode))
    }

    @Test
    fun `ferie mellom Nav-dager gir ikke gap`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(1.UTELATE, 17.FRI))
        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))

        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertEquals(1.januar, oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode))
    }

    @Test
    fun `ferie med gap dag foran blir ignorert`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(1.NAV))
        oldtid.add(tidslinje = tidslinjeOf(2.UTELATE, 16.FRI))
        oldtid.add(ORGNUMMER, tidslinjeOf(18.UTELATE, 1.NAV))

        assertTrue(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertEquals(19.januar, oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode))
    }

    @Test
    fun `periode er ikke tilstøtende til kun feriedager`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(tidslinje = tidslinjeOf(19.FRI))

        assertFalse(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertThrows<IllegalArgumentException> { oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode) }
    }

    @Test
    fun `ikke tilstøtende når det finnes et gap`() {
        val periode = 22.januar til 31.januar
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(18.NAV))

        assertFalse(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(periode))
        assertFalse(oldtid.utbetalingerInkludert(ARBEIDSGIVER).arbeidsgiverperiodeErBetalt(periode))
        assertThrows<IllegalArgumentException> { oldtid.utbetalingerInkludert(ARBEIDSGIVER).førsteUtbetalingsdag(periode) }
    }

    @Test
    fun `tom utbetalingstidslinje er aldri tilstøtende`() {
        val oldtid = Oldtidsutbetalinger()

        assertFalse(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(22.januar til 31.januar))
    }

    @Test
    fun `tilstøtende sjekker bare mot utbetalinger tidligere enn periodeTom`() {
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER, tidslinjeOf(12.UTELATE, 12.NAV))

        assertFalse(oldtid.utbetalingerInkludert(ARBEIDSGIVER).erRettFør(1.januar til 12.januar))
    }
}



