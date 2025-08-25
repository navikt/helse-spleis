package no.nav.helse.spleis.e2e

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UNG_PERSON_FØDSELSDATO
import no.nav.helse.dsl.a1
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.Foreldrepenger
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.InntekterForBeregning.Inntektsperiode
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Svangerskapspenger
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingpåminnelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.sisteBehov
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.AvsenderSystem
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status

internal fun AbstractEndToEndTest.utbetaling(
    fagsystemId: String,
    status: Oppdragstatus,
    orgnummer: String = a1,
    meldingsreferanseId: UUID = UUID.randomUUID(),
    utbetalingId: UUID? = null
) =
    UtbetalingHendelse(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        fagsystemId = fagsystemId,
        utbetalingId = utbetalingId
            ?: personlogg.sisteBehov(Aktivitet.Behov.Behovtype.Utbetaling).alleKontekster.getValue("utbetalingId")
                .let { UUID.fromString(it) },
        status = status,
        melding = "hei",
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    )

internal fun AbstractEndToEndTest.feriepengeutbetaling(
    fagsystemId: String,
    status: Oppdragstatus,
    orgnummer: String = a1,
    meldingsreferanseId: UUID = UUID.randomUUID()
) =
    FeriepengeutbetalingHendelse(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        fagsystemId = fagsystemId,
        utbetalingId = personlogg.sisteBehov(Aktivitet.Behov.Behovtype.Feriepengeutbetaling).alleKontekster.getValue("utbetalingId")
            .let { UUID.fromString(it) },
        status = status,
        melding = "hey",
        avstemmingsnøkkel = 654321L,
        overføringstidspunkt = LocalDateTime.now()
    )

internal fun AbstractEndToEndTest.sykmelding(
    id: UUID,
    vararg sykeperioder: Sykmeldingsperiode,
    orgnummer: String = a1,
    sykmeldingSkrevet: LocalDateTime? = null,
    mottatt: LocalDateTime? = null,
    fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO
) = ArbeidsgiverHendelsefabrikk(orgnummer, behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer)).lagSykmelding(
    sykeperioder = sykeperioder,
    id = id
)

internal fun AbstractEndToEndTest.søknad(
    id: UUID,
    vararg perioder: Søknadsperiode,
    andreInntektskilder: Boolean = false,
    sendtTilNAVEllerArbeidsgiver: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    orgnummer: String = a1,
    sykmeldingSkrevet: LocalDateTime? = null,
    utenlandskSykmelding: Boolean = false,
    sendTilGosys: Boolean = false,
    korrigerer: UUID? = null,
    opprinneligSendt: LocalDate? = null,
    merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(),
    permittert: Boolean = false,
    egenmeldinger: List<Periode> = emptyList(),
    inntekterFraNyeArbeidsforhold: Boolean = false
) = ArbeidsgiverHendelsefabrikk(orgnummer, behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer)).lagSøknad(
    perioder = perioder,
    andreInntektskilder = andreInntektskilder,
    sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
    sykmeldingSkrevet = sykmeldingSkrevet ?: Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay(),
    id = id,
    merknaderFraSykmelding = merknaderFraSykmelding,
    permittert = permittert,
    korrigerer = korrigerer,
    utenlandskSykmelding = utenlandskSykmelding,
    sendTilGosys = sendTilGosys,
    opprinneligSendt = opprinneligSendt,
    egenmeldinger = egenmeldinger,
    inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold
)

internal fun AbstractEndToEndTest.inntektsmelding(
    id: UUID = UUID.randomUUID(),
    arbeidsgiverperioder: List<Periode>,
    beregnetInntekt: Inntekt = INNTEKT,
    førsteFraværsdag: LocalDate? = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = a1,
    opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
    harFlereInntektsmeldinger: Boolean = false,
    mottatt: LocalDateTime? = null
): Inntektsmelding {
    val inntektsmeldinggenerator = {
        ArbeidsgiverHendelsefabrikk(orgnummer, behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer)).lagInntektsmelding(
            arbeidsgiverperioder = arbeidsgiverperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            opphørAvNaturalytelser = opphørAvNaturalytelser,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            id = id,
            harFlereInntektsmeldinger = harFlereInntektsmeldinger,
            mottatt = mottatt ?: LocalDateTime.now()
        )
    }
    val kontrakten = no.nav.inntektsmeldingkontrakt.Inntektsmelding(
        inntektsmeldingId = UUID.randomUUID().toString(),
        arbeidstakerFnr = "fnr",
        arbeidstakerAktorId = "aktør",
        virksomhetsnummer = orgnummer,
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
        mottattDato = LocalDateTime.now(),
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        naerRelasjon = null,
        avsenderSystem = AvsenderSystem("SpleisModell"),
        innsenderTelefon = "tlfnr",
        innsenderFulltNavn = "SPLEIS Modell"
    )
    inntektsmeldinger[id] = AbstractEndToEndTest.InnsendtInntektsmelding(LocalDateTime.now(), inntektsmeldinggenerator, kontrakten)
    EtterspurtBehov.fjern(ikkeBesvarteBehov, orgnummer, Aktivitet.Behov.Behovtype.Sykepengehistorikk)
    return inntektsmeldinggenerator()
}

