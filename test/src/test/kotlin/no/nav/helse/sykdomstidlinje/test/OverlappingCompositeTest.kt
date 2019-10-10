package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class OverlappingCompositeTest {

    companion object {
        private val tidligereTidspunktRapportert = Testhendelse(LocalDateTime.of(2019,9,16, 10, 45))
        private val senereTidspunktRapportert = Testhendelse(LocalDateTime.of(2019,9,17, 10, 45))

        private val førsteMandag = LocalDate.of(2019,9,23)
        private val førsteTirsdag = LocalDate.of(2019,9,24)
        private val førsteOnsdag = LocalDate.of(2019,9,25)
        private val førsteTorsdag = LocalDate.of(2019,9,26)
        private val andreMandag = LocalDate.of(2019,9,30)
    }

    private lateinit var sykdomstidslinje: Sykdomstidslinje

    @Test
    internal fun sykedagerOgFerie() {
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)

        sykdomstidslinje = sykedager + ferie

        assertInterval(førsteMandag, førsteTirsdag, 0, 2)
    }

    @Test
    internal fun overlappendeSykedager() {
        val sykedager1 = Sykdomstidslinje.sykedager(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)
        val sykedager2 = Sykdomstidslinje.sykedager(førsteMandag, førsteTirsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager2 + sykedager1

        assertInterval(førsteMandag, førsteTirsdag, 2, 2)
    }

    @Test
    internal fun trailingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun leadingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(førsteMandag, førsteTirsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteTorsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(førsteTirsdag, førsteOnsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 2, 4)
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteOnsdag, tidligereTidspunktRapportert)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(førsteTirsdag, førsteTorsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(førsteMandag, førsteTorsdag, 1, 4)
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        val sykedager = Sykdomstidslinje.sykedager(førsteTorsdag, andreMandag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + ferie

        assertInterval(førsteOnsdag, andreMandag, 4, 6)
    }

    @Test
    internal fun friskHelg() {
        val sykedager = Sykdomstidslinje.sykedager(førsteTorsdag, andreMandag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteOnsdag, førsteTorsdag, senereTidspunktRapportert)

        sykdomstidslinje = sykedager + ferie

        assertInterval(førsteOnsdag, andreMandag, 4, 6)
    }


    @Test
    internal fun `sykdomstidslinjer som er kant i kant overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteOnsdag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteTorsdag, andreMandag, senereTidspunktRapportert)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    @Test
    internal fun `sykdomstidslinjer overlapper`(){
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteOnsdag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteOnsdag, andreMandag, senereTidspunktRapportert)

        assertTrue(sykedager.overlapperMed(ferie))
    }

    @Test
    internal fun `sykdomstidslinjer med et gap på en hel dag overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(førsteMandag, førsteTirsdag, tidligereTidspunktRapportert)
        val ferie = Sykdomstidslinje.ferie(førsteTorsdag, andreMandag, senereTidspunktRapportert)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    private fun assertInterval(startdag: LocalDate, sluttdag: LocalDate, antallSykedager: Int, forventetLengde: Int) {
        Assertions.assertEquals(startdag, sykdomstidslinje.startdato())
        Assertions.assertEquals(sluttdag, sykdomstidslinje.sluttdato())
        Assertions.assertEquals(antallSykedager, sykdomstidslinje.antallSykedagerHvorViTellerMedHelg())
        Assertions.assertEquals(forventetLengde, sykdomstidslinje.flatten().size)
    }
}
