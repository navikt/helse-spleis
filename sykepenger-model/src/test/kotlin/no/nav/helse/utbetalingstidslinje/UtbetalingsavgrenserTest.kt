package no.nav.helse.utbetalingstidslinje

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
        val tidslinje = tidslinjeOf(10.AP, 10.N)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `stopper betaling etter 248 dager`() {
        val tidslinje = tidslinjeOf(249.N)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `26 uker arbeid resetter utbetalingsgrense`() {
        val tidslinje = tidslinjeOf(248.N, (26*7).A, 10.N)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `en ubetalt sykedag før opphold`() {
        val tidslinje = tidslinjeOf(249.N, (26*7).A, 10.N)
        assertEquals(listOf(6.september), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `utbetaling stopper når du blir 70 år`() {
        val tidslinje = tidslinjeOf(11.N)
        assertEquals(listOf(10.januar, 11.januar), tidslinje.utbetalingsavgrenser(PERSON_70_ÅR_FNR_2018))
    }

    @Test
    fun `noe som helst sykdom i opphold resetter teller`() {
        val tidslinje = tidslinjeOf(248.N, (24*7).A, 7.N, (2*7).A, 10.N)
        assertEquals(17, tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018).size)
    }

    @Test
    fun `fridag etter sykdag er en del av opphold`() {
        val tidslinje = tidslinjeOf(248.N, (25*7).F, 7.A, 7.N)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `opphold på mindre enn 26 uker skal ikke nullstille telleren`() {
        val tidslinje = tidslinjeOf(248.N, (26*7 - 1).F, 1.N)
        assertEquals(listOf(6.mars(2019)), tidslinje.utbetalingsavgrenser(UNG_PERSON_FNR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer`() {
        val tidslinje = tidslinjeOf(10.N, 61.N)
        assertEquals(listOf(12.mars), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    @Test
    fun `sjekk 60 dagers grense for 67 åringer med 26 ukers opphold`() {
        val tidslinje = tidslinjeOf(10.N, 60.N, (26*7).A, 60.N)
        assertEquals(emptyList<LocalDate>(), tidslinje.utbetalingsavgrenser(PERSON_67_ÅR_FNR_2018))
    }

    private fun tidslinjeOf(
        vararg dagPairs: Pair<Int, Utbetalingstidslinje.(Double, LocalDate) -> Unit>
    ) = Utbetalingstidslinje().apply {
        var startDato = LocalDate.of(2018, 1, 1)
        for ((antallDager, utbetalingsdag) in dagPairs) {
            val sluttDato = startDato.plusDays(antallDager.toLong())
            startDato.datesUntil(sluttDato).forEach {
                this.utbetalingsdag(1200.0, it)
            }
            startDato = sluttDato
        }
    }

    private fun Utbetalingstidslinje.utbetalingsavgrenser(fnr: String) =
        Utbetalingsavgrenser(
            this,
            AlderRegler(fnr,
                LocalDate.of(2018,1,1),
                LocalDate.of(2019, 12, 31)
            )).ubetalteDager().map { it.dag }
    private val Int.AP get() = Pair(this, Utbetalingstidslinje::addArbeidsgiverperiodedag)
    private val Int.N get() = Pair(this, Utbetalingstidslinje::addNAVdag)
    private val Int.A get() = Pair(this, Utbetalingstidslinje::addArbeidsdag)
    private val Int.F get() = Pair(this, Utbetalingstidslinje::addFridag)

    private val Int.januar get() = this.januar(2018)
    private fun Int.januar(år: Int) = LocalDate.of(år, 1, this)
    private val Int.mars get() = this.mars(2018)
    private fun Int.mars(år: Int) = LocalDate.of(år, 3, this)
    private val Int.september get() = this.september(2018)
    private fun Int.september(år: Int) = LocalDate.of(år, 9, this)
}
