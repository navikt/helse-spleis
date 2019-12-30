package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AlderReglerTest {

    private val startDato = 1.januar
    private val sluttDato = 1.januar.plusYears(1)

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        internal const val PERSON_67_ÅR_FNR_2018 = "01015112345"
    }

    @Test
    fun `ung person`() {
        assertTrue(UNG_PERSON_FNR_2018.navBurdeBetale(247, 28.desember))
        assertFalse(UNG_PERSON_FNR_2018.navBurdeBetale(248, 28.desember))
    }

    @Test
    fun `ung person med D-nummer`() {
        assertTrue("52029812345".navBurdeBetale(247, 28.desember))
        assertFalse("52029812345".navBurdeBetale(248, 28.desember))
    }

    @Test
    fun `person som fyller 70 i perioden`() {
        assertTrue("12024812345".navBurdeBetale(2, 11.februar))
        assertFalse("12024812345".navBurdeBetale(2, 12.februar))
    }

    @Test
    fun `person som er 70 før sykeperioden starter`() {
        assertFalse("12024712345".navBurdeBetale(2, 12.februar))
    }

    @Test
    fun `person som fyller 67 i perioden`() {
        assertTrue("12025112345".navBurdeBetale(2, 11.februar))
        assertTrue("12025112345".navBurdeBetale(100, 12.februar))
        assertTrue("12025112345".navBurdeBetale(100, 12.mai, 59))
        assertEquals(1, "12025112345".antallGjenståendeSykedager(100, 12.mai, 59))
        assertFalse("12025112345".navBurdeBetale(100, 12.mai, 60))
        assertEquals(0, "12025112345".antallGjenståendeSykedager(100, 12.mai, 60))
        assertFalse("12025112345".navBurdeBetale(248, 12.mai, 59))
        assertEquals(0, "12025112345".antallGjenståendeSykedager(248, 12.mai, 59))
    }

    @Test
    fun `person som er mellom 67 and 70 i hele perioden`() {
        assertTrue("12025012345".navBurdeBetale(2, 11.februar))
        assertTrue("12025012345".navBurdeBetale(59, 11.februar))
        assertFalse("12025012345".navBurdeBetale(60, 11.februar))
    }

    @Test
    fun `ung person får korrekt maksdato`() {
        assertEquals(15.mai, UNG_PERSON_FNR_2018.maksdato(248, 15.mai))
        assertEquals(0, UNG_PERSON_FNR_2018.antallGjenståendeSykedager(248, 15.mai))
        assertEquals(18.mai, UNG_PERSON_FNR_2018.maksdato(244, 14.mai))
        assertEquals(4, UNG_PERSON_FNR_2018.antallGjenståendeSykedager(244, 14.mai))
        assertEquals(21.mai, UNG_PERSON_FNR_2018.maksdato(243, 14.mai))
        assertEquals(5, UNG_PERSON_FNR_2018.antallGjenståendeSykedager(243, 14.mai))
        assertEquals(22.mai, UNG_PERSON_FNR_2018.maksdato(242, 14.mai))
        assertEquals(6, UNG_PERSON_FNR_2018.antallGjenståendeSykedager(242, 14.mai))
        assertEquals(28.desember, UNG_PERSON_FNR_2018.maksdato(1, 17.januar))
        assertEquals(247, UNG_PERSON_FNR_2018.antallGjenståendeSykedager(1, 17.januar))
    }

    @Test
    fun `person som fyller 70 får ikke maksdato senere enn dagen før 70-årsdagen`() {
        assertEquals(11.februar, "12024812345".maksdato(1, 17.januar, 1))
        assertEquals(18, "12024812345".antallGjenståendeSykedager(1, 17.januar, 1))
        assertEquals(22.januar, "12024812345".maksdato(57, 17.januar, 57))
        assertEquals(3, "12024812345".antallGjenståendeSykedager(57, 17.januar, 57))
    }

    @Test
    fun `person mellom 67 og 70 år får maks 60 sykepengedager`() {
        assertEquals(7.mai, "12025112345".maksdato(65, 17.januar, 0))
        assertEquals(79, "12025112345".antallGjenståendeSykedager(65, 17.januar, 0))
        assertEquals(12.februar, "12025112345".maksdato(247, 9.februar, 0))
        assertEquals(1, "12025112345".antallGjenståendeSykedager(247, 9.februar, 0))
        assertEquals(13.februar, "12025112345".maksdato(246, 9.februar, 0))
        assertEquals(2, "12025112345".antallGjenståendeSykedager(246, 9.februar, 0))
    }

    private fun String.navBurdeBetale(antallDager: Int, dagen: LocalDate, antallDagerEtter67: Int = 0) = AlderRegler(
        this,
        startDato,
        sluttDato,
        ArbeidsgiverRegler.Companion.NormalArbeidstaker
    ).navBurdeBetale(antallDager, antallDagerEtter67, dagen)
    private fun String.maksdato(antallDager: Int, dagen: LocalDate, antallDagerEtter67: Int = 0) = AlderRegler(
        this,
        startDato,
        sluttDato,
        ArbeidsgiverRegler.Companion.NormalArbeidstaker
    ).maksdato(antallDager, antallDagerEtter67, dagen)
    private fun String.antallGjenståendeSykedager(antallDager: Int, dagen: LocalDate, antallDagerEtter67: Int = 0) = AlderRegler(
        this,
        startDato,
        sluttDato,
        ArbeidsgiverRegler.Companion.NormalArbeidstaker
    ).gjenståendeDager(antallDager, antallDagerEtter67, dagen)
}
