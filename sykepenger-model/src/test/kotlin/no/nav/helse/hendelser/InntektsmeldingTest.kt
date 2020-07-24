package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class InntektsmeldingTest {

    private lateinit var inntektsmelding: Inntektsmelding

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode og førsteFraværsdag er null`() {
        assertThrows<Aktivitetslogg.AktivitetException> { inntektsmelding(emptyList(), førsteFraværsdag = null) }
    }

    @Test
    fun `inntektsmelding hvor førsteFraværsdag er null`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar)), førsteFraværsdag = null)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(2.januar, nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode med førsteFraværsdag satt`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 1.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarnings())
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(1.januar, nyTidslinje.periode()?.endInclusive)
    }

    @Test
    internal fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), emptyList())
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `arbeidsgiverperiode med gap`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), førsteFraværsdag = 4.januar)

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `ferieperiode med gap`() {
        inntektsmelding(
            listOf(Periode(1.januar, 1.januar)),
            listOf(Periode(2.januar, 3.januar), Periode(5.januar, 6.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[2.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[4.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[5.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[6.januar]::class)
    }

    @Test
    internal fun `første fraværsdag etter arbeidsgiverperiode blir arbeidsgiverdag`() {
        inntektsmelding(listOf(Periode(1.januar, 1.januar)), førsteFraværsdag = 3.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
    }

    @Test
    internal fun `arbeidsgiverperiode og ferie med gap og første fraværsdag etterpå`() {
        inntektsmelding(
            listOf(Periode(1.januar, 1.januar)),
            listOf(Periode(3.januar, 3.januar)),
            førsteFraværsdag = 5.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[2.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `ferie i arbeidsgiverperioden uten overlapp`() {
        inntektsmelding(
            listOf(Periode(1.januar, 2.januar), Periode(5.januar, 6.januar)),
            listOf(Periode(3.januar, 4.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
        assertEquals(ArbeidsgiverHelgedag::class, nyTidslinje[6.januar]::class)
    }

    @Test
    internal fun `overlapp i ferieperioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar), Periode(3.januar, 4.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[4.januar]::class)
    }

    @Test
    internal fun `førsteFraværsdag mellom ferie og arbeidsgiverperiode`() {
        inntektsmelding(
            listOf(Periode(4.januar, 5.januar)),
            listOf(Periode(1.januar, 2.januar)),
            førsteFraværsdag = 3.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag::class, nyTidslinje[1.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `førsteFraværsdag mellom arbeidsgiverperiode og ferie blir arbeidsgiverdag`() {
        inntektsmelding(
            listOf(Periode(1.januar, 2.januar)),
            listOf(Periode(4.januar, 5.januar)),
            førsteFraværsdag = 3.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[4.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `ferieperiode før arbeidsgiverperiode`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(4.januar, 5.januar)),
            ferieperioder = listOf(Periode(1.januar, 2.januar)),
            førsteFraværsdag = 4.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Feriedag::class, nyTidslinje[1.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[2.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList(), ferieperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)))
        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().periode()?.start)
    }

    @Test
    internal fun `arbeidgiverperioden kan ha overlappende perioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar, 2.januar), Periode(4.januar, 5.januar), Periode(3.januar, 4.januar)
            ),
            ferieperioder = emptyList()
        )
        inntektsmelding.valider(Periode(1.januar, 31.januar))
        assertFalse(inntektsmelding.hasErrors())
    }

    @Test
    internal fun `arbeidgiverdager vinner over feriedager`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar))
        )
        inntektsmelding.valider(Periode(1.januar, 31.januar))
        assertFalse(inntektsmelding.hasErrors())

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    internal fun `helg i opphold i arbeidsgiverperioden skal være helgedager`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 4.januar), Periode(9.januar, 10.januar))
        )

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsdag::class, nyTidslinje[5.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[6.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[7.januar]::class)
        assertEquals(Arbeidsdag::class, nyTidslinje[8.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[9.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[10.januar]::class)
    }

    @Test
    internal fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        inntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)

        assertEquals(Periode(2.januar, 2.januar), inntektsmelding.sykdomstidslinje().periode())
    }

    @Test
    internal fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), listOf(Periode(17.januar, 18.januar)))

        assertEquals(Periode(1.januar, 18.januar), inntektsmelding.sykdomstidslinje().periode())
    }

    @Test
    internal fun `inntektsmelding med refusjon beløp != beregnetInntekt er ikke gyldig`() {
        inntektsmelding(
            emptyList(),
            emptyList(),
            refusjonBeløp = 999999.månedlig,
            beregnetInntekt = 10000.månedlig
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasErrors())
    }

    @Test
    internal fun `refusjon opphører før perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 1.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrors())
    }

    @Test
    internal fun `refusjon opphører etter perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 11.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrors())
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

    @Test
    internal fun `arbeidsgiverperiodeId i inntektsmelding gir warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            arbeidsforholdId = "1234"
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarnings())
    }

    @Test
    internal fun `begrunnelseForReduksjonEllerIkkeUtbetalt i inntektsmelding gir warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse"
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarnings())
    }

    @Test
    internal fun `begrunnelseForReduksjonEllerIkkeUtbetalt som tom String i inntektsmelding gir ikke warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = ""
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarnings())
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Inntekt = 1000.månedlig,
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate? = 1.januar,
        refusjonOpphørsdato: LocalDate? = null,
        endringerIRefusjon: List<LocalDate> = emptyList(),
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null
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
            ferieperioder = ferieperioder,
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt
        )
    }
}
