package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Companion.WARN_UENIGHET_ARBEIDSGIVERPERIODE
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon
import no.nav.helse.hentErrors
import no.nav.helse.hentInfo
import no.nav.helse.hentWarnings
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class InntektsmeldingTest {

    private val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
        organisasjonsnummer = "88888888",
        fødselsnummer = "12029240045".somFødselsnummer(),
        aktørId = "100010101010",
        fødselsdato = 12.februar(1992)
    )
    private lateinit var inntektsmelding: Inntektsmelding

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode og førsteFraværsdag er null`() {
        assertThrows<Aktivitetslogg.AktivitetException> { inntektsmelding(emptyList(), førsteFraværsdag = null) }
    }

    @Test
    fun `strekker periode tilbake til første fraværsdag`() {
        val periode = 5.februar til 10.februar
        inntektsmelding(listOf(1.januar til 8.januar, 10.januar til 17.januar), førsteFraværsdag = 1.februar)
        assertEquals(1.februar til 10.februar, inntektsmelding.oppdaterFom(periode))
        inntektsmelding.trimLeft(8.januar)
        assertEquals(1.februar til 10.februar, inntektsmelding.oppdaterFom(periode))
        inntektsmelding.trimLeft(17.januar)
        assertEquals(1.februar til 10.februar, inntektsmelding.oppdaterFom(periode))
        inntektsmelding.trimLeft(3.februar)
        assertEquals(5.februar til 10.februar, inntektsmelding.oppdaterFom(periode))
    }

    @Test
    fun `periode dersom første fraværsdag er kant i kant med arbeidsgiverperioden`() {
        inntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 17.januar)
        assertEquals(1.januar til 17.januar, inntektsmelding.periode())
    }

    @Test
    fun `periode dersom første fraværsdag er etter arbeidsgiverperioden`() {
        inntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 18.januar)
        assertEquals(18.januar.somPeriode(), inntektsmelding.periode())
    }

    @Test
    fun `periode dersom første fraværsdag er i arbeidsgiverperioden`() {
        inntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 16.januar)
        assertEquals(1.januar til 16.januar, inntektsmelding.periode())
    }

    @Test
    fun `periode dersom første fraværsdag er null`() {
        inntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = null)
        assertEquals(1.januar til 16.januar, inntektsmelding.periode())
    }

    @Test
    fun `trimme inntektsmelding forbi tom`() {
        inntektsmelding(listOf(1.januar til 16.januar))
        inntektsmelding.trimLeft(17.januar)
        assertEquals(LocalDate.MIN til LocalDate.MIN, inntektsmelding.periode())
    }

    @Test
    fun `inntektsmelding hvor førsteFraværsdag er null`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar)), førsteFraværsdag = null)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(2.januar, nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `første fraværsdag treffer midt i en sammenhengende periode`() {
        inntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 5.februar)
        val førstePeriode = 3.januar til 31.januar
        val andrePeriode = 1.februar til 16.februar
        val tredjePeriode = 17.februar til 28.februar
        val perioder = listOf(førstePeriode, andrePeriode, tredjePeriode)
        val tidslinjeFør = inntektsmelding.sykdomstidslinje()
        assertFalse(inntektsmelding.erRelevant(førstePeriode, perioder))
        assertEquals(tidslinjeFør, inntektsmelding.sykdomstidslinje())
        assertFalse(inntektsmelding.hasWarningsOrWorse())
        assertTrue(inntektsmelding.erRelevant(andrePeriode, perioder))
        assertTrue(inntektsmelding.hasWarningsOrWorse())
        assertTrue(inntektsmelding.erRelevant(tredjePeriode, perioder))
        assertEquals(tidslinjeFør, inntektsmelding.sykdomstidslinje())
    }

    @Test
    fun `første fraværsdag treffer midt i en sammenhengende periode og arbeidsgiverperioden er forskjøvet`() {
        inntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 5.februar)
        val førstePeriode = 1.januar til 31.januar
        val andrePeriode = 1.februar til 16.februar
        val tredjePeriode = 17.februar til 28.februar
        val perioder = listOf(førstePeriode, andrePeriode, tredjePeriode)
        val tidslinjeFør = inntektsmelding.sykdomstidslinje()
        assertTrue(tidslinjeFør[1.januar] is UkjentDag)
        assertTrue(tidslinjeFør[2.januar] is UkjentDag)
        assertFalse(inntektsmelding.erRelevant(førstePeriode, perioder))
        val tidslinjeEtter = inntektsmelding.sykdomstidslinje()
        assertTrue(tidslinjeEtter[1.januar] is Arbeidsdag)
        assertTrue(tidslinjeEtter[2.januar] is Arbeidsdag)
        assertFalse(inntektsmelding.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsgiverperioden er forskjøvet`() {
        inntektsmelding(listOf(3.januar til 18.januar))
        val førstePeriode = 1.januar til 31.januar
        val andrePeriode = 1.februar til 16.februar
        val perioder = listOf(førstePeriode, andrePeriode)
        val tidslinjeFør = inntektsmelding.sykdomstidslinje()
        assertTrue(tidslinjeFør[1.januar] is UkjentDag)
        assertTrue(tidslinjeFør[2.januar] is UkjentDag)
        assertTrue(inntektsmelding.erRelevant(førstePeriode, perioder))
        assertTrue(inntektsmelding.erRelevant(andrePeriode, perioder))
        val tidslinjeEtter = inntektsmelding.sykdomstidslinje()
        assertTrue(tidslinjeEtter[1.januar] is Arbeidsdag)
        assertTrue(tidslinjeEtter[2.januar] is Arbeidsdag)
        assertFalse(inntektsmelding.hasWarningsOrWorse())
    }

    @Test
    fun `helt ny arbeidsgiverperiode i en sammenhengende periode`() {
        inntektsmelding(listOf(1.februar til 16.februar))
        val førstePeriode = 1.januar til 31.januar
        val andrePeriode = 1.februar til 28.februar
        val perioder = listOf(førstePeriode, andrePeriode)
        val tidslinjeFør = inntektsmelding.sykdomstidslinje()
        assertFalse(inntektsmelding.erRelevant(førstePeriode, perioder))
        assertTrue(inntektsmelding.erRelevant(andrePeriode, perioder))
        val tidslinjeEtter = inntektsmelding.sykdomstidslinje()
        assertNotEquals(tidslinjeFør, tidslinjeEtter)
        assertTrue((1.januar til 31.januar).all { tidslinjeEtter[it] is Arbeidsdag || tidslinjeEtter[it] is FriskHelgedag })
        assertFalse(inntektsmelding.hasWarningsOrWorse())
    }

    @Test
    fun `padder med arbeidsdager i forkant av arbeidsgiverperiode`() {
        inntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar)
        inntektsmelding.padLeft(1.januar)
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, tidslinje.førsteDag())
        assertTrue(tidslinje[1.januar] is Arbeidsdag)
        assertTrue(tidslinje[2.januar] is Arbeidsdag)
        assertTrue(tidslinje[3.januar] is Arbeidsgiverdag)
    }

    @Test
    fun `padder ikke med arbeidsdager i forkant av første fraværsdag uten arbeidsgiverperiode`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 3.januar)
        inntektsmelding.padLeft(1.januar)
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertNull(tidslinje.periode())
        assertTrue(tidslinje[3.januar] is UkjentDag)
    }

    @Test
    fun `padder ikke med arbeidsdager mellom siste arbeidsgiverperiode og første fraværsdag`() {
        inntektsmelding(listOf(
            1.januar til 7.januar,
            10.januar til 18.januar
        ), førsteFraværsdag = 25.januar)
        inntektsmelding.padLeft(1.januar)
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, tidslinje.førsteDag())
        assertEquals(18.januar, tidslinje.sisteDag())
        assertTrue(tidslinje[1.januar] is Arbeidsgiverdag)
        assertFalse(tidslinje[19.januar] is Arbeidsdag)
        assertFalse(tidslinje[24.januar] is Arbeidsdag)
        assertTrue(tidslinje[25.januar] is UkjentDag)
    }

    @Test
    fun `padder ikke med arbeidsdager mellom dato og første fraværsdag`() {
        inntektsmelding(listOf(
            1.januar til 7.januar,
            10.januar til 18.januar
        ), førsteFraværsdag = 25.januar)
        inntektsmelding.padLeft(20.januar)
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, tidslinje.førsteDag())
        assertEquals(18.januar, tidslinje.sisteDag())
        assertTrue(tidslinje[1.januar] is Arbeidsgiverdag)
        assertFalse(tidslinje[19.januar] is Arbeidsdag)
        assertFalse(tidslinje[20.januar] is FriskHelgedag)
        assertFalse(tidslinje[24.januar] is Arbeidsdag)
        assertTrue(tidslinje[25.januar] is UkjentDag)
    }

    @Test
    fun `padder med arbeidsdager før arbeidsgiverperiode, men ikke mellom dato og første fraværsdag`() {
        inntektsmelding(listOf(
            1.januar til 7.januar,
            10.januar til 18.januar
        ), førsteFraværsdag = 25.januar)
        inntektsmelding.padLeft(31.desember(2017))
        val tidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(31.desember(2017), tidslinje.førsteDag())
        assertEquals(18.januar, tidslinje.sisteDag())
        assertTrue(tidslinje[31.desember(2017)] is FriskHelgedag)
        assertFalse(tidslinje[19.januar] is Arbeidsdag)
        assertFalse(tidslinje[20.januar] is FriskHelgedag)
        assertFalse(tidslinje[24.januar] is Arbeidsdag)
        assertTrue(tidslinje[25.januar] is UkjentDag)
    }

    @Test
    fun `uenighet om arbeidsgiverperiode`() {
        inntektsmelding(listOf(1.januar til 10.januar, 11.januar til 16.januar))
        inntektsmelding.valider(1.februar til 28.februar, 1.januar, Arbeidsgiverperiode(listOf(1.januar til 16.januar)), SubsumsjonObserver.NullObserver)
        assertFalse(WARN_UENIGHET_ARBEIDSGIVERPERIODE in inntektsmelding.hentWarnings())

        inntektsmelding(listOf(1.januar til 10.januar, 12.januar til 17.januar))
        inntektsmelding.valider(1.februar til 28.februar, 1.januar, Arbeidsgiverperiode(listOf(1.januar til 16.januar)), SubsumsjonObserver.NullObserver)
        assertTrue(WARN_UENIGHET_ARBEIDSGIVERPERIODE in inntektsmelding.hentWarnings())

        inntektsmelding(listOf(12.januar til 27.januar))
        inntektsmelding.valider(1.februar til 28.februar, 11.januar, Arbeidsgiverperiode(listOf(11.januar til 27.januar)), SubsumsjonObserver.NullObserver)
        assertFalse(WARN_UENIGHET_ARBEIDSGIVERPERIODE in inntektsmelding.hentWarnings())

        inntektsmelding(listOf(12.januar til 27.januar))
        inntektsmelding.valider(1.februar til 28.februar, 13.januar, Arbeidsgiverperiode(listOf(13.januar til 28.januar)), SubsumsjonObserver.NullObserver)
        assertFalse(WARN_UENIGHET_ARBEIDSGIVERPERIODE in inntektsmelding.hentWarnings())

        inntektsmelding(emptyList())
        inntektsmelding.valider(1.februar til 28.februar, 1.januar, Arbeidsgiverperiode(listOf(1.januar til 16.januar)), SubsumsjonObserver.NullObserver)
        assertFalse(WARN_UENIGHET_ARBEIDSGIVERPERIODE in inntektsmelding.hentWarnings())
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
        assertEquals(15.januar, nyTidslinje.sisteSkjæringstidspunkt())
    }

    @Test
    fun `ferie sammenhengende før siste arbeidsgiverperiode påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 2.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(15.januar, nyTidslinje.sisteSkjæringstidspunkt())
    }

    @Test
    fun `ferie og helg sammenhengende med to arbeidsgiverperioder slår sammen periodene`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 5.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `to ferieperioder med gap, som er sammenhengende med hver sin arbeidsgiverperiode, påvirker ikke skjæringstidspunkt`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 5.januar), Periode(15.januar, 17.januar)),
            førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(1.januar, nyTidslinje.periode()?.start)
        assertEquals(17.januar, nyTidslinje.periode()?.endInclusive)
        assertEquals(15.januar, nyTidslinje.sisteSkjæringstidspunkt())
    }

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode med førsteFraværsdag satt`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 1.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        val aktivitetslogg = inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist())
        assertTrue(aktivitetslogg.hentInfo().contains("Inntektsmeldingen mangler arbeidsgiverperiode. Vurder om vilkårene for sykepenger er oppfylt, og om det skal være arbeidsgiverperiode"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
        assertEquals(1.januar til 1.januar, inntektsmelding.periode())
        assertNull(nyTidslinje.periode()?.start)
        assertNull(nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `inntektsmelding uten arbeidsgiverperiode med førsteFraværsdag & begrunnelseForReduksjonEllerIkkeUtbetalt satt`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 1.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse")
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        val aktivitetslogg = inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist())
        assertTrue(aktivitetslogg.hentInfo().contains("Inntektsmeldingen mangler arbeidsgiverperiode. Vurder om vilkårene for sykepenger er oppfylt, og om det skal være arbeidsgiverperiode"))
        assertTrue(aktivitetslogg.hentErrors().contains("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: begrunnelse"))
        assertNull(nyTidslinje.periode()?.start)
        assertNull(nyTidslinje.periode()?.endInclusive)
    }

    @Test
    fun `sykdom med en antatt arbeidsdag`() {
        inntektsmelding(listOf(Periode(1.januar, 2.januar), Periode(4.januar, 5.januar)))
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
    fun `første fraværsdag etter arbeidsgiverperiode blir ikke arbeidsgiverdag`() {
        inntektsmelding(listOf(Periode(1.januar, 1.januar)), førsteFraværsdag = 3.januar)
        val nyTidslinje = inntektsmelding.sykdomstidslinje()
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[1.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[2.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[3.januar]::class)
    }

    @Test
    fun `arbeidsgiverperioden i inntektsmelding kan være tom`() {
        inntektsmelding(emptyList())
        assertNull(inntektsmelding.sykdomstidslinje().periode()?.start)
    }

    @Test
    fun `arbeidgiverperioden kan ha overlappende perioder`() {
        inntektsmelding(
            arbeidsgiverperioder = listOf(
                Periode(1.januar, 2.januar), Periode(4.januar, 5.januar), Periode(3.januar, 4.januar)
            )
        )
        inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist())
        assertFalse(inntektsmelding.hasErrorsOrWorse())
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
        inntektsmelding(emptyList(), førsteFraværsdag = 2.januar)

        assertEquals(Periode(2.januar, 2.januar), inntektsmelding.periode())
        assertNull(inntektsmelding.sykdomstidslinje().periode())
    }

    @Test
    fun `begrunnelseForReduksjonEllerIkkeUtbetalt i inntektsmelding gir warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse"
        )
        assertTrue(inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).hentErrors().contains("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: begrunnelse"))
    }

    @Test
    fun `begrunnelseForReduksjonEllerIkkeUtbetalt som tom String i inntektsmelding gir ikke warning`() {
        inntektsmelding(
            listOf(Periode(1.januar, 10.januar)),
            begrunnelseForReduksjonEllerIkkeUtbetalt = ""
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på søndag gir friskhelgedag`() {
        inntektsmelding(
            listOf(Periode(5.januar, 20.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(5.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(22.januar.somPeriode(), inntektsmelding.periode())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
        assertEquals(ArbeidsgiverHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[21.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[22.januar]::class)
    }

    @Test
    fun `helg mellom arbeidsgiverperiode`() {
        inntektsmelding(
            listOf(
                1.januar til 3.januar,
                4.januar til 5.januar,
                // 6. og 7. januar er helg
                8.januar til 12.januar,
                13.januar til 18.januar
            ), førsteFraværsdag = 1.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(8.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[5.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[6.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[7.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[8.januar]::class)
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på lørdag gir friskhelgedag`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 21.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(4.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(21.januar.somPeriode(), inntektsmelding.periode())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[21.januar]::class)
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag i helg på lørdag og søndag gir friskhelgedager`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(4.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(22.januar.somPeriode(), inntektsmelding.periode())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[20.januar]::class)
        assertEquals(FriskHelgedag::class, nyTidslinje[21.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[22.januar]::class)
    }


    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag, arbeidsgiverperiode slutter på torsdag`() {
        inntektsmelding(
            listOf(Periode(3.januar, 18.januar)),
            førsteFraværsdag = 22.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(22.januar.somPeriode(), inntektsmelding.periode())
        assertEquals(3.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[3.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[18.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[19.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[20.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[21.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[22.januar]::class)
    }

    @Test
    fun `opphold mellom arbeidsgiverperiode og første fraværsdag, første fraværsdag er tirsdag`() {
        inntektsmelding(
            listOf(Periode(4.januar, 19.januar)),
            førsteFraværsdag = 23.januar
        )
        val nyTidslinje = inntektsmelding.sykdomstidslinje()

        assertEquals(23.januar til 23.januar, inntektsmelding.periode())
        assertEquals(4.januar, nyTidslinje.sisteSkjæringstidspunkt())
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[4.januar]::class)
        assertEquals(Arbeidsgiverdag::class, nyTidslinje[19.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[20.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[21.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[22.januar]::class)
        assertEquals(UkjentDag::class, nyTidslinje[23.januar]::class)
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

    @Test
    fun `er relevant når det ikke finnes arbeidsgiverperioder og første fraværsdag er i vedtaksperioden`() {
        inntektsmelding(emptyList(), førsteFraværsdag = 4.januar)
        assertTrue(inntektsmelding.erRelevant(Periode(1.januar, 18.januar)))
    }

    @Test
    fun `er relevant med arbeidsgiverperioden når første fraværdag er inni arbeidsgiverperioden`() {
        inntektsmelding(
            listOf(
                3.januar til 4.januar,
                8.januar til 9.januar,
                15.januar til 26.januar
            ), førsteFraværsdag = 15.januar
        )

        assertTrue(inntektsmelding.erRelevant(Periode(3.januar, 4.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(8.januar, 9.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(15.januar, 16.januar)))
    }

    @Test
    fun `overlapper med arbeidsgiverperioden når første fraværsdag er kant-i-kant`() {
        inntektsmelding(
            listOf(
                1.januar til 15.januar
            ), førsteFraværsdag = 16.januar
        )
        assertTrue(inntektsmelding.erRelevant(Periode(3.januar, 4.januar)))
        assertTrue(inntektsmelding.erRelevant(Periode(16.januar, 20.januar)))
    }

    @Test
    fun `førsteFraværsdag kan være null ved lagring av inntekt`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = null)
        assertDoesNotThrow { inntektsmelding.addInntekt(Inntektshistorikk(), 1.januar, MaskinellJurist()) }
    }

    @Test
    fun `lagrer inntekt for periodens skjæringstidspunkt dersom det er annerledes enn inntektmeldingens skjæringstidspunkt`() {
        inntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjonBeløp = 2000.månedlig, beregnetInntekt = 2000.månedlig, førsteFraværsdag = 3.februar)
        val inntektshistorikk = Inntektshistorikk()
        inntektsmelding.addInntekt(inntektshistorikk, 1.februar, MaskinellJurist())
        assertEquals(2000.månedlig, inntektshistorikk.omregnetÅrsinntekt(1.februar, 1.februar)?.omregnetÅrsinntekt())
        assertNull(inntektshistorikk.omregnetÅrsinntekt(3.februar, 3.februar))
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        refusjonBeløp: Inntekt = 1000.månedlig,
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjonOpphørsdato: LocalDate? = null,
        endringerIRefusjon: List<EndringIRefusjon> = emptyList(),
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null
    ) {
        inntektsmelding = hendelsefabrikk.lagInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(refusjonBeløp, refusjonOpphørsdato, endringerIRefusjon),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt
        )
    }
}
