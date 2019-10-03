package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DagTest {

    companion object {
        val rapporterthendelse = Testhendelse()
    }

    @Test
    internal fun sykedag() {
        val dagSykedagenDekker = LocalDate.of(2019,9,23)
        val sykedag = Sykdomstidslinje.sykedager(dagSykedagenDekker, rapporterthendelse)

        assertEquals(dagSykedagenDekker, sykedag.startdato())
        assertEquals(dagSykedagenDekker, sykedag.sluttdato())
        assertEquals(1, sykedag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun feriedag() {
        val dagFeriedagenDekker = LocalDate.of(2019,9,24)
        val feriedag = Sykdomstidslinje.ferie(dagFeriedagenDekker, rapporterthendelse)

        assertEquals(dagFeriedagenDekker, feriedag.startdato())
        assertEquals(dagFeriedagenDekker, feriedag.sluttdato())
        assertEquals(0, feriedag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun arbeidsdag() {
        val arbeidsdagenGjelder = LocalDate.of(2019,9,25)
        val arbeidsdag = Sykdomstidslinje.ikkeSykedag(arbeidsdagenGjelder, rapporterthendelse)

        assertEquals(arbeidsdagenGjelder, arbeidsdag.startdato())
        assertEquals(arbeidsdagenGjelder, arbeidsdag.sluttdato())
        assertEquals(0, arbeidsdag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun helgedag() {
        val helgedagenGjelder = LocalDate.of(2019,9,28)
        val helgedag = Sykdomstidslinje.ikkeSykedag(helgedagenGjelder, rapporterthendelse)

        assertEquals(helgedagenGjelder, helgedag.startdato())
        assertEquals(helgedagenGjelder, helgedag.sluttdato())
        assertEquals(0, helgedag.antallSykedagerHvorViTellerMedHelg())
    }
}
