package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.PersonHendelsefabrikk
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.lagStandardInntekterForOpptjeningsvurdering
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.sisteBehov
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning.Companion.refusjonstidslinjer
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning.Companion.tilOverstyrt
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning.Companion.tilSkjønnsmessigFastsatt
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue

internal fun AbstractEndToEndTest.håndterSykmelding(
    periode: Periode,
    orgnummer: String = a1
) = håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = orgnummer)

internal fun AbstractEndToEndTest.håndterSykmelding(
    vararg sykeperioder: Sykmeldingsperiode,
    sykmeldingSkrevet: LocalDateTime? = null,
    mottatt: LocalDateTime? = null,
    id: UUID = UUID.randomUUID(),
    orgnummer: String = a1
): UUID {
    sykmelding(
        id,
        *sykeperioder,
        sykmeldingSkrevet = sykmeldingSkrevet,
        mottatt = mottatt,
        orgnummer = orgnummer
    ).håndter(Person::håndterSykmelding)
    return id
}

internal fun AbstractEndToEndTest.håndterAvbrytSøknad(
    periode: Periode,
    orgnummer: String,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    AvbruttSøknad(
        periode,
        MeldingsreferanseId(meldingsreferanseId),
        Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer)
    ).håndter(Person::håndterAvbruttSøknad)
}

internal fun AbstractEndToEndTest.håndterAvbrytArbeidsledigSøknad(
    periode: Periode,
    orgnummer: String,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    AvbruttSøknad(
        periode,
        MeldingsreferanseId(meldingsreferanseId),
        Behandlingsporing.Yrkesaktivitet.Arbeidsledig
    ).håndter(Person::håndterAvbruttSøknad)
}

