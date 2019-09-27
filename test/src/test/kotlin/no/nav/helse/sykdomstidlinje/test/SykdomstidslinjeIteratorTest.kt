package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SykdomstidslinjeIteratorTest {

    companion object {
        val uke1Mandag = LocalDate.of(2019, 9, 23)
        val uke1Fredag = LocalDate.of(2019, 9, 27)
        val uke1Søndag = LocalDate.of(2019, 9, 29)
        val uke2Mandag = LocalDate.of(2019, 9, 30)
        val uke2Tirsdag = LocalDate.of(2019, 10, 1)
        val uke2Onsdag = LocalDate.of(2019, 10, 2)
        val uke2Fredag = LocalDate.of(2019, 10, 4)
        val uke3Mandag = LocalDate.of(2019, 10, 7)
        val uke3Fredag = LocalDate.of(2019, 10, 11)
        val uke4Mandag = LocalDate.of(2019, 10, 14)
        val uke4Onsdag = LocalDate.of(2019, 10, 16)
        val uke4Fredag = LocalDate.of(2019, 10, 18)
        val uke5Mandag = LocalDate.of(2019, 10, 21)
        val uke5Fredag = LocalDate.of(2019, 10, 25)

        val rapporteringsdato = LocalDateTime.of(2019, 10, 14, 20, 0)
    }

    @Test
    fun sammenhengendeSykdomGirEnArbeidsgiverperiode() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke3Mandag, rapporteringsdato)

        val antallSykedager = tidslinje.antallSykedagerArbeidsperiode()

        assertEquals(15, antallSykedager)
    }

    @Test
    fun sykdomInnenforEnUkeTellerAntallDager() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)

        assertEquals(5, tidslinje.antallSykedagerArbeidsperiode())
    }


    @Test
    fun testSykdagerOverHelg() {

        val sykedager = Sykdomstidslinje.sykedager(
            SykedagerTest.uke1Mandag,
            SykedagerTest.uke2Mandag,
            SykedagerTest.rapporteringsdato
        )

        println(sykedager)

        assertEquals(8, sykedager.antallSykedagerArbeidsperiode())
    }

    @Test
    fun sykmeldingMandagTilSøndagFørerTil7Dager() {
        val sykdager = Sykdomstidslinje.sykedager(uke1Mandag, uke1Søndag, rapporteringsdato)

        assertEquals(7, sykdager.antallSykedagerArbeidsperiode())
    }

}