internal fun AbstractEndToEndTest.vilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    skjæringstidspunkt: LocalDate,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = a1,
    arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017), type = Arbeidsforholdtype.ORDINÆRT)),
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering,
): Vilkårsgrunnlag {
    return Vilkårsgrunnlag(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
        inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering,
        arbeidsforhold = arbeidsforhold
    )
}

internal fun utbetalingpåminnelse(
    utbetalingId: UUID,
    status: Utbetalingstatus,
    tilstandsendringstidspunkt: LocalDateTime,
    orgnummer: String = a1
): Utbetalingpåminnelse {
    return Utbetalingpåminnelse(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        utbetalingId = utbetalingId,
        antallGangerPåminnet = 0,
        status = status,
        endringstidspunkt = tilstandsendringstidspunkt,
        påminnelsestidspunkt = LocalDateTime.now()
    )
}

internal fun sykepengegrunnlagForArbeidsgiver(
    skjæringstidspunkt: LocalDate = 1.januar,
    orgnummer: String = a1,
): SykepengegrunnlagForArbeidsgiver {
    return SykepengegrunnlagForArbeidsgiver(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        inntekter = ArbeidsgiverInntekt(orgnummer, (1..3).map {
            ArbeidsgiverInntekt.MånedligInntekt(
                yearMonth = skjæringstidspunkt.yearMonth.minusMonths(it.toLong()),
                inntekt = INNTEKT,
                type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                fordel = "",
                beskrivelse = ""
            )
        })
    )
}

internal fun påminnelse(
    vedtaksperiodeId: UUID,
    påminnetTilstand: TilstandType,
    tilstandsendringstidspunkt: LocalDateTime,
    nå: LocalDateTime = LocalDateTime.now(),
    orgnummer: String = a1,
    antallGangerPåminnet: Int = 1,
    flagg: Set<String> = emptySet()
): Påminnelse {
    return Påminnelse(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        antallGangerPåminnet = antallGangerPåminnet,
        tilstand = påminnetTilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        nå = nå,
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        flagg = flagg,
        opprettet = LocalDateTime.now()
    )
}

internal fun anmodningOmForkasting(
    vedtaksperiodeId: UUID,
    force: Boolean = false,
    orgnummer: String = a1
): AnmodningOmForkasting {
    return AnmodningOmForkasting(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        force = force
    )
}

internal fun AbstractEndToEndTest.utbetalingshistorikk(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    utbetalinger: List<Infotrygdperiode> = listOf(),
    orgnummer: String = a1,
    besvart: LocalDateTime = LocalDateTime.now(),
): Utbetalingshistorikk {
    return Utbetalingshistorikk(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer),
        besvart = LocalDateTime.now(),
        element = InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = MeldingsreferanseId(UUID.randomUUID()),
            perioder = utbetalinger
        )
    )
}

internal fun AbstractEndToEndTest.utbetalingshistorikkEtterInfotrygdEndring(
    meldingsreferanseId: UUID = UUID.randomUUID(),
    utbetalinger: List<Infotrygdperiode> = listOf(),
    besvart: LocalDateTime = LocalDateTime.now(),
): UtbetalingshistorikkEtterInfotrygdendring {
    return UtbetalingshistorikkEtterInfotrygdendring(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        element = InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = MeldingsreferanseId(meldingsreferanseId),
            perioder = utbetalinger
        ),
        besvart = LocalDateTime.now()
    )
}

