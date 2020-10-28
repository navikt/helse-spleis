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
    fun `skjæringstidspunkt skal ikke bli tidligere enn første dag i siste arbeidsgiverperiode`() {
        inntektsmelding(
            listOf(
                Periode(1.januar, 2.januar),
                Periode(10.januar, 12.januar),
                Periode(15.januar, 24.januar)
            ), førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(24.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(15.januar, nyTidslinje.skjæringstidspunkt())
    }

    @Test
    fun `ferie etter siste arbeidsgiverperiode påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(10.januar, 12.januar)),
            førsteFraværsdag = 1.januar,
            ferieperioder = listOf(Periode(16.januar, 16.januar))
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(16.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(10.januar, nyTidslinje.skjæringstidspunkt())
    }

    @Test
    fun `ferie sammenhengende etter siste arbeidsgiverperiode påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(10.januar, 12.januar)),
            førsteFraværsdag = 1.januar,
            ferieperioder = listOf(Periode(15.januar, 15.januar))
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(15.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(10.januar, nyTidslinje.skjæringstidspunkt())
    }

    @Test
    fun `ferie sammenhengende før siste arbeidsgiverperiode påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar,
            ferieperioder = listOf(Periode(10.januar, 12.januar))
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(15.januar, nyTidslinje.skjæringstidspunkt())
    }

    @Test
    fun `ferie og helg sammenhengende med to arbeidsgiverperioder slår sammen periodene`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 5.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar,
            ferieperioder = listOf(Periode(8.januar, 12.januar))
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(1.januar, inntektsmelding.førsteFraværsdag)
    }

    @Test
    fun `to ferieperioder med gap, som er sammenhengende med hver sin arbeidsgiverperiode, påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 5.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar,
            ferieperioder = listOf(Periode(8.januar, 9.januar), Periode(11.januar, 12.januar))
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(15.januar, nyTidslinje.skjæringstidspunkt())
    }

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode med førsteFraværsdag satt`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 1.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarningsOrWorse())
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(1.januar, nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), emptyList())
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    fun `arbeidsgiverperiode med gap`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)), førsteFraværsdag = 4.januar)

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    fun `ferieperiode med gap`() {
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
    fun `første fraværsdag etter arbeidsgiverperiode blir arbeidsgiverdag`() {
        inntektsmelding(listOf(Periode(1.januar, 1.januar)), førsteFraværsdag = 3.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[2.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
    }

    @Test
    fun `arbeidsgiverperiode og ferie med gap og første fraværsdag etterpå`() {
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
    fun `ferie i arbeidsgiverperioden uten overlapp`() {
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
    fun `overlapp i ferieperioder`() {
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
    fun `førsteFraværsdag mellom ferie og arbeidsgiverperiode`() {
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
    fun `førsteFraværsdag mellom arbeidsgiverperiode og ferie blir arbeidsgiverdag`() {
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
    fun `ferieperiode før arbeidsgiverperiode`() {
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
    fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList(), ferieperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)))
        assertEquals(1.januar, inntektsmelding.sykdomstidslinje().periode()?.start)
    }

    @Test
    fun `arbeidgiverperioden kan ha overlappende perioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar, 2.januar), Periode(4.januar, 5.januar), Periode(3.januar, 4.januar)
            ),
            ferieperioder = emptyList()
        )
        inntektsmelding.valider(Periode(1.januar, 31.januar))
        assertFalse(inntektsmelding.hasErrorsOrWorse())
    }

    @Test
    fun `arbeidgiverdager vinner over feriedager`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)),
            ferieperioder = listOf(Periode(3.januar, 4.januar))
        )
        inntektsmelding.valider(Periode(1.januar, 31.januar))
        assertFalse(inntektsmelding.hasErrorsOrWorse())

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[2.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
    }

    @Test
    fun `helg i opphold i arbeidsgiverperioden skal være helgedager`() {
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
    fun `bruker første fraværsdag som TOM hvis både ferieperioder og arbeidsgiverperioder i inntektsmeldingen er tomme`() {
        inntektsmelding(emptyList(), emptyList(), førsteFraværsdag = 2.januar)

        assertEquals(Periode(2.januar, 2.januar), inntektsmelding.sykdomstidslinje().periode())
    }

    @Test
    fun `ferieperiode og arbeidsgiverperiode blir slått sammen`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), listOf(Periode(17.januar, 18.januar)))

        assertEquals(Periode(1.januar, 18.januar), inntektsmelding.sykdomstidslinje().periode())
    }

    @Test
    fun `inntektsmelding med refusjon beløp != beregnetInntekt er ikke gyldig`() {
        inntektsmelding(
            emptyList(),
            emptyList(),
            refusjonBeløp = 999999.månedlig,
            beregnetInntekt = 10000.månedlig
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører før perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 1.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører etter perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 11.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører i perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 10.januar,
            endringerIRefusjon = listOf(16.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `endring i refusjon i perioden`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            emptyList(),
            refusjonOpphørsdato = 16.januar,
            endringerIRefusjon = listOf(10.januar)
        )
        assertTrue(inntektsmelding.valider(Periode(2.januar, 10.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `arbeidsgiverperiodeId i inntektsmelding gir warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            arbeidsforholdId = "1234"
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarningsOrWorse())
    }

    @Test
    fun `begrunnelseForReduksjonEllerIkkeUtbetalt i inntektsmelding gir warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse"
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarningsOrWorse())
    }

    @Test
    fun `begrunnelseForReduksjonEllerIkkeUtbetalt som tom String i inntektsmelding gir ikke warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = ""
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasWarningsOrWorse())
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på søndag gir friskhelgedag`() {
        inntektsmelding(
            listOf(Periode(5.januar, 20.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(22.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
        assertEquals(ArbeidsgiverHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[21.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[22.januar]::class)
    }


    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på lørdag gir friskhelgedag`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 21.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(21.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(ArbeidsgiverHelgedag::class, nyTidslinje[21.januar]::class)
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på lørdag og søndag gir friskhelgedager`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(22.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[21.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[22.januar]::class)
    }


    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag, arbeidsgiverperiode slutter på torsdag`() {
        inntektsmelding(
            listOf(Periode(3.januar, 18.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(22.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[18.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[19.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[20.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[21.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[22.januar]::class)
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag, første fraværsdag er tirsdag`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 23.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(23.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[20.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[21.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[22.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[23.januar]::class)
    }

    @Test
    fun `arbeidsgiverperiode og ferie slutter på fredag med gap i helg og første fraværsdag mandag`() {
        inntektsmelding(
            listOf(Periode(1.januar, 3.januar)),
            listOf(Periode(5.januar, 5.januar)),
            førsteFraværsdag = 8.januar
        )

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(8.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[4.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[5.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[6.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[7.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[8.januar]::class)
    }

    @Test
    fun `arbeidsgiverperiode slutter med ferie helg, første fraværsdag mandag`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            listOf(Periode(20.januar, 21.januar)),
            førsteFraværsdag = 22.januar
        )

        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(4.januar, nyTidslinje.skjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[20.januar]::class)
        assertEquals(Feriedag::class, nyTidslinje[21.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[22.januar]::class)
    }

    @Test
    fun `er ikke relevant når kun ferie etter gap overlapper`() {
        inntektsmelding(
            listOf(Periode(1.januar, 8.januar), Periode(10.januar, 18.januar)),
            listOf(Periode(19.januar, 23.januar)),
            førsteFraværsdag = 10.januar
        )

        assertFalse(inntektsmelding.erRelevant(Periode(19.januar, 25.januar)))
        assertFalse(inntektsmelding.erRelevant(Periode(20.januar, 25.januar)))
        assertFalse(inntektsmelding.erRelevant(Periode(24.januar, 25.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(10.januar, 25.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(18.januar, 25.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(1.januar, 25.januar)))
    }

    @Test
    fun `er relevant når første fraværsdag er etter gap etter arbeidsgiverperioden`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 22.januar)
        assertTrue(inntektsmelding.erRelevant(Periode(22.januar, 25.januar)))
        assertFalse(inntektsmelding.erRelevant(Periode(23.januar, 25.januar)))
    }

    @Test
    fun `er ikke relevant når første fraværsdag er etter vedtaksperioden`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 22.januar)
        assertFalse(inntektsmelding.erRelevant(Periode(1.januar, 18.januar)))
    }

    @Test
    fun `er relevant når første fraværsdag er dagen etter arbeidsgiverperioden`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 17.januar)
        assertTrue(inntektsmelding.erRelevant(Periode(1.januar, 18.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(17.januar, 18.januar)))
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
