package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class SimuleringHendelseTest {
    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "12345"
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 28.februar
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    @BeforeEach
    internal fun opprettPerson() {
        person = Person("12345", UNG_PERSON_FNR_2018)
    }

    @Test
    fun `simulering er OK`() {
        håndterYtelser()
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering med endret dagsats`() {
        håndterYtelser()
        person.håndter(simulering(dagsats = 500))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(0))
        assertTrue(inspektør.personLogg.warn().toString().contains("Simulering"))
    }

    @Test
    fun `simulering er ikke OK`() {
        håndterYtelser()
        person.håndter(simulering(false))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteForkastetTilstand(0))
        assertTrue(inspektør.personLogg.warn().toString().contains("Simulering"))
    }

    private fun håndterYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun ytelser(
        vedtaksperiodeId: UUID = inspektør.vedtaksperiodeId(0),
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                utbetalinger = utbetalinger,
                inntektshistorikk = emptyList(),
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
        ).apply {
            hendelse = this
        }
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100)),
            mottatt = førsteSykedag.plusMonths(3).atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100, null)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                1.januar(2018) til 1.desember(2018) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            ),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelse = this
        }

    private fun simulering(simuleringOK: Boolean = true, dagsats: Int = 1431) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(0).toString(),
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            simuleringOK = simuleringOK,
            melding = "",
            simuleringResultat = if (!simuleringOK) null else Simulering.SimuleringResultat(
                totalbeløp = 44361,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(17.januar, 31.januar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 1.februar,
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018, "Ung Person"),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(17.januar, 31.januar),
                                        konto = "11111111111",
                                        beløp = dagsats * 11,
                                        klassekode = Simulering.Klassekode(
                                            "SPREFAG-IOP",
                                            "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(dagsats, 11, "DAGLIG"),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    ),
                    Simulering.SimulertPeriode(
                        periode = Periode(1.februar, 28.februar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 1.mars,
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018, "Ung Person"),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(1.februar, 28.februar),
                                        konto = "11111111111",
                                        beløp = dagsats * 20,
                                        klassekode = Simulering.Klassekode(
                                            "SPREFAG-IOP",
                                            "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(dagsats, 20, "DAGLIG"),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    )
                )
            )
        ).apply {
            hendelse = this
        }
}
