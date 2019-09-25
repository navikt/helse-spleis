package no.nav.helse.util.unit

import no.nav.helse.util.interval.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class CompositeLeafSykdomstidslinjeTest {

    companion object {
        private val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)

        private val førsteMandag = Sykedag(
            gjelder = LocalDate.of(2019,9,23),
            rapportert = tidspunktRapportert
        )
        private val førsteTirsdag = Sykedag(
            gjelder = LocalDate.of(2019,9,24),
            rapportert = tidspunktRapportert
        )
        private val førsteOnsdag = Sykedag(
            gjelder = LocalDate.of(2019,9,25),
            rapportert = tidspunktRapportert
        )
        private val førsteTorsdag = Sykedag(
            gjelder = LocalDate.of(2019,9,26),
            rapportert = tidspunktRapportert
        )
        private val andreMandag = Sykedag(
            gjelder = LocalDate.of(2019,9,30),
            rapportert = tidspunktRapportert
        )

    }

    @Test
    internal fun sammenhengedeUkedager() {
        val interval = førsteTirsdag + førsteOnsdag

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteOnsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedager())
    }

    @Test
    internal fun sammenhengedeUkedagerBaklengs() {
        val interval = førsteOnsdag + førsteTirsdag

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteOnsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedager())
    }

    @Test
    internal fun ukedagerMedOpphold() {
        val interval = førsteTorsdag + førsteTirsdag

        assertEquals(førsteTirsdag.startdato(), interval.startdato())
        assertEquals(førsteTorsdag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedager())
        assertEquals(3, interval.flatten().size)
    }

    @Test
    internal fun mandagTilMandag() {
        val interval = førsteMandag + andreMandag

        assertEquals(førsteMandag.startdato(), interval.startdato())
        assertEquals(andreMandag.sluttdato(), interval.sluttdato())
        assertEquals(2, interval.antallSykedager())
        assertEquals(8, interval.flatten().size)
    }
}
