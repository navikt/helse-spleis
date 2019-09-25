package no.nav.helse.util.unit

import no.nav.helse.util.interval.Interval
import no.nav.helse.util.interval.Nulldag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DagSammenligningTest {

    companion object {
        private val tidligsteTidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        private val senereTidspunktRapportert = LocalDateTime.of(2019,9,17, 11, 58)

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
    internal fun arbeidsdagPrioriteresOverSykedag() {
        val sykedag = Interval.sykedager(førsteMandag, tidligsteTidspunktRapportert)
        val arbeidsdag = Interval.ikkeSykedag(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(1, arbeidsdag.compareTo(sykedag))
    }

    @Test
    internal fun velgerDenSenestRapporterteDagenAvToLikeDager() {
        val tidligereSykedag = Interval.sykedager(førsteMandag, tidligsteTidspunktRapportert)
        val senereSykedag = Interval.sykedager(førsteMandag, senereTidspunktRapportert)

        assertEquals(-1, tidligereSykedag.compareTo(senereSykedag))
    }

    @Test
    internal fun nulldagTaperMotEnSykedag() {
        val nulldag = Nulldag(førsteMandag, tidligsteTidspunktRapportert)
        val sykedag = Interval.sykedager(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(1, sykedag.compareTo(nulldag))
    }

    @Test
    internal fun toLikeDagerErLike() {
        val feriedag = Interval.ferie(førsteMandag, tidligsteTidspunktRapportert)
        val arbeidsdag = Interval.ikkeSykedag(førsteMandag, tidligsteTidspunktRapportert)

        assertEquals(0, arbeidsdag.compareTo(feriedag))
    }
}
