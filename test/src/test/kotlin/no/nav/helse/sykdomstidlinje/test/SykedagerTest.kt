package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SykedagerTest {

    companion object {
        private val sendtSykmelding = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
    }

    @Test
    fun `test sykdager over helg`() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSykmelding)
        assertEquals(8, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `er innenfor arbeidsgiverperioden`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(2.fredag, 4.mandag, sendtSykmelding)
        val tidslinje = (influensa + spysyka).syketilfeller()

        assertEquals(1, tidslinje.size)
    }

    @Test
    fun `søknad over 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 3.mandag, sendtSykmelding)
        val grupper = influensa.syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(1.mandag, grupper.first().startdato())
        assertEquals(3.mandag, grupper.first().sluttdato())
    }

    @Test
    fun `to søknader innenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(3.mandag, 3.fredag, sendtSykmelding)
        val grupper = (influensa + spysyka).syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(1.mandag, grupper.first().startdato())
        assertEquals(3.fredag, grupper.first().sluttdato())
    }

    @Test
    fun `to søknader utenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(5.mandag, 5.fredag, sendtSykmelding)
        val grupper = (influensa + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(1.mandag, grupper[0].startdato())
        assertEquals(1.fredag, grupper[0].sluttdato())

        assertEquals(5.mandag, grupper[1].startdato())
        assertEquals(5.fredag, grupper[1].sluttdato())
    }

    @Test
    fun `søknad med påfølgende ferie uten gap kobles sammen med ny søknad innenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(2.tirsdag, 3.fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(5.fredag, 6.fredag, sendtSykmelding)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(1.mandag, grupper.first().startdato())
        assertEquals(6.fredag, grupper.first().sluttdato())
    }

    @Test
    fun `søknad med ferie - helg - ferie kobles sammen til en sykeperiode`() {
        //       |D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|
        //
        // syk   |S|S|S|
        // ferie1      |F|F|
        // ferie2              |F|F|F|F|F|
        // ferie3                            |F|F|F|F|F|
        // syk                                             |S|S|S|S|
        //
        //       |S|S|S|F|F|-|-|F|F|F|F|F|-|-|F|F|F|F|F|-|-|S|S|S|S|

        val influensa = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, sendtSykmelding)
        val ferieDelEn = Sykdomstidslinje.ferie(1.torsdag, 1.fredag, sendtSykmelding)
        val ferieDelTo = Sykdomstidslinje.ferie(2.mandag, 2.fredag, sendtSykmelding)
        val ferieDelTre = Sykdomstidslinje.ferie(3.mandag, 3.fredag, sendtSykmelding)
        val malaria = Sykdomstidslinje.sykedager(5.mandag, 5.torsdag, sendtSykmelding)

        val tilfeller = (influensa + ferieDelEn + ferieDelTo + ferieDelTre + malaria).also { println(it) }.syketilfeller()

        assertEquals(1, tilfeller.size)
        assertEquals(1.mandag, tilfeller.first().startdato())
        assertEquals(5.torsdag, tilfeller.first().sluttdato())
    }

    @Test
    fun `søknad med opphold ferie kobles ikke sammen med ny søknad innenfor 16 dager`() {
        //       |D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|H|H|D|D|D|D|D|
        //
        // syk   |S|S|S|S|S|S|S|S|
        // ferie1                  |F|F|F|
        // syk                                                     |S|S|S|S|S|S|S|

        //
        //tidsl  |S|S|S|S|S|S|S|S|-|F|F|F|-|-|-|-|-|-|-|-|-|-|-|-|-|S|S|S|S|S|S|S|
        //
        //tilf 1 |S|S|S|S|S|S|S|S|
        //tilf 2                                                   |S|S|S|S|S|S|S|

        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(2.onsdag, 2.fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(4.fredag, 5.fredag, sendtSykmelding)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(1.mandag, grupper[0].startdato())
        assertEquals(2.mandag, grupper[0].sluttdato())

        assertEquals(4.fredag, grupper[1].startdato())
        assertEquals(5.fredag, grupper[1].sluttdato())
    }

    @Test
    fun `søknad med trailing ferie gir bare søknad`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(2.tirsdag, 2.fredag, sendtSykmelding)

        val grupper = (influensa + ferie).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(1.mandag, grupper.first().startdato())
        assertEquals(2.mandag, grupper.first().sluttdato())
    }

    @Test
    fun `kutter helg i slutten av gruppe`() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 1.søndag, sendtSykmelding)

        val grupper = influensa.syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(1.mandag, grupper.first().startdato())
        assertEquals(1.søndag, grupper.first().sluttdato())
    }

    @Test
    fun `gitt en person som har vært syk i 16 dager, har ferie i 15 dager, jobber i 5 dager og deretter blir syk i 10 nye dager så skal den siste perioden telle som en del av samme sykdomstilfelle`() {
        //       |D*16|D*15|D|D|D|D|D*10|
        //
        // syk1  |S*16|
        // ferie      |F*15|
        // arbeid          |A|A|A|A|
        // syk2                    |S*10|
        //
        // tidsl |S*16|F*15|A|A|A|A|S*10|
        //
        // tilf  |S*16|F*15|A|A|A|A|S*10|
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 3.tirsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(3.onsdag, 5.onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(6.mandag, 7.onsdag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()
        println(syketilfeller)

        assertEquals(1, syketilfeller.size)
    }

    @Test
    fun `gitt en person som har vært syk i 15 dager, har ferie i 15 dager, jobber i 4 dager og deretter blir syk i 10 nye dager så skal den siste perioden telle som en ny sykdomsperiode`() {
        //       |D*15|D*15|D|D|D|D|D*10|
        //
        // syk1  |S*15|
        // ferie      |F*15|
        // arbeid          |A|A|A|A|
        // syk2                    |S*10|
        //
        // tidsl |S*15|F*15|A|A|A|A|S*10|
        //
        // tilf1 |S*15|
        // tilf2                   |S*10|
        val influensa = Sykdomstidslinje.sykedager(1.tirsdag, 3.tirsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(3.onsdag, 5.onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(6.mandag, 7.onsdag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()
        println(syketilfeller)
        assertEquals(2, syketilfeller.size)
    }

    @Test
    fun `gitt en person som har vært syk i 10 dager, har ferie i 20 dager og deretter blir syk i 9 nye dager så skal den siste perioden telle som en del av samme sykdomstilfelle`() {
        //       |D*10|D*20|D*10|
        //
        // syk1  |S*10|
        // ferie      |F*20|
        // syk2            |S*10|
        //
        // tidsl |S*10|F*20|S*10|
        //
        // tilf  |S*10|F*20|S*10|
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.onsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(2.torsdag, 5.onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(5.torsdag, 6.fredag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()
        println(syketilfeller)

        assertEquals(1, syketilfeller.size)
    }

    @Test fun `gitt en person som har vært syk i 2 dager, har ferie i 10 dager og deretter blir syk i 2 nye dager er innenfor arbeidsgiverperioden`() {}

    @Test fun `gitt en person som har vært syk i 2 dager, har ferie i 14 dager og deretter blir syk i 2 nye dager er utenfor arbeidsgiverperioden`() {}
}
