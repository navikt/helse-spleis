package no.nav.helse.spleis.testhelpers

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Foreldrepenger
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.KanIkkeBehandlesHer
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Svangerskapspenger
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.VedtakFattet
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt


internal class ArbeidsgiverHendelsefabrikk(private val organisasjonsnummer: String) {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()
    private val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID()
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            orgnummer = organisasjonsnummer,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            sykmeldinger.add(this)
        }
    }

    internal fun lagSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDateTime,
        sykmeldingSkrevet: LocalDateTime,
        ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean = false,
        id: UUID = UUID.randomUUID(),
        merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(),
        permittert: Boolean = false,
        korrigerer: UUID? = null,
        utenlandskSykmelding: Boolean = false,
        arbeidUtenforNorge: Boolean = false,
        sendTilGosys: Boolean = false,
        opprinneligSendt: LocalDate? = null,
        yrkesskade: Boolean = false,
        egenmeldinger: List<Periode> = emptyList(),
        søknadstype: Søknad.Søknadstype = Søknad.Søknadstype.Arbeidstaker,
        registrert: LocalDateTime = LocalDateTime.now(),
        tilkomneInntekter: List<Søknad.TilkommenInntekt> = emptyList(),
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            orgnummer = organisasjonsnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            ikkeJobbetIDetSisteFraAnnetArbeidsforhold = ikkeJobbetIDetSisteFraAnnetArbeidsforhold,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet = sykmeldingSkrevet,
            opprinneligSendt = opprinneligSendt?.atStartOfDay(),
            utenlandskSykmelding = utenlandskSykmelding,
            arbeidUtenforNorge = arbeidUtenforNorge,
            sendTilGosys = sendTilGosys,
            yrkesskade = yrkesskade,
            egenmeldinger = egenmeldinger,
            søknadstype = søknadstype,
            registrert = registrert,
            tilkomneInntekter = tilkomneInntekter
        ).apply {
            søknader.add(this)
        }
    }

    internal fun lagInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        harFlereInntektsmeldinger: Boolean = false,
        mottatt: LocalDateTime = LocalDateTime.now()
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = refusjon,
                orgnummer = organisasjonsnummer,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                avsendersystem = Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag),
                mottatt = mottatt
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        return inntektsmeldinggenerator()
    }

    internal fun lagSykepengegrunnlagForArbeidsgiver(vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>): SykepengegrunnlagForArbeidsgiver {
        return SykepengegrunnlagForArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummer = organisasjonsnummer,
            inntekter = ArbeidsgiverInntekt(organisasjonsnummer, inntekter)
        )
    }

    internal fun lagPortalinntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        vedtaksperiodeId: UUID,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        harFlereInntektsmeldinger: Boolean = false,
        mottatt: LocalDateTime = LocalDateTime.now()
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = refusjon,
                orgnummer = organisasjonsnummer,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                avsendersystem = Inntektsmelding.Avsendersystem.NavPortal(vedtaksperiodeId, LocalDate.EPOCH, true),
                mottatt = mottatt
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        return inntektsmeldinggenerator()
    }

    internal fun lagInntektsmeldingReplayUtført(vedtaksperiodeId: UUID) =
        InntektsmeldingerReplay(UUID.randomUUID(), organisasjonsnummer, vedtaksperiodeId, emptyList())

    internal fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = UUID.randomUUID(),
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = emptyMap()
            ),
            besvart = LocalDateTime.now()
        )

    internal fun lagUtbetalingshistorikkEtterInfotrygdendring(
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        id: UUID = UUID.randomUUID()
    ) =
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = id,
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = UUID.randomUUID(),
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = emptyMap()
            ),
            besvart = LocalDateTime.now()
        )


    internal fun lagVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
        inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummer = organisasjonsnummer,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering,
            arbeidsforhold = arbeidsforhold
        )
    }

    internal fun lagYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            foreldrepenger = Foreldrepenger(
                foreldrepengeytelse = foreldrepenger,
            ),
            svangerskapspenger = Svangerskapspenger(
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
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger)
        )
    }

    internal fun lagSimulering(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fagsystemId: String,
        fagområde: String,
        simuleringOK: Boolean,
        simuleringsresultat: SimuleringResultatDto?
    ): Simulering {
        return Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            fagområde = fagområde,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringsResultat = simuleringsresultat
        )
    }

    internal fun lagUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingGodkjent: Boolean,
        automatiskBehandling: Boolean,
        utbetalingId: UUID,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now()
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        organisasjonsnummer = organisasjonsnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = godkjenttidspunkt,
        automatiskBehandling = automatiskBehandling,
    )

    internal fun lagVedtakFattet(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        automatisert: Boolean = true,
        vedtakFattetTidspunkt: LocalDateTime = LocalDateTime.now()
    ) = VedtakFattet(
        meldingsreferanseId = UUID.randomUUID(),
        organisasjonsnummer = organisasjonsnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandlerIdent = "Vedtak fattesen",
        saksbehandlerEpost = "vedtak.fattesen@nav.no",
        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        automatisert = automatisert
    )

    internal fun lagKanIkkeBehandlesHer(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        automatisert: Boolean = true
    ) = KanIkkeBehandlesHer(
        meldingsreferanseId = UUID.randomUUID(),
        organisasjonsnummer = organisasjonsnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandlerIdent = "Info trygdesen",
        saksbehandlerEpost = "info.trygdesen@nav.no",
        opprettet = LocalDateTime.now(),
        automatisert = automatisert
    )

    internal fun lagUtbetalinghendelse(
        utbetalingId: UUID,
        fagsystemId: String,
        status: Oppdragstatus,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

    internal fun lagAnnullering(utbetalingId: UUID) =
        AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            utbetalingId = utbetalingId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = LocalDateTime.now()
        )

    internal fun lagIdentOpphørt() =
        IdentOpphørt(
            meldingsreferanseId = UUID.randomUUID()
        )

    internal fun lagPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime, reberegning: Boolean = false) =
        Påminnelse(
            UUID.randomUUID(),
            organisasjonsnummer,
            vedtaksperiodeId.toString(),
            0,
            tilstand,
            tilstandsendringstidspunkt,
            LocalDateTime.now(),
            LocalDateTime.now(),
            opprettet = LocalDateTime.now(),
            ønskerReberegning = reberegning
        )

    internal fun lagGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        Grunnbeløpsregulering(
            meldingsreferanseId = UUID.randomUUID(),
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now()
        )

    internal fun lagHåndterForkastSykmeldingsperioder(periode: Periode) =
        ForkastSykmeldingsperioder(
            meldingsreferanseId = UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            periode = periode
        )

    internal fun lagAnmodningOmForkasting(vedtaksperiodeId: UUID, force: Boolean = false) =
        AnmodningOmForkasting(
            meldingsreferanseId = UUID.randomUUID(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            force = force
        )

    internal fun lagHåndterOverstyrTidslinje(overstyringsdager: List<ManuellOverskrivingDag>, meldingsreferanseId: UUID = UUID.randomUUID()) =
        OverstyrTidslinje(
            meldingsreferanseId = meldingsreferanseId,
            organisasjonsnummer = organisasjonsnummer,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        )
}
