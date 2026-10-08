package no.nav.helse.dsl

import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.fraInnteksmelding
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.*

internal class ArbeidsgiverHendelsefabrikk(
    private val organisasjonsnummer: String,
    private val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
) {
    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID(),
    ): Sykmelding =
        Sykmelding(
            meldingsreferanseId = MeldingsreferanseId(id),
            behandlingsporing = behandlingsporing,
            sykeperioder = listOf(*sykeperioder),
        )

    internal fun lagSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: Temporal? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
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
        arbeidssituasjon: Søknad.Arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER,
        registrert: LocalDateTime = LocalDateTime.now(),
        inntekterFraNyeArbeidsforhold: Boolean = false,
        pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null,
        harOppgittAvvikling: Boolean? = null,
        harOppgittVarigEndring: Boolean? = null,
        harOppgittNyIArbeidslivet: Boolean? = null,
        harOppgittOpprettholdtInntekt: Boolean? = null,
        harOppgittOppholdIUtlandet: Boolean? = null,
    ): Søknad {
        val innsendt =
            (sendtTilNAVEllerArbeidsgiver ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive).let {
                when (it) {
                    is LocalDateTime -> it
                    is LocalDate -> it.atStartOfDay()
                    else -> throw IllegalStateException("Innsendt må være enten LocalDate eller LocalDateTime")
                }
            }
        return Søknad(
            meldingsreferanseId = MeldingsreferanseId(id),
            behandlingsporing = behandlingsporing,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            ikkeJobbetIDetSisteFraAnnetArbeidsforhold = ikkeJobbetIDetSisteFraAnnetArbeidsforhold,
            sendtTilNAVEllerArbeidsgiver = innsendt,
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet =
                sykmeldingSkrevet ?: Søknad.Søknadsperiode
                    .søknadsperiode(perioder.toList())!!
                    .start
                    .atStartOfDay(),
            opprinneligSendt = opprinneligSendt?.atStartOfDay(),
            utenlandskSykmelding = utenlandskSykmelding,
            arbeidUtenforNorge = arbeidUtenforNorge,
            sendTilGosys = sendTilGosys,
            yrkesskade = yrkesskade,
            egenmeldinger = egenmeldinger,
            arbeidssituasjon = arbeidssituasjon,
            registrert = registrert,
            inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            harOppgittAvvikling = harOppgittAvvikling,
            harOppgittVarigEndring = harOppgittVarigEndring,
            harOppgittNyIArbeidslivet = harOppgittNyIArbeidslivet,
            harOppgittOpprettholdtInntekt = harOppgittOpprettholdtInntekt,
            harOppgittOppholdIUtlandet = harOppgittOppholdIUtlandet,
        )
    }

    fun lagAvbruttSøknad(sykmeldingsperiode: Periode): AvbruttSøknad = AvbruttSøknad(sykmeldingsperiode, MeldingsreferanseId(UUID.randomUUID()), behandlingsporing)

    internal fun lagInntektsopplysningerFraLagretInnteksmelding(
        meldingsreferanseId: MeldingsreferanseId,
        vedtaksperiodeId: UUID,
        inntektsmeldingMeldingsreferanseId: MeldingsreferanseId,
        inntektsmeldingMottatt: LocalDateTime,
        inntekt: Inntekt,
        refusjon: Inntekt,
    ) = InntektsopplysningerFraLagretInnteksmelding(
        meldingsreferanseId = meldingsreferanseId,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        inntektsmeldingMeldingsreferanseId = inntektsmeldingMeldingsreferanseId,
        inntekt = inntekt,
        refusjon = refusjon,
        inntektsmeldingMottatt = inntektsmeldingMottatt,
    )

    internal fun lagInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        mottatt: LocalDateTime = LocalDateTime.now(),
    ) = Inntektsmelding(
        meldingsreferanseId = MeldingsreferanseId(id),
        refusjon = refusjon,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = organisasjonsnummer),
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        begrunnelseForReduksjonEllerIkkeUtbetalt = fraInnteksmelding(begrunnelseForReduksjonEllerIkkeUtbetalt),
        opphørAvNaturalytelser = opphørAvNaturalytelser,
        førsteFraværsdag = førsteFraværsdag,
        mottatt = mottatt,
    )

    internal fun lagArbeidsgiveropplysninger(
        meldingsreferanseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        innsendt: LocalDateTime = LocalDateTime.now(),
        registrert: LocalDateTime = innsendt.plusSeconds(1),
        vararg opplysninger: Arbeidsgiveropplysning,
    ) = Arbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        innsendt = innsendt,
        registrert = registrert,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = opplysninger.toList(),
    )

    internal fun lagKorrigerteArbeidsgiveropplysninger(
        meldingsreferanseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        innsendt: LocalDateTime = LocalDateTime.now(),
        registrert: LocalDateTime = innsendt.plusSeconds(1),
        vararg opplysninger: Arbeidsgiveropplysning,
    ) = KorrigerteArbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        innsendt = innsendt,
        registrert = registrert,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = opplysninger.toList(),
    )

    internal fun lagSelvbestemteArbeidsgiveropplysninger(
        meldingsreferanseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        innsendt: LocalDateTime = LocalDateTime.now(),
        registrert: LocalDateTime = innsendt.plusSeconds(1),
        vararg opplysninger: Arbeidsgiveropplysning,
    ) = SelvbestemteArbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        innsendt = innsendt,
        registrert = registrert,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = opplysninger.toList(),
    )

    internal fun lagInntektsmeldingReplay(
        vedtaksperiodeId: UUID,
        inntektsmeldinger: List<Inntektsmelding>,
    ) = InntektsmeldingerReplay(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing =
            Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = organisasjonsnummer,
            ),
        vedtaksperiodeId = vedtaksperiodeId,
        inntektsmeldinger = inntektsmeldinger,
    )

    internal fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        besvart: LocalDateTime = LocalDateTime.now(),
    ) = Utbetalingshistorikk(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        vedtaksperiodeId = vedtaksperiodeId,
        element =
            InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                perioder = utbetalinger,
            ),
        besvart = LocalDateTime.now(),
    )

    internal fun lagUtbetalingshistorikkEtterInfotrygdendring(
        utbetalinger: List<Infotrygdperiode> = listOf(),
        besvart: LocalDateTime = LocalDateTime.now(),
        id: UUID = UUID.randomUUID(),
    ) = UtbetalingshistorikkEtterInfotrygdendring(
        meldingsreferanseId = MeldingsreferanseId(id),
        element =
            InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = MeldingsreferanseId(id),
                perioder = utbetalinger,
            ),
        besvart = LocalDateTime.now(),
    )

    internal fun lagVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
        inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering,
        forsikringsvurderingId: UUID? = null,
        opptjeningsvurderingId: UUID? = null,
    ): Vilkårsgrunnlag =
        Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingsporing = behandlingsporing,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering,
            arbeidsforhold = arbeidsforhold,
            forsikringsvurderingId = forsikringsvurderingId,
            opptjeningsvurderingId = opptjeningsvurderingId,
        )

    internal fun lagYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<GradertPeriode> = emptyList(),
        svangerskapspenger: List<GradertPeriode> = emptyList(),
        pleiepenger: List<GradertPeriode> = emptyList(),
        omsorgspenger: List<GradertPeriode> = emptyList(),
        opplæringspenger: List<GradertPeriode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspengerV2: List<Periode> = emptyList(),
        dagpengerV2: List<Periode> = emptyList(),
        inntekterForBeregning: List<InntekterForBeregning.Inntektsperiode> = emptyList(),
        graderteAndreYtelser: List<GraderteAndreYtelserForBeregning> = emptyList(),
        forsikringsvurderingResultat: ForsikringsvurderingResultat? = null,
        opptjeningsvurderingResultatOk: Boolean,
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            behandlingsporing = behandlingsporing,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            foreldrepenger =
                Foreldrepenger(
                    foreldrepengeytelse = foreldrepenger,
                ),
            svangerskapspenger =
                Svangerskapspenger(
                    svangerskapsytelse = svangerskapspenger,
                ),
            pleiepenger =
                Pleiepenger(
                    perioder = pleiepenger,
                ),
            omsorgspenger =
                Omsorgspenger(
                    perioder = omsorgspenger,
                ),
            opplæringspenger =
                Opplæringspenger(
                    perioder = opplæringspenger,
                ),
            institusjonsopphold =
                Institusjonsopphold(
                    perioder = institusjonsoppholdsperioder,
                ),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspengerV2),
            dagpenger = Dagpenger(dagpengerV2),
            inntekterForBeregning = InntekterForBeregning(inntekterForBeregning),
            graderteAndreYtelser = graderteAndreYtelser,
            forsikringsvurderingResultat = forsikringsvurderingResultat,
            opptjeningsvurderingResultatOk = opptjeningsvurderingResultatOk,
        )
    }

    internal fun lagSimulering(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fagsystemId: String,
        fagområde: String,
        simuleringOK: Boolean,
        simuleringsresultat: SimuleringResultatDto?,
    ): Simulering =
        Simulering(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            behandlingsporing = behandlingsporing,
            fagsystemId = fagsystemId,
            fagområde = fagområde,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringsResultat = simuleringsresultat,
        )

    internal fun lagUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingGodkjent: Boolean,
        automatiskBehandling: Boolean,
        utbetalingId: UUID,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = godkjenttidspunkt,
        automatiskBehandling = automatiskBehandling,
    )

    internal fun lagVedtakFattet(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        automatisert: Boolean = true,
        vedtakFattetTidspunkt: LocalDateTime = LocalDateTime.now(),
    ) = VedtakFattet(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        saksbehandlerIdent = "Vedtak fattesen",
        saksbehandlerEpost = "vedtak.fattesen@nav.no",
        vedtakFattetTidspunkt = vedtakFattetTidspunkt,
        automatisert = automatisert,
    )

    internal fun lagKanIkkeBehandlesHer(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        automatisert: Boolean = true,
    ) = KanIkkeBehandlesHer(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        saksbehandlerIdent = "Info trygdesen",
        saksbehandlerEpost = "info.trygdesen@nav.no",
        opprettet = LocalDateTime.now(),
        automatisert = automatisert,
    )

    internal fun lagUtbetalinghendelse(
        vedtaksperiodeId: UUID,
        behandlingId: UUID,
        utbetalingId: UUID,
        fagsystemId: String,
        status: Oppdragstatus,
        meldingsreferanseId: UUID = UUID.randomUUID(),
    ) = UtbetalingHendelse(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = behandlingsporing,
        fagsystemId = fagsystemId,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        status = status,
        melding = "hei",
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now(),
    )

    internal fun lagAnnullering(vedtaksperiodeId: UUID) =
        AnnullerUtbetaling(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = LocalDateTime.now(),
            årsaker = listOf("Annet"),
            begrunnelse = "",
        )

    internal fun lagIdentOpphørt() =
        IdentOpphørt(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        )

    internal fun lagPåminnelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime,
        nåtidspunkt: LocalDateTime = LocalDateTime.now(),
        flagg: Set<String> = emptySet(),
    ) = Påminnelse(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        antallGangerPåminnet = 0,
        tilstand = tilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        påminnelsestidspunkt = nåtidspunkt,
        nestePåminnelsestidspunkt = nåtidspunkt,
        opprettet = nåtidspunkt,
        flagg = flagg,
    )

    internal fun lagGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        Grunnbeløpsregulering(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now(),
        )

    internal fun lagHåndterForkastSykmeldingsperioder(periode: Periode) =
        ForkastSykmeldingsperioder(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
            periode = periode,
        )

    internal fun lagAnmodningOmForkasting(
        vedtaksperiodeId: UUID,
        force: Boolean = false,
    ) = AnmodningOmForkasting(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = behandlingsporing,
        vedtaksperiodeId = vedtaksperiodeId,
        force = force,
    )

    internal fun lagHåndterOverstyrTidslinje(
        overstyringsdager: List<ManuellOverskrivingDag>,
        meldingsreferanseId: UUID = UUID.randomUUID(),
    ) = OverstyrTidslinje(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = behandlingsporing,
        dager = overstyringsdager,
        opprettet = LocalDateTime.now(),
    )
}
