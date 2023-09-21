package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.februar
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.AIG
import no.nav.helse.testhelpers.K
import no.nav.helse.testhelpers.N
import no.nav.helse.testhelpers.P
import no.nav.helse.testhelpers.PROBLEM
import no.nav.helse.testhelpers.R
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.U
import no.nav.helse.testhelpers.YA
import no.nav.helse.testhelpers.YD
import no.nav.helse.testhelpers.YF
import no.nav.helse.testhelpers.YO
import no.nav.helse.testhelpers.YOL
import no.nav.helse.testhelpers.YP
import no.nav.helse.testhelpers.YS
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.somVilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.UforventetDagException
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UtbetalingstidslinjeBuilderTest {
    @Test
    fun problemdag() {
        assertThrows<UforventetDagException> {
            undersøke(1.PROBLEM)
        }
    }

    @Test
    fun kort() {
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(15, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 15.januar, perioder.first())
    }

    @Test
    fun `kort - skal utbetales`() {
        undersøke(15.N)
        assertEquals(15, inspektør.size)
        assertEquals(4, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.arbeidsgiverperiodedagNavTeller)
        assertEquals(1, perioder.size)
        val arbeidsgiverperiode = perioder.first()
        assertTrue(arbeidsgiverperiode.forventerInntekt(1.januar til 15.januar, Sykdomstidslinje(), SubsumsjonObserver.NullObserver))
        assertEquals(1.januar til 15.januar, perioder.first())
    }

    @Test
    fun enkel() {
        undersøke(31.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `enkel - SykedagNav`() {
        undersøke(31.N)
        assertEquals(31, inspektør.size)
        assertEquals(12, inspektør.arbeidsgiverperiodedagNavTeller)
        assertEquals(4, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
        utbetalingstidslinje[1.januar].økonomi.inspektør.also {
            assertEquals(31000.månedlig, it.aktuellDagsinntekt)
        }
    }

    @Test
    fun `arbeidsgiverperioden er ferdig tidligere`() {
        teller.fullfør()
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(emptyList<LocalDate>(), perioder.first())
        assertTrue(perioder.first().fiktiv())
    }

    @Test
    fun `arbeidsgiverperioden oppdages av noen andre`() {
        val betalteDager = listOf(10.januar til 16.januar)
        undersøke(15.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(15, inspektør.size)
        assertEquals(9, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        val arbeidsgiverperiode = 1.januar til 9.januar
        assertEquals(arbeidsgiverperiode, perioder.first())
    }

    @Test
    fun `arbeidsgiverperioden er ferdig`() {
        val betalteDager = listOf(1.januar til 1.januar)
        undersøke(15.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(15, inspektør.size)
        assertEquals(0, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(emptyList<LocalDate>(), perioder.first())
        assertTrue(perioder.first().fiktiv())
    }

    @Test
    fun `ny arbeidsgiverperiode etter infotrygd`() {
        val betalteDager = listOf(1.januar til 1.januar)
        undersøke(1.S + 16.A + 17.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(34, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.navDagTeller)
        assertEquals(1, inspektør.navHelgDagTeller)
        assertEquals(16, inspektør.arbeidsdagTeller)
        assertEquals(2, perioder.size)
        assertEquals(emptyList<LocalDate>(), perioder.first())
        assertTrue(perioder.first().fiktiv())
        assertEquals(18.januar til 2.februar, perioder.last())
    }

    @Test
    fun `opphold etter infotrygd`() {
        val betalteDager = listOf(1.januar til 1.januar)
        undersøke(1.S + 15.A + 18.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(34, inspektør.size)
        assertEquals(0, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(14, inspektør.navDagTeller)
        assertEquals(5, inspektør.navHelgDagTeller)
        assertEquals(15, inspektør.arbeidsdagTeller)
        assertEquals(1, perioder.size)
        assertEquals(emptyList<LocalDate>(), perioder.first())
        assertTrue(perioder.first().fiktiv())
    }

    @Test
    fun `infotrygd utbetaler etter vi har startet arbeidsgiverperiodetelling med opphold`() {
        val betalteDager = listOf(11.januar til 1.februar)
        undersøke(9.S + 1.A + 22.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(32, inspektør.size)
        assertEquals(9, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.navDagTeller)
        assertEquals(6, inspektør.navHelgDagTeller)
    }

    @Test
    fun `kort infotrygdperiode etter utbetalingopphold`() {
        val betalteDager = listOf(19.februar til 15.mars)
        undersøke(16.U + 1.S + 32.opphold + 5.S + 20.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(74, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(22, inspektør.arbeidsdagTeller)
        assertEquals(10, inspektør.fridagTeller)
        assertEquals(20, inspektør.navDagTeller)
        assertEquals(6, inspektør.navHelgDagTeller)
    }

    @Test
    fun `infotrygd midt i`() {
        val betalteDager = listOf(22.februar til 13.mars)
        undersøke(20.S + 32.A + 20.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(72, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(32, inspektør.arbeidsdagTeller)
        assertEquals(17, inspektør.navDagTeller)
        assertEquals(7, inspektør.navHelgDagTeller)
    }

    @Test
    fun `alt infotrygd`() {
        val betalteDager = listOf(2.januar til 20.januar, 22.februar til 13.mars)
        undersøke(20.S + 32.A + 20.S) { teller, other ->
            Infotrygddekoratør(teller, other, betalteDager)
        }
        assertEquals(72, inspektør.size)
        assertEquals(1, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(32, inspektør.arbeidsdagTeller)
        assertEquals(28, inspektør.navDagTeller)
        assertEquals(11, inspektør.navHelgDagTeller)
    }

    @Test
    fun `ferie og permisjon med i arbeidsgiverperioden`() {
        undersøkeLike({ 6.S + 6.F + 6.S }, { 6.S + 6.P + 6.S }, { 6.S + 6.AIG + 6.S }, { 6.S + 6.YF + 6.S }) {
            assertEquals(18, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(2, inspektør.navDagTeller)
            assertEquals(1, perioder.size)
            assertEquals(1.januar til 16.januar, perioder.first())
        }
    }

    @Test
    fun `ferie og permisjon fullfører arbeidsgiverperioden`() {
        undersøkeLike({ 1.S + 15.F + 6.S }, { 1.S + 15.P + 6.S }, { 1.S + 15.AIG + 6.S }, { 1.S + 15.YF + 6.S }) {
            assertEquals(22, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(4, inspektør.navDagTeller)
            assertEquals(2, inspektør.navHelgDagTeller)
            assertEquals(1, perioder.size)
            assertEquals(1.januar til 16.januar, perioder.first())
        }
    }

    @Test
    fun `ferie og permisjon etter utbetaling`() {
        undersøkeLike({ 16.S + 15.F }, { 16.S + 15.P }, { 16.S + 15.AIG }) {
            assertEquals(31, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, perioder.size)
            assertEquals(1.januar til 16.januar, perioder.first())
        }
    }

    @Test
    fun `andre ytelser etter utbetaling`() {
        undersøke(16.S + 15.YF)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.avvistDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `andre ytelser umiddelbart etter utbetaling teller ikke som opphold`() {
        undersøke(16.S + 16.YF + 10.S)
        assertEquals(42, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.avvistDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `andre ytelser umiddelbart etter utbetaling teller ikke som opphold hvis etterfølgt av arbeidsdag`() {
        undersøke(16.S + 15.YF + 1.A + 10.S)
        assertEquals(42, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(6, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(15, inspektør.avvistDagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `ferie og permisjon umiddelbart etter utbetaling teller ikke som opphold hvis etterfølgt av arbeidsdag`() {
        undersøkeLike({ 16.S + 15.F + 1.A + 10.S }, { 16.S + 15.P + 1.A + 10.S }, { 16.S + 15.AIG + 1.A + 10.S }) {
            assertEquals(42, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(6, inspektør.navDagTeller)
            assertEquals(4, inspektør.navHelgDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(1, perioder.size)
            assertEquals(1.januar til 16.januar, perioder.first())
        }
    }

    @Test
    fun `ferie og permisjon etter utbetaling teller ikke som opphold hvis etterfølgt av arbeidsdag`() {
        undersøkeLike({ 17.S + 15.F + 1.A + 9.S }, { 17.S + 15.P + 1.A + 9.S }, { 17.S + 15.AIG + 1.A + 9.S }) {
            assertEquals(42, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(6, inspektør.navDagTeller)
            assertEquals(4, inspektør.navHelgDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(1, perioder.size)
            assertEquals(1.januar til 16.januar, perioder.first())
        }
    }

    @Test
    fun `ferie og permisjon mellom utbetaling`() {
        undersøkeLike({ 16.S + 10.F + 5.S }, { 16.S + 10.P + 5.S }, { 16.S + 10.AIG + 5.S }) {
            assertEquals(31, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(3, inspektør.navDagTeller)
            assertEquals(2, inspektør.navHelgDagTeller)
            assertEquals(10, inspektør.fridagTeller)
        }
    }

    @Test
    fun `ferie og permisjon før arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøkeLike({ 1.S + 15.F + 1.A + 16.S }, { 1.S + 15.P + 1.A + 16.S }, { 1.S + 15.AIG + 1.A + 16.S }) {
            assertEquals(33, inspektør.size)
            assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(2, perioder.size)
            assertEquals(listOf(1.januar til 1.januar), perioder.first())
            assertEquals(listOf(18.januar til 2.februar), perioder.last())
        }
    }

    @Test
    fun `ferie og permisjon etter arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøkeLike({ 1.S + 1.A + 15.F + 16.S }, { 1.S + 1.A + 15.P + 16.S }, { 1.S + 1.A + 15.AIG + 16.S }) {
            assertEquals(33, inspektør.size)
            assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(2, perioder.size)
            assertEquals(listOf(1.januar til 1.januar), perioder.first())
            assertEquals(listOf(18.januar til 2.februar), perioder.last())
        }
    }

    @Test
    fun `ferie og permisjon etter frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøkeLike({ 6.S + 1.A + 15.F + 16.S }, { 6.S + 1.A + 15.P + 16.S }, { 6.S + 1.A + 15.AIG + 16.S }) {
            assertEquals(38, inspektør.size)
            assertEquals(22, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(2, perioder.size)
            assertEquals(listOf(1.januar til 6.januar), perioder.first())
            assertEquals(listOf(23.januar til 7.februar), perioder.last())
        }
    }

    @Test
    fun `ferie og permisjon før frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøkeLike({ 5.S + 15.F + 1.A + 16.S }, { 5.S + 15.P + 1.A + 16.S }, { 5.S + 15.AIG + 1.A + 16.S }) {
            assertEquals(37, inspektør.size)
            assertEquals(21, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, inspektør.arbeidsdagTeller)
            assertEquals(2, perioder.size)
            assertEquals(listOf(1.januar til 5.januar), perioder.first())
            assertEquals(listOf(22.januar til 6.februar), perioder.last())
        }
    }

    @Test
    fun `ferie og permisjon som opphold før arbeidsgiverperioden`() {
        undersøkeLike({ 15.F + 16.S }, { 15.P + 16.S }, { 15.AIG + 16.S }) {
            assertEquals(31, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(15, inspektør.fridagTeller)
            assertEquals(1, perioder.size)
            assertEquals(listOf(16.januar til 31.januar), perioder.first())
        }
    }

    @Test
    fun `bare arbeidsdager`() {
        undersøke(31.A)
        assertEquals(31, inspektør.size)
        assertEquals(31, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
        assertEquals(0, perioder.size)
    }

    @Test
    fun `bare ukjent dager`() {
        undersøke(14.opphold)
        assertEquals(0, inspektør.size)
    }

    @Test
    fun `fridager med ukjent dager i mellom`() {
        undersøke(1.F + 12.opphold + 1.F)
        assertEquals(14, inspektør.size)
        assertEquals(9, inspektør.arbeidsdagTeller)
        assertEquals(5, inspektør.fridagTeller)
        assertEquals(0, perioder.size)
    }

    @Test
    fun `fridager fullfører ikke arbeidsgiverperioden dersom etterfulgt av ukjent dager`() {
        undersøke(15.S + 1.F + 12.opphold + 1.S)
        assertEquals(29, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(8, inspektør.arbeidsdagTeller)
        assertEquals(5, inspektør.fridagTeller)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 15.januar, 29.januar til 29.januar), perioder.first())
    }

    @Test
    fun `foreldet dag regnes fortsatt som syk`() {
        undersøke(16.S + 1.K)
        assertEquals(17, inspektør.size)
        assertEquals(1, inspektør.foreldetDagTeller)
        assertEquals(100, inspektør.grad(17.januar))
    }

    @Test
    fun `foreldet dager telles som arbeidsgiverperiode`() {
        undersøke(10.K + 6.S)
        assertEquals(16, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
    }

    @Test
    fun `foreldet dager etter utbetaling forblir foreldet`() {
        undersøke(19.K)
        assertEquals(19, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.foreldetDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
    }

    @Test
    fun `ferie mellom egenmeldingsdager`() {
        undersøkeLike({ 1.U + 14.F + 1.U + 10.S }, { 1.U + 14.AIG + 1.U + 10.S }) {
            assertEquals(26, inspektør.size)
            assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
            assertEquals(8, inspektør.navDagTeller)
            assertEquals(2, inspektør.navHelgDagTeller)
            assertEquals(listOf(1.januar til 16.januar), perioder.first())
        }
    }

    @Test
    fun `egenmeldingsdager med frisk helg gir opphold i arbeidsgiverperiode`() {
        undersøke(12.U + 2.R + 2.F + 2.U)
        assertEquals(18, inspektør.size)
        assertEquals(14, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.arbeidsdagTeller)
        assertEquals(2, inspektør.fridagTeller)
        assertEquals(listOf(1.januar til 12.januar, 17.januar til 18.januar), perioder.first())
    }

    @Test
    fun `avviser egenmeldingsdager utenfor arbeidsgiverperioden`() {
        undersøke(15.U + 1.F + 1.U)
        assertEquals(17, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.avvistDagTeller)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
    }

    @Test
    fun `avviser ikke egenmeldingsdager utenfor arbeidsgiverperioden som faller på helg`() {
        undersøke(3.A + 15.U + 1.F + 1.U)
        assertEquals(20, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.avvistDagTeller)
        assertEquals(1, inspektør.navHelgDagTeller)
        assertEquals(3, inspektør.arbeidsdagTeller)
        assertEquals(listOf(4.januar til 19.januar), perioder.first())
    }

    @Test
    fun `frisk helg gir opphold i arbeidsgiverperiode`() {
        undersøke(4.U + 8.S + 2.R + 2.F + 2.S)
        assertEquals(18, inspektør.size)
        assertEquals(14, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.arbeidsdagTeller)
        assertEquals(2, inspektør.fridagTeller)
        assertEquals(listOf(1.januar til 12.januar, 17.januar til 18.januar), perioder.first())
    }

    @Test
    fun `spredt arbeidsgiverperiode`() {
        undersøke(10.S + 15.A + 7.S)
        assertEquals(32, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.navDagTeller)
        assertEquals(15, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
        assertEquals(1, perioder.size)
        val førsteDel = 1.januar til 10.januar
        val andreDel = 26.januar til 31.januar
        val arbeidsgiverperiode = listOf(førsteDel, andreDel)
        assertEquals(arbeidsgiverperiode, perioder.first())
    }

    @Test
    fun `nok opphold til å tilbakestille arbeidsgiverperiode`() {
        undersøke(10.S + 16.A + 7.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(16, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
        assertEquals(2, perioder.size)
        val førsteArbeidsgiverperiode = listOf(1.januar til 10.januar)
        val andreArbeidsgiverperiode = listOf(27.januar til 2.februar)
        assertEquals(førsteArbeidsgiverperiode, perioder.first())
        assertEquals(andreArbeidsgiverperiode, perioder.last())
    }

    @Test
    fun `masse opphold`() {
        undersøke(10.S + 31.A + 7.S)
        assertEquals(48, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(31, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
        assertEquals(2, perioder.size)
        val førsteArbeidsgiverperiode = listOf(1.januar til 10.januar)
        val andreArbeidsgiverperiode = listOf(11.februar til 17.februar)
        assertEquals(førsteArbeidsgiverperiode, perioder.first())
        assertEquals(andreArbeidsgiverperiode, perioder.last())
    }

    @Test
    fun `avviser andre ytelser med riktige begrunnelser`() {
        undersøke(16.S + 1.YF + 1.YD + 1.YA + 1.YO + 1.YP + 1.YS + 1.YOL)
        assertEquals(7, inspektør.avvistDagTeller)
        assertEquals(Begrunnelse.AndreYtelserForeldrepenger, inspektør.begrunnelse(17.januar).single())
        assertEquals(Begrunnelse.AndreYtelserDagpenger, inspektør.begrunnelse(18.januar).single())
        assertEquals(Begrunnelse.AndreYtelserAap, inspektør.begrunnelse(19.januar).single())
        assertEquals(Begrunnelse.AndreYtelserOmsorgspenger, inspektør.begrunnelse(20.januar).single())
        assertEquals(Begrunnelse.AndreYtelserPleiepenger, inspektør.begrunnelse(21.januar).single())
        assertEquals(Begrunnelse.AndreYtelserSvangerskapspenger, inspektør.begrunnelse(22.januar).single())
        assertEquals(Begrunnelse.AndreYtelserOpplaringspenger, inspektør.begrunnelse(23.januar).single())
    }

    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        reset()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var utbetalingstidslinje: Utbetalingstidslinje
    private val perioder: MutableList<Arbeidsgiverperiode> = mutableListOf()

    private val inntektsopplysningPerSkjæringstidspunkt = mapOf(
        1.januar to Inntektsmelding(1.januar, UUID.randomUUID(), 31000.månedlig, LocalDateTime.now()),
        1.februar to Inntektsmelding(1.februar, UUID.randomUUID(), 25000.månedlig, LocalDateTime.now()),
        1.mars to Inntektsmelding(1.mars, UUID.randomUUID(), 50000.månedlig, LocalDateTime.now()),
    )

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val inntekter = Inntekter(
            organisasjonsnummer = "a1",
            vilkårsgrunnlagHistorikk = inntektsopplysningPerSkjæringstidspunkt.somVilkårsgrunnlagHistorikk("a1"),
            regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
            subsumsjonObserver = SubsumsjonObserver.NullObserver
        )
        val builder = UtbetalingstidslinjeBuilder(inntekter, tidslinje.periode() ?: LocalDate.MIN.somPeriode())
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller,
            Komposittmediator(periodebuilder, builder), SubsumsjonObserver.NullObserver)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        utbetalingstidslinje = builder.result()
        inspektør = utbetalingstidslinje.inspektør
        perioder.addAll(periodebuilder.result())
    }

    // undersøker forskjellige tidslinjer som skal ha samme funksjonelle betydning
    private fun undersøkeLike(vararg tidslinje: () -> Sykdomstidslinje, assertBlock: () -> Unit) {
        tidslinje.forEach {
            undersøke(resetSeed(tidslinjegenerator = it))
            assertBlock()
            reset()
        }
    }

    private fun reset() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        perioder.clear()
    }

    private class Komposittmediator(private val mediators: List<ArbeidsgiverperiodeMediator>) : ArbeidsgiverperiodeMediator {
        constructor(vararg mediator: ArbeidsgiverperiodeMediator) : this(mediator.toList())

        override fun fridag(dato: LocalDate) {
            mediators.forEach { it.fridag(dato) }
        }
        override fun fridagOppholdsdag(dato: LocalDate) {
            mediators.forEach { it.fridagOppholdsdag(dato) }
        }

        override fun arbeidsdag(dato: LocalDate) {
            mediators.forEach { it.arbeidsdag(dato) }
        }

        override fun arbeidsgiverperiodedag(
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) {
            mediators.forEach { it.arbeidsgiverperiodedag(dato, økonomi, kilde) }
        }

        override fun arbeidsgiverperiodedagNav(
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) {
            mediators.forEach { it.arbeidsgiverperiodedagNav(dato, økonomi, kilde) }
        }

        override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            mediators.forEach { it.utbetalingsdag(dato, økonomi, kilde) }
        }

        override fun arbeidsgiverperiodeAvbrutt() {
            mediators.forEach { it.arbeidsgiverperiodeAvbrutt() }
        }

        override fun arbeidsgiverperiodeFerdig() {
            mediators.forEach { it.arbeidsgiverperiodeFerdig() }
        }

        override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
            mediators.forEach { it.foreldetDag(dato, økonomi) }
        }

        override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
            mediators.forEach { it.avvistDag(dato, begrunnelse, økonomi) }
        }
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
