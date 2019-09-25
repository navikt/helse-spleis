package no.nav.helse.util.unit

import no.nav.helse.util.interval.Interval
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class CompositeIntervalTest {

    companion object {
        private val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)

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
    internal fun toSykeperioderMedMellomrom() {
        val førsteInterval = Interval.sykedager(førsteMandag, førsteTirsdag, tidspunktRapportert)
        val andreInterval = Interval.sykedager(førsteFredag, andreMandag, tidspunktRapportert)

        val interval =  andreInterval + førsteInterval

        Assertions.assertEquals(førsteMandag, interval.startdato())
        Assertions.assertEquals(andreMandag, interval.sluttdato())
        Assertions.assertEquals(6, interval.antallSykedager())
        Assertions.assertEquals(8, interval.flatten().size)
    }
}
