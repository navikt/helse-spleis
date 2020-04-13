package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntektsmeldingTest {

    private lateinit var inntektsmelding: Inntektsmelding

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode`() {
        inntektsmelding(emptyList())
        assertTrue(inntektsmelding.valider().hasWarnings())
    }

    @Test
    internal fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), emptyList())
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `arbeidsgiverperiode med gap`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), førsteFraværsdag = 4.januar)

        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `ferieperiode med gap`() {
        inntektsmelding(
            listOf(Periode(1.januar, 1.januar)),
            listOf(Periode(2.januar, 3.januar), Periode(5.januar, 6.januar)),
            førsteFraværsdag = 1.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(ImplisittDag::class, tidslinje[4.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[6.januar]!!::class)
    }

    @Test
    internal fun `første fraværsdag etter arbeidsgiverperiode`() {
        inntektsmelding(listOf(Periode(1.januar, 1.januar)), førsteFraværsdag = 3.januar)
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(ImplisittDag::class, tidslinje[2.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
    }

    @Test
    internal fun `arbeidsgiverperiode og ferie med gap og første fraværsdag etterpå`() {
        inntektsmelding(
            listOf(Periode(1.januar, 1.januar)),
            listOf(Periode(3.januar, 3.januar)),
            førsteFraværsdag = 5.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(ImplisittDag::class, tidslinje[2.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(ImplisittDag::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `ferie i arbeidsgiverperioden uten overlapp`() {
        inntektsmelding(
            listOf(Periode(1.januar, 2.januar), Periode(5.januar, 6.januar)),
            listOf(Periode(3.januar, 4.januar)),
            førsteFraværsdag = 1.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[6.januar]!!::class)
    }

    @Test
    internal fun `overlapp i ferieperioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar), Periode(3.januar, 4.januar)),
            førsteFraværsdag = 1.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
    }

    @Test
    internal fun `førsteFraværsdag mellom ferie og arbeidsgiverperiode`() {
        inntektsmelding(
            listOf(Periode(4.januar, 5.januar)),
            listOf(Periode(1.januar, 2.januar)),
            førsteFraværsdag = 3.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `førsteFraværsdag mellom arbeidsgiverperiode og ferie`() {
        inntektsmelding(
            listOf(Periode(1.januar, 2.januar)),
            listOf(Periode(4.januar, 5.januar)),
            førsteFraværsdag = 3.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `ferieperiode før arbeidsgiverperiode`() {
        inntektsmelding(
            listOf(Periode(4.januar, 5.januar)),
            listOf(Periode(1.januar, 2.januar)),
            førsteFraværsdag = 4.januar
        )
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(ImplisittDag::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList(), listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)))
        val sykdomstidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, sykdomstidslinje.førsteDag())
    }

    @Test
    internal fun `arbeidgiverperioden kan ha overlappende perioder`() {
        inntektsmelding(
            listOf(
                Periode(1.januar, 2.januar), Periode(4.januar, 5.januar), Periode(3.januar, 4.januar)
            ), emptyList()
        )
        inntektsmelding.valider()
        assertFalse(inntektsmelding.hasErrors())
    }

    @Test
    internal fun `arbeidgiverdager vinner over feriedager`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar))
        )
        inntektsmelding.valider()
        assertFalse(inntektsmelding.hasErrors())

        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Feriedag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
    }

    @Test
    internal fun `helg i opphold i arbeidsgiverperioden skal være helgedager`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 4.januar), Periode(9.januar, 10.januar))
        )

        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[1.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[2.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[3.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[4.januar]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, tidslinje[5.januar]!!::class)
        assertEquals(FriskHelgedag.Inntektsmelding::class, tidslinje[6.januar]!!::class)
        assertEquals(FriskHelgedag.Inntektsmelding::class, tidslinje[7.januar]!!::class)
        assertEquals(Arbeidsdag.Inntektsmelding::class, tidslinje[8.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[9.januar]!!::class)
        assertEquals(Egenmeldingsdag.Inntektsmelding::class, tidslinje[10.januar]!!::class)
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        inntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(2.januar, inntektsmelding.sykdomstidslinje().sisteDag())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), listOf(Periode(17.januar, 18.januar)))

        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().førsteDag())
        assertEquals(18.januar, inntektsmelding.sykdomstidslinje().sisteDag())
        assertEquals(18, inntektsmelding.sykdomstidslinje().flatten().size)
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
    internal fun `refusjon opphører før perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 1.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertFalse(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrors())
    }

    @Test
    internal fun `refusjon opphører i perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 10.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrors())
    }

    @Test
    internal fun `endring i refusjon i perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 16.januar,
            endringerIRefusjon = listOf(10.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrors())
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
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
