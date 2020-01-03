package no.nav.helse.utbetalingstidslinje

import no.nav.helse.fixtures.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingsavgrenserTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val PERSON_70_ÅR_FNR_2018 = "10014812345"
        private const val PERSON_67_ÅR_FNR_2018 = "10015112345"
    }

    @Test
    fun `riktig antall dager`() {
        val tidslinje = tidslinjeOf(10.AP, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(249.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(248.NAV, (26*7).ARB, 10.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(249.NAV, (26*7).ARB, 10.NAV)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.NAV)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test
    fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje = tidslinjeOf(248.NAV, (24*7).ARB, 7.NAV, (2*7).ARB, 10.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (25*7).FRI, 7.ARB, 7.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(248.NAV, (26*7 - 1).FRI, 1.NAV)
        assertEquals(listOf(6.mars(2019)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(10.NAV, 61.NAV)
        assertEquals(listOf(12.mars), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.NAV, 60.NAV, (26*7).ARB, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling`() {
        val tidslinje = tidslinjeOf(248.NAV, (26*7).NAV, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `sjekk at 26 uker med syk etter karantene starter utbetaling gammel person`() {
        val tidslinje = tidslinjeOf(60.NAV, (26*7).NAV, 60.NAV)
        assertEquals(26*7, tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018).size)
    }

    @Test
    fun `helgedager teller som opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (26*7).HELG, 60.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `helgedager sammen med utbetalingsdager teller som opphold`() {
        val tidslinje = tidslinjeOf(248.NAV, (20*7).HELG, 7.NAV, (5*7).HELG, 60.NAV)
        assertEquals(7, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `sjekk at sykdom i arbgiver periode ikke ødelegger oppholdsperioden`() {
        val tidslinje = tidslinjeOf(50.NAV, (25*7).ARB, 7.AP, 248.NAV)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `helgedager innimellom utbetalingsdager betales ikke`() {
        val tidslinje = tidslinjeOf(200.NAV, 40.HELG, 48.NAV, 1.NAV)
        assertEquals(listOf(16.oktober), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    private fun Utbetalingstidslinje.utbetalingsavgrenser(fnr: String) =
        Utbetalingsgrense(Alder(fnr), ArbeidsgiverRegler.Companion.NormalArbeidstaker)
            .also { this.accept(it) }
            .ubetalteDager().map { it.dag }
}
