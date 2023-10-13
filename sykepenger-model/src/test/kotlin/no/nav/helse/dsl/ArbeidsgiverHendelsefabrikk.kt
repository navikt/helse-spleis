package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt


internal class ArbeidsgiverHendelsefabrikk(
    private val aktørId: String,
    private val personidentifikator: Personidentifikator,
    private val organisasjonsnummer: String
) {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()
    private val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID()
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = personidentifikator.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            sykmeldinger.add(this)
        }
    }

    internal fun lagSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
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
        aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
        egenmeldinger: List<Søknad.Søknadsperiode.Arbeidsgiverdag> = emptyList(),
        søknadstype: Søknad.Søknadstype = Søknad.Søknadstype.Arbeidstaker
    ): Søknad {
        val innsendt = sendtTilNAVEllerArbeidsgiver ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive
        return Søknad(
            meldingsreferanseId = id,
            fnr = personidentifikator.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            ikkeJobbetIDetSisteFraAnnetArbeidsforhold = ikkeJobbetIDetSisteFraAnnetArbeidsforhold,
            sendtTilNAVEllerArbeidsgiver = innsendt.atStartOfDay(),
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay(),
            opprinneligSendt = opprinneligSendt?.atStartOfDay(),
            utenlandskSykmelding = utenlandskSykmelding,
            arbeidUtenforNorge = arbeidUtenforNorge,
            sendTilGosys = sendTilGosys,
            aktivitetslogg = aktivitetslogg,
            yrkesskade = yrkesskade,
            egenmeldinger = egenmeldinger,
            søknadstype = søknadstype
        ).apply {
            søknader.add(this)
        }
    }

    internal fun lagInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOf { it.start },
        inntektsdato: LocalDate? = null,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
        harFlereInntektsmeldinger: Boolean = false,
        avsendersystem: Inntektsmelding.Avsendersystem = Inntektsmelding.Avsendersystem.NAV_NO
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = refusjon,
                orgnummer = organisasjonsnummer,
                fødselsnummer = personidentifikator.toString(),
                aktørId = aktørId,
                førsteFraværsdag = førsteFraværsdag,
                inntektsdato = inntektsdato,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arbeidsforholdId = arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                avsendersystem = avsendersystem,
                mottatt = LocalDateTime.now(),
                aktivitetslogg = aktivitetslogg
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        return inntektsmeldinggenerator()
    }

    internal fun lagInntektsmeldingReplay(vedtaksperiodeId: UUID, sammenhengendePeriode: Periode) =
        inntektsmeldinger.mapNotNull { (_, gen) ->
            val inntektsmelding = gen()
            if (!inntektsmelding.aktuellForReplay(sammenhengendePeriode)) null
            else InntektsmeldingReplay(
                wrapped = inntektsmelding,
                vedtaksperiodeId = vedtaksperiodeId
            )
        }

    internal fun lagInntektsmeldingReplayUtført(vedtaksperiodeId: UUID) =
        InntektsmeldingReplayUtført(UUID.randomUUID(), personidentifikator.toString(), aktørId, organisasjonsnummer, vedtaksperiodeId)

    internal fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        harStatslønn: Boolean = false,
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = UUID.randomUUID(),
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = emptyMap()
            )
        )

    internal fun lagUtbetalingshistorikkEtterInfotrygdendring(
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = UUID.randomUUID(),
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = emptyMap()
            )
        )


    internal fun lagVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        inntektsvurdering: Inntektsvurdering,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            skjæringstidspunkt = skjæringstidspunkt,
            aktørId = aktørId,
            personidentifikator = personidentifikator,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = inntektsvurdering,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        )
    }

    internal fun lagYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: List<Periode> = emptyList(),
        svangerskapspenger: List<Periode> = emptyList(),
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
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
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger),
            aktivitetslogg = Aktivitetslogg()
        )
    }

    internal fun lagSimulering(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fagsystemId: String,
        fagområde: String,
        simuleringOK: Boolean,
        simuleringsresultat: SimuleringResultat?
    ): Simulering {
        return Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            fagområde = fagområde,
            simuleringOK = simuleringOK,
            melding = "",
            utbetalingId = utbetalingId,
            simuleringResultat = simuleringsresultat
        )
    }

    internal fun lagUtbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingGodkjent: Boolean,
        automatiskBehandling: Boolean,
        utbetalingId: UUID
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = personidentifikator.toString(),
        organisasjonsnummer = organisasjonsnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    )

    internal fun lagUtbetalinghendelse(
        utbetalingId: UUID,
        fagsystemId: String,
        status: Oppdragstatus,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId.toString(),
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

    internal fun lagAnnullering(fagsystemId: String) =
        AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            saksbehandlerIdent = "Ola Nordmann",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = LocalDateTime.now()
        )

    internal fun lagIdentOpphørt() =
        IdentOpphørt(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString()
        )

    internal fun lagPåminnelse(vedtaksperiodeId: UUID, tilstand: TilstandType, tilstandsendringstidspunkt: LocalDateTime) =
        Påminnelse(
            UUID.randomUUID(),
            aktørId,
            personidentifikator.toString(),
            organisasjonsnummer,
            vedtaksperiodeId.toString(),
            0,
            tilstand,
            tilstandsendringstidspunkt,
            LocalDateTime.now(),
            LocalDateTime.now()
        )

    internal fun lagGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) =
        Grunnbeløpsregulering(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId,
            personidentifikator.toString(),
            skjæringstidspunkt = skjæringstidspunkt
        )

    internal fun lagHåndterForkastSykmeldingsperioder(periode: Periode) =
        ForkastSykmeldingsperioder(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            periode = periode
        )

    internal fun lagAnmodningOmForkasting(vedtaksperiodeId: UUID) =
        AnmodningOmForkasting(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId
        )

    internal fun lagHåndterOverstyrTidslinje(overstyringsdager: List<ManuellOverskrivingDag>) =
        OverstyrTidslinje(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        )

    internal fun lagOverstyrInntekt(hendelseId: UUID, skjæringstidspunkt: LocalDate, inntekt: Inntekt, orgnummer: String) =
        PersonHendelsefabrikk(aktørId, personidentifikator).lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt, listOf(
            OverstyrtArbeidsgiveropplysning(orgnummer, inntekt, "forklaring", null, emptyList())
        ), meldingsreferanseId = hendelseId)
}