internal fun AbstractEndToEndTest.tilGodkjenning(
    periode: Periode,
    vararg organisasjonsnummere: String,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null),
    arbeidsgiverperiode: List<Periode> = listOf(Periode(periode.start, periode.start.plusDays(15))),
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode
) {
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnummere.forEach { nyPeriode(periode, it) }
    organisasjonsnummere.forEach {
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = arbeidsgiverperiode,
            beregnetInntekt = beregnetInntekt,
            orgnummer = it,
            id = inntektsmeldingId,
            vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
            refusjon = refusjon
        )
    }

    organisasjonsnummere.first().let { organisasjonsnummer ->
        val vedtaksperiode = observatør.sisteVedtaksperiode()
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = vedtaksperiode,
            orgnummer = organisasjonsnummer
        )
        this@tilGodkjenning.håndterYtelser(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(vedtaksperiode, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyeVedtak(
    periode: Periode,
    vararg organisasjonsnummere: String,
    inntekt: Inntekt = INNTEKT
) {
    val vedtaksperiode = observatør.sisteVedtaksperiode()
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    tilGodkjenning(periode, *organisasjonsnummere, beregnetInntekt = inntekt)
    val (første, resten) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

    første.let { organisasjonsnummer ->
        this@nyeVedtak.håndterUtbetalingsgodkjenning(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }

    resten.forEach { organisasjonsnummer ->
        this@nyeVedtak.håndterYtelser(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(vedtaksperiode, orgnummer = organisasjonsnummer)
        this@nyeVedtak.håndterUtbetalingsgodkjenning(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.førstegangTilGodkjenning(
    periode: Periode,
    vararg arbeidsgivere: Pair<String, IdInnhenter>,
) {
    require(arbeidsgivere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    arbeidsgivere.forEach {
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = it.first)
    }
    arbeidsgivere.forEach {
        håndterSøknad(Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = it.first)

    }
    arbeidsgivere.forEach {
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            beregnetInntekt = INNTEKT,
            orgnummer = it.first,
            vedtaksperiodeIdInnhenter = it.second
        )
    }

    val vedtaksperiode = observatør.sisteVedtaksperiode()

    arbeidsgivere.first().let { arbeidsgiver ->
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = vedtaksperiode,
            orgnummer = arbeidsgiver.first
        )
        this@førstegangTilGodkjenning.håndterYtelser(vedtaksperiode, orgnummer = arbeidsgiver.first)
        håndterSimulering(vedtaksperiode, orgnummer = arbeidsgiver.first)
    }
}

internal fun AbstractEndToEndTest.forlengelseTilGodkjenning(periode: Periode, vararg organisasjonsnumre: String) {
    require(organisasjonsnumre.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnumre.forEach { nyPeriode(periode, it) }
    this@forlengelseTilGodkjenning.håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnumre.first())
    håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnumre.first())
}

internal fun AbstractEndToEndTest.forlengVedtak(periode: Periode, vararg organisasjonsnumre: String) {
    require(organisasjonsnumre.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnumre.forEach { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = it) }
    organisasjonsnumre.forEach { håndterSøknad(Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = it) }
    organisasjonsnumre.forEach { organisasjonsnummer ->
        this@forlengVedtak.håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        this@forlengVedtak.håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyttVedtak(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode
) {
    tilGodkjent(periode, grad, orgnummer = orgnummer, beregnetInntekt = beregnetInntekt, refusjon = refusjon, arbeidsgiverperiode = arbeidsgiverperiode, inntektsmeldingId = inntektsmeldingId, vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter)
    håndterUtbetalt(status = status, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.tilGodkjent(
    periode: Periode,
    grad: Prosentdel,
    orgnummer: String = a1,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode
): IdInnhenter {
    val id = tilGodkjenning(
        periode = periode,
        grad = grad,
        orgnummer = orgnummer,
        refusjon = refusjon,
        arbeidsgiverperiode = arbeidsgiverperiode,
        beregnetInntekt = beregnetInntekt,
        inntektsmeldingId = inntektsmeldingId,
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter
    )
    this@tilGodkjent.håndterUtbetalingsgodkjenning(id, true, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilGodkjenning(
    periode: Periode,
    grad: Prosentdel,
    orgnummer: String = a1,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode
): IdInnhenter {
    val id = tilSimulering(periode, grad, orgnummer = orgnummer, beregnetInntekt = beregnetInntekt, refusjon = refusjon, arbeidsgiverperiode = arbeidsgiverperiode, inntektsmeldingId = inntektsmeldingId, vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter)
    håndterSimulering(id, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilSimulering(
    periode: Periode,
    grad: Prosentdel,
    orgnummer: String = a1,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode
): IdInnhenter {
    return tilYtelser(periode, grad, orgnummer = orgnummer, beregnetInntekt = beregnetInntekt, refusjon = refusjon, arbeidsgiverperiode = arbeidsgiverperiode, inntektsmeldingId = inntektsmeldingId, vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter)
}

internal fun AbstractEndToEndTest.tilYtelser(
    periode: Periode,
    grad: Prosentdel,
    orgnummer: String = a1,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    inntektsmeldingId: UUID = UUID.randomUUID(),
    vedtaksperiodeIdInnhenter: IdInnhenter
): IdInnhenter {
    håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = orgnummer)
    håndterSøknad(Søknadsperiode.Sykdom(periode.start, periode.endInclusive, grad), orgnummer = orgnummer)
    håndterArbeidsgiveropplysninger(
        arbeidsgiverperiode ?: listOf(Periode(periode.start, periode.start.plusDays(15))),
        beregnetInntekt = beregnetInntekt,
        refusjon = refusjon,
        orgnummer = orgnummer,
        id = inntektsmeldingId,
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter
    )
    val id = observatør.sisteVedtaksperiode()
    håndterVilkårsgrunnlag(vedtaksperiodeIdInnhenter = id, orgnummer = orgnummer)
    this@tilYtelser.håndterYtelser(id, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.forlengTilGodkjentVedtak(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1
) {
    forlengTilGodkjenning(periode, grad, orgnummer)
    this@forlengTilGodkjentVedtak.håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), true, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengTilSimulering(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1
) {
    nyPeriode(periode, orgnummer, grad = grad)
    val id: IdInnhenter = observatør.sisteVedtaksperiode()
    this@forlengTilSimulering.håndterYtelser(id, orgnummer = orgnummer)
    assertTrue(personlogg.etterspurteBehov(id, Behovtype.Simulering, orgnummer)) { "Forventet at simulering er etterspurt" }
}

internal fun AbstractEndToEndTest.forlengTilGodkjenning(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1
) {
    nyPeriode(periode, orgnummer, grad = grad)
    val id: IdInnhenter = observatør.sisteVedtaksperiode()
    this@forlengTilGodkjenning.håndterYtelser(id, orgnummer = orgnummer)
    if (personlogg.etterspurteBehov(id, Behovtype.Simulering, orgnummer)) håndterSimulering(id, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengVedtak(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1
) {
    forlengTilGodkjentVedtak(periode, grad, orgnummer)
    håndterUtbetalt(status = Oppdragstatus.AKSEPTERT, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengPeriode(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    orgnummer: String = a1,
) {
    nyPeriode(periode, orgnummer, grad = grad)
}

internal fun AbstractEndToEndTest.håndterSøknad(
    periode: Periode,
    orgnummer: String = a1,
    sendTilGosys: Boolean = false,
    sendtTilNAVEllerArbeidsgiver: LocalDate = periode.endInclusive
): UUID {
    return håndterSøknad(
        Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
        orgnummer = orgnummer,
        sykmeldingSkrevet = periode.start.atStartOfDay(),
        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
        sendTilGosys = sendTilGosys
    )
}

internal fun AbstractEndToEndTest.håndterSøknad(
    vararg perioder: Søknadsperiode,
    andreInntektskilder: Boolean = false,
    sendtTilNAVEllerArbeidsgiver: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    id: UUID = UUID.randomUUID(),
    orgnummer: String = a1,
    sykmeldingSkrevet: LocalDateTime? = null,
    korrigerer: UUID? = null,
    utenlandskSykmelding: Boolean = false,
    sendTilGosys: Boolean = false,
    opprinneligSendt: LocalDate? = null,
    merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(),
    permittert: Boolean = false,
    egenmeldinger: List<Periode> = emptyList()
): UUID {
    håndterOgReplayInntektsmeldinger(orgnummer) {
        søknad(
            id,
            *perioder,
            andreInntektskilder = andreInntektskilder,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
            orgnummer = orgnummer,
            sykmeldingSkrevet = sykmeldingSkrevet,
            korrigerer = korrigerer,
            utenlandskSykmelding = utenlandskSykmelding,
            sendTilGosys = sendTilGosys,
            opprinneligSendt = opprinneligSendt,
            merknaderFraSykmelding = merknaderFraSykmelding,
            permittert = permittert,
            egenmeldinger = egenmeldinger
        ).håndter(Person::håndterSøknad)
        val vedtaksperiodeId: IdInnhenter = observatør.sisteVedtaksperiode()
        if (hendelselogg.etterspurteBehov(vedtaksperiodeId, Behovtype.Sykepengehistorikk, orgnummer = orgnummer)) {
            this@håndterSøknad.håndterUtbetalingshistorikk(vedtaksperiodeId, orgnummer = orgnummer)
        }
    }
    return id
}

private fun AbstractEndToEndTest.håndterOgReplayInntektsmeldinger(orgnummer: String, block: () -> Unit) {
    observatør.replayInntektsmeldinger { block() }.forEach { forespørsel ->
        val imReplays = inntektsmeldinger
            .entries
            .sortedBy { it.value.tidspunkt }
            .filter { forespørsel.erInntektsmeldingRelevant(it.value.inntektsmeldingkontrakt) }
            .map { it.value.generator() }
            .filter { im -> im.metadata.meldingsreferanseId.id !in observatør.inntektsmeldingHåndtert.map(Pair<*, *>::first) }
            .filter { im -> im.behandlingsporing.organisasjonsnummer == orgnummer }
        InntektsmeldingerReplay(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = orgnummer
            ),
            vedtaksperiodeId = forespørsel.vedtaksperiodeId,
            inntektsmeldinger = imReplays
        ).håndter(Person::håndterInntektsmeldingerReplay)
        observatør.kvitterInntektsmeldingReplay(forespørsel.vedtaksperiodeId)
    }
}

internal fun AbstractEndToEndTest.håndterArbeidsgiveropplysninger(
    arbeidsgiverperioder: List<Periode>?,
    beregnetInntekt: Inntekt? = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = a1,
    id: UUID = UUID.randomUUID(),
    opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
    vedtaksperiodeIdInnhenter: IdInnhenter,
    innsendt: LocalDateTime = LocalDateTime.now()
): UUID {

    val vedtaksperiodeId = inspektør(orgnummer).vedtaksperiodeId(vedtaksperiodeIdInnhenter)

    val arbeidsgiveropplysninger = Arbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(id),
        innsendt = innsendt,
        registrert = innsendt.plusSeconds(1),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
            organisasjonsnummer = orgnummer
        ),
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = Arbeidsgiveropplysning.fraInntektsmelding(
            beregnetInntekt = beregnetInntekt,
            refusjon = refusjon,
            arbeidsgiverperioder = arbeidsgiverperioder,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            opphørAvNaturalytelser = opphørAvNaturalytelser
        )
    )

    observatør.forsikreForespurteArbeidsgiveropplysninger(vedtaksperiodeId, *arbeidsgiveropplysninger.toTypedArray())

    return håndterArbeidsgiveropplysninger(arbeidsgiveropplysninger)
}

internal fun AbstractEndToEndTest.håndterInntektsmelding(
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate? = null,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = a1,
    id: UUID = UUID.randomUUID(),
    opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
    harFlereInntektsmeldinger: Boolean = false,
    mottatt: LocalDateTime? = null
): UUID {
    return håndterInntektsmelding(
        inntektsmelding(
            id = id,
            arbeidsgiverperioder = arbeidsgiverperioder,
            beregnetInntekt = beregnetInntekt,
            førsteFraværsdag = førsteFraværsdag,
            refusjon = refusjon,
            orgnummer = orgnummer,
            opphørAvNaturalytelser = opphørAvNaturalytelser,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            harFlereInntektsmeldinger = harFlereInntektsmeldinger,
            mottatt = mottatt
        )
    )
}

internal fun AbstractEndToEndTest.håndterKorrigerteArbeidsgiveropplysninger(
    vararg opplysning: Arbeidsgiveropplysning,
    vedtaksperiodeId: IdInnhenter,
    id: UUID = UUID.randomUUID(),
    orgnummer: String = a1
): UUID {
    KorrigerteArbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(id),
        innsendt = LocalDateTime.now(),
        registrert = LocalDateTime.now().plusSeconds(1),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        vedtaksperiodeId = vedtaksperiodeId.id(orgnummer),
        opplysninger = opplysning.toList()
    ).håndter(Person::håndterKorrigerteArbeidsgiveropplysninger)
    return id
}

internal fun AbstractEndToEndTest.håndterArbeidsgiveropplysninger(arbeidsgiveropplysninger: Arbeidsgiveropplysninger): UUID {
    arbeidsgiveropplysninger.håndter(Person::håndterArbeidsgiveropplysninger)
    return arbeidsgiveropplysninger.metadata.meldingsreferanseId.id
}

internal fun AbstractEndToEndTest.håndterInntektsmelding(inntektsmelding: Inntektsmelding): UUID {
    håndterOgReplayInntektsmeldinger(inntektsmelding.behandlingsporing.organisasjonsnummer) {
        inntektsmelding.håndter(Person::håndterInntektsmelding)
    }
    return inntektsmelding.metadata.meldingsreferanseId.id
}

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    orgnummer: String = a1
) = håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
    medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer = orgnummer
)

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
    orgnummer: String = a1
) = håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
    medlemskapstatus = medlemskapstatus,
    orgnummer = orgnummer,
    skatteinntekter = listOf(orgnummer to INNTEKT),
    arbeidsforhold = finnArbeidsgivere().map { Triple(it, LocalDate.EPOCH, null) }
)

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    arbeidsforhold: List<Triple<String, LocalDate, LocalDate?>>,
    orgnummer: String = a1
) = håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
    medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer = orgnummer,
    skatteinntekter = arbeidsforhold.map { (orgnr, _, _) -> orgnr to INNTEKT },
    arbeidsforhold = arbeidsforhold
)

/**
 * lager et vilkårsgrunnlag med samme inntekt for de oppgitte arbeidsgiverne,
 * inkl. arbeidsforhold.
 *
 * snarvei for å få et vilkårsgrunnlag for flere arbeidsgivere uten noe fuzz
 */
internal fun AbstractEndToEndTest.håndterVilkårsgrunnlagFlereArbeidsgivere(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    vararg orgnumre: String,
    inntekt: Inntekt = INNTEKT,
    orgnummer: String = a1
) = håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
    medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer = orgnummer,
    skatteinntekter = orgnumre.map { it to inntekt }
)

/**
 * lager tre månedsinntekter for hver arbeidsgiver i skatteinntekter, med samme beløp hver måned.
 * arbeidsforhold-listen blir by default fylt ut med alle arbeidsgiverne i inntekt-listen
 */
internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    skatteinntekter: List<Pair<String, Inntekt>>,
    arbeidsforhold: List<Triple<String, LocalDate, LocalDate?>> = skatteinntekter.map { (orgnr, _) -> Triple(orgnr, LocalDate.EPOCH, null) },
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = a1
): Vilkårsgrunnlag {
    val skjæringstidspunkt = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)
    return håndterVilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        medlemskapstatus = medlemskapstatus,
        orgnummer = orgnummer,
        inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(skatteinntekter, skjæringstidspunkt),
        inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering(orgnummer, INNTEKT, skjæringstidspunkt),
        arbeidsforhold = arbeidsforhold.map { (orgnr, fom, tom) ->
            Vilkårsgrunnlag.Arbeidsforhold(orgnr, fom, tom, type = Arbeidsforholdtype.ORDINÆRT)
        },
        skjæringstidspunkt = skjæringstidspunkt
    )
}

/**
 * lager månedsinntekter fra de oppgitte månedene; hver måned har en liste av orgnummer-til-inntekt
 * lager by default arbeidsforhold for alle oppgitte orgnumre i inntekt-listen
 */
internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    månedligeInntekter: Map<YearMonth, List<Pair<String, Inntekt>>>,
    arbeidsforhold: List<Triple<String, LocalDate, LocalDate?>> = månedligeInntekter
        .flatMap { (_, inntekter) -> inntekter.map { (orgnr, _) -> orgnr } }
        .toSet()
        .map { orgnr -> Triple(orgnr, LocalDate.EPOCH, null) },
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = a1
): Vilkårsgrunnlag {
    val skjæringstidspunkt = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)
    return håndterVilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        medlemskapstatus = medlemskapstatus,
        orgnummer = orgnummer,
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = månedligeInntekter
                .flatMap { (måned, inntekter) -> inntekter.map { (orgnr, inntekt) -> Triple(måned, orgnr, inntekt) } }
                .groupBy { (_, orgnr, _) -> orgnr }
                .map { (orgnr, inntekter) ->
                    ArbeidsgiverInntekt(
                        arbeidsgiver = orgnr,
                        inntekter = inntekter.map { (måned, _, inntekt) ->
                            ArbeidsgiverInntekt.MånedligInntekt(
                                yearMonth = måned,
                                inntekt = inntekt,
                                type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                                fordel = "",
                                beskrivelse = ""
                            )
                        }
                    )
                }
        ),
        inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering(orgnummer, INNTEKT, skjæringstidspunkt),
        arbeidsforhold = arbeidsforhold.map { (orgnr, fom, tom) ->
            Vilkårsgrunnlag.Arbeidsforhold(orgnr, fom, tom, type = Arbeidsforholdtype.ORDINÆRT)
        },
        skjæringstidspunkt = skjæringstidspunkt
    )
}

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = a1,
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering(a1, INNTEKT, inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)),
    arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
    skjæringstidspunkt: LocalDate
): Vilkårsgrunnlag {
    fun assertEtterspurt(behovtype: Behovtype) =
        assertEtterspurt(Vilkårsgrunnlag::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

    assertEtterspurt(Behovtype.InntekterForSykepengegrunnlag)
    assertEtterspurt(Behovtype.InntekterForOpptjeningsvurdering)
    assertEtterspurt(Behovtype.ArbeidsforholdV2)
    assertEtterspurt(Behovtype.Medlemskap)
    return vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        skjæringstidspunkt = skjæringstidspunkt,
        medlemskapstatus = medlemskapstatus,
        orgnummer = orgnummer,
        arbeidsforhold = arbeidsforhold,
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
        inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering
    ).håndter(Person::håndterVilkårsgrunnlag)
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    simuleringOK: Boolean = true,
    orgnummer: String = a1,
    simuleringsresultat: SimuleringResultatDto? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    simulering(vedtaksperiodeIdInnhenter, simuleringOK, orgnummer, simuleringsresultat)
        .also {
            check(it.isNotEmpty()) { "det er ingenting å simulere ??" }
        }
        .forEach { simulering ->
            simulering.håndter(Person::håndterSimulering)
        }
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingId: UUID,
    fagsystemId: String,
    fagområde: Fagområde,
    simuleringOK: Boolean = true,
    orgnummer: String = a1,
    simuleringsresultat: SimuleringResultatDto? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    Simulering(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        fagsystemId = fagsystemId,
        fagområde = fagområde.toString(),
        simuleringOK = simuleringOK,
        melding = "",
        utbetalingId = utbetalingId,
        simuleringsResultat = simuleringsresultat
    ).håndter(Person::håndterSimulering)
}

