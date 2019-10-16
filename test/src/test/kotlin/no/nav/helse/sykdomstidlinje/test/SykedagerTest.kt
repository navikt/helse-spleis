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
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, sendtSykmelding)
        val ferieDelEn = Sykdomstidslinje.ferie(1.torsdag, 1.fredag, sendtSykmelding)
        val ferieDelTo = Sykdomstidslinje.ferie(2.mandag, 2.fredag, sendtSykmelding)
        val ferieDelTre = Sykdomstidslinje.ferie(3.mandag, 3.fredag, sendtSykmelding)
        val malaria = Sykdomstidslinje.sykedager(5.mandag, 5.torsdag, sendtSykmelding)

        val tilfeller = (influensa + ferieDelEn + ferieDelTo + ferieDelTre + malaria).also { println(it) }.syketilfeller()

        println(tilfeller)

        assertEquals(1, tilfeller.size)
        assertEquals(1.mandag, tilfeller.first().startdato())
        assertEquals(5.torsdag, tilfeller.first().sluttdato())
    }

    @Test
    fun `søknad med opphold ferie kobles ikke sammen med ny søknad innenfor 16 dager`() {
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
}
