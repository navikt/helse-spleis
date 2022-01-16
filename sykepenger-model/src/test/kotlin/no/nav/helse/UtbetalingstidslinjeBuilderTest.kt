package no.nav.helse

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilderTest {
    @Test
    fun kort() {
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(15, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, perioder.size)
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
    fun `arbeidsgiverperioden er ferdig tidligere`() {
        teller.fullfør()
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(0, perioder.size)
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
        assertEquals(1.januar til 9.januar, perioder.first())
    }

    @Test
    fun `ferie med i arbeidsgiverperioden`() {
        undersøke(6.S + 6.F + 6.S)
        assertEquals(18, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `ferie fullfører arbeidsgiverperioden`() {
        undersøke(1.S + 15.F + 6.S)
        assertEquals(22, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `ferie etter utbetaling`() {
        undersøke(16.S + 15.F)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `ferie mellom utbetaling`() {
        undersøke(16.S + 10.F + 5.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(3, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.fridagTeller)
    }

    @Test
    fun `ferie før arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøke(1.S + 15.F + 1.A + 16.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 1.januar), perioder.first())
        assertEquals(listOf(18.januar til 2.februar), perioder.last())
    }

    @Test
    fun `ferie etter arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøke(1.S + 1.A + 15.F + 16.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 1.januar), perioder.first())
        assertEquals(listOf(18.januar til 2.februar), perioder.last())
    }

    @Test
    fun `ferie etter frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(6.S + 1.A + 15.F + 16.S)
        assertEquals(38, inspektør.size)
        assertEquals(22, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 6.januar), perioder.first())
        assertEquals(listOf(23.januar til 7.februar), perioder.last())
    }

    @Test
    fun `ferie før frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(5.S + 15.F + 1.A + 16.S)
        assertEquals(37, inspektør.size)
        assertEquals(21, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 5.januar), perioder.first())
        assertEquals(listOf(22.januar til 6.februar), perioder.last())
    }

    @Test
    fun `ferie som opphold før arbeidsgiverperioden`() {
        undersøke(15.F + 16.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, perioder.size)
        assertEquals(listOf(16.januar til 31.januar), perioder.first())
    }

    @Test
    fun `bare arbeidsdager`() {
        undersøke(31.A)
        assertEquals(31, inspektør.size)
        assertEquals(23, inspektør.arbeidsdagTeller)
        assertEquals(8, inspektør.fridagTeller)
        assertEquals(0, perioder.size)
    }

    @Test
    fun `spredt arbeidsgiverperiode`() {
        undersøke(10.S + 15.A + 7.S)
        assertEquals(32, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.navDagTeller)
        assertEquals(11, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
        assertEquals(1, perioder.size)
        assertEquals(listOf(1.januar til 10.januar, 26.januar til 31.januar), perioder.first())
    }

    @Test
    fun `nok opphold til å tilbakestille arbeidsgiverperiode`() {
        undersøke(10.S + 16.A + 7.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(12, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 10.januar), perioder.first())
        assertEquals(listOf(27.januar til 2.februar), perioder.last())
    }

    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        perioder.clear()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private val perioder: MutableList<Arbeidsgiverperiode> = mutableListOf()

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val builder = UtbetalingstidslinjeBuilder()
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, Komposittmediator(periodebuilder, builder))
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        inspektør = builder.result().inspektør
        perioder.addAll(periodebuilder.result())
    }

    private class Komposittmediator(private val mediators: List<ArbeidsgiverperiodeMediator>) : ArbeidsgiverperiodeMediator {
        constructor(vararg mediator: ArbeidsgiverperiodeMediator) : this(mediator.toList())

        override fun fridag(dato: LocalDate) {
            mediators.forEach { it.fridag(dato) }
        }

        override fun arbeidsdag(dato: LocalDate) {
            mediators.forEach { it.arbeidsdag(dato) }
        }

        override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
            mediators.forEach { it.arbeidsgiverperiodedag(dato, økonomi) }
        }

        override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
            mediators.forEach { it.utbetalingsdag(dato, økonomi) }
        }

        override fun arbeidsgiverperiodeAvbrutt() {
            mediators.forEach { it.arbeidsgiverperiodeAvbrutt() }
        }

        override fun arbeidsgiverperiodeFerdig() {
            mediators.forEach { it.arbeidsgiverperiodeFerdig() }
        }
    }

    private fun assertEquals(expected: Iterable<LocalDate>, actual: Arbeidsgiverperiode) {
        assertEquals(expected.toList(), actual.toList())
    }

    private fun assertEquals(expected: List<Iterable<LocalDate>>, actual: Arbeidsgiverperiode) {
        assertEquals(expected.flatMap { it.toList() }, actual.toList())
    }
}
