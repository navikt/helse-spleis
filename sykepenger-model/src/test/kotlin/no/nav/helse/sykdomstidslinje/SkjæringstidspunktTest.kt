package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.perioder
import no.nav.helse.testhelpers.*
import no.nav.helse.tournament.dagturnering
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class SkjæringstidspunktTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `skjæringstidspunkt er null for ugyldige situasjoner`() {
        assertNull(1.UT.skjæringstidspunkt())
        assertNull(1.EDU.skjæringstidspunkt())
        assertNull(1.F.skjæringstidspunkt())
        assertNull(1.P.skjæringstidspunkt())
        assertNull(1.n_.skjæringstidspunkt())
        assertNull(1.A.skjæringstidspunkt())
    }

    @Test
    fun `skjæringstidspunkt er sykedag, arbeidsgiverdag eller syk helgedag`() {
        assertFørsteDagErSkjæringstidspunkt(1.S)
        assertFørsteDagErSkjæringstidspunkt(1.U)
        assertFørsteDagErSkjæringstidspunkt(1.H)
    }

    @Test
    fun `syk-ferie-helg-syk regnes ikke som gap`() {
        assertFørsteDagErSkjæringstidspunkt(15.S + 4.F + 2.n_ + 1.S)
    }

    @Test
    fun `syk-helg-ferie-syk regnes ikke som gap`() {
        assertFørsteDagErSkjæringstidspunkt(12.S + 2.n_ + 2.F + 1.S)
    }

    @Test
    fun `skjæringstidspunkt er første arbeidsgiverdag, sykedag eller syk helgedag i en sammenhengende periode`() {
        assertFørsteDagErSkjæringstidspunkt(2.S)
        assertFørsteDagErSkjæringstidspunkt(2.U)

        resetSeed(4.januar)
        perioder(2.S, 2.n_, 2.S) { periode1, _, _ ->
            assertFørsteDagErSkjæringstidspunkt(periode1, this)
        }
        perioder(2.S, 2.A, 2.F, 2.S) { _, _, _, periode4 ->
            assertFørsteDagErSkjæringstidspunkt(periode4, this)
        }
        perioder(2.S, 2.A, 2.P, 2.S) { _, _, _, periode4 ->
            assertFørsteDagErSkjæringstidspunkt(periode4, this)
        }
        perioder(2.S, 2.F, 2.S) { periode1, _, _ ->
            assertFørsteDagErSkjæringstidspunkt(periode1, this)
        }
        perioder(2.S, 2.P, 2.S) { periode1, _, _ ->
            assertFørsteDagErSkjæringstidspunkt(periode1, this)
        }
        perioder(2.S, 2.n_, 1.H) { _, _, sisteSykedager ->
            assertFørsteDagErSkjæringstidspunkt(sisteSykedager, this)
        }
        perioder(2.S, 2.n_, 1.U) { _, _, sisteSykedager ->
            assertFørsteDagErSkjæringstidspunkt(sisteSykedager, this)
        }
        perioder(2.S, 2.A, 2.S, 2.A) { _, _, sisteSykedager, _ ->
            assertFørsteDagErSkjæringstidspunkt(sisteSykedager, this)
        }
    }

    @Test
    fun `tidslinjer som slutter med ignorerte dager`() {
        perioder(2.S, 2.F) { periode1, _ ->
            assertFørsteDagErSkjæringstidspunkt(periode1, this)
        }
        perioder(2.S, 2.P) { periode1, _ ->
            assertFørsteDagErSkjæringstidspunkt(periode1, this)
        }
    }

    @Test
    fun `ferie i framtiden`() {
        perioder(2.S, 2.n_, 2.F) { sykedager, _, _ ->
            assertFørsteDagErSkjæringstidspunkt(sykedager, this)
        }
    }

    @Test
    fun `sykedager etterfulgt av arbeidsdager`() {
        perioder(2.S, 2.A) { sykedager, _ ->
            assertFørsteDagErSkjæringstidspunkt(sykedager, this)
        }
    }

    @Test
    fun `sykedager etterfulgt av implisittdager`() {
        perioder(2.S, 2.n_) { sykedager, _ ->
            assertFørsteDagErSkjæringstidspunkt(sykedager, this)
        }
    }

    @Test
    fun `søknad med arbeidgjenopptatt gir ikke feil`() {
        perioder(2.S, 2.n_) { sykedager, _ ->
            assertFørsteDagErSkjæringstidspunkt(sykedager, this)
        }
    }

    @Test
    fun `helg mellom to sykmeldinger`() {
        val sykmelding1 = sykmelding(Sykmeldingsperiode(1.januar, 26.januar, 100)) // slutter fredag
        val sykmelding2 = sykmelding(Sykmeldingsperiode(29.januar, 9.februar, 100)) // starter mandag
        assertSkjæringstidspunkt(1.januar, sykmelding1, sykmelding2)
    }

    @Test
    fun `fredag og helg mellom to sykmeldinger`() {
        val sykmelding1 = sykmelding(Sykmeldingsperiode(1.januar, 25.januar, 100)) // slutter torsdag
        val sykmelding2 = sykmelding(Sykmeldingsperiode(29.januar, 9.februar, 100)) // starter mandag
        assertSkjæringstidspunkt(29.januar, sykmelding1, sykmelding2)
    }

    @Test
    fun `helg og mandag mellom to sykmeldinger`() {
        val sykmelding1 = sykmelding(Sykmeldingsperiode(1.januar, 26.januar, 100)) // slutter fredag
        val sykmelding2 = sykmelding(Sykmeldingsperiode(30.januar, 9.februar, 100)) // starter tirsdag
        assertSkjæringstidspunkt(30.januar, sykmelding1, sykmelding2)
    }

    @Test
    fun `sykeperiode starter på skjæringstidspunkt`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(4.februar(2020), 21.februar(2020), 100))
        val skjæringstidspunkt = 4.februar(2020)
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(20.januar(2020), 20.januar(2020)),
                Periode(4.februar(2020), 18.februar(2020))
            ), førsteFraværsdag = skjæringstidspunkt
        )
        assertSkjæringstidspunkt(skjæringstidspunkt, sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `skjæringstidspunkt er riktig selv om første fraværsdag er satt for tidlig`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(4.februar(2020), 21.februar(2020), 100))
        val skjæringstidspunkt = 4.februar(2020)
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(20.januar(2020), 20.januar(2020)),
                Periode(4.februar(2020), 18.februar(2020))
            ), førsteFraværsdag = 20.januar(2020)
        )
        assertSkjæringstidspunkt(skjæringstidspunkt, sykmelding, søknad, inntektsmelding)
    }


    @Test
    fun `skjæringstidspunkt er riktig selv om første fraværsdag er satt for sent`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(4.februar(2020), 21.februar(2020), 100))
        val skjæringstidspunkt = 4.februar(2020)
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(20.januar(2020), 20.januar(2020)),
                Periode(4.februar(2020), 18.februar(2020))
            ), førsteFraværsdag = 18.februar(2020)
        )
        assertSkjæringstidspunkt(skjæringstidspunkt, sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `sykeperioden starter etter skjæringstidspunkt`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(29.januar(2020), 16.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(29.januar(2020), 16.februar(2020), 100))
        val skjæringstidspunkt = 20.januar(2020)
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(13.januar(2020), 17.januar(2020)),
                Periode(20.januar(2020), 30.januar(2020))
            ), førsteFraværsdag = skjæringstidspunkt
        )
        assertSkjæringstidspunkt(skjæringstidspunkt, sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `arbeidsgiverperiode med enkeltdager før skjæringstidspunkt`() {
        val sykmelding = sykmelding(Sykmeldingsperiode(12.februar(2020), 19.februar(2020), 100))
        val søknad = søknad(Søknad.Søknadsperiode.Sykdom(12.februar(2020), 19.februar(2020), 100))
        val skjæringstidspunkt = 3.februar(2020)
        val inntektsmelding = inntektsmelding(
            listOf(
                Periode(14.januar(2020), 14.januar(2020)),
                Periode(28.januar(2020), 28.januar(2020)),
                Periode(3.februar(2020), 16.februar(2020))
            ), førsteFraværsdag = skjæringstidspunkt
        )
        assertSkjæringstidspunkt(skjæringstidspunkt, sykmelding, søknad, inntektsmelding)
    }

    @Test
    fun `tilstøtende arbeidsgivertidslinjer`() {
        val arbeidsgiver1tidslinje = 14.S
        val arbeidsgiver2tidslinje = 14.S
        assertTrue(arbeidsgiver1tidslinje.sisteDag().erRettFør(arbeidsgiver2tidslinje.førsteDag()))
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med helgegap`() {
        val arbeidsgiver1tidslinje = 12.S + 2.n_
        val arbeidsgiver2tidslinje = 14.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med arbeidsdager i helg`() {
        val arbeidsgiver1tidslinje = 12.S + 1.A
        val arbeidsgiver2tidslinje = 1.A + 14.S
        assertSkjæringstidspunkt(15.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med arbeidsdager og sykedager i helg`() {
        val arbeidsgiver1tidslinje = 14.S
        resetSeed(10.januar)
        val arbeidsgiver2tidslinje = 2.S + 2.A + 7.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med helgegap og arbeidsdag på fredag`() {
        val arbeidsgiver1tidslinje = 11.S + 1.A + 2.n_
        val arbeidsgiver2tidslinje = 14.S
        assertSkjæringstidspunkt(15.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med helgegap og arbeidsdag på mandag`() {
        val arbeidsgiver1tidslinje = 12.S + 2.n_
        val arbeidsgiver2tidslinje = 1.A + 13.S
        assertSkjæringstidspunkt(16.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer overlappende arbeidsdager og sykedager`() {
        val arbeidsgiver1tidslinje = 6.S + 6.A
        resetSeed()
        val arbeidsgiver2tidslinje = 14.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med gap blir sammenhengende`() {
        val arbeidsgiver1tidslinje = 7.S + 7.A
        resetSeed()
        val arbeidsgiver2tidslinje = 14.A + 14.S
        resetSeed()
        val arbeidsgiver3tidslinje = 7.A + 7.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje, arbeidsgiver3tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med ferie som gap blir sammenhengende`() {
        val arbeidsgiver1tidslinje = 7.S + 1.F
        val arbeidsgiver2tidslinje = 1.F + 14.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med ferie som gap, med arbeidsdag på samme dager, forblir usammenhengende`() {
        val arbeidsgiver1tidslinje = 7.S + 2.F + 7.S
        resetSeed(1.januar)
        val arbeidsgiver2tidslinje = 7.S + 2.A + 7.S
        assertSkjæringstidspunkt(1.januar, 31.januar, arbeidsgiver1tidslinje)
        assertSkjæringstidspunkt(10.januar, 31.januar, arbeidsgiver2tidslinje)
        assertSkjæringstidspunkt(10.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
        assertSkjæringstidspunkt(10.januar, 10.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
        assertSkjæringstidspunkt(1.januar, 9.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
        assertSkjæringstidspunkt(1.januar, 7.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    @Test
    fun `arbeidsgivertidslinjer med ferie i begynnelsen, med arbeidsdag på samme dager, forblir usammenhengende`() {
        val arbeidsgiver1tidslinje = 7.F + 7.S
        resetSeed(1.januar)
        val arbeidsgiver2tidslinje = 7.A + 7.S
        assertSkjæringstidspunkt(8.januar, 31.januar, arbeidsgiver1tidslinje)
        assertSkjæringstidspunkt(8.januar, 31.januar, arbeidsgiver2tidslinje)
        assertSkjæringstidspunkt(8.januar, 31.januar, arbeidsgiver1tidslinje, arbeidsgiver2tidslinje)
    }

    private fun assertSkjæringstidspunkt(forventetSkjæringstidspunkt: LocalDate, kuttdato: LocalDate, vararg tidslinje: Sykdomstidslinje) {
        assertEquals(forventetSkjæringstidspunkt, Sykdomstidslinje.skjæringstidspunkt(kuttdato, tidslinje.toList()))
    }

    private fun assertSkjæringstidspunkt(
        forventetSkjæringstidspunkt: LocalDate,
        vararg hendelse: SykdomstidslinjeHendelse
    ) {
        val tidslinje = hendelse
            .map { it.sykdomstidslinje() }
            .merge(dagturnering::beste)

        val skjæringstidspunkt = tidslinje.skjæringstidspunkt()
        assertEquals(forventetSkjæringstidspunkt, skjæringstidspunkt) {
            "Forventet skjæringstidspunkt $forventetSkjæringstidspunkt. " +
                "Fikk $skjæringstidspunkt\n" +
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
            mottatt = sykeperioder.minOfOrNull { it.fom }?.atStartOfDay() ?: LocalDateTime.now()
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

        private fun assertDagenErSkjæringstidspunkt(
            dagen: LocalDate,
            sykdomstidslinje: Sykdomstidslinje
        ) {
            val skjæringstidspunkt = sykdomstidslinje.skjæringstidspunkt()
            assertEquals(
                dagen,
                skjæringstidspunkt
            ) { "Forventet $dagen, men fikk $skjæringstidspunkt.\nPeriode: ${sykdomstidslinje.periode()}\nTidslinjen:\n$sykdomstidslinje" }
        }

        private fun assertFørsteDagErSkjæringstidspunkt(sykdomstidslinje: Sykdomstidslinje) {
            val førsteDag = sykdomstidslinje.periode()?.start
            assertNotNull(førsteDag)
            val skjæringstidspunkt = sykdomstidslinje.skjæringstidspunkt()
            assertEquals(førsteDag, skjæringstidspunkt) {
                "Forventet $førsteDag, men fikk $skjæringstidspunkt.\nPeriode: ${sykdomstidslinje.periode()}\nTidslinjen:\n$sykdomstidslinje"
            }
        }

        private fun assertFørsteDagErSkjæringstidspunkt(
            perioden: Sykdomstidslinje,
            sykdomstidslinje: Sykdomstidslinje
        ) {
            val førsteDag = perioden.periode()?.start ?: fail { "Tom periode" }
            assertNotNull(førsteDag)
            assertDagenErSkjæringstidspunkt(førsteDag, sykdomstidslinje)
        }
    }
}
