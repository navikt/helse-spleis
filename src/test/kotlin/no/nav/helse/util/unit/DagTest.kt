package no.nav.helse.util.unit

import no.nav.helse.util.interval.Arbeidsdag
import no.nav.helse.util.interval.Helgedag
import no.nav.helse.util.interval.Interval
import no.nav.helse.util.interval.Sykedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

internal class DagTest {

    @Test
    internal fun sykedag() {
        val dagSykedagenDekker = LocalDate.of(2019,9,23)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val sykedag = Interval.sykedager(dagSykedagenDekker, tidspunktRapportert)

        assertEquals(dagSykedagenDekker, sykedag.startdato())
        assertEquals(dagSykedagenDekker, sykedag.sluttdato())
        assertEquals(1, sykedag.antallSykedager())
    }

    @Test
    internal fun feriedag() {
        val dagFeriedagenDekker = LocalDate.of(2019,9,24)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val feriedag = Interval.feriedag(dagFeriedagenDekker, tidspunktRapportert)

        assertEquals(dagFeriedagenDekker, feriedag.startdato())
        assertEquals(dagFeriedagenDekker, feriedag.sluttdato())
        assertEquals(0, feriedag.antallSykedager())
    }

    @Test
    internal fun arbeidsdag() {
        val arbeidsdagenGjelder = LocalDate.of(2019,9,25)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val arbeidsdag = Interval.ikkeSykedag(arbeidsdagenGjelder, tidspunktRapportert)

        assertEquals(arbeidsdagenGjelder, arbeidsdag.startdato())
        assertEquals(arbeidsdagenGjelder, arbeidsdag.sluttdato())
        assertEquals(0, arbeidsdag.antallSykedager())
    }

    @Test
    internal fun helgedag() {
        val helgedagenGjelder = LocalDate.of(2019,9,28)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val helgedag = Interval.ikkeSykedag(helgedagenGjelder, tidspunktRapportert)

        assertEquals(helgedagenGjelder, helgedag.startdato())
        assertEquals(helgedagenGjelder, helgedag.sluttdato())
        assertEquals(0, helgedag.antallSykedager())
    }

    @Test
    internal fun ugyldigHelgedag() {
        val ikkeEnHelgedag = LocalDate.of(2019,9,25)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        assertThrows<IllegalArgumentException>{ Helgedag(ikkeEnHelgedag, tidspunktRapportert)}
    }
}