internal fun AbstractEndToEndTest.håndterInfotrygdendring() {
    Infotrygdendring(MeldingsreferanseId(UUID.randomUUID()))
        .håndter(Person::håndterInfotrygdendringer)
}

private fun AbstractEndToEndTest.håndterUtbetalingshistorikk(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg utbetalinger: Infotrygdperiode,
    orgnummer: String = a1,
    besvart: LocalDateTime = LocalDateTime.now()
) {
    val bedtOmSykepengehistorikk = personlogg.etterspurteBehov(vedtaksperiodeIdInnhenter, Behovtype.Sykepengehistorikk, orgnummer)
    if (bedtOmSykepengehistorikk) assertEtterspurt(Utbetalingshistorikk::class, Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingshistorikk(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        utbetalinger = utbetalinger.toList(),
        orgnummer = orgnummer,
        besvart = besvart
    ).håndter(Person::håndterUtbetalingshistorikk)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikkEtterInfotrygdendring(
    vararg utbetalinger: Infotrygdperiode,
    besvart: LocalDateTime = LocalDateTime.now(),
    meldingsreferanseId: UUID = UUID.randomUUID()
): UUID {
    utbetalingshistorikkEtterInfotrygdEndring(
        meldingsreferanseId = meldingsreferanseId,
        utbetalinger = utbetalinger.toList(),
        besvart = besvart
    ).håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)
    return meldingsreferanseId
}

private fun AbstractEndToEndTest.finnArbeidsgivere() = person.inspektør.arbeidsgivere()

internal fun AbstractEndToEndTest.håndterYtelser(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    foreldrepenger: List<GradertPeriode> = emptyList(),
    svangerskapspenger: List<GradertPeriode> = emptyList(),
    pleiepenger: List<GradertPeriode> = emptyList(),
    omsorgspenger: List<GradertPeriode> = emptyList(),
    opplæringspenger: List<GradertPeriode> = emptyList(),
    institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
    orgnummer: String = a1,
    arbeidsavklaringspenger: List<Periode> = emptyList(),
    dagpenger: List<Periode> = emptyList()
) {
    fun assertEtterspurt(behovtype: Behovtype) =
        assertEtterspurt(Ytelser::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

    assertEtterspurt(Behovtype.Foreldrepenger)
    assertEtterspurt(Behovtype.Pleiepenger)
    assertEtterspurt(Behovtype.Omsorgspenger)
    assertEtterspurt(Behovtype.Opplæringspenger)
    assertEtterspurt(Behovtype.Arbeidsavklaringspenger)
    assertEtterspurt(Behovtype.Dagpenger)
    assertEtterspurt(Behovtype.Institusjonsopphold)

    ytelser(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        foreldrepenger = foreldrepenger,
        svangerskapspenger = svangerskapspenger,
        pleiepenger = pleiepenger,
        omsorgspenger = omsorgspenger,
        opplæringspenger = opplæringspenger,
        institusjonsoppholdsperioder = institusjonsoppholdsperioder,
        orgnummer = orgnummer,
        arbeidsavklaringspenger = arbeidsavklaringspenger,
        dagpenger = dagpenger
    ).håndter(Person::håndterYtelser)
}

internal fun AbstractEndToEndTest.håndterUtbetalingpåminnelse(
    utbetalingIndeks: Int,
    status: Utbetalingstatus,
    tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
) {
    utbetalingpåminnelse(inspektør.utbetalingId(utbetalingIndeks), status, tilstandsendringstidspunkt).håndter(Person::håndterUtbetalingPåminnelse)
}

internal fun AbstractEndToEndTest.håndterPersonPåminnelse() = PersonHendelsefabrikk().lagPåminnelse().håndter(Person::håndterPersonPåminnelse)

internal fun AbstractEndToEndTest.håndterSykepengegrunnlagForArbeidsgiver(
    skjæringstidspunkt: LocalDate = 1.januar,
    orgnummer: String = a1
): UUID {
    val inntektFraAOrdningen: SykepengegrunnlagForArbeidsgiver = sykepengegrunnlagForArbeidsgiver(skjæringstidspunkt, orgnummer)
    inntektFraAOrdningen.håndter(Person::håndterSykepengegrunnlagForArbeidsgiver)
    return inntektFraAOrdningen.metadata.meldingsreferanseId.id
}

internal fun AbstractEndToEndTest.håndterPåminnelse(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    påminnetTilstand: TilstandType,
    tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
    nå: LocalDateTime = LocalDateTime.now(),
    orgnummer: String = a1,
    antallGangerPåminnet: Int = 1,
    flagg: Set<String> = emptySet()
) {
    påminnelse(
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer),
        påminnetTilstand = påminnetTilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        nå = nå,
        orgnummer = orgnummer,
        antallGangerPåminnet = antallGangerPåminnet,
        flagg = flagg
    ).håndter(Person::håndterPåminnelse)
}

internal fun AbstractEndToEndTest.håndterAnmodningOmForkasting(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    force: Boolean = false,
    orgnummer: String = a1
) {
    anmodningOmForkasting(
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer),
        force = force,
        orgnummer = orgnummer,
    ).håndter(Person::håndterAnmodningOmForkasting)
}

