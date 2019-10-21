package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.hendelse.DokumentMottattHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverlappingCompositeTest {

    private val nySøknad = Testhendelse(
        rapportertdato = Uke(2).fredag.atTime(12, 0),
        hendelsetype = DokumentMottattHendelse.Type.NySøknadOpprettet
    )
    private val sendtSøknad = Testhendelse(
        rapportertdato = Uke(3).fredag.atTime(12, 0),
        hendelsetype = DokumentMottattHendelse.Type.SendtSøknadMottatt
    )

    private lateinit var sykdomstidslinje: Sykdomstidslinje

    @Test
    internal fun sykedagerOgFerie() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).mandag, Uke(1).tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(Uke(1).mandag, Uke(1).tirsdag, 0, 2)
    }

    @Test
    internal fun overlappendeSykedager() {
        val sykedager1 = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, nySøknad)
        val sykedager2 = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager2 + sykedager1

        assertInterval(Uke(1).mandag, Uke(1).tirsdag, 2, 2)
    }

    @Test
    internal fun trailingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(1).onsdag, Uke(1).torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(Uke(1).mandag, Uke(1).torsdag, 2, 4)
    }

    @Test
    internal fun leadingOverlapp() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(1).tirsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(Uke(1).mandag, Uke(1).torsdag, 2, 4)
    }

    @Test
    internal fun arbeidIMidtenAvSykdom() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).torsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(1).tirsdag, Uke(1).onsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(Uke(1).mandag, Uke(1).torsdag, 2, 4)
    }

    @Test
    internal fun leadingAndTrailingIntervals() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, nySøknad)
        val arbeidsdager = Sykdomstidslinje.ikkeSykedager(Uke(1).tirsdag, Uke(1).torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + arbeidsdager

        assertInterval(Uke(1).mandag, Uke(1).torsdag, 1, 4)
    }

    @Test
    internal fun sykHelgMedLedendeHelg() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).torsdag, Uke(2).mandag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).onsdag, Uke(1).torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(Uke(1).onsdag, Uke(2).mandag, 4, 6)
    }

    @Test
    internal fun friskHelg() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).torsdag, Uke(2).mandag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).onsdag, Uke(1).torsdag, sendtSøknad)

        sykdomstidslinje = sykedager + ferie

        assertInterval(Uke(1).onsdag, Uke(2).mandag, 4, 6)
    }


    @Test
    internal fun `sykdomstidslinjer som er kant i kant overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).torsdag, Uke(2).mandag, sendtSøknad)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    @Test
    internal fun `sykdomstidslinjer overlapper`(){
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).onsdag, Uke(2).mandag, sendtSøknad)

        assertTrue(sykedager.overlapperMed(ferie))
    }

    @Test
    internal fun `sykdomstidslinjer med et gap på en hel dag overlapper ikke`(){
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag, nySøknad)
        val ferie = Sykdomstidslinje.ferie(Uke(1).torsdag, Uke(2).mandag, sendtSøknad)

        assertFalse(sykedager.overlapperMed(ferie))
    }


    private fun assertInterval(startdag: LocalDate, sluttdag: LocalDate, antallSykedager: Int, forventetLengde: Int) {
        assertEquals(startdag, sykdomstidslinje.startdato())
        assertEquals(sluttdag, sykdomstidslinje.sluttdato())
        assertEquals(antallSykedager, sykdomstidslinje.antallSykedagerHvorViTellerMedHelg())
        assertEquals(forventetLengde, sykdomstidslinje.flatten().size)
    }
}
