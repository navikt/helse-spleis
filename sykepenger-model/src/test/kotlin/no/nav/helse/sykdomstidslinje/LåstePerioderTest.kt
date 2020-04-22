package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.NyDag.NyArbeidsdag
import no.nav.helse.sykdomstidslinje.NyDag.ProblemDag
import no.nav.helse.testhelpers.ferieTil
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.jobbTil
import no.nav.helse.testhelpers.merge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LåstePerioderTest {
    @Test
    internal fun `låse hele sykdomtidslinjen`() {
        val actual = (1.januar jobbTil 5.januar).lås(Periode(1.januar, 5.januar)).merge(1.januar ferieTil 5.januar)
        assertEquals(5, actual.filterIsInstance<NyArbeidsdag>().size)
    }

    @Test
    internal fun `låse deler av sykdomtidslinje`() {
        val actual = (1.januar jobbTil 5.januar).lås(Periode(2.januar, 4.januar)).merge(1.januar ferieTil 5.januar)
        assertEquals(3, actual.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(2, actual.filterIsInstance<ProblemDag>().size)
    }

    @Test
    internal fun `låse opp sykdomtidslinje`() {
        val initial = (1.januar jobbTil 5.januar).lås(Periode(2.januar, 4.januar)).merge(1.januar ferieTil 5.januar)
        assertEquals(3, initial.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(2, initial.filterIsInstance<ProblemDag>().size)

        val actual = initial.låsOpp(Periode(2.januar, 4.januar)).merge(1.januar ferieTil 5.januar)
        assertEquals(5, actual.filterIsInstance<ProblemDag>().size)
    }

    @Test
    internal fun `kan bare låse perioder som dekkes av tidslinjen`() {
        assertThrows<IllegalArgumentException> { (2.januar jobbTil 4.januar).lås(Periode(1.januar, 4.januar)) }
        assertThrows<IllegalArgumentException> { (2.januar jobbTil 4.januar).lås(Periode(2.januar, 5.januar)) }
        assertThrows<IllegalArgumentException> { (2.januar jobbTil 4.januar).lås(Periode(10.januar, 15.januar)) }
    }

    @Test
    internal fun `kan bare låse opp perioder som matcher`() {
        val initial = (1.januar jobbTil 5.januar).lås(Periode(2.januar, 4.januar)).merge(1.januar ferieTil 5.januar)
        assertEquals(3, initial.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(2, initial.filterIsInstance<ProblemDag>().size)

        assertThrows<IllegalArgumentException> { initial.låsOpp(Periode(3.januar, 4.januar)) }
        assertThrows<IllegalArgumentException> { initial.låsOpp(Periode(1.januar, 4.januar)) }
    }

    @Test
    internal fun `kan bare låse opp perioder som allerede er låst`() {
        assertThrows<IllegalArgumentException> { (1.januar jobbTil 5.januar).låsOpp(Periode(2.januar, 4.januar)) }
    }

}
