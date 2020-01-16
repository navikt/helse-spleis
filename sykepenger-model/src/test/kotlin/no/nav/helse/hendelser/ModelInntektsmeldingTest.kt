package no.nav.helse.hendelser

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.person.Problemer
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.*

internal class ModelInntektsmeldingTest {

    private lateinit var inntektsmelding: ModelInntektsmelding
    private lateinit var problemer: Problemer

    @BeforeEach
    internal fun setup() {
        problemer = Problemer()
    }

    @Test
    internal fun `mellomrom mellom arbeidsgiverperioder skal være arbeidsdager`() {
        nyInntektsmelding(listOf(1.januar..2.januar, 4.januar..5.januar), emptyList())
        assertEquals(Arbeidsdag::class, inntektsmelding.sykdomstidslinje().dag(3.januar)!!::class)
    }

    @Test
    internal fun `arbeidsgiverperioden skal være egenmeldingsdager`() {
        nyInntektsmelding(listOf(1.januar..2.januar, 4.januar..5.januar), emptyList())
        assertEquals(Egenmeldingsdag::class, inntektsmelding.sykdomstidslinje().dag(1.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, inntektsmelding.sykdomstidslinje().dag(2.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, inntektsmelding.sykdomstidslinje().dag(4.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, inntektsmelding.sykdomstidslinje().dag(5.januar)!!::class)
    }

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        nyInntektsmelding(emptyList(), listOf(1.januar..2.januar, 4.januar..5.januar))
        val sykdomstidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, sykdomstidslinje.førsteDag())
        assertThrows<IllegalStateException> {
            assertEquals(
                1.januar,
                sykdomstidslinje.utgangspunktForBeregningAvYtelse()
            )
        }
    }

    @Test
    internal fun `arbeidgiverperioden kan ikke ha overlappende perioder`() {
        nyInntektsmelding(listOf(1.januar..2.januar, 4.januar..5.januar, 3.januar..4.januar), emptyList())
        inntektsmelding.valider()
        assertTrue(problemer.hasErrors())
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        nyInntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().utgangspunktForBeregningAvYtelse())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().sisteDag())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        nyInntektsmelding(listOf(1.januar..16.januar), listOf(17.januar..18.januar))

        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().utgangspunktForBeregningAvYtelse())
        assertEquals(18.januar, inntektsmelding.sykdomstidslinje().sisteDag())
        assertEquals(18, inntektsmelding.sykdomstidslinje().flatten().size)
    }

    @Test
    internal fun `inntektsmelding med refusjon beløp != beregnetInntekt er ikke gyldig`() {
        assertThrows<Problemer> {
            nyInntektsmelding(
                emptyList(),
                emptyList(),
                refusjonBeløp = 100000.00,
                beregnetInntekt = 10000.00
            )
        }
    }

    @Test
    internal fun `når refusjonsopphørsdato er før siste utbetalingsdag, gir det endring i refusjon`() {
        nyInntektsmelding(listOf(1.januar .. 3.januar), emptyList(), refusjonOpphørsdato = 2.januar, endringerIRefusjon = listOf(2.januar))
        val sisteUtbetalingsdag = 3.januar
        assertTrue(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    @Test
    internal fun `når refusjonsopphørsdato er etter siste utbetalingsdag, gir det ikke endring i refusjon`() {
        nyInntektsmelding(listOf(1.januar .. 3.januar), emptyList(), refusjonOpphørsdato = 4.januar, endringerIRefusjon = listOf(4.januar))
        val sisteUtbetalingsdag = 3.januar
        assertFalse(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    private fun nyInntektsmelding(
        arbeidsgiverperioder: List<ClosedRange<LocalDate>>,
        ferieperioder: List<ClosedRange<LocalDate>>,
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) {
        inntektsmelding = ModelInntektsmelding(
            UUID.randomUUID(),
            ModelInntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            "88888888",
            "12020052345",
            "100010101010",
            1.februar.atStartOfDay(),
            førsteFraværsdag,
            beregnetInntekt,
            problemer,
            arbeidsgiverperioder,
            ferieperioder
        )
    }
}
