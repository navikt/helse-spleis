package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektCreator
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sisteBehov
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.fail

internal fun AbstractEndToEndTest.utbetaling(
    fagsystemId: String,
    status: Oppdragstatus,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    meldingsreferanseId: UUID = UUID.randomUUID(),
    utbetalingId: UUID? = null
) =
    UtbetalingHendelse(
        meldingsreferanseId = meldingsreferanseId,
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = fagsystemId,
        utbetalingId = utbetalingId?.toString() ?: person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).kontekst().getValue("utbetalingId"),
        status = status,
        melding = "hei",
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    ).apply {
        hendelselogg = this
    }

internal fun AbstractEndToEndTest.feriepengeutbetaling(
    fagsystemId: String,
    status: Oppdragstatus,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    meldingsreferanseId: UUID = UUID.randomUUID()
) =
    UtbetalingHendelse(
        meldingsreferanseId = meldingsreferanseId,
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = fagsystemId,
        utbetalingId = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).kontekst().getValue("utbetalingId"),
        status = status,
        melding = "hey",
        avstemmingsnøkkel = 654321L,
        overføringstidspunkt = LocalDateTime.now()
    ).apply {
        hendelselogg = this
    }

internal fun AbstractEndToEndTest.sykmelding(
    id: UUID,
    vararg sykeperioder: Sykmeldingsperiode,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    sykmeldingSkrevet: LocalDateTime? = null,
    mottatt: LocalDateTime? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): Sykmelding {
    return Sykmelding(
        meldingsreferanseId = id,
        fnr = fnr.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        orgnummer = orgnummer,
        sykeperioder = listOf(*sykeperioder),
        sykmeldingSkrevet = sykmeldingSkrevet ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
        mottatt = mottatt ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
    ).apply {
        hendelselogg = this
    }
}

internal fun AbstractEndToEndTest.søknad(
    id: UUID,
    vararg perioder: Søknadsperiode,
    andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
    sendtTilNAVEllerArbeidsgiver: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    sykmeldingSkrevet: LocalDateTime? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
): Søknad {
    return Søknad(
        meldingsreferanseId = id,
        fnr = fnr.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        orgnummer = orgnummer,
        perioder = listOf(*perioder),
        andreInntektskilder = andreInntektskilder,
        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = sykmeldingSkrevet ?: Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay()
    ).apply {
        hendelselogg = this
    }
}

internal fun AbstractEndToEndTest.inntektsmeldingReplay(
    inntektsmelding: Inntektsmelding,
    vedtaksperiodeId: UUID
): InntektsmeldingReplay {
    return InntektsmeldingReplay(
        wrapped = inntektsmelding,
        vedtaksperiodeId = vedtaksperiodeId
    ).apply {
        hendelselogg = this
    }
}

internal fun AbstractEndToEndTest.inntektsmelding(
    id: UUID,
    arbeidsgiverperioder: List<Periode>,
    beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    harOpphørAvNaturalytelser: Boolean = false,
    arbeidsforholdId: String? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null
): Inntektsmelding {
    val inntektsmeldinggenerator = {
        Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = refusjon,
            orgnummer = orgnummer,
            fødselsnummer = fnr.toString(),
            aktørId = AbstractPersonTest.AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
            mottatt = LocalDateTime.now()
        )
    }
    inntektsmeldinger[id] = inntektsmeldinggenerator
    inntekter[id] = beregnetInntekt
    EtterspurtBehov.fjern(ikkeBesvarteBehov, orgnummer, Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk)
    return inntektsmeldinggenerator().apply { hendelselogg = this }
}

internal fun AbstractEndToEndTest.vilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017))),
    inntektsvurdering: Inntektsvurdering,
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
): Vilkårsgrunnlag {
    return Vilkårsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr,
        orgnummer = orgnummer,
        inntektsvurdering = inntektsvurdering,
        medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
        arbeidsforhold = arbeidsforhold
    ).apply {
        hendelselogg = this
    }
}

internal fun utbetalingpåminnelse(
    utbetalingId: UUID,
    status: Utbetalingstatus,
    tilstandsendringstidspunkt: LocalDateTime,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
): Utbetalingpåminnelse {
    return Utbetalingpåminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        utbetalingId = utbetalingId,
        antallGangerPåminnet = 0,
        status = status,
        endringstidspunkt = tilstandsendringstidspunkt,
        påminnelsestidspunkt = LocalDateTime.now()
    )
}

