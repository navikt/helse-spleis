package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning.Companion.tilOverstyrt
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.AbstractPersonTest.Companion.UNG_PERSON_FØDSELSDATO
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt


internal class ArbeidsgiverHendelsefabrikk(
    private val aktørId: String,
    private val personidentifikator: Personidentifikator,
    private val organisasjonsnummer: String,
    private val fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO
) {

    private val sykmeldinger = mutableListOf<Sykmelding>()
    private val søknader = mutableListOf<Søknad>()
    private val inntektsmeldinger = mutableMapOf<UUID, () -> Inntektsmelding>()
    private val personopplysninger = Personopplysninger(
        personidentifikator, aktørId, fødselsdato
    )

    internal fun lagSykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        sykmeldingSkrevet: LocalDateTime? = null,
        id: UUID = UUID.randomUUID()
    ): Sykmelding {
        val sykmeldingSkrevetEkte = sykmeldingSkrevet ?: Sykmeldingsperiode.periode(sykeperioder.toList())!!.start.atStartOfDay()
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = personidentifikator.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            sykeperioder = listOf(*sykeperioder),
            sykmeldingSkrevet = sykmeldingSkrevetEkte,
            personopplysninger = personopplysninger
        ).apply {
            sykmeldinger.add(this)
        }
    }

    internal fun lagSøknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: Boolean = false,
        sendtTilNAVEllerArbeidsgiver: LocalDate? = null,
        sykmeldingSkrevet: LocalDateTime? = null,
        id: UUID = UUID.randomUUID(),
        merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(),
        permittert: Boolean = false,
        korrigerer: UUID? = null,
        opprinneligSendt: LocalDate? = null,
        aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
    ): Søknad {
        val innsendt = sendtTilNAVEllerArbeidsgiver ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive
        return Søknad(
            meldingsreferanseId = id,
            fnr = personidentifikator.toString(),
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAVEllerArbeidsgiver = innsendt.atStartOfDay(),
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet = sykmeldingSkrevet ?: Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.start.atStartOfDay(),
            korrigerer = korrigerer,
            opprinneligSendt = opprinneligSendt?.atStartOfDay(),
            aktivitetslogg = aktivitetslogg,
            personopplysninger = personopplysninger
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
        arbeidsforholdId: String? = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
        id: UUID = UUID.randomUUID(),
        aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
        harFlereInntektsmeldinger: Boolean = false
    ): Inntektsmelding {
        val inntektsmeldinggenerator = {
            Inntektsmelding(
                meldingsreferanseId = id,
                refusjon = refusjon,
                orgnummer = organisasjonsnummer,
                fødselsnummer = personidentifikator.toString(),
                aktørId = aktørId,
                førsteFraværsdag = førsteFraværsdag,
                beregnetInntekt = beregnetInntekt,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arbeidsforholdId = arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                mottatt = LocalDateTime.now(),
                aktivitetslogg = aktivitetslogg,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                personopplysninger = personopplysninger
            )
        }
        inntektsmeldinger[id] = inntektsmeldinggenerator
        return inntektsmeldinggenerator()
    }

    internal fun lagInntektsmeldingReplay(vedtaksperiodeId: UUID) =
        inntektsmeldinger.map { (_, gen) ->
            InntektsmeldingReplay(
                wrapped = gen(),
                vedtaksperiodeId = vedtaksperiodeId
            )
        }

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
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = harStatslønn
            )
        )


    internal fun lagVilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        inntektsvurdering: Inntektsvurdering,
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
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
        utbetalinger: List<Infotrygdperiode>,
        inntektshistorikk: List<Inntektsopplysning>,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
    ): Ytelser {
        val element = { meldingsreferanseId: UUID ->
            InfotrygdhistorikkElement.opprett(
                oppdatert = besvart,
                hendelseId = meldingsreferanseId,
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = arbeidskategorikoder,
                ugyldigePerioder = emptyList(),
                harStatslønn = statslønn
            )
        }
        return lagYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, dødsdato, arbeidsavklaringspenger, dagpenger, element)
    }

    internal fun lagYtelser(
        vedtaksperiodeId: UUID,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        element: (UUID) -> InfotrygdhistorikkElement? = { null }
    ): Ytelser {
        val meldingsreferanseId = UUID.randomUUID()

        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            infotrygdhistorikk = element(meldingsreferanseId),
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
            aktivitetslogg = Aktivitetslogg()
        )
    }

    internal fun lagSimulering(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fagsystemId: String,
        fagområde: String,
        simuleringOK: Boolean,
        simuleringsresultat: Simulering.SimuleringResultat?
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

    internal fun lagUtbetalingOverført(utbetalingId: UUID, fagsystemId: String) =
        UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId.toString(),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
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
        lagOverstyrArbeidsgiveropplysninger(hendelseId, skjæringstidspunkt, listOf(
            OverstyrtArbeidsgiveropplysning(orgnummer, inntekt, "forklaring", null, emptyList())
        ))
        /*OverstyrInntekt(
            meldingsreferanseId = hendelseId,
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            inntekt = inntekt,
            skjæringstidspunkt = skjæringstidspunkt,
            forklaring = "test",
            subsumsjon = null
        )*/

    internal fun lagOverstyrArbeidsgiveropplysninger(hendelseId: UUID, skjæringstidspunkt: LocalDate, opplysninger: List<OverstyrtArbeidsgiveropplysning>) =
        OverstyrArbeidsgiveropplysninger(
            meldingsreferanseId = hendelseId,
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = opplysninger.tilOverstyrt(hendelseId, skjæringstidspunkt)
        )
}


internal class OverstyrtArbeidsgiveropplysning(
    private val orgnummer: String,
    private val inntekt: Inntekt,
    private val forklaring: String,
    private val subsumsjon: Subsumsjon?,
    private val refusjonsopplysninger: List<Triple<LocalDate, LocalDate?, Inntekt>>
) {
    internal companion object {
        internal fun List<OverstyrtArbeidsgiveropplysning>.tilOverstyrt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) = map {
            ArbeidsgiverInntektsopplysning(it.orgnummer, Saksbehandler(skjæringstidspunkt, meldingsreferanseId, it.inntekt, it.forklaring, it.subsumsjon, LocalDateTime.now()), Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder()
                .apply {
                it.refusjonsopplysninger.forEach { (fom, tom, refusjonsbeløp) -> leggTil(Refusjonsopplysning(meldingsreferanseId, fom, tom, refusjonsbeløp), LocalDateTime.now())}
            }.build())
        }
    }
}