internal fun AbstractEndToEndTest.håndterUtbetalingsgodkjenning(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingGodkjent: Boolean = true,
    orgnummer: String = a1,
    automatiskBehandling: Boolean = false,
    utbetalingId: UUID = UUID.fromString(
        personlogg.sisteBehov(Behovtype.Godkjenning).alleKontekster["utbetalingId"]
            ?: throw IllegalStateException("Finner ikke utbetalingId i: ${personlogg.sisteBehov(Behovtype.Godkjenning).alleKontekster}")
    ),
) {
    assertEtterspurt(Utbetalingsgodkjenning::class, Behovtype.Godkjenning, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingsgodkjenning(vedtaksperiodeIdInnhenter, utbetalingGodkjent, orgnummer, automatiskBehandling, utbetalingId).håndter(Person::håndterUtbetalingsgodkjenning)
}

internal fun AbstractEndToEndTest.håndterUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    orgnummer: String = a1,
    fagsystemId: String,
    utbetalingId: UUID? = null,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    val faktiskUtbetalingId = utbetalingId?.toString() ?: personlogg.sisteBehov(Behovtype.Utbetaling).alleKontekster.getValue("utbetalingId")
    utbetaling(
        fagsystemId = fagsystemId,
        status = status,
        orgnummer = orgnummer,
        meldingsreferanseId = meldingsreferanseId,
        utbetalingId = UUID.fromString(faktiskUtbetalingId)
    ).håndter(Person::håndterUtbetalingHendelse)
}

