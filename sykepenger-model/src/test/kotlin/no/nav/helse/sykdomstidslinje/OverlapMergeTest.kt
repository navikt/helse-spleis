package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverlapMergeTest {
    private lateinit var tidslinje: Sykdomstidslinje
    private val inspektør get() = SykdomstidslinjeInspektør(tidslinje)

    @Test
    fun `overlap av samme type`() {
        tidslinje = (1.januar jobbTil 12.januar).merge(7.januar jobbTil 19.januar)

        assertEquals(Periode(1.januar, 19.januar), tidslinje.periode())
        assertEquals(15, tidslinje.filterIsInstance<Arbeidsdag>().size)
        assertEquals(4, tidslinje.filterIsInstance<FriskHelgedag>().size)

        inspektør.also {
            assertTrue(it[1.januar] is Arbeidsdag)
        }
    }

    @Test
    fun `dagturnering`() {
        val actual =
            (1.januar ferieTil 12.januar).merge(7.januar betalingTil 19.januar).merge(17.januar ferieTil 26.januar)

        assertEquals(Periode(1.januar, 26.januar), actual.periode())
        assertEquals(6 + 7, actual.filterIsInstance<Feriedag>().size)
        assertEquals(2, actual.filterIsInstance<ArbeidsgiverHelgedag>().size)
        assertEquals(2, actual.filterIsInstance<Arbeidsgiverdag>().size)
        assertEquals(9, actual.filterIsInstance<ProblemDag>().size)
    }

    @Test
    fun `inntektsmelding uten ferie`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<Arbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<ArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 1 + 3, actual.filterIsInstance<Arbeidsdag>().size)
        assertEquals(3, actual.filterIsInstance<FriskHelgedag>().size)
    }

    @Test
    fun `inntektsmelding med ferie`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar,
            1.januar ferieTil 3.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<Arbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<ArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 3, actual.filterIsInstance<Arbeidsdag>().size)
        assertEquals(3, actual.filterIsInstance<FriskHelgedag>().size)
        assertEquals(1, actual.filterIsInstance<Feriedag>().size)
    }

    @Test
    fun `inntektsmelding med ferie i helg`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar,
            5.januar ferieTil 8.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<Arbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<ArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 3, actual.filterIsInstance<Arbeidsdag>().size)
        assertEquals(2, actual.filterIsInstance<FriskHelgedag>().size)
        assertEquals(2, actual.filterIsInstance<Feriedag>().size)
    }

    @Test
    fun `første fraværsdag`() {
        tidslinje = listOf(
            1.januar sykTil 1.januar grad 50,
            5.januar ferieTil 8.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 8.januar), tidslinje.periode())
        assertEquals(4, tidslinje.filterIsInstance<Feriedag>().size)
        assertEquals(1, tidslinje.filterIsInstance<Sykedag>().size)

        inspektør.also {
            assertEquals(50, it.grader[1.januar])
        }
    }

    @Test
    fun `problemdager med melding`() {
        tidslinje = listOf(1.januar sykTil 10.januar, 5.januar sykTil 15.januar).merge(testBeste)

        inspektør.also {
            assertTrue(it.problemdagmeldinger[6.januar]?.contains("Kan ikke velge mellom") ?: false)
            assertTrue(it.problemdagmeldinger[6.januar]?.contains("TestHendelse") ?: false)
        }
    }

    private val testBeste: BesteStrategy = { venstre: Dag, høyre: Dag ->
        when {
            venstre is UkjentDag -> høyre
            høyre is UkjentDag -> venstre
            venstre is Arbeidsgiverdag || venstre is ArbeidsgiverHelgedag -> venstre
            høyre is Arbeidsgiverdag || høyre is ArbeidsgiverHelgedag -> høyre
            venstre is Sykedag -> venstre
            høyre is Sykedag -> høyre
            venstre is Feriedag && høyre is Arbeidsdag -> venstre
            høyre is Feriedag && venstre is Arbeidsdag -> høyre
            venstre is Feriedag && høyre is FriskHelgedag -> venstre
            høyre is Feriedag && venstre is FriskHelgedag -> høyre
            else -> venstre.problem(høyre)
        }
    }
}