internal fun påminnelse(
    vedtaksperiodeId: UUID,
    påminnetTilstand: TilstandType,
    tilstandsendringstidspunkt: LocalDateTime,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    antallGangerPåminnet: Int = 1
): Påminnelse {
    return Påminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        antallGangerPåminnet = antallGangerPåminnet,
        tilstand = påminnetTilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    )
}

internal fun AbstractEndToEndTest.utbetalingshistorikk(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    utbetalinger: List<Infotrygdperiode> = listOf(),
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    harStatslønn: Boolean = false,
    besvart: LocalDateTime = LocalDateTime.now(),
): Utbetalingshistorikk {
    return Utbetalingshistorikk(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        arbeidskategorikoder = emptyMap(),
        harStatslønn = harStatslønn,
        perioder = utbetalinger,
        inntektshistorikk = inntektshistorikk,
        ugyldigePerioder = emptyList(),
        besvart = besvart
    ).apply {
        hendelselogg = this
    }
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
    skalBeregnesManuelt: Boolean
): UtbetalingshistorikkForFeriepenger {
    return UtbetalingshistorikkForFeriepenger(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        utbetalinger = utbetalinger,
        feriepengehistorikk = feriepengehistorikk,
        arbeidskategorikoder = arbeidskategorikoder,
        opptjeningsår = opptjeningsår,
        skalBeregnesManuelt = skalBeregnesManuelt
    ).apply {
        hendelselogg = this
    }
}

internal fun AbstractEndToEndTest.ytelser(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    utbetalinger: List<Infotrygdperiode> = listOf(),
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    foreldrepenger: Periode? = null,
    svangerskapspenger: Periode? = null,
    pleiepenger: List<Periode> = emptyList(),
    omsorgspenger: List<Periode> = emptyList(),
    opplæringspenger: List<Periode> = emptyList(),
    institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    dødsdato: LocalDate? = null,
    statslønn: Boolean = false,
    arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
    arbeidsavklaringspenger: List<Periode> = emptyList(),
    dagpenger: List<Periode> = emptyList(),
    besvart: LocalDateTime = LocalDateTime.now(),
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
): Ytelser {
    val aktivitetslogg = Aktivitetslogg()
    val meldingsreferanseId = UUID.randomUUID()

    val bedtOmSykepengehistorikk = erEtterspurt(
        Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer,
        TilstandType.AVVENTER_HISTORIKK
    )
        || erEtterspurt(
        Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer,
        TilstandType.AVVENTER_HISTORIKK_REVURDERING
    )
    if (bedtOmSykepengehistorikk) assertEtterspurt(
        Ytelser::class,
        Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk,
        vedtaksperiodeIdInnhenter,
        orgnummer
    )
    val harSpesifisertSykepengehistorikk = utbetalinger.isNotEmpty() || arbeidskategorikoder.isNotEmpty()

    if (!bedtOmSykepengehistorikk && harSpesifisertSykepengehistorikk) {
        fail(
            "Vedtaksperiode ${vedtaksperiodeIdInnhenter.id(orgnummer)} har ikke bedt om Sykepengehistorikk" +
                "\nfordi den har gjenbrukt Infotrygdhistorikk-cache." +
                "\nTrenger ikke sende inn utbetalinger og inntektsopplysninger da." +
                "\nEnten ta bort overflødig historikk, eller sett 'besvart'-tidspunktet tilbake i tid " +
                "på forrige Ytelser-innsending" +
                "\n\n${person.personLogg}"
        )
    }

    val utbetalingshistorikk = if (!bedtOmSykepengehistorikk)
        null
    else
        Utbetalingshistorikk(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AbstractPersonTest.AKTØRID,
            fødselsnummer = fnr.toString(),
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
            arbeidskategorikoder = arbeidskategorikoder,
            harStatslønn = statslønn,
            perioder = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            ugyldigePerioder = emptyList(),
            besvart = besvart
        )
    return Ytelser(
        meldingsreferanseId = meldingsreferanseId,
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        utbetalingshistorikk = utbetalingshistorikk,
        foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = foreldrepenger,
            svangerskapsytelse = svangerskapspenger,
            aktivitetslogg = aktivitetslogg
        ),
        pleiepenger = Pleiepenger(
            perioder = pleiepenger,
            aktivitetslogg = aktivitetslogg
        ),
        omsorgspenger = Omsorgspenger(
            perioder = omsorgspenger,
            aktivitetslogg = aktivitetslogg
        ),
        opplæringspenger = Opplæringspenger(
            perioder = opplæringspenger,
            aktivitetslogg = aktivitetslogg
        ),
        institusjonsopphold = Institusjonsopphold(
            perioder = institusjonsoppholdsperioder,
            aktivitetslogg = aktivitetslogg
        ),
        dødsinfo = Dødsinfo(dødsdato),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
        dagpenger = Dagpenger(dagpenger),
        aktivitetslogg = aktivitetslogg
    ).apply {
        hendelselogg = this
    }
}