private fun Oppdrag.fagsytemIdOrNull() = if (harUtbetalinger()) inspektør.fagsystemId() else null

private fun AbstractEndToEndTest.førsteUhåndterteUtbetalingsbehov(orgnummer: String): Pair<UUID, List<String>>? {
    val utbetalingsbehovUtbetalingIder = personlogg.behov
        .filter { it.type == Behovtype.Utbetaling }
        .map { UUID.fromString(it.alleKontekster.getValue("utbetalingId")) }

    return inspektør(orgnummer).utbetalingerInFlight()
        .also { require(it.size < 2) { "For mange utbetalinger i spill! Er sendt ut godkjenningsbehov for periodene ${it.map { utbetaling -> utbetaling.periode }}" } }
        .firstOrNull { it.utbetalingId in utbetalingsbehovUtbetalingIder }
        ?.let {
            it.utbetalingId to listOfNotNull(
                it.arbeidsgiverOppdrag.fagsytemIdOrNull(),
                it.personOppdrag.fagsytemIdOrNull()
            )
        }
}

internal fun AbstractEndToEndTest.håndterUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    sendOverførtKvittering: Boolean = true,
    orgnummer: String = a1,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    førsteUhåndterteUtbetalingsbehov(orgnummer)?.also { (utbetalingId, fagsystemIder) ->
        fagsystemIder.forEach { fagsystemId ->
            håndterUtbetalt(status, orgnummer, fagsystemId, utbetalingId, meldingsreferanseId)
        }
    }
}

