package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Grad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverlapMergeTest {
    private lateinit var tidslinje: NySykdomstidslinje
    private val inspektør get() = SykdomstidslinjeInspektør(tidslinje)

    @Test
    internal fun `overlap av samme type`() {
        tidslinje = (1.januar jobbTil 12.januar).merge(7.januar jobbTil 19.januar)

        assertEquals(Periode(1.januar, 19.januar), tidslinje.periode())
        assertEquals(15, tidslinje.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(4, tidslinje.filterIsInstance<NyFriskHelgedag>().size)

        inspektør.also {
            assertTrue(it[1.januar] is NyArbeidsdag)
        }
    }

    @Test
    internal fun `dagturnering`() {
        val actual =
            (1.januar ferieTil 12.januar).merge(7.januar betalingTil 19.januar).merge(17.januar ferieTil 26.januar)

        assertEquals(Periode(1.januar, 26.januar), actual.periode())
        assertEquals(6 + 7, actual.filterIsInstance<NyFeriedag>().size)
        assertEquals(2, actual.filterIsInstance<NyArbeidsgiverHelgedag>().size)
        assertEquals(2, actual.filterIsInstance<NyArbeidsgiverdag>().size)
        assertEquals(9, actual.filterIsInstance<ProblemDag>().size)
    }

    @Test
    internal fun `inntektsmelding uten ferie`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<NyArbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<NyArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 1 + 3, actual.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(3, actual.filterIsInstance<NyFriskHelgedag>().size)
    }

    @Test
    internal fun `inntektsmelding med ferie`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar,
            1.januar ferieTil 3.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<NyArbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<NyArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 3, actual.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(3, actual.filterIsInstance<NyFriskHelgedag>().size)
        assertEquals(1, actual.filterIsInstance<NyFeriedag>().size)
    }

    @Test
    internal fun `inntektsmelding med ferie i helg`() {
        val actual = listOf(
            1.januar jobbTil 17.januar,
            2.januar betalingTil 4.januar,
            7.januar betalingTil 11.januar,
            5.januar ferieTil 8.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 17.januar), actual.periode())
        assertEquals(3 + 4, actual.filterIsInstance<NyArbeidsgiverdag>().size)
        assertEquals(1, actual.filterIsInstance<NyArbeidsgiverHelgedag>().size)
        assertEquals(1 + 1 + 3, actual.filterIsInstance<NyArbeidsdag>().size)
        assertEquals(2, actual.filterIsInstance<NyFriskHelgedag>().size)
        assertEquals(2, actual.filterIsInstance<NyFeriedag>().size)
    }

    @Test
    internal fun `første fraværsdag`() {
        tidslinje = listOf(
            1.januar sykTil 1.januar grad 50,
            5.januar ferieTil 8.januar
        ).merge(testBeste)

        assertEquals(Periode(1.januar, 8.januar), tidslinje.periode())
        assertEquals(4, tidslinje.filterIsInstance<NyFeriedag>().size)
        assertEquals(1, tidslinje.filterIsInstance<NySykedag>().size)

        inspektør.also {
            assertEquals(Grad.sykdomsgrad(50), it.grader[1.januar])
        }
    }

    @Test
    internal fun `problemdager med melding`() {
        tidslinje = listOf(1.januar sykTil 10.januar, 5.januar sykTil 15.januar).merge(testBeste)

        inspektør.also {
            assertTrue(it.problemdagmeldinger[6.januar]?.contains("Kan ikke velge mellom") ?: false)
            assertTrue(it.problemdagmeldinger[6.januar]?.contains("TestHendelse") ?: false)
        }
    }

    private val testBeste = { venstre: NyDag, høyre: NyDag ->
        when {
            venstre is NyUkjentDag -> høyre
            høyre is NyUkjentDag -> venstre
            venstre is NyArbeidsgiverdag || venstre is NyArbeidsgiverHelgedag -> venstre
            høyre is NyArbeidsgiverdag || høyre is NyArbeidsgiverHelgedag -> høyre
            venstre is NySykedag -> venstre
            høyre is NySykedag -> høyre
            venstre is NyFeriedag && høyre is NyArbeidsdag -> venstre
            høyre is NyFeriedag && venstre is NyArbeidsdag -> høyre
            venstre is NyFeriedag && høyre is NyFriskHelgedag -> venstre
            høyre is NyFeriedag && venstre is NyFriskHelgedag -> høyre
            else -> venstre.problem(høyre)
        }
    }
}
