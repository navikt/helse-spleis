package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverperiodesubsumsjonTest {
    @Test
    fun ingenting() {
        undersøke(1.A + 29.opphold + 1.A)
        assertEquals(31, observatør.dager)
        assertEquals(23, observatør.arbeidsdager)
        assertEquals(8, observatør.fridager)
        assertEquals(8, jurist.subsumsjoner)
        assertEquals(8, jurist.`§ 8-17 ledd 2`)
    }

    @Test
    fun kurant() {
        undersøke(31.S)
        assertEquals(31, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.utbetalingsdager)
        assertEquals(38, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(4, jurist.`§ 8-11 første ledd`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
        assertEquals(1, jurist.`§ 8-19 første ledd - beregning`)
    }

    @Test
    fun `utbetaling kun i helg`() {
        undersøke(3.opphold + 18.S)
        assertEquals(18, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(2, observatør.utbetalingsdager)
        assertEquals(35, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(0, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(2, jurist.`§ 8-11 første ledd`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
        assertEquals(1, jurist.`§ 8-19 første ledd - beregning`)

    }

    @Test
    fun infotrygd() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(1.januar til 10.januar))
        }
        assertEquals(31, observatør.dager)
        assertEquals(0, observatør.arbeidsgiverperiodedager)
        assertEquals(31, observatør.utbetalingsdager)
        assertEquals(8, jurist.subsumsjoner)
        assertEquals(8, jurist.`§ 8-11 første ledd`)
        assertEquals(0, jurist.`§ 8-19 første ledd - beregning`)
        assertEquals(0, jurist.`§ 8-19 andre ledd - beregning`)
        assertEquals(0, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
    }

    @Test
    fun `infotrygd etter arbeidsgiverperiode`() {
        undersøke(31.S) { teller, other ->
            Infotrygddekoratør(teller, other, listOf(17.januar til 20.januar))
        }
        assertEquals(31, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.utbetalingsdager)
        assertEquals(38, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(4, jurist.`§ 8-11 første ledd`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
        assertEquals(1, jurist.`§ 8-19 første ledd - beregning`)

    }

    private lateinit var jurist: Subsumsjonobservatør
    private lateinit var teller: Arbeidsgiverperiodeteller
    private lateinit var observatør: Dagobservatør

    @BeforeEach
    fun setup() {
        resetSeed()
        jurist = Subsumsjonobservatør()
        observatør = Dagobservatør()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
    }

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, observatør, jurist)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
    }

    private class Subsumsjonobservatør : SubsumsjonObserver {
        var subsumsjoner = 0
        var `§ 8-17 første ledd bokstav a - ikke oppfylt` = 0
        var `§ 8-17 første ledd bokstav a - oppfylt` = 0
        var `§ 8-17 ledd 2` = 0
        var `§ 8-11 første ledd` = 0
        var `§ 8-19 andre ledd - beregning` = 0
        var `§ 8-19 første ledd - beregning`= 0

        override fun `§ 8-17 ledd 1 bokstav a`(oppfylt: Boolean, dagen: LocalDate) {
            subsumsjoner += 1
            if (oppfylt) `§ 8-17 første ledd bokstav a - oppfylt` += 1
            else `§ 8-17 første ledd bokstav a - ikke oppfylt` += 1
        }

        override fun `§ 8-17 ledd 2`(dato: LocalDate, sykdomstidslinje: List<SubsumsjonObserver.Tidslinjedag>) {
            subsumsjoner += 1
            `§ 8-17 ledd 2` += 1
        }

        override fun `§ 8-11 første ledd`(dato: LocalDate) {
            subsumsjoner += 1
            `§ 8-11 første ledd` += 1
        }

        override fun `§ 8-19 andre ledd`(dato: LocalDate, beregnetTidslinje: List<SubsumsjonObserver.Tidslinjedag>) {
            subsumsjoner += 1
            `§ 8-19 andre ledd - beregning` += 1
        }

        override fun `§ 8-19 første ledd`(dato: LocalDate, beregnetTidslinje: List<SubsumsjonObserver.Tidslinjedag>) {
            subsumsjoner += 1
            `§ 8-19 første ledd - beregning` += 1
        }
    }

    private class Dagobservatør : ArbeidsgiverperiodeMediator {
        val dager get() = fridager + arbeidsdager + arbeidsgiverperiodedager + utbetalingsdager + foreldetdager + avvistdager
        var fridager = 0
        var arbeidsdager = 0
        var arbeidsgiverperiodedager = 0
        var utbetalingsdager = 0
        var foreldetdager = 0
        var avvistdager = 0

        override fun fridag(dato: LocalDate) {
            fridager += 1
        }

        override fun arbeidsdag(dato: LocalDate) {
            arbeidsdager += 1
        }

        override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
            arbeidsgiverperiodedager += 1
        }

        override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
            utbetalingsdager += 1
        }

        override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
            foreldetdager += 1
        }

        override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
            avvistdager += 1
        }
    }
}