internal fun AbstractEndToEndTest.håndterAnnullerUtbetaling(
    orgnummer: String = a1,
    utbetalingId: UUID = inspektør.sisteUtbetaling().utbetalingId,
    opprettet: LocalDateTime = LocalDateTime.now()
) {
    AnnullerUtbetaling(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
            organisasjonsnummer = orgnummer
        ),
        utbetalingId = utbetalingId,
        saksbehandlerIdent = "Ola Nordmann",
        saksbehandlerEpost = "tbd@nav.no",
        opprettet = opprettet,
        årsaker = listOf("Annet"),
        begrunnelse = ""
    ).håndter(Person::håndterAnnulerUtbetaling)
}

internal fun AbstractEndToEndTest.håndterOverstyrInntekt(
    inntekt: Inntekt = 31000.månedlig,
    orgnummer: String = a1,
    skjæringstidspunkt: LocalDate,
    meldingsreferanseId: UUID = UUID.randomUUID(),
    forklaring: String = "forklaring",
    begrunnelse: OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse.Begrunnelse? = null
) {
    this@håndterOverstyrInntekt.håndterOverstyrArbeidsgiveropplysninger(
        skjæringstidspunkt,
        listOf(OverstyrtArbeidsgiveropplysning(orgnummer, inntekt, emptyList(), OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse(forklaring, begrunnelse))),
        listOf(OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse(forklaring, begrunnelse)),
        meldingsreferanseId
    )
}

