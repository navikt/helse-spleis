package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SimpleMergeTest {
    @Test
    fun `ekspanderer tom tidslinje`() {
        val actual = Sykdomstidslinje().merge(1.januar jobbTil 5.januar)

        assertEquals(Periode(1.januar, 5.januar), actual.periode())
        assertTrue(actual[3.januar] is Arbeidsdag)
    }

    @Test
    fun `ekspanderer med tidslinje`() {
        val actual = (1.januar jobbTil 5.januar).merge(Sykdomstidslinje())

        assertEquals(Periode(1.januar, 5.januar), actual.periode())
        assertTrue(actual[3.januar] is Arbeidsdag)
    }

    @Test
    fun `ekspanderer tom tidslinje med tom tidslinje`() {
        val actual = Sykdomstidslinje().merge(Sykdomstidslinje())

        assertNull(actual.periode())
    }

    @Test
    fun `legge sammen to perioder uten overlapp`() {
        val actual = (1.januar jobbTil 5.januar).merge(15.januar jobbTil 19.januar)

        assertEquals(Periode(1.januar, 19.januar), actual.periode())
        assertTrue(actual[3.januar] is Arbeidsdag)
        assertTrue(actual[17.januar] is Arbeidsdag)
        assertTrue(actual[10.januar] is UkjentDag)
    }

    @Test
    fun `kan subsette en tidslinje`() {
        val original = (1.januar jobbTil 5.januar).merge(15.januar jobbTil 19.januar)
        Periode(3.januar, 17.januar).also {
            assertEquals(it, original.subset(it).periode())
        }

        Periode(3.januar, 10.januar).also {
            assertEquals(it, original.subset(it).periode())
        }

        Periode(6.januar, 14.januar).also {
            assertEquals(it, original.subset(it).periode())
        }

        Periode(1.februar, 14.februar).also {
            original.subset(it).also { tidslinje ->
                assertEquals(it, tidslinje.periode())
                assertSize(14, tidslinje)
                assertEquals(14, tidslinje.filterIsInstance<UkjentDag>().size)
            }
        }
    }

    @Test
    fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar jobbTil 5.januar).merge(15.januar jobbTil 19.januar)
        assertSize(19, actual)
        assertSize(15, actual.subset(Periode(3.januar, 17.januar)))
        assertSize(9, actual.subset(Periode(6.januar, 14.januar)))
        assertEquals(10, actual.filterIsInstance<Arbeidsdag>().size)
        assertEquals(9, actual.filterIsInstance<UkjentDag>().size)
    }

    @Test
    fun `tidslinjen kan kuttes`() {
        val original = (1.januar jobbTil 5.januar).merge(15.januar jobbTil 19.januar)

        original.fremTilOgMed(17.januar).also {
            assertEquals(Periode(1.januar, 17.januar), it.periode())
            assertSize(17, it)
        }

        original.fremTilOgMed(10.januar).also {
            assertEquals(Periode(1.januar, 5.januar), it.periode())
            assertSize(5, it)
        }

        original.fremTilOgMed(1.februar).also {
            assertEquals(Periode(1.januar, 19.januar), it.periode())
            assertSize(19, it)
        }

        original.fremTilOgMed(1.desember(2017)).also {
            assertNull(it.periode())
            assertSize(0, it)
        }
    }

    @Test
    fun `kan merge arbeidsdager med feriedager`() {
        val actual = (1.januar jobbTil 8.januar).merge(15.januar ferieTil 19.januar)
        assertSize(19, actual)
        assertEquals(6, actual.filterIsInstance<Arbeidsdag>().size)
        assertEquals(2, actual.filterIsInstance<FriskHelgedag>().size)
        assertEquals(5, actual.filterIsInstance<Feriedag>().size)
        assertEquals(6, actual.filterIsInstance<UkjentDag>().size)
    }

    @Test
    fun `støtter sykedager`() {
        val actual = (1.januar sykTil 8.januar grad 50).merge(15.januar ferieTil 19.januar)
        assertSize(19, actual)
        assertEquals(6, actual.filterIsInstance<Sykedag>().size)
        assertEquals(2, actual.filterIsInstance<SykHelgedag>().size)
        assertEquals(5, actual.filterIsInstance<Feriedag>().size)
        assertEquals(6, actual.filterIsInstance<UkjentDag>().size)
    }

    private fun assertSize(expected: Int, sykdomstidslinje: Sykdomstidslinje) {
        var count = 0
        sykdomstidslinje.forEach { _ -> count++ }
        assertEquals(expected, count)

        count = 0
        for (dag: Dag in sykdomstidslinje) {
             count++
        }
        assertEquals(expected, count)
    }
}
