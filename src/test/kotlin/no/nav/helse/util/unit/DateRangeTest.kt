package no.nav.helse.util.unit

import no.nav.helse.util.interval.Arbeidsdag
import no.nav.helse.util.interval.Helgedag
import no.nav.helse.util.interval.Interval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.LocalDateTime

internal class DateRangeTest {

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
    fun påfølgendeSykedager() {
        val sykedager = Interval.sykedager(førsteTirsdag, førsteOnsdag, tidspunktRapportert)

        assertEquals(førsteTirsdag, sykedager.startdato())
        assertEquals(førsteOnsdag, sykedager.sluttdato())
        assertEquals(2, sykedager.antallSykedager())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { Interval.sykedager(førsteOnsdag, førsteTirsdag, tidspunktRapportert) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = Interval.sykedager(førsteTirsdag, førsteTirsdag, tidspunktRapportert)

        assertEquals(førsteTirsdag, sykedager.startdato())
        assertEquals(førsteTirsdag, sykedager.sluttdato())
        assertEquals(1, sykedager.antallSykedager())
    }

    @Test
    fun mandagTilMandag() {
        val dager = Interval.ikkeSykedager(førsteMandag, andreMandag, tidspunktRapportert).flatten()

        assertEquals(8, dager.size)
        assert(dager[0] is Arbeidsdag)
        assert(dager[5] is Helgedag)
        assert(dager[6] is Helgedag)
        assert(dager[7] is Arbeidsdag)
    }
}
