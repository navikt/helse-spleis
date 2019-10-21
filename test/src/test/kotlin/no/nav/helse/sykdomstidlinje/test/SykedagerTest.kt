package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SykedagerTest {

    companion object {
        private val sendtSykmelding = Testhendelse(Uke(4).fredag.atStartOfDay())
    }

    @Test
    fun `test sykdager over helg`() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        assertEquals(8, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `er innenfor arbeidsgiverperioden`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(2).fredag, Uke(4).mandag, sendtSykmelding)
        val tidslinje = (influensa + spysyka).syketilfeller()

        assertEquals(1, tidslinje.size)
    }

    @Test
    fun `søknad over 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).mandag, sendtSykmelding)
        val grupper = influensa.syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(Uke(1).mandag, grupper.first().startdato())
        assertEquals(Uke(3).mandag, grupper.first().sluttdato())
    }

    @Test
    fun `to søknader innenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(3).mandag, Uke(3).fredag, sendtSykmelding)
        val grupper = (influensa + spysyka).syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(Uke(1).mandag, grupper.first().startdato())
        assertEquals(Uke(3).fredag, grupper.first().sluttdato())
    }

    @Test
    fun `to søknader utenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(5).mandag, Uke(5).fredag, sendtSykmelding)
        val grupper = (influensa + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(Uke(1).mandag, grupper[0].startdato())
        assertEquals(Uke(1).fredag, grupper[0].sluttdato())

        assertEquals(Uke(5).mandag, grupper[1].startdato())
        assertEquals(Uke(5).fredag, grupper[1].sluttdato())
    }

    @Test
    fun `søknad med påfølgende ferie uten gap kobles sammen med ny søknad innenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(2).tirsdag, Uke(3).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).onsdag, Uke(6).fredag, sendtSykmelding)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(Uke(1).mandag, grupper.first().startdato())
        assertEquals(Uke(6).fredag, grupper.first().sluttdato())
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

        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag, sendtSykmelding)
        val ferieDelEn = Sykdomstidslinje.ferie(Uke(1).torsdag, Uke(1).fredag, sendtSykmelding)
        val ferieDelTo = Sykdomstidslinje.ferie(Uke(2).mandag, Uke(2).fredag, sendtSykmelding)
        val ferieDelTre = Sykdomstidslinje.ferie(Uke(3).mandag, Uke(3).fredag, sendtSykmelding)
        val malaria = Sykdomstidslinje.sykedager(Uke(4).mandag, Uke(4).torsdag, sendtSykmelding)

        val tilfeller = (influensa + ferieDelEn + ferieDelTo + ferieDelTre + malaria).syketilfeller()

        assertEquals(1, tilfeller.size)
        assertEquals(Uke(1).mandag, tilfeller.first().startdato())
        assertEquals(Uke(4).torsdag, tilfeller.first().sluttdato())
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

        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(2).onsdag, Uke(2).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).fredag, Uke(5).fredag, sendtSykmelding)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(Uke(1).mandag, grupper[0].startdato())
        assertEquals(Uke(2).mandag, grupper[0].sluttdato())

        assertEquals(Uke(4).fredag, grupper[1].startdato())
        assertEquals(Uke(5).fredag, grupper[1].sluttdato())
    }

    @Test
    fun `søknad med trailing ferie gir bare søknad`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(2).tirsdag, Uke(2).fredag, sendtSykmelding)

        val grupper = (influensa + ferie).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(Uke(1).mandag, grupper.first().startdato())
        assertEquals(Uke(2).mandag, grupper.first().sluttdato())
    }

    @Test
    fun `kutter helg i slutten av gruppe`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).søndag, sendtSykmelding)

        val grupper = influensa.syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(Uke(1).mandag, grupper.first().startdato())
        assertEquals(Uke(1).søndag, grupper.first().sluttdato())
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
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).tirsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(3).onsdag, Uke(5).onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(6).mandag, Uke(7).onsdag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()

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
        val influensa = Sykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(3).tirsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(3).onsdag, Uke(5).onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(6).mandag, Uke(7).onsdag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()
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
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).onsdag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(2).torsdag, Uke(5).onsdag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(5).torsdag, Uke(6).fredag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(1, syketilfeller.size)
    }

    @Test
    fun `gitt en person som har vært syk i 19 dager, har ferie i 19 dager og deretter blir syk i 10 nye dager så skal den siste perioden telle som en del av samme sykdomstilfelle`() {
        //       |D*19|D*19|D*10|
        //
        // syk1  |S*19|
        // ferie      |F*19|
        // syk2            |S*10|
        //
        // tidsl |S*19|F*19|S*10|
        //
        // tilf  |S*19|F*19|S*10|
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(4).fredag, sendtSykmelding)
        val ferie = Sykdomstidslinje.ferie(Uke(5).mandag, Uke(8).fredag, sendtSykmelding)
        val spysyka = Sykdomstidslinje.sykedager(Uke(9).mandag, Uke(10).fredag, sendtSykmelding)

        val syketilfeller = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(1, syketilfeller.size)
    }

    @Test
    fun `syketilfeller støtter ikke ubestemte dager`() {
        assertThrows<IllegalStateException> {
            (Sykdomstidslinje.utenlandsdag(Uke(1).mandag, sendtSykmelding) + Sykdomstidslinje.permisjonsdag(Uke(1).mandag, sendtSykmelding)).syketilfeller()
        }
    }

    @Test
    fun `syketilfeller støtter ikke permisjonsdager`() {
        assertThrows<IllegalStateException> {
            Sykdomstidslinje.permisjonsdag(Uke(1).mandag, sendtSykmelding).syketilfeller()
        }
    }
}
