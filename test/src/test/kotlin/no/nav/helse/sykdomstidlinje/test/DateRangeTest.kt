package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.time.LocalDate

internal class DateRangeTest {

    companion object {
        private val tidspunktRapportert = Testhendelse()

        private val førsteMandag = LocalDate.of(2019,9,23)
        private val førsteTirsdag = LocalDate.of(2019,9,24)
        private val førsteOnsdag = LocalDate.of(2019,9,25)
        private val førsteTorsdag = LocalDate.of(2019,9,26)
        private val førsteFredag = LocalDate.of(2019,9,27)
        private val førsteLørdag = LocalDate.of(2019,9,28)
        private val førsteSøndag = LocalDate.of(2019,9,29)
        private val andreMandag = LocalDate.of(2019,9,30)

    }

    @Test
    fun påfølgendeSykedager() {
        val sykedager = Sykdomstidslinje.sykedager(førsteTirsdag, førsteOnsdag, tidspunktRapportert)

        assertEquals(førsteTirsdag, sykedager.startdato())
        assertEquals(førsteOnsdag, sykedager.sluttdato())
        assertEquals(2, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { Sykdomstidslinje.sykedager(førsteOnsdag, førsteTirsdag, tidspunktRapportert) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = Sykdomstidslinje.sykedager(førsteTirsdag, førsteTirsdag, tidspunktRapportert)

        assertEquals(førsteTirsdag, sykedager.startdato())
        assertEquals(førsteTirsdag, sykedager.sluttdato())
        assertEquals(1, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = Sykdomstidslinje.ikkeSykedager(førsteMandag, andreMandag, tidspunktRapportert)

        assertEquals(0, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = Sykdomstidslinje.sykedager(førsteMandag, andreMandag, tidspunktRapportert)
        assertEquals(8, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = Sykdomstidslinje.sykedager(førsteMandag, andreMandag, tidspunktRapportert)
        assertEquals(6, interval.antallSykedagerHvorViIkkeTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
