package no.nav.helse.util.unit

import no.nav.helse.util.interval.Sykdomstidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class CompositeSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)

        private val førsteMandag = LocalDate.of(2019,9,23)
        private val førsteTirsdag = LocalDate.of(2019,9,24)
        private val førsteFredag = LocalDate.of(2019,9,27)
        private val andreMandag = LocalDate.of(2019,9,30)
    }


    @Test
    internal fun toSykeperioderMedMellomrom() {
        val førsteInterval = Sykdomstidslinje.sykedager(førsteMandag, førsteTirsdag, tidspunktRapportert)
        val andreInterval = Sykdomstidslinje.sykedager(førsteFredag, andreMandag, tidspunktRapportert)

        val interval =  andreInterval + førsteInterval

        Assertions.assertEquals(førsteMandag, interval.startdato())
        Assertions.assertEquals(andreMandag, interval.sluttdato())
        Assertions.assertEquals(6, interval.antallSykedager())
        Assertions.assertEquals(8, interval.flatten().size)
    }
}
