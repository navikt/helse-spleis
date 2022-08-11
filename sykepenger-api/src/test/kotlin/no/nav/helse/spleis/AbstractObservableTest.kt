package no.nav.helse.spleis

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.testhelpers.TestObservatør
import no.nav.helse.spleis.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.spleis.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

abstract class AbstractObservableTest {
    protected companion object {
        const val UNG_PERSON_FNR = "12029240045"
        val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        const val ORGNUMMER = "987654321"
        const val AKTØRID = "42"
        val INNTEKTSMELDING_ID: UUID = UUID.randomUUID()
        val SØKNAD_ID: UUID = UUID.randomUUID()
        val SYKMELDING_ID: UUID = UUID.randomUUID()
        val INNTEKT: Inntekt = 31000.00.månedlig
        val FOM: LocalDate = LocalDate.of(2018, 1, 1)
        val TOM: LocalDate = LocalDate.of(2018, 1, 30)
    }

    protected lateinit var person: Person
    internal lateinit var observatør: TestObservatør

    private val Int.vedtaksperiode: IdInnhenter get() = { orgnummer -> this.vedtaksperiode(orgnummer) }
    private fun Int.vedtaksperiode(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)

    protected fun sykmelding(
        id: UUID = SYKMELDING_ID,
        sykeperioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(FOM, TOM, 100.prosent)),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime = FOM.atStartOfDay(),
        mottatt: LocalDateTime = TOM.plusDays(1).atStartOfDay(),
        fnr: String = UNG_PERSON_FNR,
        fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO
    ): Sykmelding = Sykmelding(
        meldingsreferanseId = id,
        fnr = fnr,
        fødselsdato = fødselsdato,
        aktørId = AKTØRID,
        orgnummer = orgnummer,
        sykeperioder = sykeperioder,
        sykmeldingSkrevet = sykmeldingSkrevet,
        mottatt = mottatt
    )

    protected fun søknad(
        id: UUID = SØKNAD_ID,
        vararg perioder: Søknadsperiode = arrayOf(Sykdom(FOM, TOM, 100.prosent)),
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNAVEllerArbeidsgiver: LocalDate = TOM.plusDays(1),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime = FOM.atStartOfDay(),
        fnr: String = UNG_PERSON_FNR,
        fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO
    ): Søknad = Søknad(
        meldingsreferanseId = id,
        fnr = fnr,
        aktørId = AKTØRID,
        orgnummer = orgnummer,
        perioder = listOf(*perioder),
        andreInntektskilder = andreInntektskilder,
        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = sykmeldingSkrevet,
        fødselsdato = fødselsdato,
        korrigerer = null
    )

    protected fun inntektsmelding(
        id: UUID = INNTEKTSMELDING_ID,
        arbeidsgiverperioder: List<Periode> = listOf(Periode(FOM, TOM)),
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: LocalDate.of(2018, 1, 1),
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        orgnummer: String = ORGNUMMER,
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        fnr: String = UNG_PERSON_FNR,
        fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO
    ): Inntektsmelding = Inntektsmelding(
        meldingsreferanseId = id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fnr,
        fødselsdato = fødselsdato,
        aktørId = AKTØRID,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        arbeidsforholdId = arbeidsforholdId,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
        mottatt = LocalDateTime.now()
    )

    protected fun vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, FOM.minusYears(1))),
        inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioderForSammenligningsgrunnlag {
                        Periode(FOM.minusYears(1), FOM.minusDays(1)) inntekter {
                            ORGNUMMER inntekt INNTEKT
                        }
                    }
            ),
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = inntektperioderForSykepengegrunnlag {
                        Periode(FOM.minusMonths(3), FOM.minusDays(1)) inntekter {
                            ORGNUMMER inntekt INNTEKT
                        }
                    }, arbeidsforhold = emptyList()),
        fnr: String = UNG_PERSON_FNR
    ): Vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            aktørId = AKTØRID,
            fødselsnummer = fnr.somFødselsnummer(),
            orgnummer = orgnummer,
            inntektsvurdering = inntektsvurdering,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
    )

    protected fun ytelser(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        fnr: String = UNG_PERSON_FNR
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            infotrygdhistorikk = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = meldingsreferanseId,
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = arbeidskategorikoder,
                ugyldigePerioder = emptyList(),
                harStatslønn = statslønn
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepenger,
                svangerskapsytelse = svangerskapspenger
            ),
            pleiepenger = Pleiepenger(
                perioder = pleiepenger
            ),
            omsorgspenger = Omsorgspenger(
                perioder = omsorgspenger
            ),
            opplæringspenger = Opplæringspenger(
                perioder = opplæringspenger
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = institusjonsoppholdsperioder
            ),
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger),
            aktivitetslogg = aktivitetslogg
        )
    }

    protected fun simulering(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER,
        fom: LocalDate = FOM.plusDays(16),
        tom: LocalDate = TOM,
        fagsystemId: String,
        fagområde: String,
        utbetalingId: UUID
    ) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            fagområde = fagområde,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringResultat = Simulering.SimuleringResultat(
                totalbeløp = 2000,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(fom, tom),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = tom.plusDays(1),
                                utbetalesTil = Simulering.Mottaker(
                                    id = orgnummer,
                                    navn = "Org Orgesen AS"
                                ),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(fom, tom),
                                        konto = "81549300",
                                        beløp = 2000,
                                        klassekode = Simulering.Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTEL",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(
                                            sats = 1000.0,
                                            antall = 2,
                                            type = "DAG"
                                        ),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

    protected fun utbetalingsgodkjenning(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        utbetalingGodkjent: Boolean = true,
        orgnummer: String = ORGNUMMER,
        automatiskBehandling: Boolean = false,
        utbetalingId: UUID
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR,
        organisasjonsnummer = orgnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    )

    protected fun utbetaling(
        fagsystemId: String,
        status: Oppdragstatus = AKSEPTERT,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID(),
        utbetalingId: UUID
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = "$utbetalingId",
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )
}

internal typealias IdInnhenter = (orgnummer: String) -> UUID

