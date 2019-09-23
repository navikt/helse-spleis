package no.nav.helse.util.unit

import no.nav.helse.util.interval.Arbeidsdag
import no.nav.helse.util.interval.Feriedag
import no.nav.helse.util.interval.Sykedag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class LeafIntervalTest {

    @Test
    internal fun sykedag() {
        val dagSykedagenDekker = LocalDate.of(2019,9,23)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val sykedag = Sykedag(dagSykedagenDekker, tidspunktRapportert)

        assertEquals(dagSykedagenDekker, sykedag.startdato())
        assertEquals(dagSykedagenDekker, sykedag.sluttdato())
        assertEquals(1, sykedag.antallSykedager())
    }

    @Test
    internal fun feriedag() {
        val dagFeriedagenDekker = LocalDate.of(2019,9,24)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val feriedag = Feriedag(dagFeriedagenDekker, tidspunktRapportert)

        assertEquals(dagFeriedagenDekker, feriedag.startdato())
        assertEquals(dagFeriedagenDekker, feriedag.sluttdato())
        assertEquals(0, feriedag.antallSykedager())
    }

    @Test
    internal fun arbeidsdag() {
        val arbeidsdagenGjelder = LocalDate.of(2019,9,25)
        val tidspunktRapportert = LocalDateTime.of(2019,9,16, 10, 45)
        val arbeidsdag = Arbeidsdag(arbeidsdagenGjelder, tidspunktRapportert)

        assertEquals(arbeidsdagenGjelder, arbeidsdag.startdato())
        assertEquals(arbeidsdagenGjelder, arbeidsdag.sluttdato())
        assertEquals(0, arbeidsdag.antallSykedager())
    }
}
