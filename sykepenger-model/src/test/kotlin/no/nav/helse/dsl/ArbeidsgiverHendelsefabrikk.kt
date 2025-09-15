package no.nav.helse.dsl

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Foreldrepenger
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsendringer
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.fraInnteksmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.KanIkkeBehandlesHer
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
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
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status

internal class ArbeidsgiverHendelsefabrikk(
    private val organisasjonsnummer: String,
    private val behandlingsporing: Behandlingsporing.Yrkesaktivitet
) {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()
    private val inntektsmeldinger = mutableMapOf<UUID, AbstractEndToEndTest.InnsendtInntektsmelding>()

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID()
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = MeldingsreferanseId(id),
            behandlingsporing = behandlingsporing,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            sykmeldinger.add(this)
        }
    }

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
        pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null
    ): Søknad {
        val innsendt = (sendtTilNAVEllerArbeidsgiver ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive).let {
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
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay(),
            opprinneligSendt = opprinneligSendt?.atStartOfDay(),
            utenlandskSykmelding = utenlandskSykmelding,
            arbeidUtenforNorge = arbeidUtenforNorge,
            sendTilGosys = sendTilGosys,
            yrkesskade = yrkesskade,
            egenmeldinger = egenmeldinger,
            arbeidssituasjon = arbeidssituasjon,
            registrert = registrert,
            inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter
        ).apply {
            søknader.add(this)
        }
    }

    fun lagAvbruttSøknad(sykmeldingsperiode: Periode): AvbruttSøknad =
        AvbruttSøknad(sykmeldingsperiode, MeldingsreferanseId(UUID.randomUUID()), behandlingsporing)

    internal fun lagInntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOf { it.start },
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        harFlereInntektsmeldinger: Boolean = false,
        mottatt: LocalDateTime = LocalDateTime.now()
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = MeldingsreferanseId(id),
                refusjon = refusjon,
                behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = organisasjonsnummer),
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                begrunnelseForReduksjonEllerIkkeUtbetalt = fraInnteksmelding(begrunnelseForReduksjonEllerIkkeUtbetalt),
                opphørAvNaturalytelser = opphørAvNaturalytelser,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                førsteFraværsdag = førsteFraværsdag,
                mottatt = mottatt
            )
        }
        val kontrakten = no.nav.inntektsmeldingkontrakt.Inntektsmelding(
            inntektsmeldingId = UUID.randomUUID().toString(),
            arbeidstakerFnr = "fnr",
            arbeidstakerAktorId = "aktør",
            virksomhetsnummer = organisasjonsnummer,
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = BigDecimal.valueOf(beregnetInntekt.månedlig),
            refusjon = Refusjon(BigDecimal.valueOf(beregnetInntekt.månedlig), null),
            endringIRefusjoner = emptyList(),
            opphoerAvNaturalytelser = emptyList(),
            gjenopptakelseNaturalytelser = emptyList(),
            arbeidsgiverperioder = arbeidsgiverperioder.map {
                no.nav.inntektsmeldingkontrakt.Periode(it.start, it.endInclusive)
            },
            status = Status.GYLDIG,
            arkivreferanse = "",
            ferieperioder = emptyList(),
            foersteFravaersdag = førsteFraværsdag,
            mottattDato = mottatt,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            naerRelasjon = null,
            avsenderSystem = AvsenderSystem("SpleisModell"),
            innsenderTelefon = "tlfnr",
            innsenderFulltNavn = "SPLEIS Modell"
        )
        inntektsmeldinger[id] = AbstractEndToEndTest.InnsendtInntektsmelding(
            tidspunkt = LocalDateTime.now(),
            generator = inntektsmeldinggenerator,
            inntektsmeldingkontrakt = kontrakten
        )
        return inntektsmeldinggenerator()
    }

    internal fun lagArbeidsgiveropplysninger(
        meldingsreferanseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        innsendt: LocalDateTime = LocalDateTime.now(),
        registrert: LocalDateTime = innsendt.plusSeconds(1),
        vararg opplysninger: Arbeidsgiveropplysning
    ) = Arbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        innsendt = innsendt,
        registrert = registrert,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer = organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = opplysninger.toList()
    )

    internal fun lagKorrigerteArbeidsgiveropplysninger(
        meldingsreferanseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        innsendt: LocalDateTime = LocalDateTime.now(),
        registrert: LocalDateTime = innsendt.plusSeconds(1),
        vararg opplysninger: Arbeidsgiveropplysning
    ) = KorrigerteArbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        innsendt = innsendt,
        registrert = registrert,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = opplysninger.toList()
    )

    internal fun lagInntektsmeldingReplay(forespørsel: Forespørsel, håndterteInntektsmeldinger: Set<UUID>) =
        InntektsmeldingerReplay(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = organisasjonsnummer
            ),
            vedtaksperiodeId = forespørsel.vedtaksperiodeId,
            inntektsmeldinger = inntektsmeldinger
                .filter { forespørsel.erInntektsmeldingRelevant(it.value.inntektsmeldingkontrakt) }
                .map { (_, im) -> im.generator() }
                .filterNot { it.metadata.meldingsreferanseId.id in håndterteInntektsmeldinger }
        )

    internal fun lagUtbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) =
        Utbetalingshistorikk(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
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

    internal fun lagSykepengegrunnlagForArbeidsgiver(skjæringstidspunkt: LocalDate, inntekter: List<ArbeidsgiverInntekt.MånedligInntekt>): SykepengegrunnlagForArbeidsgiver {
        return SykepengegrunnlagForArbeidsgiver(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
            inntekter = ArbeidsgiverInntekt(organisasjonsnummer, inntekter)
        )
    }

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
            behandlingsporing = behandlingsporing,
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
        dagpenger: List<Periode> = emptyList(),
        inntekterForBeregning: List<InntekterForBeregning.Inntektsperiode> = emptyList()
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            behandlingsporing = behandlingsporing,
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
            dagpenger = Dagpenger(dagpenger),
            inntekterForBeregning = InntekterForBeregning(inntekterForBeregning)
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
            behandlingsporing = behandlingsporing,
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
        behandlingsporing = behandlingsporing,
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
        behandlingsporing = behandlingsporing,
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
        behandlingsporing = behandlingsporing,
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
            behandlingsporing = behandlingsporing,
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
            behandlingsporing = behandlingsporing,
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

    internal fun lagPåminnelse(
        vedtaksperiodeId: UUID,
        tilstand: TilstandType,
        tilstandsendringstidspunkt: LocalDateTime,
        nåtidspunkt: LocalDateTime = LocalDateTime.now(),
        flagg: Set<String> = emptySet()
    ) =
        Påminnelse(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = tilstand,
            tilstandsendringstidspunkt = tilstandsendringstidspunkt,
            nå = nåtidspunkt,
            påminnelsestidspunkt = nåtidspunkt,
            nestePåminnelsestidspunkt = nåtidspunkt,
            opprettet = nåtidspunkt,
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
            behandlingsporing = behandlingsporing,
            periode = periode
        )

    internal fun lagAnmodningOmForkasting(vedtaksperiodeId: UUID, force: Boolean = false) =
        AnmodningOmForkasting(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
            vedtaksperiodeId = vedtaksperiodeId,
            force = force
        )

    internal fun lagHåndterOverstyrTidslinje(overstyringsdager: List<ManuellOverskrivingDag>) =
        OverstyrTidslinje(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = behandlingsporing,
            dager = overstyringsdager,
            opprettet = LocalDateTime.now()
        )

    internal fun lagOverstyrInntekt(hendelseId: UUID, skjæringstidspunkt: LocalDate, inntekt: Inntekt, orgnummer: String, tidsstempel: LocalDateTime = LocalDateTime.now()) =
        PersonHendelsefabrikk().lagOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(orgnummer, inntekt, emptyList())),
            meldingsreferanseId = hendelseId,
            tidsstempel = tidsstempel
        )

    internal fun lagInntektsendringer(inntektsendringerFom: LocalDate) = Inntektsendringer(MeldingsreferanseId(UUID.randomUUID()), inntektsendringerFom)
}
