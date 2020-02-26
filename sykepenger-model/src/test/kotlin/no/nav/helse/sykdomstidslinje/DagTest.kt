package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DagTest {

    @Test
    internal fun sykedag() {
        val dagSykedagenDekker = LocalDate.of(2019, 9, 23)
        val sykedag = ConcreteSykdomstidslinje.sykedag(
            dagSykedagenDekker,
            100.0,
            Søknad.SøknadDagFactory
        )

        assertEquals(dagSykedagenDekker, sykedag.førsteDag())
        assertEquals(dagSykedagenDekker, sykedag.sisteDag())
    }

    @Test
    internal fun feriedag() {
        val dagFeriedagenDekker = LocalDate.of(2019, 9, 24)
        val feriedag = ConcreteSykdomstidslinje.ferie(
            dagFeriedagenDekker,
            Søknad.SøknadDagFactory
        )

        assertEquals(dagFeriedagenDekker, feriedag.førsteDag())
        assertEquals(dagFeriedagenDekker, feriedag.sisteDag())
    }

    @Test
    internal fun arbeidsdag() {
        val arbeidsdagenGjelder = LocalDate.of(2019, 9, 25)
        val arbeidsdag = ConcreteSykdomstidslinje.ikkeSykedag(
            arbeidsdagenGjelder,
            Søknad.SøknadDagFactory
        )

        assertEquals(arbeidsdagenGjelder, arbeidsdag.førsteDag())
        assertEquals(arbeidsdagenGjelder, arbeidsdag.sisteDag())
    }

    @Test
    internal fun helgedag() {
        val helgedagenGjelder = LocalDate.of(2019, 9, 28)
        val helgedag = ConcreteSykdomstidslinje.ikkeSykedag(
            helgedagenGjelder,
            Søknad.SøknadDagFactory
        )

        assertEquals(helgedagenGjelder, helgedag.førsteDag())
        assertEquals(helgedagenGjelder, helgedag.sisteDag())
    }

    @Test
    internal fun studiedag() {
        val dagSykedagenDekker = LocalDate.of(2019, 9, 23)
        val studiedag = ConcreteSykdomstidslinje.studiedag(
            dagSykedagenDekker,
            Søknad.SøknadDagFactory
        )

        assertEquals(dagSykedagenDekker, studiedag.førsteDag())
        assertEquals(dagSykedagenDekker, studiedag.sisteDag())
    }
}
