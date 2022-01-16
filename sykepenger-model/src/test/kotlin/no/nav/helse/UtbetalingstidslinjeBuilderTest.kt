package no.nav.helse

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingstidslinjeBuilderTest {
    @Test
    fun kort() {
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(15, inspektør.arbeidsgiverperiodeDagTeller)
    }

    @Test
    fun enkel() {
        undersøke(31.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
    }

    @Test
    fun `arbeidsgiverperioden er ferdig tidligere`() {
        teller.fullfør()
        undersøke(15.S)
        assertEquals(15, inspektør.size)
        assertEquals(11, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
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
    }

    @Test
    fun `ferie med i arbeidsgiverperioden`() {
        undersøke(6.S + 6.F + 6.S)
        assertEquals(18, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(2, inspektør.navDagTeller)
    }

    @Test
    fun `ferie fullfører arbeidsgiverperioden`() {
        undersøke(1.S + 15.F + 6.S)
        assertEquals(22, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(4, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `ferie etter utbetaling`() {
        undersøke(16.S + 15.F)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
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
    }

    @Test
    fun `ferie etter arbeidsdag tilbakestiller arbeidsgiverperioden`() {
        undersøke(1.S + 1.A + 15.F + 16.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
    }

    @Test
    fun `ferie etter frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(6.S + 1.A + 15.F + 16.S)
        assertEquals(38, inspektør.size)
        assertEquals(22, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
    }

    @Test
    fun `ferie før frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(5.S + 15.F + 1.A + 16.S)
        assertEquals(37, inspektør.size)
        assertEquals(21, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(16, inspektør.fridagTeller)
    }

    @Test
    fun `ferie som opphold før arbeidsgiverperioden`() {
        undersøke(15.F + 16.S)
        assertEquals(31, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
    }

    @Test
    fun `bare arbeidsdager`() {
        undersøke(31.A)
        assertEquals(31, inspektør.size)
        assertEquals(23, inspektør.arbeidsdagTeller)
        assertEquals(8, inspektør.fridagTeller)
    }

    @Test
    fun `spredt arbeidsgiverperiode`() {
        undersøke(10.S + 15.A + 7.S)
        assertEquals(32, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(1, inspektør.navDagTeller)
        assertEquals(11, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    @Test
    fun `nok opphold til å tilbakestille arbeidsgiverperiode`() {
        undersøke(10.S + 16.A + 7.S)
        assertEquals(33, inspektør.size)
        assertEquals(17, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(12, inspektør.arbeidsdagTeller)
        assertEquals(4, inspektør.fridagTeller)
    }

    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
    }
    private lateinit var inspektør: UtbetalingstidslinjeInspektør

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val builder = UtbetalingstidslinjeBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, builder)
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        inspektør = builder.result().inspektør
    }
}
