package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingTellerTest {
    internal val UNG_PERSON_FNR_2018 = Alder("15010052345")
    internal val PERSON_67_ÅR_FNR_2018 = Alder("15015112345")
    internal val PERSON_70_ÅR_FNR_2018 = Alder("15014812345")
    private lateinit var grense: UtbetalingTeller

    @Test
    internal fun `Person under 67 år får utbetalt 248 dager`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertFalse(grense.påGrensen(31.desember))
        grense.inkrementer(31.desember)
        assertTrue(grense.påGrensen(31.desember))
    }

    @Test
    internal fun `Person som blir 67 år får utbetalt 60 dager etter 67 årsdagen`() {
        grense(PERSON_67_ÅR_FNR_2018, 15 + 59)
        assertFalse(grense.påGrensen(31.desember))
        grense.inkrementer(31.desember)
        assertTrue(grense.påGrensen(31.desember))
    }

    @Test
    internal fun `Person som blir 70 år har ikke utbetaling på 70 årsdagen`() {
        grense(PERSON_70_ÅR_FNR_2018, 11)
        assertFalse(grense.påGrensen(11.januar))
        grense.inkrementer(12.januar)
        assertTrue(grense.påGrensen(12.januar))
    }

    @Test
    internal fun `Person under 67 år får utbetalt 248 `() {
        grense(UNG_PERSON_FNR_2018, 248)
        assertTrue(grense.påGrensen(31.desember))
        grense.resett(31.desember)
        assertFalse(grense.påGrensen(31.desember))
    }

    @Test
    internal fun `Person under 67`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertFalse(grense.påGrensen(30.desember))
        grense.dekrementer(1.januar)
        grense.inkrementer(30.desember)
        assertFalse(grense.påGrensen(30.desember))
        grense.inkrementer(31.desember)
        assertTrue(grense.påGrensen(31.desember))
    }

    @Test
    internal fun `Reset decrement impact`() {
        grense(UNG_PERSON_FNR_2018, 247)
        assertFalse(grense.påGrensen(30.desember))
        grense.dekrementer(1.januar.minusDays(1))
        grense.inkrementer(31.desember)
        assertTrue(grense.påGrensen(31.desember))
    }

    @Test
    internal fun `maksdato`() {
        assertEquals(15.mai, UNG_PERSON_FNR_2018, 248, 15.mai)
        assertEquals(18.mai, UNG_PERSON_FNR_2018, 244, 14.mai)
        assertEquals(21.mai, UNG_PERSON_FNR_2018, 243, 14.mai)
        assertEquals(22.mai, UNG_PERSON_FNR_2018, 242, 14.mai)
        assertEquals(28.desember, UNG_PERSON_FNR_2018, 1, 17.januar)
        assertEquals(9.februar, Alder("12024812345"), 1, 17.januar)
        assertEquals(22.januar, Alder("12024812345"), 57, 17.januar)
        assertEquals(7.mai, Alder("12025112345"), 65, 17.januar)
        assertEquals(12.februar, Alder("12025112345"), 247, 9.februar)
        assertEquals(13.februar, Alder("12025112345"), 246, 9.februar)

    }

    private fun assertEquals(expected: LocalDate, alder: Alder, dager: Int, sisteUtbetalingsdag: LocalDate) {
        grense(alder, dager, sisteUtbetalingsdag.minusDays(dager.toLong() - 1))
        assertEquals(expected, grense.maksdato(sisteUtbetalingsdag))
    }

    private fun grense(alder: Alder, dager: Int, dato: LocalDate = 1.januar) {
        grense = UtbetalingTeller(alder, ArbeidsgiverRegler.Companion.NormalArbeidstaker).apply {
            this.resett(dato)
            (0 until dager).forEach { this.inkrementer(dato.plusDays(it.toLong())) }
        }
    }

}
