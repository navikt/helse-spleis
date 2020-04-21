package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OverlapMergeTest {
    @Test
    internal fun `overlap av samme type`() {
        val actual = (1.januar jobbTil 12.januar).merge(7.januar jobbTil 19.januar)

        assertEquals(Periode(1.januar, 19.januar), actual.periode())
        assertEquals(15, actual.filterIsInstance<NyDag.NyArbeidsdag>().size)
        assertEquals(4, actual.filterIsInstance<NyDag.NyFriskHelgedag>().size)
    }

    @Test
    internal fun `dagturnering`() {
        val actual = (1.januar ferieTil 12.januar).merge(7.januar betalingTil 19.januar).merge(17.januar ferieTil 26.januar)

        assertEquals(Periode(1.januar, 26.januar), actual.periode())
        assertEquals(6 + 7, actual.filterIsInstance<NyDag.NyFeriedag>().size)
        assertEquals(2, actual.filterIsInstance<NyDag.NyArbeidsgiverHelgedag>().size)
        assertEquals(2, actual.filterIsInstance<NyDag.NyArbeidsgiverdag>().size)
        assertEquals(9, actual.filterIsInstance<NyDag.ProblemDag>().size)
    }
}
