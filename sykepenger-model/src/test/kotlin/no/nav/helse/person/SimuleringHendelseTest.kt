package no.nav.helse.person

import no.nav.helse.Toggle
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
        håndterSimuleringer()
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering med endret dagsats`() {
        håndterYtelser()
        håndterSimuleringer(mapOf(Fagområde.SykepengerRefusjon to Pair(true, 500)))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.personLogg.warn().toString().contains("Simulering"))
    }

    @Test
    fun `simulering er ikke OK`() {
        håndterYtelser()
        håndterSimuleringer(mapOf(Fagområde.SykepengerRefusjon to Pair(false, 1431)))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `simulering ved delvis refusjon`() = listOf(Toggle.LageBrukerutbetaling, Toggle.DelvisRefusjon).enable {
        håndterYtelser(Inntektsmelding.Refusjon(31000.månedlig, sisteSykedag.minusDays(7), emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.SykepengerRefusjon to Pair(true, 1431),
            Fagområde.Sykepenger to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering ved delvis refusjon hvor vi avventer en simulering`() = listOf(Toggle.LageBrukerutbetaling, Toggle.DelvisRefusjon).enable {
        håndterYtelser(Inntektsmelding.Refusjon(31000.månedlig, sisteSykedag.minusDays(7), emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.SykepengerRefusjon to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_SIMULERING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    @Test
    fun `simulering ved ingen refusjon`() = listOf(Toggle.LageBrukerutbetaling, Toggle.DelvisRefusjon).enable {
        håndterYtelser(Inntektsmelding.Refusjon(INGEN, null, emptyList()))
        håndterSimuleringer(mapOf(
            Fagområde.Sykepenger to Pair(true, 1431)
        ))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertFalse(inspektør.personLogg.hasWarningsOrWorse())
    }

    private fun håndterYtelser(
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList())
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding(refusjon))
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
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
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            organisasjonsnummer = ORGNUMMER.toString(),
            vedtaksperiodeId = "${1.vedtaksperiode(ORGNUMMER)}",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                organisasjonsnummer = ORGNUMMER.toString(),
                vedtaksperiodeId = "${1.vedtaksperiode(ORGNUMMER)}",
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
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "aktørId",
            orgnummer = ORGNUMMER.toString(),
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            sykmeldingSkrevet = førsteSykedag.atStartOfDay(),
            mottatt = sisteSykedag.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "aktørId",
            orgnummer = ORGNUMMER.toString(),
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding(
        refusjon: Inntektsmelding.Refusjon
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = refusjon,
            orgnummer = ORGNUMMER.toString(),
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
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
            vedtaksperiodeId = "${1.vedtaksperiode(ORGNUMMER)}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER.toString(),
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 31000.månedlig
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Arbeidsforhold(
                        ORGNUMMER.toString(),
                        1.januar(2017)
                    )
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt 31000.månedlig
                    }
                }),
            arbeidsforhold = listOf(
                Arbeidsforhold(
                    ORGNUMMER.toString(),
                    1.januar(2017)
                )
            )
        ).apply {
            hendelse = this
        }

    private fun håndterSimuleringer(simuleringsdetaljer: Map<Fagområde, Pair<Boolean,Int>> = mapOf(Fagområde.SykepengerRefusjon to Pair(true, 1431))) {
        hendelse.behov().filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering }.forEach { simuleringsBehov ->
            val fagsystemId = simuleringsBehov.detaljer().getValue("fagsystemId") as String
            val fagområde = Fagområde.from(simuleringsBehov.detaljer().getValue("fagområde") as String)
            val utbetalingId = UUID.fromString(simuleringsBehov.kontekst().getValue("utbetalingId"))
            if (!simuleringsdetaljer.containsKey(fagområde)) return@forEach
            val (simuleringOk, dagsats) = simuleringsdetaljer.getValue(fagområde)
            person.håndter(simulering(simuleringOk, dagsats, fagområde, fagsystemId, utbetalingId))
        }
    }

    private fun simulering(
        simuleringOK: Boolean,
        dagsats: Int,
        fagområde: Fagområde,
        fagsystemId: String,
        utbetalingId: UUID) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode(ORGNUMMER)}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER.toString(),
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringResultat = if (!simuleringOK) null else Simulering.SimuleringResultat(
                totalbeløp = 44361,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(17.januar, 31.januar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 1.februar,
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018.toString(), "Ung Person"),
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
                                        refunderesOrgnummer = ORGNUMMER.toString()
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
                                utbetalesTil = Simulering.Mottaker(UNG_PERSON_FNR_2018.toString(), "Ung Person"),
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
                                        refunderesOrgnummer = ORGNUMMER.toString()
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
