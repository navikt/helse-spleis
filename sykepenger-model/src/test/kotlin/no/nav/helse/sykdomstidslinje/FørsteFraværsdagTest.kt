package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.perioder
import no.nav.helse.spleis.e2e.er
import no.nav.helse.testhelpers.*
import no.nav.helse.tournament.dagturnering
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class FørsteFraværsdagTest {

    @Test
    fun `førsteFraværsdag er null for ugyldige situasjoner`() {
        assertNull(1.UT.førsteFraværsdag())
        assertNull(1.EDU.førsteFraværsdag())
        assertNull(1.F.førsteFraværsdag())
        assertNull(1.P.førsteFraværsdag())
        assertNull(1.n_.førsteFraværsdag())
        assertNull(1.A.førsteFraværsdag())
    }

    @Test
    fun `utgangspunkt for beregning er sykedag, arbeidsgiverdag eller syk helgedag`() {
        assertFørsteDagErUtgangspunktForBeregning(1.S)
        assertFørsteDagErUtgangspunktForBeregning(1.U)
        assertFørsteDagErUtgangspunktForBeregning(1.H)
    }

    @Test
    fun `utgangspunkt for beregning av ytelse er første arbeidsgiverdag, sykedag eller syk helgedag i en sammenhengende periode`() {
        assertFørsteDagErUtgangspunktForBeregning(2.S)
        assertFørsteDagErUtgangspunktForBeregning(2.U)

        perioder(2.S, 2.n_, 2.S) { _, _, periode3 ->
            assertFørsteDagErUtgangspunktForBeregning(periode3, this)
        }
        perioder(2.S, 2.A, 2.F, 2.S) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.S, 2.A, 2.P, 2.S) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.S, 2.F, 2.S) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.S, 2.P, 2.S) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.S, 2.n_, 1.H) { _, _, sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
        perioder(2.S, 2.n_, 1.U) { _, _, sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
        perioder(2.S, 2.A, 2.S, 2.A) { _, _, sisteSykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
    }

    @Test
    fun `tidslinjer som slutter med ignorerte dager`() {
        perioder(2.S, 2.F) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.S, 2.P) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    fun `ferie i framtiden`() {
        perioder(2.S, 2.n_, 2.F) { sykedager, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    fun `sykedager etterfulgt av arbeidsdager`() {
        perioder(2.S, 2.A) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    fun `sykedager etterfulgt av implisittdager`() {
        perioder(2.S, 2.n_) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    fun `søknad med arbeidgjenopptatt gir ikke feil`() {
        perioder(2.S, 2.n_) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    fun `sykeperiode starter på første fraværsdag`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(4.februar(2020), 21.februar(2020), 100))
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(20.januar(2020), 20.januar(2020)),
                Periode(4.februar(2020), 18.februar(2020))
            ), førsteFraværsdag = 4.februar(2020)
        )
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `sykeperioden starter etter første fraværsdag`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(29.januar(2020), 16.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(29.januar(2020), 16.februar(2020), 100))
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(13.januar(2020), 17.januar(2020)),
                Periode(20.januar(2020), 30.januar(2020))
            ), førsteFraværsdag = 20.januar(2020)
        )
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `arbeidsgiverperiode med enkeltdager før første fraværsdag`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(12.februar(2020), 19.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(12.februar(2020), 19.februar(2020), 100))
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(14.januar(2020), 14.januar(2020)),
                Periode(28.januar(2020), 28.januar(2020)),
                Periode(3.februar(2020), 16.februar(2020))
            ), førsteFraværsdag = 3.februar(2020)
        )
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `ved annullering regnes annullering som sykdom - saken behandles videre i infotrygd, så vi bryr oss egentlig ikke`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100))
        val inntektsmelding = inntektsmelding(
            listOf(
                1.januar til 31.januar
            ), førsteFraværsdag = 1.januar
        )
        val annullering = annullering(1.januar til 31.januar)
        val tidslinje = listOf(
            sykmelding,
            søknad,
            inntektsmelding,
            annullering
        ).map { it.sykdomstidslinje() }
            .merge(dagturnering::beste)
        val førsteFraværsdag = tidslinje.førsteFraværsdag()
        førsteFraværsdag er 1.januar
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


    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
        )
    }

    private fun annullering(periode: Periode): Annullering {
        return Annullering(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fom = periode.start,
            tom = periode.endInclusive,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            saksbehandlerIdent = "z999999",
            saksbehandler = "Test Testesen",
            saksbehandlerEpost = "test@test.test",
            opprettet = LocalDateTime.MAX
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
        refusjonBeløp: Inntekt = INNTEKT_PR_MÅNED,
        beregnetInntekt: Inntekt = INNTEKT_PR_MÅNED,
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
        private val INNTEKT_PR_MÅNED = INNTEKT.månedlig

        private fun assertDagenErUtgangspunktForBeregning(
            dagen: Dag,
            sykdomstidslinje: Sykdomstidslinje
        ) {
            val førsteFraværsdag = sykdomstidslinje.førsteFraværsdag()?.let { sykdomstidslinje[it] }
            assertEquals(
                dagen,
                førsteFraværsdag
            ) { "Forventet $dagen, men fikk $førsteFraværsdag.\nTidslinjen:\n$sykdomstidslinje" }
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
