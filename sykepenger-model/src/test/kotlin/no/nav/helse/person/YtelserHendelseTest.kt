package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class YtelserHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNR = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `ytelser på feil tidspunkt`() {
        assertThrows<Aktivitetslogg.AktivitetException> { person.håndter(ytelser(vedtaksperiodeId = UUID.randomUUID())) }

        person.håndter(sykmelding())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))

        person.håndter(søknad())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_INNTEKTSMELDING_FERDIG_GAP, inspektør.sisteTilstand(0))

        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(AVVENTER_VILKÅRSPRØVING_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    fun `historie nyere enn perioden`() {
        val sisteHistoriskeSykedag = førsteSykedag.plusMonths(2)
        håndterYtelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                    sisteHistoriskeSykedag.minusDays(14),
                    sisteHistoriskeSykedag,
                    1000,
                    100,
                    ORGNR
                )
            ),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(
                    sisteHistoriskeSykedag.minusDays(30),
                    20000.månedlig,
                    ORGNR,
                    true
                )
            )
        )
    }

    @Test
    fun `ugyldig utbetalinghistorikk før inntektsmelding kaster perioden ut`() {
        håndterUgyldigYtelser()
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `ugyldig utbetalinghistorikk etter inntektsmelding kaster perioden ut`() {
        håndterYtelser(utbetalinger = listOf(Utbetalingshistorikk.Periode.Ugyldig(null, null)))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `fordrepengeytelse før periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
    }

    @Test
    fun `fordrepengeytelse i periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `fordrepengeytelse etter periode`() {
        håndterYtelser(foreldrepengeytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
    }

    @Test
    fun `svangerskapsytelse før periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(10), førsteSykedag.minusDays(1)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
    }

    @Test
    fun `svangerskapsytelse i periode`() {
        håndterYtelser(svangerskapsytelse = Periode(førsteSykedag.minusDays(2), førsteSykedag))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
    }

    @Test
    fun `svangerskapsytelse etter periode`() {
        håndterYtelser(svangerskapsytelse = Periode(sisteSykedag.plusDays(1), sisteSykedag.plusDays(10)))
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
    }

    private fun håndterYtelser(
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning> = emptyList(),
        foreldrepengeytelse: Periode? = null,
        svangerskapsytelse: Periode? = null
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(
            ytelser(
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                foreldrepengeYtelse = foreldrepengeytelse,
                svangerskapYtelse = svangerskapsytelse
            )
        )
    }

    private fun håndterUgyldigYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(
            ytelser(
                utbetalinger = listOf(Utbetalingshistorikk.Periode.Ugyldig(null, null)),
                foreldrepengeYtelse = null,
                svangerskapYtelse = null
            )
        )
    }

    private fun ytelser(
        vedtaksperiodeId: UUID = inspektør.vedtaksperiodeId(0),
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNR,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
                aktivitetslogg = it
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            aktivitetslogg = it
        )
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100)),
            mottatt = førsteSykedag.plusMonths(3).atStartOfDay()
        )

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNR,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false
        )

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
            null,
            31000.månedlig,
            emptyList()
        )
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                1.januar(2018) til 1.desember(2018) inntekter {
                    ORGNR inntekt 31000.månedlig
                }
            }),
            erEgenAnsatt = false,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(ORGNR, 1.januar(2017))
                )
            ),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        )

    private fun simulering() =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNR,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        )

}
