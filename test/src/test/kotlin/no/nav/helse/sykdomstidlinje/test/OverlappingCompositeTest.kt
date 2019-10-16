package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverlappingCompositeTest {

    private val nySøknad = Testhendelse(
        rapportertdato = 2.fredag.atTime(12, 0),
        hendelsetype = Sykdomshendelse.Type.NySykepengesøknad
    )
    private val sendtSøknad = Testhendelse(
        rapportertdato = 3.fredag.atTime(12, 0),
        hendelsetype = Sykdomshendelse.Type.SendtSykepengesøknad
    )

    private lateinit var sykdomstidslinje: Sykdomstidslinje

    @Test
    internal fun sykedagerOgFerie() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.mandag, 1.tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(1.mandag, 1.tirsdag, 0, 2)
    }

    @Test
    internal fun overlappendeSykedager() {
        val sykedager1 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, nySøknad)
        val sykedager2 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager2 + sykedager1

        assertInterval(1.mandag, 1.tirsdag, 2, 2)
    }

    @Test
    internal fun trailingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(1.onsdag, 1.torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(1.mandag, 1.torsdag, 2, 4)
    }

    @Test
    internal fun leadingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(1.mandag, 1.tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(1.mandag, 1.torsdag, 2, 4)
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(1.tirsdag, 1.onsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(1.mandag, 1.torsdag, 2, 4)
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(1.tirsdag, 1.torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(1.mandag, 1.torsdag, 1, 4)
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        val sykedager = Sykdomstidslinje.sykedager(1.torsdag, 2.mandag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.onsdag, 1.torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(1.onsdag, 2.mandag, 4, 6)
    }

    @Test
    internal fun friskHelg() {
        val sykedager = Sykdomstidslinje.sykedager(1.torsdag, 2.mandag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.onsdag, 1.torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(1.onsdag, 2.mandag, 4, 6)
    }


    @Test
    internal fun `sykdomstidslinjer som er kant i kant overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.torsdag, 2.mandag, sendtSøknad)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    @Test
    internal fun `sykdomstidslinjer overlapper`(){
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.onsdag, 2.mandag, sendtSøknad)

        assertTrue(sykedager.overlapperMed(ferie))
    }

    @Test
    internal fun `sykdomstidslinjer med et gap på en hel dag overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(1.torsdag, 2.mandag, sendtSøknad)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    private fun assertInterval(startdag: LocalDate, sluttdag: LocalDate, antallSykedager: Int, forventetLengde: Int) {
        assertEquals(startdag, sykdomstidslinje.startdato())
        assertEquals(sluttdag, sykdomstidslinje.sluttdato())
        assertEquals(antallSykedager, sykdomstidslinje.antallSykedagerHvorViTellerMedHelg())
        assertEquals(forventetLengde, sykdomstidslinje.flatten().size)
    }
}
