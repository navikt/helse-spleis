package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SimuleringHendelseTest : AbstractPersonTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 28.februar
    }

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `simulering er OK`() {
        håndterYtelser()
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering med endret dagsats`() {
        håndterYtelser()
        person.håndter(simulering(dagsats = 500))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.personLogg.warn().toString().contains("Simulering"))
    }

    @Test
    fun `simulering er ikke OK`() {
        håndterYtelser()
        person.håndter(simulering(false))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun håndterYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(utbetalingsgrunnlag())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun utbetalingsgrunnlag() = Utbetalingsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        vedtaksperiodeId = 1.vedtaksperiode,
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList())
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "${1.vedtaksperiode}",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = "${1.vedtaksperiode}",
                arbeidskategorikoder = emptyMap(),
                harStatslønn = false,
                perioder = utbetalinger,
                inntektshistorikk = emptyList(),
                ugyldigePerioder = emptyList(),
                aktivitetslogg = it,
                besvart = LocalDateTime.now()
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
            dødsinfo = Dødsinfo(null),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
            dagpenger = Dagpenger(emptyList()),
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
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            sykmeldingSkrevet = førsteSykedag.atStartOfDay(),
            mottatt = sisteSykedag.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 31000.månedlig
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        ORGNUMMER,
                        1.januar(2017)
                    )
                )
            )
        ).apply {
            hendelse = this
        }

    private fun simulering(simuleringOK: Boolean = true, dagsats: Int = 1431) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
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
                                        refunderesOrgnummer = ORGNUMMER
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
                                        refunderesOrgnummer = ORGNUMMER
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
