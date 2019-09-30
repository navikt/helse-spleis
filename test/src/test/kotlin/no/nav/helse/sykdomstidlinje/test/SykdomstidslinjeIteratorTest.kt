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
        val uke1Lørdag = LocalDate.of(2019, 9, 28)
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
        val uke6Fredag = LocalDate.of(2019, 11, 1)
        val uke7Fredag = LocalDate.of(2019, 11, 8)

        val rapporteringsdato = LocalDateTime.of(2019, 10, 14, 20, 0)
    }

    @Test
    fun sammenhengendeSykdomGirEnArbeidsgiverperiode() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke3Mandag, rapporteringsdato)

        val tidslinjer = tidslinje.syketilfeller()
        val antallSykedager = tidslinjer.first().antallSykedager()

        assertEquals(1, tidslinjer.size)
        assertEquals(15, antallSykedager)
    }

    @Test
    fun sykdomInnenforEnUkeTellerAntallDager() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        assertEquals(5, tidslinje.syketilfeller().first().antallSykedager())
    }

    @Test
    fun testSykdagerOverHelg() {
        val sykedager = Sykdomstidslinje.sykedager(
            SykedagerTest.uke1Mandag,
            SykedagerTest.uke2Mandag,
            SykedagerTest.rapporteringsdato
        )
        assertEquals(8, sykedager.syketilfeller().first().antallSykedager())
    }

    @Test
    fun sykmeldingMandagTilSøndagFørerTil7Dager() {
        val sykdager = Sykdomstidslinje.sykedager(uke1Mandag, uke1Søndag, rapporteringsdato)

        assertEquals(7, sykdager.syketilfeller().first().antallSykedager())
    }

    @Test
    fun sykmeldingMandagTilLørdagFørerTil6Dager() {
        val sykedager = Sykdomstidslinje.sykedager(uke1Mandag, uke1Lørdag, rapporteringsdato)

        assertEquals(6, sykedager.syketilfeller().first().antallSykedager())
        assertEquals(5, sykedager.syketilfeller().first().antallSykeVirkedager())
    }

    @Test
    fun toSykmeldingerMedGapStørreEnn16DagerGirToArbeidsgiverPerioder() {
        val spysyke = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        val malaria = Sykdomstidslinje.sykedager(uke5Mandag, uke7Fredag, rapporteringsdato)

        val syketilfeller = (spysyke + malaria).syketilfeller()
        assertEquals(2, syketilfeller.size)
        assertEquals(uke1Mandag, syketilfeller[0].startdato())
        assertEquals(uke1Fredag, syketilfeller[0].sluttdato())

        assertEquals(uke5Mandag, syketilfeller[1].startdato())
        assertEquals(uke7Fredag, syketilfeller[1].sluttdato())
    }

    @Test
    fun søknadMedOppholdFerieKoblesIkkeSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Onsdag, uke2Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke4Fredag, uke5Fredag, rapporteringsdato)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(uke1Mandag, grupper[0].startdato())
        assertEquals(uke2Mandag, grupper[0].sluttdato())

        assertEquals(uke4Fredag, grupper[1].startdato())
        assertEquals(uke5Fredag, grupper[1].sluttdato())
    }

    @Test
    fun fjernerLeadingOgTrailingDagerForTidslinjer() {
        val arbeidsdager1 = Sykdomstidslinje.ikkeSykedag(uke1Mandag, rapporteringsdato)
        val sykdom = Sykdomstidslinje.sykedager(uke2Mandag, uke2Fredag, rapporteringsdato)
        val arbeidsdager2 = Sykdomstidslinje.ikkeSykedag(uke3Mandag, rapporteringsdato)

        val trimmedTimeline = (arbeidsdager1 + sykdom + arbeidsdager2).trim()
        assertEquals(uke2Mandag, trimmedTimeline.startdato())
        assertEquals(uke2Fredag, trimmedTimeline.sluttdato())
        assertEquals(5, trimmedTimeline.antallSykedager())
    }
}
