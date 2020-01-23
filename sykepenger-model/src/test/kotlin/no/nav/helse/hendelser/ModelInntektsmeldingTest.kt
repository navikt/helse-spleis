package no.nav.helse.hendelser

import no.nav.helse.Uke
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class ModelInntektsmeldingTest {

    private lateinit var inntektsmelding: ModelInntektsmelding
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
    }

    @Test
    internal fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(1.januar..2.januar, 4.januar..5.januar), emptyList())
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsdag::class, tidslinje.dag(3.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje.dag(1.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje.dag(2.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje.dag(4.januar)!!::class)
        assertEquals(Egenmeldingsdag::class, tidslinje.dag(5.januar)!!::class)
        assertEquals(Uke(1).torsdag, tidslinje.utgangspunktForBeregningAvYtelse())
    }

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList(), listOf(1.januar..2.januar, 4.januar..5.januar))
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
        inntektsmelding(listOf(1.januar..2.januar, 4.januar..5.januar, 3.januar..4.januar), emptyList())
        inntektsmelding.valider()
        assertTrue(aktivitetslogger.hasErrors())
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        inntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().utgangspunktForBeregningAvYtelse())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().sisteDag())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        inntektsmelding(listOf(1.januar..16.januar), listOf(17.januar..18.januar))

        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().utgangspunktForBeregningAvYtelse())
        assertEquals(18.januar, inntektsmelding.sykdomstidslinje().sisteDag())
        assertEquals(18, inntektsmelding.sykdomstidslinje().flatten().size)
    }

    @Test
    internal fun `inntektsmelding med refusjon beløp != beregnetInntekt er ikke gyldig`() {
        assertThrows<Aktivitetslogger.AktivitetException> {
            inntektsmelding(
                emptyList(),
                emptyList(),
                refusjonBeløp = 100000.00,
                beregnetInntekt = 10000.00
            )
        }
    }

    @Test
    internal fun `når refusjonsopphørsdato er før siste utbetalingsdag, gir det endring i refusjon`() {
        inntektsmelding(listOf(1.januar .. 3.januar), emptyList(), refusjonOpphørsdato = 2.januar, endringerIRefusjon = listOf(2.januar))
        val sisteUtbetalingsdag = 3.januar
        assertTrue(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    @Test
    internal fun `når refusjonsopphørsdato er etter siste utbetalingsdag, gir det ikke endring i refusjon`() {
        inntektsmelding(listOf(1.januar .. 3.januar), emptyList(), refusjonOpphørsdato = 4.januar, endringerIRefusjon = listOf(4.januar))
        val sisteUtbetalingsdag = 3.januar
        assertFalse(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    private fun inntektsmelding(
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
            aktivitetslogger,
            "{}",
            arbeidsgiverperioder,
            ferieperioder
        )
    }
}
