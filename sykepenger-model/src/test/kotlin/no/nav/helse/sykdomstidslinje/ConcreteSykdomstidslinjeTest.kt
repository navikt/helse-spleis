package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ConcreteSykdomstidslinjeTest {

    @Test
    internal fun subset() {
        val tidslinje = ConcreteSykdomstidslinje.sykedager(3.januar, 4.januar, 100.0, Søknad.SøknadDagFactory)
        assertTidslinje(tidslinje.subset(null, 4.januar), 3.januar, 4.januar)
        assertTidslinje(tidslinje.subset(null, 5.januar), 3.januar, 4.januar)
        assertNull(tidslinje.subset(null, 2.januar))

        assertTidslinje(tidslinje.subset(2.januar, 4.januar), 3.januar, 4.januar)
        assertTidslinje(tidslinje.subset(3.januar, 4.januar), 3.januar, 4.januar)
        assertTidslinje(tidslinje.subset(4.januar, 5.januar), 4.januar, 4.januar)
        assertNull(tidslinje.subset(1.januar, 2.januar))
    }

    @Test
    internal fun kutt() {
        val tidslinje = ConcreteSykdomstidslinje.sykedager(3.januar, 4.januar, 100.0, Søknad.SøknadDagFactory)
        assertTidslinje(tidslinje.kutt(4.januar), 3.januar, 4.januar)
        assertTidslinje(tidslinje.kutt(5.januar), 3.januar, 4.januar)
        assertNull(tidslinje.kutt(2.januar))
    }

    private fun assertTidslinje(tidslinje: ConcreteSykdomstidslinje?, førsteDag: LocalDate, sisteDag: LocalDate) {
        assertNotNull(tidslinje)
        assertEquals(førsteDag, tidslinje?.førsteDag())
        assertEquals(sisteDag, tidslinje?.sisteDag())
    }
}
