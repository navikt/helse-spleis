package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SykedagerTest {

    companion object {
        private val uke1Mandag = LocalDate.of(2019, 9, 23)
        private val uke1Fredag = LocalDate.of(2019, 9, 27)
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
        private val uke6Fredag = LocalDate.of(2019, 11, 4)
        private val uke7Fredag = LocalDate.of(2019, 11, 11)

        private val rapporteringsdato = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
    }

    @Test
    fun testSykdagerOverHelg() {
        val sykedager = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        assertEquals(8, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun erInnenforArbeidsgiverperioden() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke2Fredag, uke4Mandag, rapporteringsdato)

        val tidslinje = (influensa + spysyka).syketilfeller()

        assertEquals(1, tidslinje.size)
    }

    @Test
    fun søknadOver16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke3Mandag, rapporteringsdato)
        val grupper = influensa.syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke3Mandag, grupper.first().sluttdato())
    }

    @Test
    fun toSøknaderInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke3Mandag, uke3Fredag, rapporteringsdato)
        val grupper = (influensa + spysyka).syketilfeller()
        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke3Fredag, grupper.first().sluttdato())
    }

    @Disabled
    @Test
    fun toSøknaderUtenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke5Mandag, uke5Fredag, rapporteringsdato)
        val grupper = (influensa + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(uke1Mandag, grupper[0].startdato())
        assertEquals(uke1Fredag, grupper[0].sluttdato())

        assertEquals(uke5Mandag, grupper[1].startdato())
        assertEquals(uke5Fredag, grupper[1].sluttdato())
    }

    @Test
    fun søknadMedPåfølgendeFerieUtenGapKoblesSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Tirsdag, uke3Fredag, rapporteringsdato)
        val spysyka = Sykdomstidslinje.sykedager(uke5Fredag, uke6Fredag, rapporteringsdato)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke6Fredag, grupper.first().sluttdato())
    }

    @Disabled
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
    fun søknadMedTrailingFerieGirBareSøknad() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke2Mandag, rapporteringsdato)
        val ferie = Sykdomstidslinje.ferie(uke2Tirsdag, uke2Fredag, rapporteringsdato)

        val grupper = (influensa + ferie).syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke2Mandag, grupper.first().sluttdato())
    }

    @Test
    fun kutterHelgISluttenAvGruppe() {
        val influensa = Sykdomstidslinje.sykedager(uke1Mandag, uke1Søndag, rapporteringsdato)

        val grupper = influensa.syketilfeller()

        assertEquals(1, grupper.size)
        assertEquals(uke1Mandag, grupper.first().startdato())
        assertEquals(uke1Søndag, grupper.first().sluttdato())
    }
}
