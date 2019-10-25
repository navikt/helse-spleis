package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SykdomstidslinjeIteratorTest {

    companion object {
        private val rapporteringshendelse = Testhendelse(LocalDateTime.of(2019, 7, 31, 20, 0))
    }

    @Test
    fun `sammenhengende sykdom gir en arbeidsgiverperiode`() {
        val tidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(3).mandag, rapporteringshendelse)

        val tidslinjer = tidslinje.syketilfeller()
        val antallSykedager = tidslinjer.first().tidslinje!!.antallSykedagerHvorViTellerMedHelg()

        assertEquals(1, tidslinjer.size)
        assertEquals(15, antallSykedager)
    }

    @Test
    fun `sykdom innenfor en uke teller antall dager`() {
        val tidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, rapporteringshendelse)
        assertEquals(5, tidslinje.syketilfeller().first().tidslinje!!.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `test sykdager over helg`() {
        val sykedager = Sykdomstidslinje.sykedager(
            Uke(1).mandag,
            Uke(2).mandag,
            rapporteringshendelse
        )
        assertEquals(8, sykedager.syketilfeller().first().tidslinje!!.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `sykmelding mandag til søndag fører til 7 dager hvor vi teller med helg`() {
        val sykdager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).søndag, rapporteringshendelse)

        assertEquals(7, sykdager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun `sykmelding mandag til lørdag fører til 6 dager`() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).lørdag, rapporteringshendelse)

        assertEquals(6, sykedager.antallSykedagerHvorViTellerMedHelg())
        assertEquals(5, sykedager.antallSykedagerHvorViIkkeTellerMedHelg())
    }

    @Test
    fun `to sykmeldinger med gap større enn 16 dager gir to arbeidsgiver perioder`() {
        val spysyke = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, rapporteringshendelse)
        val malaria = Sykdomstidslinje.sykedager(Uke(5).mandag, Uke(7).fredag, rapporteringshendelse)

        val syketilfeller = (spysyke + malaria).syketilfeller()

        assertEquals(2, syketilfeller.size, "Forventer to perioder i syketilfellet")
        assertEquals(Uke(1).mandag, syketilfeller[0].tidslinje!!.startdato())
        assertEquals(Uke(1).fredag, syketilfeller[0].tidslinje!!.sluttdato())

        assertEquals(Uke(5).mandag, syketilfeller[1].tidslinje!!.startdato())
        assertEquals(Uke(7).fredag, syketilfeller[1].tidslinje!!.sluttdato())
    }

    @Test
    fun `søknad med opphold (ferie) kobles ikke sammen med ny søknad innenfor 16 dager`() {
        val influensa = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, rapporteringshendelse)
        val ferie = Sykdomstidslinje.ferie(Uke(2).onsdag, Uke(2).fredag, rapporteringshendelse)
        val spysyka = Sykdomstidslinje.sykedager(Uke(4).fredag, Uke(5).fredag, rapporteringshendelse)

        val grupper = (influensa + ferie + spysyka).syketilfeller()

        assertEquals(2, grupper.size)
        assertEquals(Uke(1).mandag, grupper[0].tidslinje!!.startdato())
        assertEquals(Uke(2).mandag, grupper[0].tidslinje!!.sluttdato())

        assertEquals(Uke(4).fredag, grupper[1].tidslinje!!.startdato())
        assertEquals(Uke(5).fredag, grupper[1].tidslinje!!.sluttdato())
    }
}
