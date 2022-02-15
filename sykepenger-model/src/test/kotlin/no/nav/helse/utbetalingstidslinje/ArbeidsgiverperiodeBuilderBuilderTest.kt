package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
        assertFalse(perioder.first().erFørsteUtbetalingsdagEtter(31.januar))
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
        assertFalse(perioder.first().erFørsteUtbetalingsdagEtter(31.januar))
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
        assertFalse(perioder.first().erFørsteUtbetalingsdagEtter(31.januar))
        assertTrue(perioder.first().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first())
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
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, periodebuilder, MaskinellJurist())
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        perioder.addAll(periodebuilder.result())
    }

    private fun assertEquals(expected: Iterable<LocalDate>, actual: Arbeidsgiverperiode?) {
        no.nav.helse.testhelpers.assertNotNull(actual)
        assertEquals(expected.toList(), actual.toList())
    }

    private fun assertEquals(expected: List<Iterable<LocalDate>>, actual: Arbeidsgiverperiode?) {
        no.nav.helse.testhelpers.assertNotNull(actual)
        assertEquals(expected.flatMap { it.toList() }, actual.toList())
    }
}
