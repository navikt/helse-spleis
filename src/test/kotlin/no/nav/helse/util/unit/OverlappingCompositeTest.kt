package no.nav.helse.util.unit

import no.nav.helse.util.interval.Interval
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class OverlappingCompositeTest {

    companion object {
        private val tidligereTidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        private val senereTidspunktRapportert = LocalDateTime.of(2019,9,17, 10, 45)

        private val førsteMandag = LocalDate.of(2019,9,23)
        private val førsteTirsdag = LocalDate.of(2019,9,24)
        private val førsteOnsdag = LocalDate.of(2019,9,25)
        private val førsteTorsdag = LocalDate.of(2019,9,26)
        private val førsteFredag = LocalDate.of(2019,9,27)
        private val førsteLørdag = LocalDate.of(2019,9,28)
        private val førsteSøndag = LocalDate.of(2019,9,29)
        private val andreMandag = LocalDate.of(2019,9,30)
        private val andreTirsdag = LocalDate.of(2019,10,1)

    }

    private lateinit var interval: Interval

    @Test
    internal fun sykedagerOgFerie() {
        val sykedager = Interval.sykedager(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)
        val ferie = Interval.ferie(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)

        interval = sykedager + ferie

        assertInterval(førsteMandag, førsteTirsdag, 0, 2)
    }

    @Test
    internal fun overlappendeSykedager() {
        val sykedager1 = Interval.sykedager(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)
        val sykedager2 = Interval.sykedager(førsteMandag, førsteTirsdag, senereTidspunktRapportert)

        interval = sykedager2 + sykedager1

        assertInterval(førsteMandag, førsteTirsdag, 2, 2)
    }

    @Test
    internal fun trailingOverlapp() {
        val sykedager = Interval.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Interval.ikkeSykedager(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        interval = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun leadingOverlapp() {
        val sykedager = Interval.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Interval.ikkeSykedager(førsteMandag, førsteTirsdag, senereTidspunktRapportert)

        interval = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        val sykedager = Interval.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Interval.ikkeSykedager(førsteTirsdag, førsteOnsdag, senereTidspunktRapportert)

        interval = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        val sykedager = Interval.sykedager(førsteMandag, førsteOnsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Interval.ikkeSykedager(førsteTirsdag, førsteTorsdag, senereTidspunktRapportert)

        interval = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 1, 4)
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        val sykedager = Interval.sykedager(førsteTorsdag, andreMandag, tidligereTidspunktRapportert)
        val ferie = Interval.ferie(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        interval = sykedager + ferie

        assertInterval(førsteOnsdag, andreMandag, 4, 6)
    }

    @Test
    internal fun friskHelg() {
        val sykedager = Interval.sykedager(førsteTorsdag, andreMandag, tidligereTidspunktRapportert)
        val ferie = Interval.ferie(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        interval = sykedager + ferie

        assertInterval(førsteOnsdag, andreMandag, 4, 6)
    }




    private fun assertInterval(startdag: LocalDate, sluttdag: LocalDate, antallSykedager: Int, forventetLengde: Int) {
        Assertions.assertEquals(startdag, interval.startdato())
        Assertions.assertEquals(sluttdag, interval.sluttdato())
        Assertions.assertEquals(antallSykedager, interval.antallSykedager())
        Assertions.assertEquals(forventetLengde, interval.flatten().size)
    }
}