internal fun AbstractEndToEndTest.håndterMinimumSykdomsgradVurdert(
    perioderMedMinimumSykdomsgradVurdertOK: List<Periode>,
    perioderMedMinimumSykdomsgradVurdertIkkeOK: List<Periode> = emptyList()
) {
    MinimumSykdomsgradsvurderingMelding(
        perioderMedMinimumSykdomsgradVurdertOK.toSet(),
        perioderMedMinimumSykdomsgradVurdertIkkeOK.toSet(),
        MeldingsreferanseId(UUID.randomUUID())
    ).håndter(Person::håndterMinimumSykdomsgradsvurderingMelding)
}

internal fun AbstractEndToEndTest.håndterOverstyrArbeidsgiveropplysninger(
    skjæringstidspunkt: LocalDate,
    arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>,
    begrunnelser: List<OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse> = emptyList(),
    meldingsreferanseId: UUID = UUID.randomUUID()
): UUID {
    val opprettet = LocalDateTime.now()
    OverstyrArbeidsgiveropplysninger(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiveropplysninger = arbeidsgiveropplysninger.tilOverstyrt(meldingsreferanseId, skjæringstidspunkt),
        refusjonstidslinjer = arbeidsgiveropplysninger.refusjonstidslinjer(meldingsreferanseId, opprettet),
        opprettet = opprettet
    ).håndter(Person::håndterOverstyrArbeidsgiveropplysninger)
    return meldingsreferanseId
}

internal fun AbstractEndToEndTest.håndterSkjønnsmessigFastsettelse(
    skjæringstidspunkt: LocalDate,
    arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>,
    meldingsreferanseId: UUID = UUID.randomUUID()
): UUID {
    SkjønnsmessigFastsettelse(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiveropplysninger = arbeidsgiveropplysninger.tilSkjønnsmessigFastsatt(meldingsreferanseId, skjæringstidspunkt),
        opprettet = LocalDateTime.now()
    ).håndter(Person::håndterSkjønnsmessigFastsettelse)
    return meldingsreferanseId
}

