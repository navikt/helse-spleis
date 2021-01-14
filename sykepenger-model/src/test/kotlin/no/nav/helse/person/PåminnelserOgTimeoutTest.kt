package no.nav.helse.person

import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.*
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class PåminnelserOgTimeoutTest : AbstractPersonTest() {

    private companion object {
        private val nå = LocalDate.now()
    }

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `påminnelse i mottatt sykmelding innenfor makstid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(nå.minusDays(30), nå, 100.prosent)))
        person.håndter(påminnelse(MOTTATT_SYKMELDING_FERDIG_GAP, 1.vedtaksperiode))
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Sykepengehistorikk))
    }

    @Test
    fun `påminnelse i mottatt søknad`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        person.håndter(påminnelse(AVVENTER_GAP, 1.vedtaksperiode))
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Sykepengehistorikk))
    }

    @Test
    fun `påminnelse i mottatt søknad innenfor makstid`() {
        person.håndter(sykmelding(Sykmeldingsperiode(nå.minusDays(60), nå.minusDays(31), 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(nå.minusDays(30), nå, 100.prosent)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(nå.minusDays(30), nå, 100.prosent)))
        assertEquals(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
        person.håndter(påminnelse(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2.vedtaksperiode))
        assertEquals(AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(2.vedtaksperiode, Behovtype.Sykepengehistorikk))
    }

    @Test
    fun `påminnelse i mottatt inntektsmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(nå.minusDays(30), nå, 100.prosent)))
        person.håndter(inntektsmelding(Periode(nå.minusDays(30), nå.minusDays(14))))
        person.håndter(påminnelse(AVVENTER_SØKNAD_FERDIG_GAP, 1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertEquals(AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `påminnelse i vilkårsprøving`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        assertEquals(3, hendelse.behov().size)
        person.håndter(påminnelse(AVVENTER_VILKÅRSPRØVING_GAP, 1.vedtaksperiode))
        assertEquals(AVVENTER_VILKÅRSPRØVING_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(3, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.InntekterForSammenligningsgrunnlag))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Opptjening))
    }

    @Test
    fun `påminnelse i ytelser`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        assertEquals(9, hendelse.behov().size)
        person.håndter(påminnelse(AVVENTER_HISTORIKK, 1.vedtaksperiode))
        assertEquals(AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(9, hendelse.behov().size)
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Foreldrepenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Pleiepenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Omsorgspenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Opplæringspenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Arbeidsavklaringspenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Dagpenger))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Institusjonsopphold))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Sykepengehistorikk))
        assertTrue(hendelse.etterspurteBehov(1.vedtaksperiode, Behovtype.Dødsinfo))
    }

    @Test
    fun `påminnelse i avventer simulering`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        assertEquals(AVVENTER_SIMULERING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertTrue(hendelse.behov().any { it.type == Behovtype.Simulering })
        person.håndter(påminnelse(AVVENTER_SIMULERING, 1.vedtaksperiode))
        assertEquals(AVVENTER_SIMULERING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
    }

    @Test
    fun `påminnelse i til godkjenning`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(simulering())
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
        assertTrue(hendelse.behov().any { it.type == Behovtype.Godkjenning })
        person.håndter(påminnelse(AVVENTER_GODKJENNING, 1.vedtaksperiode))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
    }

    @Test
    fun `påminnelse i til utbetaling`() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning())
        assertEquals(1, hendelse.behov().size)
        person.håndter(utbetalingpåminnelse(inspektør.utbetalingId(0), Utbetalingstatus.SENDT))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, hendelse.behov().size)
    }

    @Test
    fun `ignorerer påminnelser på tidligere tilstander`() {
        person.håndter(sykmelding())
        person.håndter(påminnelse(TIL_INFOTRYGD, 1.vedtaksperiode))
        assertEquals(MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(søknad())
        person.håndter(påminnelse(MOTTATT_SYKMELDING_FERDIG_GAP, 1.vedtaksperiode))
        assertEquals(AVVENTER_GAP, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(inntektsmelding())
        person.håndter(påminnelse(AVVENTER_GAP, 1.vedtaksperiode))
        assertEquals(AVVENTER_VILKÅRSPRØVING_GAP, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(vilkårsgrunnlag())
        person.håndter(påminnelse(AVVENTER_VILKÅRSPRØVING_GAP, 1.vedtaksperiode))
        assertEquals(AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(ytelser())
        person.håndter(påminnelse(AVVENTER_HISTORIKK, 1.vedtaksperiode))
        assertEquals(AVVENTER_SIMULERING, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(simulering())
        person.håndter(påminnelse(AVVENTER_SIMULERING, 1.vedtaksperiode))
        assertEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(utbetalingsgodkjenning())
        person.håndter(påminnelse(AVVENTER_GODKJENNING, 1.vedtaksperiode))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))

        person.håndter(påminnelse(TIL_UTBETALING, 1.vedtaksperiode))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun søknad(
        vararg perioder: Søknad.Søknadsperiode = arrayOf(
            Søknad.Søknadsperiode.Sykdom(
                1.januar,
                20.januar,
                100.prosent
            )
        )
    ) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = ORGNUMMER,
            perioder = perioder.toList(),
            harAndreInntektskilder = false,
            sendtTilNAV = 20.januar.atStartOfDay(),
            permittert = false
        ).apply {
            hendelse = this
        }

    private fun sykmelding(
        vararg perioder: Sykmeldingsperiode = arrayOf(
            Sykmeldingsperiode(
                1.januar,
                20.januar,
                100.prosent
            )
        )
    ) =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = ORGNUMMER,
            sykeperioder = perioder.toList(),
            mottatt = Sykmeldingsperiode.periode(perioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding(
        vararg arbeidsgiverperiode: Periode = arrayOf(Periode(1.januar, 1.januar.plusDays(15))),
        førsteFraværsdag: LocalDate = 1.januar
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = arbeidsgiverperiode.toList(),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
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
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                1.januar(2018) til 1.desember(2018) inntekter {
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

    private fun simulering() =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            simuleringOK = true,
            melding = "",
            simuleringResultat = Simulering.SimuleringResultat(
                totalbeløp = 2000,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(17.januar, 20.januar),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = 21.januar,
                                utbetalesTil = Simulering.Mottaker(
                                    id = ORGNUMMER,
                                    navn = "Org Orgesen AS"
                                ),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(17.januar, 20.januar),
                                        konto = "81549300",
                                        beløp = 2000,
                                        klassekode = Simulering.Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTELSE",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(
                                            sats = 1000,
                                            antall = 2,
                                            type = "DAGLIG"
                                        ),
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

    private fun ytelser(): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
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
                utbetalinger = listOf(RefusjonTilArbeidsgiver(17.januar(2017), 20.januar(2017), 1000.daglig,  100.prosent,  ORGNUMMER)),
                inntektshistorikk = listOf(
                    Utbetalingshistorikk.Inntektsopplysning(17.januar(2017), 31000.månedlig, ORGNUMMER, true)
                ),
                aktivitetslogg = Aktivitetslogg()
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = null,
                svangerskapsytelse = null,
                aktivitetslogg = Aktivitetslogg()
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList(),
                aktivitetslogg = Aktivitetslogg()
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList(),
                aktivitetslogg = Aktivitetslogg()
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList(),
                aktivitetslogg = Aktivitetslogg()
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
                aktivitetslogg = Aktivitetslogg()
            ),
            aktivitetslogg = Aktivitetslogg(),
            dødsinfo = Dødsinfo(null),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
            dagpenger = Dagpenger(emptyList())
        ).apply {
            hendelse = this
        }
    }

    private fun utbetalingsgodkjenning() = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        utbetalingId = UUID.fromString(inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"] ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(
            Behovtype.Godkjenning
        ).kontekst()}")),
        vedtaksperiodeId = "${1.vedtaksperiode}",
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = true,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = false,
        saksbehandlerEpost = "ola@normann.ss",
        makstidOppnådd = false,
    ).apply {
        hendelse = this
    }

    private fun påminnelse(tilstandType: TilstandType, vedtaksperiodeId: UUID) = Påminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = "$vedtaksperiodeId",
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    private fun utbetalingpåminnelse(utbetalingId: UUID, status: Utbetalingstatus) = Utbetalingpåminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        utbetalingId = utbetalingId,
        status = status,
        antallGangerPåminnet = 1,
        endringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }
}
