package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

class SykdomstidslinjeIteratorTest {

    companion object {
        private val rapporteringshendelse = Testhendelse(LocalDateTime.of(2019, 7, 31, 20, 0))
    }

    @Test
    fun `sammenhengendeSykdom gir en arbeidsgiverperiode`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.mandag, 3.mandag, rapporteringshendelse)

        val tidslinjer = tidslinje.syketilfeller()
        val antallSykedager = tidslinjer.first().antallSykedagerHvorViTellerMedHelg()

        assertEquals(1, tidslinjer.size)
        assertEquals(15, antallSykedager)
    }

    @Test
    fun `sykdom innenfor en uke teller antall dager`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, rapporteringshendelse)
        assertEquals(5, tidslinje.syketilfeller().first().antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `test sykdager over helg`() {
        val sykedager = Sykdomstidslinje.sykedager(
            1.mandag,
            2.mandag,
            rapporteringshendelse
        )
        assertEquals(8, sykedager.syketilfeller().first().antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `sykmelding mandag til søndag fører til 7 dager hvor vi teller med helg`() {
        val sykdager = Sykdomstidslinje.sykedager(1.mandag, 1.søndag, rapporteringshendelse)

        assertEquals(7, sykdager.syketilfeller().first().antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun sykmeldingMandagTilLørdagFørerTil6Dager() {
        val sykedager = Sykdomstidslinje.sykedager(1.mandag, 1.lørdag, rapporteringshendelse)

        assertEquals(6, sykedager.syketilfeller().first().antallSykedagerHvorViTellerMedHelg())
        assertEquals(5, sykedager.syketilfeller().first().antallSykedagerHvorViIkkeTellerMedHelg())
    }

    @Disabled
    @Test
    fun toSykmeldingerMedGapStørreEnn16DagerGirToArbeidsgiverPerioder() {
        val spysyke = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, rapporteringshendelse)
        val malaria = Sykdomstidslinje.sykedager(5.mandag, 7.fredag, rapporteringshendelse)

        val syketilfeller = (spysyke + malaria).syketilfeller()
        assertEquals(2, syketilfeller.size)
        assertEquals(1.mandag, syketilfeller[0].startdato())
        assertEquals(1.fredag, syketilfeller[0].sluttdato())

        assertEquals(5.mandag, syketilfeller[1].startdato())
        assertEquals(7.fredag, syketilfeller[1].sluttdato())
    }

    @Disabled
    @Test
    fun søknadMedOppholdFerieKoblesIkkeSammenMedNySøknadInnenfor16Dager() {
        val influensa = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, rapporteringshendelse)
        val ferie = Sykdomstidslinje.ferie(2.onsdag, 2.fredag, rapporteringshendelse)
        val spysyka = Sykdomstidslinje.sykedager(4.fredag, 5.fredag, rapporteringshendelse)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(1.mandag, grupper[0].startdato())
        assertEquals(2.mandag, grupper[0].sluttdato())

        assertEquals(4.fredag, grupper[1].startdato())
        assertEquals(5.fredag, grupper[1].sluttdato())
    }

    private val Int.juli get() = LocalDate.of(2019, Month.JULY, this)

    private val Int.mandag get() = LocalDate.of(2019, Month.JULY, 7*(this - 1) + 1 )
    private val Int.tirsdag get() = this.mandag.plusDays(1)
    private val Int.onsdag get() = this.mandag.plusDays(2)
    private val Int.torsdag get() = this.mandag.plusDays(3)
    private val Int.fredag get() = this.mandag.plusDays(4)
    private val Int.lørdag get() = this.mandag.plusDays(5)
    private val Int.søndag get() = this.mandag.plusDays(6)
}
