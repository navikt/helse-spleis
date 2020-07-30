package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde.INNTEKTSMELDING
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class UtbetalingstidslinjeBuilderTest {
    private val hendelseId = UUID.randomUUID()
    private lateinit var tidslinje: Utbetalingstidslinje
    private val inspektør get() = TestTidslinjeInspektør(tidslinje)

    @BeforeEach
    internal fun reset() {
        resetSeed()
    }

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        2.nS.utbetalingslinjer()
        assertEquals(null, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }


    @Test
    fun `sykedager i periode som starter i helg får riktig inntekt`() {
        resetSeed(6.januar)
        (16.nS + 4.nS).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertDagsats(1431)
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        (16.nS + 3.nS).utbetalingslinjer()
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        (16.nS + 6.nS).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `utbetalingstidslinjer kan starte i helg`() {
        (3.nA + 16.nS + 6.nS).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        (16.nS + 7.nS + 2.nA + 1.nS).utbetalingslinjer() //6 utbetalingsdager
        assertEquals(6, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        (15.nS + 2.nA + 1.nS + 7.nS).utbetalingslinjer()
        assertEquals(5, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        (1.nS + 2.nF + 13.nS + 1.nS).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(14, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperioden`() {
        (1.nS + 2.nF + 1.nA + 1.nS + 14.nS + 3.nS).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        (1.nS + 1.nA + 2.nF + 1.nS + 14.nS + 3.nS).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        (16.nS + 2.nF + 1.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperiode teller som gap, men ikke ferie`() {
        (15.nS + 2.nF + 1.nA + 1.nS).utbetalingslinjer()
        assertEquals(null, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.nS + 16.nF + 1.nA + 3.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        (15.nS + 16.nF + 1.nA + 3.nS).utbetalingslinjer()
        assertEquals(18, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
        (2.nF + 2.nA + 16.nS + 2.nF).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[Fridag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
        (10.nS + 20.nF + 1.nA + 10.nS + 20.nF).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(40, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        (10.nS + 20.nF + 10.nS).utbetalingslinjer()
        assertEquals(10, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(20, inspektør.dagtelling[Fridag::class])
        assertEquals(8, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        (20.nS + 20.nF + 10.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(9, inspektør.dagtelling[NavDag::class])
        assertEquals(5, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(20, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
        (5.nU + 15.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(1, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `16 dagers opphold etter en utbetaling gir ny arbeidsgiverperiode ved påfølgende sykdom`() {
        (22.nS + 16.nA + 10.nS).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `Ferie i arbeidsgiverperioden direkte etterfulgt av en arbeidsdag gjør at ferien teller som opphold`() {
        (10.nS + 15.nF + 1.nA + 10.nS).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertNull(inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie etter arbeidsdag i arbeidsgiverperioden gjør at ferien teller som opphold`() {
        (10.nS + 1.nA + 15.nF + 10.nS).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertNull(inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie direkte etter arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (16.nS + 15.nF + 1.nA + 10.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(6, inspektør.dagtelling[NavDag::class])
        assertEquals(4, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (20.nS + 15.nF + 1.nA + 10.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(11, inspektør.dagtelling[NavDag::class])
        assertEquals(3, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold`() {
        (21.nS + 1.nA + 15.nF + 10.nS).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 15F gir ett opphold på 16 dager og dette resulterer i to arbeidsgiverperioder`() {
        (17.nS + 4.nF + 1.nA + 15.nF + 10.nS).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(19, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 13F gir ett opphold på 14 dager og dette resulterer i én arbeidsgiverperiode`() {
        (17.nS + 4.nF + 1.nA + 13.nF + 10.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(17, inspektør.dagtelling[Fridag::class])
        assertEquals(9, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg`() {
        (3.nA + 19.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `arbeidsgiverperioden slutter på en fredag`() {
        (3.nA + 5.nS + 2.nF + 13.nS).utbetalingslinjer()
        assertEquals(14, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(2, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(3, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `ferie før arbeidsdag etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.nS + 6.nS + 16.nF + 1.nA + 16.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(15, inspektør.dagtelling[NavDag::class])
        assertEquals(7, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
        (16.nS + 6.nS + 16.nA + 16.nS).utbetalingslinjer()
        assertEquals(32, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `resetter arbeidsgiverperioden etter 16 arbeidsdager`() {
        (15.nS + 16.nA + 14.nS).utbetalingslinjer()
        assertEquals(29, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `oppdelt utbetalingstidslinje har ingen sykedager betalt av nav`() {
        (50.nS).utbetalingslinjer(sisteDag = 10.januar)
        assertNull(inspektør.dagtelling[NavDag::class])
        assertEquals(10, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `oppdelt tidslinje i arbeidsgiverperioden`() {
        (50.nS).utbetalingslinjer(sisteDag = 20.januar)
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(1, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på mandag`() {
        (1.nS + 3.nA + 4.nS + 3.nA + 11.nS + 4.nS).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[22.januar])
        assertEquals(NavDag::class, inspektør.datoer[23.januar])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på søndag`() {
        (1.nS + 3.nA + 4.nS + 2.nA + 12.nS + 4.nS).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[21.januar])
        assertEquals(NavDag::class, inspektør.datoer[22.januar])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på lørdag`() {
        (1.nS + 3.nA + 4.nS + 1.nA + 13.nS + 4.nS).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[20.januar])
        assertEquals(NavHelgDag::class, inspektør.datoer[21.januar])
        assertEquals(NavDag::class, inspektør.datoer[22.januar])
    }

    @Test
    fun `ForeldetSykedag godkjennes som ArbeidsgverperiodeDag`() {
        (10.nK + 6.nS).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `ForeldetSykedag blir ForeldetDag utenfor arbeidsgiverperioden`() {
        (20.nK).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[ForeldetDag::class])
        assertEquals(1, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `riktig inntekt for riktig dag`() {
        resetSeed(1.desember(2017))
        20.nS.utbetalingslinjer()
        assertDagsats(1431)

        resetSeed(8.januar)
        20.nS.utbetalingslinjer()
        assertDagsats(1431)

        resetSeed(8.januar)
        40.nS.utbetalingslinjer()
        assertDagsats(1431)

        resetSeed(1.februar)
        40.nS.utbetalingslinjer()
        assertDagsats(1154)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag`() {
        (15.nU + 1.nF + 1.nU + 10.nS).utbetalingslinjer(
            inntektshistorikk = Inntekthistorikk().apply {
                add(17.januar, hendelseId, 31000.månedlig, INNTEKTSMELDING)
            }
        )
        assertNotEquals(0.0, inspektør.navdager.first().økonomi.toMap()["dekningsgrunnlag"])
        assertEquals(18.januar, inspektør.navdager.first().dato)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag med påfølgende helg`() {
        resetSeed(1.januar(2020))
        (10.nU + 7.nF + 14.nS).utbetalingslinjer(
            inntektshistorikk = Inntekthistorikk().apply {
                add(17.januar(2020), hendelseId, 31000.månedlig, INNTEKTSMELDING)
            }
        )
        assertEquals(31, inspektør.datoer.size)
        assertEquals(Fridag::class, inspektør.datoer[17.januar(2020)])
        assertEquals(NavHelgDag::class, inspektør.datoer[18.januar(2020)])
        assertEquals(NavHelgDag::class, inspektør.datoer[19.januar(2020)])
        assertEquals(NavDag::class, inspektør.datoer[20.januar(2020)])
    }

    private val inntekthistorikk = Inntekthistorikk().apply {
        add(1.januar.minusDays(1), hendelseId, 31000.månedlig, INNTEKTSMELDING)
        add(1.februar.minusDays(1), hendelseId, 25000.månedlig, INNTEKTSMELDING)
        add(1.mars.minusDays(1), hendelseId, 50000.månedlig, INNTEKTSMELDING)
    }

    private fun assertDagsats(dagsats: Int) {
        inspektør.navdager.forEach {
            it.økonomi.reflectionRounded { _, _, dekningsgrunnlag, _, _, _, _ ->
                assertEquals(dagsats, dekningsgrunnlag)
            }
        }
    }

    private fun Sykdomstidslinje.utbetalingslinjer(
        sisteDag: LocalDate = this.periode()?.endInclusive ?: 1.januar,
        inntektshistorikk: Inntekthistorikk = inntekthistorikk
    ) {
        tidslinje = UtbetalingstidslinjeBuilder(
            sisteDag = sisteDag,
            inntekthistorikk = inntektshistorikk
        ).result(this)
    }

    private class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {

        internal val navdager = mutableListOf<NavDag>()
        internal val dagtelling: MutableMap<KClass<out Utbetalingsdag>, Int> = mutableMapOf()
        internal val datoer = mutableMapOf<LocalDate, KClass<out Utbetalingsdag>>()

        init {
            tidslinje.accept(this)
        }

        override fun visit(
            dag: NavDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dato] = NavDag::class
            navdager.add(dag)
            inkrementer(NavDag::class)
        }

        override fun visit(
            dag: Arbeidsdag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = Arbeidsdag::class
            inkrementer(Arbeidsdag::class)
        }

        override fun visit(
            dag: NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = NavHelgDag::class
            inkrementer(NavHelgDag::class)
        }

        override fun visit(
            dag: UkjentDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = UkjentDag::class
            inkrementer(UkjentDag::class)
        }

        override fun visit(
            dag: ArbeidsgiverperiodeDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = ArbeidsgiverperiodeDag::class
            inkrementer(ArbeidsgiverperiodeDag::class)
        }

        override fun visit(
            dag: Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = Fridag::class
            inkrementer(Fridag::class)
        }

        override fun visit(
            dag: ForeldetDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = ForeldetDag::class
            inkrementer(ForeldetDag::class)
        }

        override fun visit(
            dag: AvvistDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = AvvistDag::class
            inkrementer(AvvistDag::class)
        }

        private fun inkrementer(klasse: KClass<out Utbetalingsdag>) {
            dagtelling.compute(klasse) { _, value -> 1 + (value ?: 0) }
        }
    }
}
