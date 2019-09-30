package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SykdomstidslinjeIteratorTest {

    companion object {
        private val uke1Mandag = LocalDate.of(2019, 9, 23)
        private val uke1Fredag = LocalDate.of(2019, 9, 27)
        private val uke1Lørdag = LocalDate.of(2019, 9, 28)
        private val uke1Søndag = LocalDate.of(2019, 9, 29)
        private val uke2Mandag = LocalDate.of(2019, 9, 30)
        private val uke2Tirsdag = LocalDate.of(2019, 10, 1)
        private val uke2Onsdag = LocalDate.of(2019, 10, 2)
        private val uke2Fredag = LocalDate.of(2019, 10, 4)
        private val uke3Mandag = LocalDate.of(2019, 10, 7)
        private val uke3Fredag = LocalDate.of(2019, 10, 11)
        private val uke4Mandag = LocalDate.of(2019, 10, 14)
        private val uke4Onsdag = LocalDate.of(2019, 10, 16)
        private val uke4Fredag = LocalDate.of(2019, 10, 18)
        private val uke5Mandag = LocalDate.of(2019, 10, 21)
        private val uke5Fredag = LocalDate.of(2019, 10, 25)
        private val uke6Fredag = LocalDate.of(2019, 11, 1)
        private val uke7Fredag = LocalDate.of(2019, 11, 8)

        private val rapporteringshendelse = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
    }

    @Test
    fun sammenhengendeSykdomGirEnArbeidsgiverperiode() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke3Mandag, rapporteringshendelse)

        val tidslinjer = tidslinje.syketilfeller()
        val antallSykedager = tidslinjer.first().antallSykedagerUtenHelg()

        assertEquals(1, tidslinjer.size)
        assertEquals(15, antallSykedager)
    }

    @Test
    fun sykdomInnenforEnUkeTellerAntallDager() {
        val tidslinje = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringshendelse)
        assertEquals(5, tidslinje.syketilfeller().first().antallSykedagerUtenHelg())
    }

    @Test
    fun testSykdagerOverHelg() {
        val sykedager = Sykdomstidslinje.sykedager(
            uke1Mandag,
            uke2Mandag,
            rapporteringshendelse
        )
        assertEquals(8, sykedager.syketilfeller().first().antallSykedagerUtenHelg())
    }

    @Test
    fun sykmeldingMandagTilSøndagFørerTil7Dager() {
        val sykdager = Sykdomstidslinje.sykedager(uke1Mandag, uke1Søndag, rapporteringshendelse)

        assertEquals(7, sykdager.syketilfeller().first().antallSykedagerUtenHelg())
    }

    @Test
    fun sykmeldingMandagTilLørdagFørerTil6Dager() {
        val sykedager = Sykdomstidslinje.sykedager(uke1Mandag, uke1Lørdag, rapporteringshendelse)

        assertEquals(6, sykedager.syketilfeller().first().antallSykedagerUtenHelg())
        assertEquals(5, sykedager.syketilfeller().first().antallSykedagerMedHelg())
    }

    @Test
    fun toSykmeldingerMedGapStørreEnn16DagerGirToArbeidsgiverPerioder() {
        val spysyke = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringshendelse)
        val malaria = Sykdomstidslinje.sykedager(uke5Mandag, uke7Fredag, rapporteringshendelse)

        val syketilfeller = (spysyke + malaria).syketilfeller()
        assertEquals(2, syketilfeller.size)
        assertEquals(uke1Mandag, syketilfeller[0].startdato())
        assertEquals(uke1Fredag, syketilfeller[0].sluttdato())

        assertEquals(uke5Mandag, syketilfeller[1].startdato())
        assertEquals(uke7Fredag, syketilfeller[1].sluttdato())
    }

    @Test
    fun søknadMedOppholdFerieKoblesIkkeSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringshendelse)
        val ferie = Sykdomstidslinje.ferie(uke2Onsdag, uke2Fredag, rapporteringshendelse)
        val spysyka = Sykdomstidslinje.sykedager(uke4Fredag, uke5Fredag, rapporteringshendelse)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(uke1Mandag, grupper[0].startdato())
        assertEquals(uke2Mandag, grupper[0].sluttdato())

        assertEquals(uke4Fredag, grupper[1].startdato())
        assertEquals(uke5Fredag, grupper[1].sluttdato())
    }

    @Test
    fun fjernerLeadingOgTrailingDagerForTidslinjer() {
        val arbeidsdager1 = Sykdomstidslinje.ikkeSykedag(uke1Mandag, rapporteringshendelse)
        val sykdom = Sykdomstidslinje.sykedager(uke2Mandag, uke2Fredag, rapporteringshendelse)
        val arbeidsdager2 = Sykdomstidslinje.ikkeSykedag(uke3Mandag, rapporteringshendelse)

        val trimmedTimeline = (arbeidsdager1 + sykdom + arbeidsdager2).trim()
        assertEquals(uke2Mandag, trimmedTimeline.startdato())
        assertEquals(uke2Fredag, trimmedTimeline.sluttdato())
        assertEquals(5, trimmedTimeline.antallSykedagerUtenHelg())
    }
}
