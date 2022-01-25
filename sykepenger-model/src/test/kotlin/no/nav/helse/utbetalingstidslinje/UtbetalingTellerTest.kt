package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.somFødselsnummer
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mai
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingTellerTest {
    internal val UNG_PERSON_FNR_2018 = "15010052345".somFødselsnummer().alder()
    internal val PERSON_67_ÅR_FNR_2018 = "15015112345".somFødselsnummer().alder()
    internal val PERSON_70_ÅR_FNR_2018 = "15014812345".somFødselsnummer().alder()
    private lateinit var grense: UtbetalingTeller

    @Test
    fun `Person under 67 år får utbetalt 248 dager`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertTrue(grense.erFørMaksdato(31.desember))
        grense.inkrementer(31.desember)
        assertFalse(grense.erFørMaksdato(31.desember))
    }

    @Test
    fun `Person som blir 67 år får utbetalt 60 dager etter 67 årsdagen`() {
        grense(PERSON_67_ÅR_FNR_2018, 15 + 59)
        assertTrue(grense.erFørMaksdato(31.desember))
        grense.inkrementer(31.desember)
        assertFalse(grense.erFørMaksdato(31.desember))
    }

    @Test
    fun `Person som blir 70 år har ikke utbetaling på 70 årsdagen`() {
        grense(PERSON_70_ÅR_FNR_2018, 11)
        assertTrue(grense.erFørMaksdato(11.januar))
        grense.inkrementer(12.januar)
        assertFalse(grense.erFørMaksdato(12.januar))
    }

    @Test
    fun `Person under 67 år får utbetalt 248 `() {
        grense(UNG_PERSON_FNR_2018, 248)
        assertFalse(grense.erFørMaksdato(31.desember))
        grense.resett()
        assertTrue(grense.erFørMaksdato(31.desember))
    }

    @Test
    fun `Person under 67`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertTrue(grense.erFørMaksdato(30.desember))
        grense.dekrementer(2.januar(2021))
        grense.inkrementer(30.desember)
        assertTrue(grense.erFørMaksdato(30.desember))
        grense.inkrementer(31.desember)
        assertFalse(grense.erFørMaksdato(31.desember))
    }

    @Test
    fun `Reset decrement impact`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertTrue(grense.erFørMaksdato(30.desember))
        grense.dekrementer(1.januar.minusDays(1))
        grense.inkrementer(31.desember)
        assertFalse(grense.erFørMaksdato(31.desember))
    }

    @Test
    fun maksdato() {
        undersøke(15.mai, UNG_PERSON_FNR_2018, 248, 15.mai)
        undersøke(18.mai, UNG_PERSON_FNR_2018, 244, 14.mai)
        undersøke(21.mai, UNG_PERSON_FNR_2018, 243, 14.mai)
        undersøke(22.mai, UNG_PERSON_FNR_2018, 242, 14.mai)
        undersøke(28.desember, UNG_PERSON_FNR_2018, 1, 17.januar)
        undersøke(9.februar, "12024812345".somFødselsnummer().alder(), 1, 17.januar)
        undersøke(22.januar, "12024812345".somFødselsnummer().alder(), 57, 17.januar)
        undersøke(7.mai, "12025112345".somFødselsnummer().alder(), 65, 17.januar)
        undersøke(12.februar, "12025112345".somFødselsnummer().alder(), 247, 9.februar)
        undersøke(13.februar, "12025112345".somFødselsnummer().alder(), 246, 9.februar)
    }

    private fun undersøke(expected: LocalDate, alder: Alder, dager: Int, sisteUtbetalingsdag: LocalDate) {
        grense(alder, dager, sisteUtbetalingsdag.minusDays(dager.toLong() - 1))
        assertEquals(expected, grense.maksdato(sisteUtbetalingsdag))
    }

    private fun grense(alder: Alder, dager: Int, dato: LocalDate = 1.januar) {
        grense = UtbetalingTeller(alder, ArbeidsgiverRegler.Companion.NormalArbeidstaker, Aktivitetslogg()).apply {
            this.resett()
            (0 until dager).forEach { this.inkrementer(dato.plusDays(it.toLong())) }
        }
    }

}
