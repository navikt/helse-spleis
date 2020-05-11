package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.perioder
import no.nav.helse.testhelpers.*
import no.nav.helse.tournament.dagturnering
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class FørsteFraværsdagTest {

    @Test
    internal fun `førsteFraværsdag er null for ugyldige situasjoner`() {
        assertNull(1.nUT.førsteFraværsdag())
        assertNull(1.nEDU.førsteFraværsdag())
        assertNull(1.nF.førsteFraværsdag())
        assertNull(1.nP.førsteFraværsdag())
        assertNull(1.n_.førsteFraværsdag())
        assertNull(1.nA.førsteFraværsdag())
    }

    @Test
    internal fun `utgangspunkt for beregning er sykedag, arbeidsgiverdag eller syk helgedag`() {
        assertFørsteDagErUtgangspunktForBeregning(1.nS)
        assertFørsteDagErUtgangspunktForBeregning(1.nU)
        assertFørsteDagErUtgangspunktForBeregning(1.nH)
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse er første arbeidsgiverdag, sykedag eller syk helgedag i en sammenhengende periode`() {
        assertFørsteDagErUtgangspunktForBeregning(2.nS)
        assertFørsteDagErUtgangspunktForBeregning(2.nU)

        perioder(2.nS, 2.n_, 2.nS) { _, _, periode3 ->
            assertFørsteDagErUtgangspunktForBeregning(periode3, this)
        }
        perioder(2.nS, 2.nA, 2.nF, 2.nS) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.nS, 2.nA, 2.nP, 2.nS) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.nS, 2.nF, 2.nS) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.nS, 2.nP, 2.nS) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.nS, 2.n_, 1.nH) { _, _ , sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
        perioder(2.nS, 2.n_, 1.nU) { _, _ , sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
        perioder(2.nS, 2.nA, 2.nS, 2.nA) { _, _ , sisteSykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med ignorerte dager`() {
        perioder(2.nS, 2.nF) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.nS, 2.nP) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `ferie i framtiden`() {
        perioder(2.nS, 2.n_, 2.nF) { sykedager, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykedager etterfulgt av arbeidsdager`() {
        perioder(2.nS, 2.nA) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykedager etterfulgt av implisittdager`() {
        perioder(2.nS, 2.n_) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `søknad med arbeidgjenopptatt gir ikke feil`() {
        perioder(2.nS, 2.n_) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykeperiode starter på første fraværsdag`() {
        val sykmelding = sykmelding(Triple(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(4.februar(2020),  21.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(20.januar(2020), 20.januar(2020)),
            Periode(4.februar(2020), 18.februar(2020))
        ), førsteFraværsdag = 4.februar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    @Test
    internal fun `sykeperioden starter etter første fraværsdag`() {
        val sykmelding = sykmelding(Triple(29.januar(2020), 16.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(29.januar(2020),  16.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(13.januar(2020), 17.januar(2020)),
            Periode(20.januar(2020), 30.januar(2020))
        ), førsteFraværsdag = 20.januar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    @Test
    internal fun `arbeidsgiverperiode med enkeltdager før første fraværsdag`() {
        val sykmelding = sykmelding(Triple(12.februar(2020), 19.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(12.februar(2020),  19.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(14.januar(2020), 14.januar(2020)),
            Periode(28.januar(2020), 28.januar(2020)),
            Periode(3.februar(2020), 16.februar(2020))
        ), førsteFraværsdag = 3.februar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    private fun assertFørsteFraværsdag(
        sykmelding: Sykmelding,
        søknad: Søknad,
        inntektsmelding: Inntektsmelding
    ) {
        val tidslinje = listOf(
            sykmelding.sykdomstidslinje(),
            søknad.sykdomstidslinje(),
            inntektsmelding.sykdomstidslinje()
        ).merge(dagturnering::beste)

        val førsteFraværsdag = tidslinje.førsteFraværsdag()
        assertEquals(inntektsmelding.førsteFraværsdag, førsteFraværsdag) {
            "Forventet første fraværsdag ${inntektsmelding.førsteFraværsdag}. " +
                "Fikk $førsteFraværsdag\n" +
                "Tidslinje:\n$tidslinje"
        }
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder)
        )
    }

    private fun søknad(vararg perioder: Søknad.Søknadsperiode): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false
        )
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
    }

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00

        private fun assertDagenErUtgangspunktForBeregning(
            dagen: Dag,
            sykdomstidslinje: Sykdomstidslinje
        ) {
            val førsteFraværsdag = sykdomstidslinje.førsteFraværsdag()?.let { sykdomstidslinje[it] }
            assertEquals(dagen, førsteFraværsdag) { "Forventet $dagen, men fikk $førsteFraværsdag.\nTidslinjen:\n$sykdomstidslinje"}
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(sykdomstidslinje: Sykdomstidslinje) {
            val førsteDag = sykdomstidslinje.periode()?.start
            assertNotNull(førsteDag)
            assertEquals(førsteDag, sykdomstidslinje.førsteFraværsdag())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(
            perioden: Sykdomstidslinje,
            sykdomstidslinje: Sykdomstidslinje
        ) {
            val førsteDag = perioden.periode()?.start
            assertNotNull(førsteDag)
            assertDagenErUtgangspunktForBeregning(perioden[førsteDag!!], sykdomstidslinje)
        }
    }
}