internal fun AbstractEndToEndTest.utbetalingshistorikkForFeriepenger(
    utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
    feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
    arbeidskategorikoder: UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder =
        UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf(
                UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                    LocalDate.MIN til LocalDate.MAX,
                    UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
                )
            )
        ),
    opptjeningsår: Year = Year.of(2017),
    datoForSisteFeriepengekjøringIInfotrygd: LocalDate,
    skalBeregnesManuelt: Boolean
): UtbetalingshistorikkForFeriepenger {
    return UtbetalingshistorikkForFeriepenger(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        utbetalinger = utbetalinger,
        feriepengehistorikk = feriepengehistorikk,
        arbeidskategorikoder = arbeidskategorikoder,
        opptjeningsår = opptjeningsår,
        skalBeregnesManuelt = skalBeregnesManuelt,
        datoForSisteFeriepengekjøringIInfotrygd = datoForSisteFeriepengekjøringIInfotrygd
    )
}

internal fun AbstractEndToEndTest.ytelser(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    foreldrepenger: List<GradertPeriode> = emptyList(),
    svangerskapspenger: List<GradertPeriode> = emptyList(),
    pleiepenger: List<GradertPeriode> = emptyList(),
    omsorgspenger: List<GradertPeriode> = emptyList(),
    opplæringspenger: List<GradertPeriode> = emptyList(),
    institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
    orgnummer: String = a1,
    arbeidsavklaringspenger: List<Periode> = emptyList(),
    inntekterForBeregning: List<Inntektsperiode> = emptyList(),
    dagpenger: List<Periode> = emptyList(),
): Ytelser {
    val meldingsreferanseId = UUID.randomUUID()
    return Ytelser(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        foreldrepenger = Foreldrepenger(
            foreldrepengeytelse = foreldrepenger
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

internal fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
internal fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
internal fun manuellForeldrepengedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Foreldrepengerdag)
internal fun manuellSykedag(dato: LocalDate, grad: Int = 100) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, grad)
internal fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)

internal fun AbstractEndToEndTest.simulering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    simuleringOK: Boolean = true,
    orgnummer: String = a1,
    simuleringsresultat: SimuleringResultatDto? = standardSimuleringsresultat(orgnummer)
) = personlogg.etterspurteBehov(vedtaksperiodeIdInnhenter, orgnummer).filter { it.type == Aktivitet.Behov.Behovtype.Simulering }.map { simuleringsBehov ->
    Simulering(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        fagsystemId = simuleringsBehov.detaljer().getValue("fagsystemId") as String,
        fagområde = simuleringsBehov.detaljer().getValue("fagområde") as String,
        simuleringOK = simuleringOK,
        melding = "",
        utbetalingId = UUID.fromString(simuleringsBehov.alleKontekster.getValue("utbetalingId")),
        simuleringsResultat = simuleringsresultat
    )
}

internal fun standardSimuleringsresultat(orgnummer: String, totalbeløp: Int = 2000) = SimuleringResultatDto(
    totalbeløp = totalbeløp,
    perioder = listOf(
        SimuleringResultatDto.SimulertPeriode(
            fom = 17.januar,
            tom = 20.januar,
            utbetalinger = listOf(
                SimuleringResultatDto.SimulertUtbetaling(
                    forfallsdato = 21.januar,
                    utbetalesTil = SimuleringResultatDto.Mottaker(
                        id = orgnummer,
                        navn = "Org Orgesen AS"
                    ),
                    feilkonto = false,
                    detaljer = listOf(
                        SimuleringResultatDto.Detaljer(
                            fom = 17.januar,
                            tom = 20.januar,
                            konto = "81549300",
                            beløp = 2000,
                            klassekode = SimuleringResultatDto.Klassekode(
                                kode = "SPREFAG-IOP",
                                beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                            ),
                            uføregrad = 100,
                            utbetalingstype = "YTEL",
                            tilbakeføring = false,
                            sats = SimuleringResultatDto.Sats(
                                sats = 1000.toDouble(),
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

internal fun AbstractEndToEndTest.utbetalingsgodkjenning(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    utbetalingGodkjent: Boolean,
    orgnummer: String,
    automatiskBehandling: Boolean,
    utbetalingId: UUID = UUID.fromString(
        personlogg.sisteBehov(Aktivitet.Behov.Behovtype.Godkjenning).alleKontekster["utbetalingId"]
            ?: throw IllegalStateException(
                "Finner ikke utbetalingId i: ${
                    personlogg.sisteBehov(Aktivitet.Behov.Behovtype.Godkjenning).alleKontekster
                }"
            )
    ),
) = Utbetalingsgodkjenning(
    meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
    behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
    utbetalingId = utbetalingId,
    vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
    saksbehandler = "Ola Nordmann",
    saksbehandlerEpost = "ola.nordmann@nav.no",
    utbetalingGodkjent = utbetalingGodkjent,
    godkjenttidspunkt = LocalDateTime.now(),
    automatiskBehandling = automatiskBehandling,
)
