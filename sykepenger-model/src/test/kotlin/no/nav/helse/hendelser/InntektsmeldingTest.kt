package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsmeldingTest {

    private lateinit var inntektsmelding: Inntektsmelding

    @Test
    internal fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), emptyList())
        val tidslinje = inntektsmelding.sykdomstidslinje(5.januar)
        assertEquals(Arbeidsdag.Inntektsmelding::class, tidslinje.dag(3.januar)!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje.dag(1.januar)!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje.dag(2.januar)!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje.dag(4.januar)!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje.dag(5.januar)!!::class)
    }

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList(), listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)))
        val sykdomstidslinje = inntektsmelding.sykdomstidslinje(5.januar)
        assertEquals(1.januar, sykdomstidslinje.førsteDag())
    }

    @Test
    internal fun `arbeidgiverperioden kan ikke ha overlappende perioder`() {
        inntektsmelding(
            listOf(
                Periode(1.januar, 2.januar), Periode(4.januar, 5.januar), Periode(3.januar, 4.januar)
            ), emptyList()
        )
        inntektsmelding.valider()
        assertTrue(inntektsmelding.hasErrors())
    }

    @Test
    internal fun `arbeidgiverperioden kan ikke overlappe med ferieperioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar))
        )
        inntektsmelding.valider()
        assertTrue(inntektsmelding.hasErrors())
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        inntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje(2.januar).førsteDag())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje(2.januar).sisteDag())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), listOf(Periode(17.januar, 18.januar)))

        assertEquals(1.januar, inntektsmelding.sykdomstidslinje(18.januar).førsteDag())
        assertEquals(18.januar, inntektsmelding.sykdomstidslinje(18.januar).sisteDag())
        assertEquals(18, inntektsmelding.sykdomstidslinje(18.januar).flatten().size)
    }

    @Test
    internal fun `inntektsmelding med refusjon beløp != beregnetInntekt er ikke gyldig`() {
        inntektsmelding(
            emptyList(),
            emptyList(),
            refusjonBeløp = 100000.00,
            beregnetInntekt = 10000.00
        )
        assertTrue(inntektsmelding.valider().hasErrors())
    }

    @Test
    internal fun `når refusjonsopphørsdato er før siste utbetalingsdag, gir det endring i refusjon`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 2.januar,
            endringerIRefusjon = listOf(2.januar)
        )
        val sisteUtbetalingsdag = 3.januar
        assertTrue(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    @Test
    internal fun `når refusjonsopphørsdato er etter siste utbetalingsdag, gir det ikke endring i refusjon`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 4.januar,
            endringerIRefusjon = listOf(4.januar)
        )
        val sisteUtbetalingsdag = 3.januar
        assertFalse(inntektsmelding.harEndringIRefusjon(sisteUtbetalingsdag))
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode>,
        refusjonBeløp: Double = 1000.00,
        beregnetInntekt: Double = 1000.00,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 1.januar,
        endringerIRefusjon: List<LocalDate> = emptyList()
    ) {
        inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = "88888888",
            fødselsnummer = "12020052345",
            aktørId = "100010101010",
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder
        )
    }
}
