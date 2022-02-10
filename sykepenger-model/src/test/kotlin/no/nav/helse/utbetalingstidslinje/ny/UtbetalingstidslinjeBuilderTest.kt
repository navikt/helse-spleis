package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

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
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
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
        utbetalingstidslinje.subset(10.januar til utbetalingstidslinje.periode().endInclusive).forEach {
            assertEquals(arbeidsgiverperiode, it.økonomi.arbeidsgiverperiode)
        }
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
        assertEquals(0, perioder.size)
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
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
        assertEquals(1, perioder.size)
        assertEquals(18.januar til 2.februar, perioder.first())
        utbetalingstidslinje.subset(1.januar til 17.januar).forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(18.januar til 2.februar).forEach {
            assertEquals(18.januar til it.dato, it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(2.februar til utbetalingstidslinje.periode().endInclusive).forEach {
            assertEquals(18.januar til 2.februar, it.økonomi.arbeidsgiverperiode)
        }
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
        assertEquals(0, perioder.size)
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
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
    fun `ferie umiddelbart etter utbetaling teller ikke som opphold hvis etterfølgt av arbeidsdag`() {
        undersøke(16.S + 15.F + 1.A + 10.S)
        assertEquals(42, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(6, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(1, perioder.size)
        assertEquals(1.januar til 16.januar, perioder.first())
    }

    @Test
    fun `ferie etter utbetaling teller ikke som opphold hvis etterfølgt av arbeidsdag`() {
        undersøke(17.S + 15.F + 1.A + 9.S)
        assertEquals(42, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(6, inspektør.navDagTeller)
        assertEquals(4, inspektør.navHelgDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
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
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
        assertEquals(2, perioder.size)
        assertEquals(listOf(1.januar til 6.januar), perioder.first())
        assertEquals(listOf(23.januar til 7.februar), perioder.last())
    }

    @Test
    fun `ferie før frisk helg tilbakestiller arbeidsgiverperioden`() {
        undersøke(5.S + 15.F + 1.A + 16.S)
        assertEquals(37, inspektør.size)
        assertEquals(21, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(15, inspektør.fridagTeller)
        assertEquals(1, inspektør.arbeidsdagTeller)
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
        assertEquals(31, inspektør.arbeidsdagTeller)
        assertEquals(0, inspektør.fridagTeller)
        assertEquals(0, perioder.size)
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
    }

    @Test
    fun `bare ukjent dager`() {
        undersøke(14.opphold)
        assertEquals(0, inspektør.size)
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
    }

    @Test
    fun `fridager med ukjent dager i mellom`() {
        undersøke(1.F + 12.opphold + 1.F)
        assertEquals(14, inspektør.size)
        assertEquals(9, inspektør.arbeidsdagTeller)
        assertEquals(5, inspektør.fridagTeller)
        assertEquals(0, perioder.size)
        utbetalingstidslinje.forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
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
        undersøke(1.U + 14.F + 1.F + 10.S)
        assertEquals(26, inspektør.size)
        assertEquals(16, inspektør.arbeidsgiverperiodeDagTeller)
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(listOf(1.januar til 16.januar), perioder.first())
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
        utbetalingstidslinje.subset(førsteDel).forEach {
            assertEquals(førsteDel.start til it.dato, it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(førsteDel.endInclusive.plusDays(1) til 25.januar).forEach {
            assertEquals(førsteDel, it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(andreDel).forEach {
            assertEquals(listOf(førsteDel, andreDel.start til it.dato), it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(andreDel.endInclusive.plusDays(1) til utbetalingstidslinje.periode().endInclusive).forEach {
            assertEquals(arbeidsgiverperiode, it.økonomi.arbeidsgiverperiode)
        }
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
        utbetalingstidslinje.subset(11.januar til 25.januar).forEach {
            assertEquals(førsteArbeidsgiverperiode, it.økonomi.arbeidsgiverperiode)
        }
        assertNull(utbetalingstidslinje[26.januar].økonomi.arbeidsgiverperiode)
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
        utbetalingstidslinje.subset(11.januar til 25.januar).forEach {
            assertEquals(førsteArbeidsgiverperiode, it.økonomi.arbeidsgiverperiode)
        }
        utbetalingstidslinje.subset(26.januar til 10.februar).forEach {
            assertNull(it.økonomi.arbeidsgiverperiode)
        }
    }

    private val Økonomi.arbeidsgiverperiode get() = this.get<Arbeidsgiverperiode?>("arbeidsgiverperiode")
    private lateinit var teller: Arbeidsgiverperiodeteller

    @BeforeEach
    fun setup() {
        resetSeed()
        teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        perioder.clear()
    }

    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var utbetalingstidslinje: Utbetalingstidslinje
    private val perioder: MutableList<Arbeidsgiverperiode> = mutableListOf()

    private val inntektsopplysningPerSkjæringstidspunkt = mapOf(
        1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 31000.månedlig),
        1.februar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.februar, UUID.randomUUID(), 25000.månedlig),
        1.mars to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.mars, UUID.randomUUID(), 50000.månedlig),
    )

    private fun undersøke(tidslinje: Sykdomstidslinje, delegator: ((Arbeidsgiverperiodeteller, SykdomstidslinjeVisitor) -> SykdomstidslinjeVisitor)? = null) {
        val inntekter = Inntekter(
            skjæringstidspunkter = listOf(1.januar),
            inntektPerSkjæringstidspunkt = inntektsopplysningPerSkjæringstidspunkt,
            regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist()
        )
        val builder = UtbetalingstidslinjeBuilder(inntekter)
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        val arbeidsgiverperiodeBuilder = ArbeidsgiverperiodeBuilder(teller, Komposittmediator(periodebuilder, builder))
        tidslinje.accept(delegator?.invoke(teller, arbeidsgiverperiodeBuilder) ?: arbeidsgiverperiodeBuilder)
        utbetalingstidslinje = builder.result()
        inspektør = utbetalingstidslinje.inspektør
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

        override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
            mediators.forEach { it.foreldetDag(dato, økonomi) }
        }

        override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
            mediators.forEach { it.avvistDag(dato, begrunnelse) }
        }
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
