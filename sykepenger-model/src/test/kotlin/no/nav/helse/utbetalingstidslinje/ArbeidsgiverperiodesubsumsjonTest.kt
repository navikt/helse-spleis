package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dsl.a1
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Subsumsjon
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.annetLedd
import no.nav.helse.etterlevelse.bokstavA
import no.nav.helse.etterlevelse.folketrygdloven
import no.nav.helse.etterlevelse.førsteLedd
import no.nav.helse.etterlevelse.paragraf
import no.nav.helse.etterlevelse.tredjeLedd
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.UK
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodesubsumsjonTest {
    @Test
    fun ingenting() {
        undersøke(1.A + 29.opphold + 1.A)
        assertEquals(31, observatør.dager)
        assertEquals(23, observatør.arbeidsdager)
        assertEquals(8, observatør.fridager)
        assertEquals(4, jurist.subsumsjoner)
        assertEquals(8, jurist.`§ 8-17 ledd 2`)
    }

    @Test
    fun kurant() {
        undersøke(31.S)
        assertEquals(31, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(4, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun spredt() {
        undersøke(4.S + 1.A + 4.S + 1.A + 4.S + 10.A + 11.S)
        assertEquals(35, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(7, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(2, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `spredt utbetalingsperiode`() {
        undersøke(17.S + 10.A + 4.S + 10.A + 4.S)
        assertEquals(45, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(9, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(2, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `ny arbeidsgiverperiode etter fullført`() {
        undersøke(16.S + 32.A + 16.S)
        assertEquals(64, observatør.dager)
        assertEquals(32, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.utbetalingsdager)
        assertEquals(4, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(32, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(0, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(0, jurist.`§ 8-11`)
        assertEquals(32, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `ny arbeidsgiverperiode etter halvferdig`() {
        undersøke(8.S + 32.A + 16.S)
        assertEquals(56, observatør.dager)
        assertEquals(24, observatør.arbeidsgiverperiodedager)
        assertEquals(0, observatør.utbetalingsdager)
        assertEquals(4, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(24, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(0, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(0, jurist.`§ 8-11`)
        assertEquals(24, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `siste dag i arbeidsgiverperioden er også første dag etter opphold i arbeidsgiverperioden`() {
        undersøke(15.S + 1.A + 2.S)
        assertEquals(18, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(1, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(0, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `utbetaling kun i helg`() {
        undersøke(3.opphold + 18.S)
        assertEquals(18, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(2, observatør.utbetalingsdager)
        assertEquals(4, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(0, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(2, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun infotrygd() {
        undersøke(31.S, infotrygdBetalteDager = listOf(1.januar til 10.januar))
        assertEquals(31, observatør.dager)
        assertEquals(0, observatør.arbeidsgiverperiodedager)
        assertEquals(31, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(8, jurist.`§ 8-11`)
        assertEquals(0, jurist.`§ 8-19 andre ledd - beregning`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
    }

    @Test
    fun `infotrygd etter arbeidsgiverperiode`() {
        undersøke(31.S, infotrygdBetalteDager = listOf(17.januar til 20.januar))
        assertEquals(31, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(15, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(0, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(4, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    @Test
    fun `ferie i infotrygd mellom`() {
        undersøke(20.S + 11.UK + 20.S, infotrygdFerieperioder = listOf(21.januar til 31.januar))
        assertEquals(51, observatør.dager)
        assertEquals(16, observatør.arbeidsgiverperiodedager)
        assertEquals(24, observatør.utbetalingsdager)
        assertEquals(5, jurist.subsumsjoner)
        assertEquals(3, jurist.`§ 8-17 ledd 2`)
        assertEquals(16, jurist.`§ 8-17 første ledd bokstav a - ikke oppfylt`)
        assertEquals(1, jurist.`§ 8-17 første ledd bokstav a - oppfylt`)
        assertEquals(7, jurist.`§ 8-11`)
        assertEquals(16, jurist.`§ 8-19 andre ledd - beregning`)
    }

    private lateinit var jurist: Subsumsjonobservatør
    private lateinit var teller: Arbeidsgiverperiodeteller
    private lateinit var observatør: Dagobservatør

    @BeforeEach
    fun setup() {
        resetSeed()
        jurist = Subsumsjonobservatør()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
    }

    private fun undersøke(tidslinje: Sykdomstidslinje, infotrygdBetalteDager: List<Periode> = emptyList(), infotrygdFerieperioder: List<Periode> = emptyList()) {
        val arbeidsgiverperiodeberegner = Arbeidsgiverperiodeberegner(teller)
        val arbeidsgiverperioder = arbeidsgiverperiodeberegner.resultat(tidslinje, infotrygdBetalteDager, infotrygdFerieperioder)
        val builder = ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
            arbeidsgiverperiode = arbeidsgiverperioder.flatMap { it.arbeidsgiverperiode }.grupperSammenhengendePerioder(),
            dagerNavOvertarAnsvar = emptyList(),
            refusjonstidslinje = tidslinje.periode()?.let { ARBEIDSGIVER.beløpstidslinje(it, 31000.månedlig) } ?: Beløpstidslinje(),
            fastsattÅrsinntekt = 31000.månedlig,
            inntektjusteringer = Beløpstidslinje()
        )

        val utbetalingstidslinje = builder.result(tidslinje)

        observatør = Dagobservatør(utbetalingstidslinje)

        val subsummering = Utbetalingstidslinjesubsumsjon(jurist, tidslinje, utbetalingstidslinje)
        subsummering.subsummer(tidslinje.periode()!!, Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1))
    }

    private class Subsumsjonobservatør : Subsumsjonslogg {
        var subsumsjoner = 0
        var `§ 8-17 første ledd bokstav a - ikke oppfylt` = 0
        var `§ 8-17 første ledd bokstav a - oppfylt` = 0
        var `§ 8-17 ledd 2` = 0
        var `§ 8-11` = 0
        var `§ 8-19 andre ledd - beregning` = 0

        private val sykepengerFraTrygden = folketrygdloven.paragraf(Paragraf.PARAGRAF_8_17)
        private val beregningAvArbeidsgiverperiode = folketrygdloven.paragraf(Paragraf.PARAGRAF_8_19)

        private fun ClosedRange<LocalDate>.antallDager() = start.datesUntil(endInclusive.nesteDag).count().toInt()
        private fun Collection<ClosedRange<LocalDate>>.antallDager() = sumOf { it.antallDager() }
        private val Subsumsjon.perioder
            get() = output["perioder"]
                ?.let { it as List<*> }
                ?.map { it as Map<*, *> }
                ?.mapNotNull {
                    val fom = it["fom"] as? LocalDate
                    val tom = it["tom"] as? LocalDate
                    if (fom != null && tom != null) fom..tom else null
                }
                ?: emptyList()

        override fun logg(subsumsjon: Subsumsjon) {
            when {
                subsumsjon.er(folketrygdloven.paragraf(Paragraf.PARAGRAF_8_11)) -> {
                    subsumsjoner += 1
                    `§ 8-11` += subsumsjon.perioder.antallDager()
                }

                subsumsjon.er(sykepengerFraTrygden.førsteLedd.bokstavA) -> {
                    subsumsjoner += 1
                    if (subsumsjon.utfall == Utfall.VILKAR_OPPFYLT) `§ 8-17 første ledd bokstav a - oppfylt` += subsumsjon.perioder.antallDager()
                    else `§ 8-17 første ledd bokstav a - ikke oppfylt` += subsumsjon.perioder.antallDager()
                }

                subsumsjon.er(sykepengerFraTrygden.annetLedd) -> {
                    subsumsjoner += 1
                    `§ 8-17 ledd 2` += subsumsjon.perioder.antallDager()
                }

                subsumsjon.er(beregningAvArbeidsgiverperiode.annetLedd) -> {
                    subsumsjoner += 1
                    `§ 8-19 andre ledd - beregning` += subsumsjon.perioder.antallDager()
                }

                subsumsjon.er(beregningAvArbeidsgiverperiode.tredjeLedd) -> {
                }
            }
        }
    }

    private class Dagobservatør(utbetalingstidslinje: Utbetalingstidslinje) {
        val dager get() = fridager + arbeidsdager + arbeidsgiverperiodedager + utbetalingsdager + foreldetdager + avvistdager + ventetidsdager
        var fridager = 0
        var arbeidsdager = 0
        var arbeidsgiverperiodedager = 0
        var ventetidsdager = 0
        var arbeidsgiverperiodedagerNavAnsvar = 0
        var utbetalingsdager = 0
        var foreldetdager = 0
        var avvistdager = 0

        init {
            utbetalingstidslinje.forEach { dag ->
                when (dag) {
                    is Utbetalingsdag.Arbeidsdag -> arbeidsdager += 1
                    is Utbetalingsdag.ArbeidsgiverperiodeDag -> arbeidsgiverperiodedager += 1
                    is Utbetalingsdag.Ventetidsdag -> ventetidsdager += 1
                    is Utbetalingsdag.ArbeidsgiverperiodedagNav -> arbeidsgiverperiodedagerNavAnsvar += 1
                    is Utbetalingsdag.AvvistDag -> avvistdager += 1
                    is Utbetalingsdag.ForeldetDag -> foreldetdager += 1
                    is Utbetalingsdag.Fridag -> fridager += 1
                    is Utbetalingsdag.NavDag -> utbetalingsdager += 1
                    is Utbetalingsdag.NavHelgDag -> utbetalingsdager += 1
                    is Utbetalingsdag.UkjentDag -> {}
                }
            }
        }
    }
}