internal fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
internal fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
internal fun manuellSykedag(dato: LocalDate, grad: Int = 100) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, grad)
internal fun manuellArbeidsgiverdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Egenmeldingsdag)


internal fun AbstractEndToEndTest.simulering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    simuleringOK: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    simuleringsresultat: Simulering.SimuleringResultat? = standardSimuleringsresultat(orgnummer)
) = person.personLogg.etterspurteBehov(vedtaksperiodeIdInnhenter, orgnummer).filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering }.map { simuleringsBehov ->
    Simulering(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = simuleringsBehov.detaljer().getValue("fagsystemId") as String,
        fagområde = simuleringsBehov.detaljer().getValue("fagområde") as String,
        simuleringOK = simuleringOK,
        melding = "",
        utbetalingId = UUID.fromString(simuleringsBehov.kontekst().getValue("utbetalingId")),
        simuleringResultat = simuleringsresultat
    ).apply {
        hendelselogg = this
    }
}

internal fun standardSimuleringsresultat(orgnummer: String) = Simulering.SimuleringResultat(
    totalbeløp = 2000,
    perioder = listOf(
        Simulering.SimulertPeriode(
            periode = Periode(17.januar, 20.januar),
            utbetalinger = listOf(
                Simulering.SimulertUtbetaling(
                    forfallsdato = 21.januar,
                    utbetalesTil = Simulering.Mottaker(
                        id = orgnummer,
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
                            utbetalingstype = "YTEL",
                            tilbakeføring = false,
                            sats = Simulering.Sats(
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
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String,
    automatiskBehandling: Boolean,
    utbetalingId: UUID = UUID.fromString(
        person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()["utbetalingId"]
            ?: throw IllegalStateException(
                "Finner ikke utbetalingId i: ${
                    person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()
                }"
            )
    ),
) = Utbetalingsgodkjenning(
    meldingsreferanseId = UUID.randomUUID(),
    aktørId = AbstractPersonTest.AKTØRID,
    fødselsnummer = fnr.toString(),
    organisasjonsnummer = orgnummer,
    utbetalingId = utbetalingId,
    vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
    saksbehandler = "Ola Nordmann",
    saksbehandlerEpost = "ola.nordmann@nav.no",
    utbetalingGodkjent = utbetalingGodkjent,
    godkjenttidspunkt = LocalDateTime.now(),
    automatiskBehandling = automatiskBehandling,
).apply {
    hendelselogg = this
}

internal fun grunnlag(
    orgnummer: String,
    skjæringstidspunkt: LocalDate,
    inntekter: List<Inntekt>
) = lagMånedsinntekter(orgnummer, skjæringstidspunkt, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sykepengegrunnlag)

internal fun sammenligningsgrunnlag(
    orgnummer: String,
    skjæringstidspunkt: LocalDate,
    inntekter: List<Inntekt>
) = lagMånedsinntekter(orgnummer, skjæringstidspunkt, inntekter, creator = ArbeidsgiverInntekt.MånedligInntekt::Sammenligningsgrunnlag)

private fun lagMånedsinntekter(
    orgnummer: String,
    skjæringstidspunkt: LocalDate,
    inntekter: List<Inntekt>,
    creator: InntektCreator
) = ArbeidsgiverInntekt(
    orgnummer, inntekter.mapIndexed { index, inntekt ->
        val sluttMnd = YearMonth.from(skjæringstidspunkt)
        creator(
            sluttMnd.minusMonths((inntekter.size - index).toLong()),
            inntekt,
            ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
            "Juidy inntekt",
            "Juidy fordel"
        )
    }
)

internal fun Inntektperioder.lagInntektperioder(
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fom: LocalDate,
    inntekt: Inntekt = AbstractEndToEndTest.INNTEKT
) =
    fom.minusYears(1) til fom.minusMonths(1) inntekter {
        orgnummer inntekt inntekt
    }