internal class OverstyrtArbeidsgiveropplysning(
    private val orgnummer: String,
    private val inntekt: Inntekt,
    private val refusjonsopplysninger: List<Triple<LocalDate, LocalDate?, Inntekt>>,
    private val overstyringbegrunnelse: OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse? = null
) {
    internal constructor(orgnummer: String, inntekt: Inntekt) : this(orgnummer, inntekt, emptyList())

    internal companion object {
        internal fun List<OverstyrtArbeidsgiveropplysning>.tilOverstyrt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) =
            map {
                OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning(
                    organisasjonsnummer = it.orgnummer,
                    inntektsdata = Inntektsdata(
                        hendelseId = MeldingsreferanseId(meldingsreferanseId),
                        dato = skjæringstidspunkt,
                        beløp = it.inntekt,
                        tidsstempel = LocalDateTime.now()
                    ),
                    begrunnelse = it.overstyringbegrunnelse ?: OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse(
                        forklaring = "forklaring",
                        begrunnelse = OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse.Begrunnelse.VARIG_LØNNSENDRING
                    )
                )
            }

        internal fun List<OverstyrtArbeidsgiveropplysning>.tilSkjønnsmessigFastsatt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) =
            map {
                SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt(
                    orgnummer = it.orgnummer,
                    inntektsdata = Inntektsdata(
                        hendelseId = MeldingsreferanseId(meldingsreferanseId),
                        dato = skjæringstidspunkt,
                        beløp = it.inntekt,
                        tidsstempel = LocalDateTime.now()
                    )
                )
            }

        internal fun List<OverstyrtArbeidsgiveropplysning>.refusjonstidslinjer(meldingsreferanseId: UUID, opprettet: LocalDateTime) = this.associateBy { it.orgnummer }.mapValues { (_, opplysning) ->
            val strekkbar = opplysning.refusjonsopplysninger.any { (_, tom) -> tom == null }
            opplysning.refusjonsopplysninger.fold(Beløpstidslinje()) { acc, (fom, tom, beløp) ->
                acc + Beløpstidslinje.fra(fom til (tom ?: fom), beløp, Kilde(MeldingsreferanseId(meldingsreferanseId), SAKSBEHANDLER, opprettet))
            } to strekkbar
        }
    }
}

internal fun AbstractEndToEndTest.håndterOverstyrTidslinje(
    overstyringsdager: List<ManuellOverskrivingDag> = listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag, 100)),
    orgnummer: String = a1,
    meldingsreferanseId: UUID = UUID.randomUUID()
): OverstyrTidslinje {
    val hendelse = OverstyrTidslinje(
        meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
        dager = overstyringsdager,
        opprettet = LocalDateTime.now()
    )
    håndterOgReplayInntektsmeldinger(orgnummer) {
        hendelse.also { it.håndter(Person::håndterOverstyrTidslinje) }
    }
    return hendelse
}

internal fun AbstractEndToEndTest.håndterOverstyrArbeidsforhold(
    skjæringstidspunkt: LocalDate,
    overstyrteArbeidsforhold: List<OverstyrArbeidsforhold.ArbeidsforholdOverstyrt>
) {
    OverstyrArbeidsforhold(
        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
        skjæringstidspunkt = skjæringstidspunkt,
        overstyrteArbeidsforhold = overstyrteArbeidsforhold,
        opprettet = LocalDateTime.now()
    ).håndter(Person::håndterOverstyrArbeidsforhold)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikkForFeriepenger(
    opptjeningsår: Year,
    utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
    feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
    datoForSisteFeriepengekjøringIInfotrygd: LocalDate,
    skalBeregnesManuelt: Boolean = false
) {
    utbetalingshistorikkForFeriepenger(
        opptjeningsår = opptjeningsår,
        utbetalinger = utbetalinger,
        feriepengehistorikk = feriepengehistorikk,
        datoForSisteFeriepengekjøringIInfotrygd = datoForSisteFeriepengekjøringIInfotrygd,
        skalBeregnesManuelt = skalBeregnesManuelt
    ).håndter(Person::håndterUtbetalingshistorikkForFeriepenger)
}

internal fun AbstractEndToEndTest.håndterFeriepengerUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    orgnummer: String = a1,
    fagsystemId: String,
    meldingsreferanseId: UUID = UUID.randomUUID()
): FeriepengeutbetalingHendelse {
    return feriepengeutbetaling(
        fagsystemId = fagsystemId,
        status = status,
        orgnummer = orgnummer,
        meldingsreferanseId = meldingsreferanseId
    ).håndter(Person::håndterFeriepengeutbetalingHendelse)
}

internal fun AbstractEndToEndTest.håndterOverstyringSykedag(periode: Periode) = this@håndterOverstyringSykedag.håndterOverstyrTidslinje(periode.map { manuellSykedag(it) })

internal fun AbstractEndToEndTest.nyPeriode(periode: Periode, orgnummer: String = a1, grad: Prosentdel = 100.prosent) {
    håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive), orgnummer = orgnummer)
    håndterSøknad(Søknadsperiode.Sykdom(periode.start, periode.endInclusive, grad), orgnummer = orgnummer)
}
