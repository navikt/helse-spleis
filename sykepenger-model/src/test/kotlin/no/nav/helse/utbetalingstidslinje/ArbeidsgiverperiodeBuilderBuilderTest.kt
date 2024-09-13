package no.nav.helse.utbetalingstidslinje

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.AIG
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.S
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
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 31.januar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = (17.januar til 31.januar).toSet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().erFørsteUtbetalingsdagFørEllerLik(januar))
        assertTrue(perioder.first().somArbeidsgiverperiode().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first().somArbeidsgiverperiode())
    }

    @Test
    fun infotrygd() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(1.januar til 10.januar))
        }
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 31.januar,
            arbeidsgiverperiode = emptySet(),
            utbetalingsperioder = (1.januar til 31.januar).toSet(),
            oppholdsperioder = emptySet(),
            fullstendig = false,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().erFørsteUtbetalingsdagFørEllerLik(januar))
        assertTrue(perioder.first().somArbeidsgiverperiode().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first().somArbeidsgiverperiode())
    }

    @Test
    fun `infotrygd etter arbeidsgiverperiode`() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(17.januar til 20.januar))
        }
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 31.januar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = (17.januar til 31.januar).toSet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().erFørsteUtbetalingsdagFørEllerLik(januar))
        assertTrue(perioder.first().somArbeidsgiverperiode().hørerTil(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first().somArbeidsgiverperiode())
    }

    @Test
    fun `ferie i agp`() {
        undersøke(5.S + 5.F + 10.S)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 20.januar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = (17.januar til 20.januar).toSet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().forventerInntekt(17.januar til 31.januar))
    }

    @Test
    fun `ferie uten sykmelding i agp`() {
        undersøke(5.S + 5.AIG + 10.S)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 20.januar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = (17.januar til 20.januar).toSet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().forventerInntekt(17.januar til 31.januar))
    }

    @Test
    fun `ferie i nesten fullført agp`() {
        undersøke(15.S + 5.F)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 20.januar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = emptySet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(16.januar til 20.januar in perioder.first().somArbeidsgiverperiode())
    }

    @Test
    fun `arbeid etter ferie i agp`() {
        undersøke(5.S + 5.F + 5.A + 11.S + 1.F + 13.S)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 9.februar,
            arbeidsgiverperiode = (1.januar til 10.januar).toSet() + (16.januar til 21.januar).toSet(),
            utbetalingsperioder = (22.januar til 26.januar).toSet() + (28.januar til 9.februar).toSet(),
            oppholdsperioder = (11.januar til 15.januar).toSet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().forventerInntekt(22.januar til 31.januar))
    }

    @Test
    fun `arbeid etter ferie uten sykmelding i agp`() {
        undersøke(5.S + 5.AIG + 5.A + 11.S + 1.AIG + 13.S)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 9.februar,
            arbeidsgiverperiode = (1.januar til 5.januar).toSet() + (16.januar til 26.januar).toSet(),
            utbetalingsperioder = (28.januar til 9.februar).toSet(),
            oppholdsperioder = (6.januar til 15.januar).toSet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().forventerInntekt(29.januar til 31.januar))
    }

    @Test
    fun `opphold direkte etter fullført agp`() {
        undersøke(16.S + 16.A)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 1.februar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = emptySet(),
            oppholdsperioder = (17.januar til 1.februar).toSet(),
            fullstendig = true,
            sisteDag = 1.februar
        ), perioder.single())
        assertFalse(perioder.first().somArbeidsgiverperiode().forventerInntekt(17.januar til 31.januar))
        assertTrue(17.januar til 31.januar in perioder.first().somArbeidsgiverperiode())
    }

    @Test
    fun `opphold etter litt utbetaling`() {
        undersøke(17.S + 15.A)
        assertEquals(1, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 1.februar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = (17.januar til 17.januar).toSet(),
            oppholdsperioder = (18.januar til 1.februar).toSet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.single())
        assertTrue(perioder.first().somArbeidsgiverperiode().forventerInntekt(17.januar til 31.januar))
        assertFalse(perioder.first().somArbeidsgiverperiode().forventerInntekt(18.januar til 31.januar))
        assertFalse(perioder.first().somArbeidsgiverperiode().forventerInntekt(19.januar til 31.januar))
    }

    @Test
    fun `ferie etter opphold etter fullført agp`() {
        undersøke(17.S + 1.A + 16.F + 16.S)
        assertEquals(2, perioder.size)
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 1.januar til 2.februar,
            arbeidsgiverperiode = (1.januar til 16.januar).toSet(),
            utbetalingsperioder = setOf(17.januar),
            oppholdsperioder = (18.januar til 2.februar).toSet(),
            fullstendig = true,
            sisteDag = 2.februar
        ), perioder.first())
        assertEquals(Arbeidsgiverperioderesultat(
            omsluttendePeriode = 4.februar til 19.februar,
            arbeidsgiverperiode = (4.februar til 19.februar).toSet(),
            utbetalingsperioder = emptySet(),
            oppholdsperioder = emptySet(),
            fullstendig = true,
            sisteDag = null
        ), perioder.last())
        assertFalse(perioder.first().somArbeidsgiverperiode().forventerInntekt(18.januar til 31.januar))
    }


    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        perioder.clear()
    }

    private val perioder: MutableList<Arbeidsgiverperioderesultat> = mutableListOf()

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val arbeidsgiverperiodeberegner = Arbeidsgiverperiodeberegner(teller)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeberegner) ?: arbeidsgiverperiodeberegner)
        val arbeidsgiverperioder = arbeidsgiverperiodeberegner.resultat()
        perioder.addAll(arbeidsgiverperioder)
    }
}
