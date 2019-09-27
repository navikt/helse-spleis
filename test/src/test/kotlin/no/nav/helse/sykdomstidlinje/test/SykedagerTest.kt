package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SykedagerTest {

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
    fun testSykdagerOverHelg() {

        val sykedager = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)

        println(sykedager)

        assertEquals(8, sykedager.antallSykedager())
    }

    @Test
    fun erInnenforArbeidsgiverperioden() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke2Fredag, uke4Mandag, rapporteringsdato)

        val tidslinje = (influensa + spysyka).grupperPerioderMedMaksAvstand()

        assertEquals(1, tidslinje.size)
    }

    @Test
    fun søknadOver16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke3Mandag, rapporteringsdato)
        val grupper = influensa.grupperPerioderMedMaksAvstand()
        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke3Mandag, grupper.first().sluttdato())
    }

    @Test
    fun toSøknaderInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke3Mandag, uke3Fredag, rapporteringsdato)
        val grupper = (influensa + spysyka).grupperPerioderMedMaksAvstand()
        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke3Fredag, grupper.first().sluttdato())
    }

    @Test
    fun toSøknaderUtenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke5Mandag, uke5Fredag, rapporteringsdato)
        val grupper = (influensa + spysyka).grupperPerioderMedMaksAvstand()

        assertEquals(2, grupper.size)
        assertEquals(uke1Mandag, grupper[0].startdato())
        assertEquals(uke1Fredag, grupper[0].sluttdato())

        assertEquals(uke5Mandag, grupper[1].startdato())
        assertEquals(uke5Fredag, grupper[1].sluttdato())
    }

    @Test
    fun søknadMedPåfølgendeFerieUtenGapKoblesSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Tirsdag, uke2Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke4Fredag, uke5Fredag, rapporteringsdato)

        val grupper = (influensa + ferie + spysyka).grupperPerioderMedMaksAvstand()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke5Fredag, grupper.first().sluttdato())
    }

    @Test
    fun søknadMedOppholdFerieKoblesIkkeSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Onsdag, uke2Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke4Fredag, uke5Fredag, rapporteringsdato)

        val grupper = (influensa + ferie + spysyka).grupperPerioderMedMaksAvstand()

        assertEquals(2, grupper.size)
        assertEquals(uke1Mandag, grupper[0].startdato())
        assertEquals(uke2Mandag, grupper[0].sluttdato())

        assertEquals(uke4Fredag, grupper[1].startdato())
        assertEquals(uke5Fredag, grupper[1].sluttdato())
    }

    @Test
    fun søknadMedTrailingFerieGirBareSøknad() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Tirsdag, uke2Fredag, rapporteringsdato)

        val grupper = (influensa + ferie).grupperPerioderMedMaksAvstand()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke2Mandag, grupper.first().sluttdato())
    }

    @Test
    fun kutterHelgISluttenAvGruppe() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Søndag, rapporteringsdato)

        val grupper = (influensa).grupperPerioderMedMaksAvstand()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke1Søndag, grupper.first().sluttdato())
    }
}
