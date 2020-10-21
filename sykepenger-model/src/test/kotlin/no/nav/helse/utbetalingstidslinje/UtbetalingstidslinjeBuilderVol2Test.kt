package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class UtbetalingstidslinjeBuilderVol2Test {
    private val hendelseId = UUID.randomUUID()
    private lateinit var tidslinje: Utbetalingstidslinje
    private val inspektør get() = TestTidslinjeInspektør(tidslinje)

    @BeforeEach
    internal fun reset() {
        resetSeed()
    }

    @Test
    fun `to dager blir betalt av arbeidsgiver`() {
        2.S.utbetalingslinjer()
        assertEquals(null, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }


    @Test
    fun `sykedager i periode som starter i helg får riktig inntekt`() {
        resetSeed(6.januar)
        (16.S + 4.S).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertInntekter(1431)
    }

    @Test
    fun `en utbetalingslinje med tre dager`() {
        (16.S + 3.S).utbetalingslinjer()
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `en utbetalingslinje med helg`() {
        (16.S + 6.S).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `utbetalingstidslinjer kan starte i helg`() {
        (3.A + 16.S + 6.S).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Sykedager med inneklemte arbeidsdager`() {
        (16.S + 7.S + 2.A + 1.S).utbetalingslinjer() //6 utbetalingsdager
        assertEquals(6, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdager i arbeidsgiverperioden`() {
        (15.S + 2.A + 1.S + 7.S).utbetalingslinjer()
        assertEquals(5, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie i arbeidsgiverperiode`() {
        (1.S + 2.F + 13.S + 1.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(14, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperioden`() {
        (1.S + 2.F + 1.A + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Arbeidsdag før ferie i arbeidsgiverperioden`() {
        (1.S + 1.A + 2.F + 1.S + 14.S + 3.S).utbetalingslinjer()
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie etter arbeidsgiverperioden`() {
        (16.S + 2.F + 1.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Arbeidsdag etter ferie i arbeidsgiverperiode teller som gap, men ikke ferie`() {
        (15.S + 2.F + 1.A + 1.S).utbetalingslinjer()
        assertEquals(null, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `Ferie rett etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.S + 16.F + 1.A + 3.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie i slutten av arbeidsgiverperioden teller som opphold`() {
        (15.S + 16.F + 1.A + 3.S).utbetalingslinjer()
        assertEquals(18, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Ferie og arbeid påvirker ikke initiell tilstand`() {
        (2.F + 2.A + 16.S + 2.F).utbetalingslinjer()
        assertEquals(4, inspektør.dagtelling[Fridag::class])
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Arbeidsgiverperioden resettes når det er opphold over 16 dager`() {
        (10.S + 20.F + 1.A + 10.S + 20.F).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(40, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `Ferie fullfører arbeidsgiverperioden`() {
        (10.S + 20.F + 10.S).utbetalingslinjer()
        assertEquals(10, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(20, inspektør.dagtelling[Fridag::class])
        assertEquals(8, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie mer enn 16 dager gir ikke ny arbeidsgiverperiode`() {
        (20.S + 20.F + 10.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(9, inspektør.dagtelling[NavDag::class])
        assertEquals(5, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(20, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `egenmelding sammen med sykdom oppfører seg som sykdom`() {
        (5.U + 15.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(1, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `16 dagers opphold etter en utbetaling gir ny arbeidsgiverperiode ved påfølgende sykdom`() {
        (22.S + 16.A + 10.S).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `Ferie i arbeidsgiverperioden direkte etterfulgt av en arbeidsdag gjør at ferien teller som opphold`() {
        (10.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertNull(inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie etter arbeidsdag i arbeidsgiverperioden gjør at ferien teller som opphold`() {
        (10.S + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(20, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertNull(inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie direkte etter arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (16.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(6, inspektør.dagtelling[NavDag::class])
        assertEquals(4, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, selv om det er en direkte etterfølgende arbeidsdag`() {
        (20.S + 15.F + 1.A + 10.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(11, inspektør.dagtelling[NavDag::class])
        assertEquals(3, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold`() {
        (21.S + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(15, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(3, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 15F gir ett opphold på 16 dager og dette resulterer i to arbeidsgiverperioder`() {
        (17.S + 4.F + 1.A + 15.F + 10.S).utbetalingslinjer()
        assertEquals(26, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(19, inspektør.dagtelling[Fridag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
    }

    @Test
    fun `Ferie direkte etter en sykedag utenfor arbeidsgiverperioden teller ikke som opphold, mens ferie direkte etter en arbeidsdag utenfor arbeidsgiverperioden teller som opphold, så A + 13F gir ett opphold på 14 dager og dette resulterer i én arbeidsgiverperiode`() {
        (17.S + 4.F + 1.A + 13.F + 10.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(17, inspektør.dagtelling[Fridag::class])
        assertEquals(9, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `arbeidsgiverperiode med tre påfølgende sykedager i helg`() {
        (3.A + 19.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(1, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `arbeidsgiverperioden slutter på en fredag`() {
        (3.A + 5.S + 2.F + 13.S).utbetalingslinjer()
        assertEquals(14, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(2, inspektør.dagtelling[Fridag::class])
        assertEquals(2, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(3, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `ferie før arbeidsdag etter arbeidsgiverperioden teller ikke som opphold`() {
        (16.S + 6.S + 16.F + 1.A + 16.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(16, inspektør.dagtelling[Fridag::class])
        assertEquals(15, inspektør.dagtelling[NavDag::class])
        assertEquals(7, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(1, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `ta hensyn til en andre arbeidsgiverperiode, arbeidsdageropphold`() {
        (16.S + 6.S + 16.A + 16.S).utbetalingslinjer()
        assertEquals(32, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[NavDag::class])
        assertEquals(2, inspektør.dagtelling[NavHelgDag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
    }

    @Test
    fun `resetter arbeidsgiverperioden etter 16 arbeidsdager`() {
        (15.S + 16.A + 14.S).utbetalingslinjer()
        assertEquals(29, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(4, inspektør.dagtelling[Fridag::class])
        assertEquals(12, inspektør.dagtelling[Arbeidsdag::class])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på mandag`() {
        (1.S + 3.A + 4.S + 3.A + 11.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[22.januar])
        assertEquals(NavDag::class, inspektør.datoer[23.januar])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på søndag`() {
        (1.S + 3.A + 4.S + 2.A + 12.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[21.januar])
        assertEquals(NavDag::class, inspektør.datoer[22.januar])
    }

    @Test
    fun `siste dag i arbeidsgiverperioden faller på lørdag`() {
        (1.S + 3.A + 4.S + 1.A + 13.S + 4.S).utbetalingslinjer()
        assertEquals(ArbeidsgiverperiodeDag::class, inspektør.datoer[20.januar])
        assertEquals(NavHelgDag::class, inspektør.datoer[21.januar])
        assertEquals(NavDag::class, inspektør.datoer[22.januar])
    }

    @Test
    fun `ForeldetSykedag godkjennes som ArbeidsgverperiodeDag`() {
        (10.K + 6.S).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
    }

    @Test
    fun `ForeldetSykedag blir ForeldetDag utenfor arbeidsgiverperioden`() {
        (20.K).utbetalingslinjer()
        assertEquals(16, inspektør.dagtelling[ArbeidsgiverperiodeDag::class])
        assertEquals(3, inspektør.dagtelling[ForeldetDag::class])
        assertEquals(1, inspektør.dagtelling[NavHelgDag::class])
    }

    @Test
    fun `riktig inntekt for riktig dag`() {
        resetSeed(8.januar)
        20.S.utbetalingslinjer()
        assertInntekter(1431)

        resetSeed(8.januar)
        40.S.utbetalingslinjer()
        assertInntekter(1431)

        resetSeed(1.februar)
        40.S.utbetalingslinjer()
        assertInntekter(1154)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag`() {
        (15.U + 1.F + 1.U + 10.S).utbetalingslinjer()
        assertNotEquals(
            0.0,
            inspektør
                .navdager
                .first()
                .økonomi
                .reflection { _, _, dekningsgrunnlag, _, _, _, _ -> dekningsgrunnlag }
        )
        assertEquals(18.januar, inspektør.navdager.first().dato)
    }

    @Test
    fun `feriedag før siste arbeidsgiverperiodedag med påfølgende helg`() {
        resetSeed(1.januar(2020))
        (10.U + 7.F + 14.S).utbetalingslinjer()
        assertEquals(31, inspektør.datoer.size)
        assertEquals(Fridag::class, inspektør.datoer[17.januar(2020)])
        assertEquals(NavHelgDag::class, inspektør.datoer[18.januar(2020)])
        assertEquals(NavHelgDag::class, inspektør.datoer[19.januar(2020)])
        assertEquals(NavDag::class, inspektør.datoer[20.januar(2020)])
    }

    @Test
    fun `Setter inntekt basert på inntektsdatoer`() {
        resetSeed(1.januar(2020))
        (14.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                invoke {
                    addInntektsmelding(1.januar(2020), hendelseId, 31000.månedlig)
                }
            },
            inntektsdatoer = listOf(1.januar(2020))
        )
        inspektør.navdager.assertDekningsgrunnlag(1.januar(2020) til 31.januar(2020), 31000.månedlig)
    }

    @Test
    fun `Setter inntekt basert på inntektsdato for siste del av arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (10.S + 10.A + 10.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                invoke {
                    addInntektsmelding(21.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(21.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 10.januar(2020), null)
        inspektør.arbeidsdager.assertDekningsgrunnlag(11.januar(2020) til 20.januar(2020), null)
        inspektør.navdager.assertDekningsgrunnlag(21.januar(2020) til 30.januar(2020), 30000.månedlig)
    }

    @Test
    fun `Setter inntekt basert på inntektsdatoer med gap`() {
        resetSeed(1.januar(2020))
        (20.S + 10.A + 10.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                invoke {
                    addInntektsmelding(1.januar(2020), hendelseId, 31000.månedlig)
                    addInntektsmelding(31.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(1.januar(2020), 31.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), 31000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 20.januar(2020), 31000.månedlig)
        inspektør.arbeidsdager.assertDekningsgrunnlag(21.januar(2020) til 30.januar(2020), 31000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(31.januar(2020) til 9.februar(2020), 30000.månedlig)
    }

    @Test
    fun `Arbeidsgiverdager før frisk helg har ikke inntekt`() {
        resetSeed(1.januar(2020))
        (3.S + 2.A + 5.S + 2.A + 20.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                invoke {
                    addInntektsmelding(13.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(13.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 3.januar(2020), null)
        inspektør.fridager.assertDekningsgrunnlag(4.januar(2020) til 5.januar(2020), null)
        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(6.januar(2020) til 10.januar(2020), null)
        inspektør.fridager.assertDekningsgrunnlag(11.januar(2020) til 12.januar(2020), null)
        inspektør.navdager.assertDekningsgrunnlag(13.januar(2020) til 1.februar(2020), 30000.månedlig)
    }

    @Test
    fun `Endrer ikke inntekt ved ferie`() {
        resetSeed(1.januar(2020))
        (5.S + 5.F + 15.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                this {
                    addInntektsmelding(1.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(1.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 5.januar(2020), 30000.månedlig)
        inspektør.fridager.assertDekningsgrunnlag(6.januar(2020) til 10.januar(2020), 30000.månedlig)
        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(11.januar(2020) til 16.januar(2020), 30000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 25.januar(2020), 30000.månedlig)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold i arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (2.S + 1.A + 7.F + 17.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                this {
                    addInntektsmelding(11.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(11.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 2.januar(2020), null)
        inspektør.arbeidsdager.assertDekningsgrunnlag(3.januar(2020) til 3.januar(2020), null)
        inspektør.fridager.assertDekningsgrunnlag(4.januar(2020) til 10.januar(2020), null)
        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(11.januar(2020) til 24.januar(2020), 30000.månedlig)
        inspektør.navHelgdager.assertDekningsgrunnlag(25.januar(2020) til 26.januar(2020), 0.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(27.januar(2020) til 27.januar(2020), 30000.månedlig)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold utenfor arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (20.S + 1.A + 3.F + 3.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                this {
                    addInntektsmelding(1.januar(2020), hendelseId, 30000.månedlig)
                    addInntektsmelding(25.januar(2020), hendelseId, 31000.månedlig)
                }
            },
            inntektsdatoer = listOf(1.januar(2020), 25.januar(2020))
        )

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), 30000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(17.januar(2020) til 20.januar(2020), 30000.månedlig)
        inspektør.arbeidsdager.assertDekningsgrunnlag(21.januar(2020) til 21.januar(2020), 30000.månedlig)
        inspektør.fridager.assertDekningsgrunnlag(22.januar(2020) til 26.januar(2020), 30000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(27.januar(2020) til 27.januar(2020), 31000.månedlig)
    }

    @Test
    fun `Setter inntekt ved sykedag i helg etter opphold rett etter arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        (16.S + 2.A + 3.S).utbetalingslinjer(
            inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                this {
                    addInntektsmelding(19.januar(2020), hendelseId, 30000.månedlig)
                }
            },
            inntektsdatoer = listOf(19.januar(2020))
        )
        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 16.januar(2020), null)
        inspektør.arbeidsdager.assertDekningsgrunnlag(17.januar(2020) til 17.januar(2020), null)
        inspektør.fridager.assertDekningsgrunnlag(18.januar(2020) til 18.januar(2020), null)
        inspektør.navdager.assertDekningsgrunnlag(19.januar(2020) til 21.januar(2020), 30000.månedlig)
    }

    @Test
    fun `opphold i arbeidsgiverperioden`() {
        resetSeed(1.januar(2020))
        assertDoesNotThrow {
            (1.S + 11.A + 21.S).utbetalingslinjer(
                inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                    this {
                        addInntektsmelding(13.januar(2020), hendelseId, 30000.månedlig)
                    }
                },
                inntektsdatoer = listOf(13.januar(2020), 1.januar(2020))
            )
        }

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(1.januar(2020) til 1.januar(2020), null)
        inspektør.arbeidsdager.assertDekningsgrunnlag(2.januar(2020) til 12.januar(2020), null)
        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(13.januar(2020) til 28.januar(2020), 30000.månedlig)
        inspektør.navdager.assertDekningsgrunnlag(29.januar(2020) til 31.januar(2020), 30000.månedlig)
    }

    @Test
    fun `opphold i arbeidsgiverperiode`() {
        resetSeed(4.januar(2020))
        assertDoesNotThrow {
            (16.U + 2.A + 2.S).utbetalingslinjer(
                inntektshistorikkVol2 = InntektshistorikkVol2().apply {
                    this {
                        addInntektsmelding(22.januar(2020), hendelseId, 30000.månedlig)
                    }
                },
                inntektsdatoer = listOf(22.januar(2020), 4.januar(2020))
            )
        }

        inspektør.arbeidsgiverdager.assertDekningsgrunnlag(4.januar(2020) til 19.januar(2020), null)
        inspektør.arbeidsdager.assertDekningsgrunnlag(20.januar(2020) til 21.januar(2020), null)
        inspektør.navdager.assertDekningsgrunnlag(22.januar(2020) til 23.januar(2020), 30000.månedlig)
    }

    private val inntektshistorikkVol2 = InntektshistorikkVol2().apply {
        invoke {
            addInntektsmelding(1.januar, hendelseId, 31000.månedlig)
            addInntektsmelding(1.februar, hendelseId, 25000.månedlig)
            addInntektsmelding(1.mars, hendelseId, 50000.månedlig)
        }
    }

    private fun assertInntekter(dekningsgrunnlaget: Int? = null, aktuelleDagsinntekten: Int? = null) {
        inspektør.navdager.forEach { navDag ->
            navDag.økonomi.reflectionRounded { _, _, dekningsgrunnlag, aktuellDagsinntekt, _, _, _ ->
                dekningsgrunnlaget?.let { assertEquals(it, dekningsgrunnlag) }
                aktuelleDagsinntekten?.let { assertEquals(it, aktuellDagsinntekt) }
            }
        }
    }

    private fun List<Utbetalingsdag>.assertDekningsgrunnlag(periode: Periode, dekningsgrunnlaget: Inntekt?) =
        filter { it.dato in periode }
            .forEach { utbetalingsdag ->
                val daglig = dekningsgrunnlaget?.reflection { _, _, _, daglig -> daglig }
                utbetalingsdag.økonomi.reflectionRounded { _, _, dekningsgrunnlag, _, _, _, _ ->
                    assertEquals(daglig, dekningsgrunnlag)
                }
            }

    private fun Sykdomstidslinje.utbetalingslinjer(
        inntektshistorikkVol2: InntektshistorikkVol2 = this@UtbetalingstidslinjeBuilderVol2Test.inntektshistorikkVol2,
        inntektsdatoer: List<LocalDate> = listOf(1.januar, 1.februar, 1.mars)
    ) {
        tidslinje = UtbetalingstidslinjeBuilderVol2(
            sammenhengendePeriode = this.periode()!!,
            inntektshistorikkVol2 = inntektshistorikkVol2,
            inntektsdatoer = inntektsdatoer
        ).result(this)
    }

    private class TestTidslinjeInspektør(tidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {

        val navdager = mutableListOf<NavDag>()
        val navHelgdager = mutableListOf<NavHelgDag>()
        val arbeidsdager = mutableListOf<Arbeidsdag>()
        val arbeidsgiverdager = mutableListOf<ArbeidsgiverperiodeDag>()
        val fridager = mutableListOf<Fridag>()
        val dagtelling: MutableMap<KClass<out Utbetalingsdag>, Int> = mutableMapOf()
        val datoer = mutableMapOf<LocalDate, KClass<out Utbetalingsdag>>()

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
            arbeidsdager.add(dag)
            inkrementer(Arbeidsdag::class)
        }

        override fun visit(
            dag: NavHelgDag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = NavHelgDag::class
            navHelgdager.add(dag)
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
            arbeidsgiverdager.add(dag)
            inkrementer(ArbeidsgiverperiodeDag::class)
        }

        override fun visit(
            dag: Fridag,
            dato: LocalDate,
            økonomi: Økonomi
        ) {
            datoer[dag.dato] = Fridag::class
            fridager.add(dag)
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
