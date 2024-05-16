package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.NullObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.AIG
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodeBuilderBuilderTest {

    @Test
    fun ingenting() {
        undersøke(1.A + 29.opphold + 1.A)
        assertEquals(0, perioder.size)
    }

    @Test
    fun kurant() {
        undersøke(31.S)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(perioder.first().erFørsteUtbetalingsdagFørEllerLik(1.januar til 31.januar))
        assertTrue(perioder.first().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first())
    }

    @Test
    fun infotrygd() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(1.januar til 10.januar))
        }
        assertEquals(1, perioder.size)
        assertEquals(emptyList<LocalDate>(), perioder.first())
        assertTrue(perioder.first().erFørsteUtbetalingsdagFørEllerLik(1.januar til 31.januar))
        assertTrue(perioder.first().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first())
    }

    @Test
    fun `infotrygd etter arbeidsgiverperiode`() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(17.januar til 20.januar))
        }
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(perioder.first().erFørsteUtbetalingsdagFørEllerLik(1.januar til 31.januar))
        assertTrue(perioder.first().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first())
    }

    @Test
    fun `ferie i agp`() {
        undersøke(5.S + 5.F + 10.S)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(perioder.first().forventerInntekt(17.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }

    @Test
    fun `ferie uten sykmelding i agp`() {
        undersøke(5.S + 5.AIG + 10.S)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(perioder.first().forventerInntekt(17.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }

    @Test
    fun `ferie i nesten fullført agp`() {
        undersøke(15.S + 5.F)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(16.januar til 20.januar in perioder.first())
    }

    @Test
    fun `arbeid etter ferie i agp`() {
        undersøke(5.S + 5.F + 5.A + 11.S + 1.F + 13.S)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 10.januar, 16.januar til 21.januar), perioder.first())
        assertTrue(perioder.first().forventerInntekt(22.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }

    @Test
    fun `arbeid etter ferie uten sykmelding i agp`() {
        undersøke(5.S + 5.AIG + 5.A + 11.S + 1.AIG + 13.S)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 5.januar, 16.januar til 26.januar), perioder.first())
        assertTrue(perioder.first().forventerInntekt(29.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }

    @Test
    fun `opphold direkte etter fullført agp`() {
        undersøke(16.S + 15.A)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertFalse(perioder.first().forventerInntekt(17.januar til 31.januar, Sykdomstidslinje(), NullObserver))
        assertTrue(17.januar til 31.januar in perioder.first())
    }

    @Test
    fun `opphold etter litt utbetaling`() {
        undersøke(17.S + 15.A)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertTrue(perioder.first().forventerInntekt(17.januar til 31.januar, Sykdomstidslinje(), NullObserver))
        assertFalse(perioder.first().forventerInntekt(18.januar til 31.januar, Sykdomstidslinje(), NullObserver))
        assertFalse(perioder.first().forventerInntekt(19.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }

    @Test
    fun `ferie etter opphold etter fullført agp`() {
        undersøke(17.S + 1.A + 16.F + 16.S)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
        assertEquals(listOf(4.februar til 19.februar), perioder.last())
        assertFalse(perioder.first().forventerInntekt(18.januar til 31.januar, Sykdomstidslinje(), NullObserver))
    }


    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        perioder.clear()
    }

    private val perioder: MutableList<Arbeidsgiverperiode> = mutableListOf()

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, periodebuilder, NullObserver)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        perioder.addAll(periodebuilder.result())
    }

    private fun assertEquals(expected: Iterable<LocalDate>, actual: Arbeidsgiverperiode?) {
        assertNotNull(actual)
        assertEquals(expected.toList(), actual.toList())
    }

    private fun assertEquals(expected: List<Iterable<LocalDate>>, actual: Arbeidsgiverperiode?) {
        assertNotNull(actual)
        assertEquals(expected.flatMap { it.toList() }, actual.toList())
    }
}
