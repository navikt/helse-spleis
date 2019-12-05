package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
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
        val sykedag = Sykdomstidslinje.sykedag(dagSykedagenDekker,
            rapporterthendelse
        )

        assertEquals(dagSykedagenDekker, sykedag.førsteDag())
        assertEquals(dagSykedagenDekker, sykedag.sisteDag())
        assertEquals(1, sykedag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun feriedag() {
        val dagFeriedagenDekker = LocalDate.of(2019,9,24)
        val feriedag = Sykdomstidslinje.ferie(dagFeriedagenDekker,
            rapporterthendelse
        )

        assertEquals(dagFeriedagenDekker, feriedag.førsteDag())
        assertEquals(dagFeriedagenDekker, feriedag.sisteDag())
        assertEquals(0, feriedag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun arbeidsdag() {
        val arbeidsdagenGjelder = LocalDate.of(2019,9,25)
        val arbeidsdag = Sykdomstidslinje.ikkeSykedag(arbeidsdagenGjelder,
            rapporterthendelse
        )

        assertEquals(arbeidsdagenGjelder, arbeidsdag.førsteDag())
        assertEquals(arbeidsdagenGjelder, arbeidsdag.sisteDag())
        assertEquals(0, arbeidsdag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun helgedag() {
        val helgedagenGjelder = LocalDate.of(2019,9,28)
        val helgedag = Sykdomstidslinje.ikkeSykedag(helgedagenGjelder,
            rapporterthendelse
        )

        assertEquals(helgedagenGjelder, helgedag.førsteDag())
        assertEquals(helgedagenGjelder, helgedag.sisteDag())
        assertEquals(0, helgedag.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    internal fun studiedag() {
        val dagSykedagenDekker = LocalDate.of(2019,9,23)
        val studiedag = Sykdomstidslinje.studiedag(dagSykedagenDekker,
            rapporterthendelse
        )

        assertEquals(dagSykedagenDekker, studiedag.førsteDag())
        assertEquals(dagSykedagenDekker, studiedag.sisteDag())
        assertEquals(0, studiedag.antallSykedagerHvorViTellerMedHelg())
    }
}
