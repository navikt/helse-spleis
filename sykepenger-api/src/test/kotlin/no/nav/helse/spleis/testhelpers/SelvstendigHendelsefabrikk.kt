package no.nav.helse.spleis.testhelpers

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Foreldrepenger
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.InntekterForBeregning.Inntektsperiode
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.KanIkkeBehandlesHer
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Svangerskapspenger
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.PensjonsgivendeInntekt
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.VedtakFattet
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.utbetalingslinjer.Oppdragstatus

internal class SelvstendigHendelsefabrikk() {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID()
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = MeldingsreferanseId(id),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
        registrert: LocalDateTime = LocalDateTime.now(),
        inntekterFraNyeArbeidsforhold: Boolean = false,
        pensjonsgivendeInntekter: List<PensjonsgivendeInntekt> = emptyList(),
        fraværFørSykmelding: Boolean? = null,
        harOppgittAvvikling: Boolean? = null,
        harOppgittNyIArbeidslivet: Boolean? = null,
        harOppgittVarigEndring: Boolean? = null,
        harOppgittÅHaForsikring: Boolean? = null,
        arbeidssituasjon: Søknad.Arbeidssituasjon
    ): Søknad {
        return Søknad(
            meldingsreferanseId = MeldingsreferanseId(id),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
            arbeidssituasjon = arbeidssituasjon,
            registrert = registrert,
            inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            fraværFørSykmelding = fraværFørSykmelding,
            harOppgittAvvikling = harOppgittAvvikling,
            harOppgittNyIArbeidslivet = harOppgittNyIArbeidslivet,
            harOppgittVarigEndring = harOppgittVarigEndring,
            harOppgittÅHaForsikring = harOppgittÅHaForsikring
        ).apply {
            søknader.add(this)
        }
    }

    internal fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        Utbetalingshistorikk(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            vedtaksperiodeId = vedtaksperiodeId,
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                perioder = utbetalinger
            ),
            besvart = LocalDateTime.now()
        )

    internal fun lagUtbetalingshistorikkEtterInfotrygdendring(
        utbetalinger: List<Infotrygdperiode> = listOf(),
        besvart: LocalDateTime = LocalDateTime.now(),
        id: UUID = UUID.randomUUID()
    ) =
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = MeldingsreferanseId(id),
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                perioder = utbetalinger
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
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
        inntekterForBeregning: List<Inntektsperiode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
            inntekterForBeregning = InntekterForBeregning(inntekterForBeregning),
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
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
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
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

    internal fun lagAnnullering(utbetalingId: UUID) =
        AnnullerUtbetaling(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            utbetalingId = utbetalingId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = LocalDateTime.now(),
            årsaker = listOf("Annet"),
            begrunnelse = ""
        )

    internal fun lagIdentOpphørt() =
        IdentOpphørt(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())
        )

    internal fun lagPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime, flagg: Set<String> = emptySet()) =
        Påminnelse(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = tilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now(),
            opprettet = LocalDateTime.now(),
            flagg = flagg
        )

    internal fun lagGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        Grunnbeløpsregulering(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = LocalDateTime.now()
        )

    internal fun lagHåndterForkastSykmeldingsperioder(periode: Periode) =
        ForkastSykmeldingsperioder(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            periode = periode
        )

    internal fun lagAnmodningOmForkasting(vedtaksperiodeId: UUID, force: Boolean = false) =
        AnmodningOmForkasting(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            vedtaksperiodeId = vedtaksperiodeId,
            force = force
        )

    internal fun lagHåndterOverstyrTidslinje(overstyringsdager: List<ManuellOverskrivingDag>, meldingsreferanseId: UUID = UUID.randomUUID()) =
        OverstyrTidslinje(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        )
}
