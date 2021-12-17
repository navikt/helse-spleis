package no.nav.helse.spleis.e2e

import no.nav.helse.Fødselsnummer
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Companion
import no.nav.helse.hendelser.utbetaling.*
import no.nav.helse.person.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*


internal fun AbstractEndToEndTest.håndterSykmelding(
    vararg sykeperioder: Sykmeldingsperiode,
    sykmeldingSkrevet: LocalDateTime? = null,
    mottatt: LocalDateTime? = null,
    id: UUID = UUID.randomUUID(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    sykmelding(
        id,
        *sykeperioder,
        sykmeldingSkrevet = sykmeldingSkrevet,
        mottatt = mottatt,
        orgnummer = orgnummer,
        fnr = fnr
    ).håndter(Person::håndter)
    sykmeldinger[id] = sykeperioder
    return id
}

internal fun AbstractEndToEndTest.tilGodkjenning(fom: LocalDate, tom: LocalDate, vararg organisasjonsnummere: Organisasjonsnummer) {
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnummere.forEach {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it)
    }
    organisasjonsnummere.forEach {
        håndterSøknad(Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = it)

    }
    organisasjonsnummere.forEach {
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            beregnetInntekt = 20000.månedlig,
            orgnummer = it
        )
    }
    val (første, _) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

    første.let { organisasjonsnummer ->
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                fom.minusYears(1) til fom.minusMonths(1) inntekter {
                    organisasjonsnummere.forEach {
                        it inntekt 20000.månedlig
                    }
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyeVedtak(
    fom: LocalDate,
    tom: LocalDate,
    vararg organisasjonsnummere: Organisasjonsnummer,
    inntekterBlock: Inntektperioder.() -> Unit = {
        organisasjonsnummere.forEach {
            lagInntektperioder(it, fom, 20000.månedlig)
        }
    }
) {
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnummere.forEach {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it)
    }
    organisasjonsnummere.forEach {
        håndterSøknad(Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = it)

    }
    organisasjonsnummere.forEach {
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            beregnetInntekt = 20000.månedlig,
            orgnummer = it
        )
    }
    val (første, resten) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

    første.let { organisasjonsnummer ->
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                inntekterBlock()
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = organisasjonsnummer)
    }

    resten.forEach { organisasjonsnummer ->
        håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.forlengVedtak(fom: LocalDate, tom: LocalDate, vararg organisasjonsnumre: Organisasjonsnummer) {
    require(organisasjonsnumre.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnumre.forEach { håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it) }
    organisasjonsnumre.forEach { håndterSøknad(Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = it) }
    organisasjonsnumre.forEach { håndterYtelser(vedtaksperiodeIdInnhenter = { _ -> observatør.sisteVedtaksperiode(it) }, orgnummer = it) }
    organisasjonsnumre.forEach { organisasjonsnummer ->
        val vedtaksperiodeIdInnhenter: IdInnhenter = { observatør.sisteVedtaksperiode(organisasjonsnummer) }
        håndterYtelser(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
        håndterSimulering(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
        håndterUtbetalingsgodkjenning(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
        håndterUtbetalt(vedtaksperiodeIdInnhenter, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyttVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    førsteFraværsdag: LocalDate = fom,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
) {
    val id = tilGodkjent(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
    håndterUtbetalt({ id }, status = Oppdragstatus.AKSEPTERT, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.tilGodkjent(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
): UUID {
    val id = tilGodkjenning(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
    håndterUtbetalingsgodkjenning({ id }, true, fnr = fnr, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilGodkjenning(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
): UUID {
    val id = tilYtelser(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock)
    håndterSimulering({ id }, fnr = fnr, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilYtelser(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
        inntekter = inntektperioderForSykepengegrunnlag {
            fom.minusMonths(3) til fom.minusMonths(1) inntekter {
                orgnummer inntekt AbstractEndToEndTest.INNTEKT
            }
        }
    )
): UUID {
    håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
    håndterInntektsmeldingMedValidering(
        id,
        listOf(Periode(fom, fom.plusDays(15))),
        førsteFraværsdag = førsteFraværsdag,
        fnr = fnr,
        orgnummer = orgnummer,
        refusjon = refusjon
    )
    håndterSøknadMedValidering(id, Søknadsperiode.Sykdom(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer)
    håndterVilkårsgrunnlag(
        id,
        AbstractEndToEndTest.INNTEKT,
        fnr = fnr,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag(inntekterBlock)
        ),
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag
    )
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer)
    return id(orgnummer)
}

internal fun AbstractEndToEndTest.forlengTilGodkjentVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    skalSimuleres: Boolean = true
) {
    håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
    håndterSøknadMedValidering(id, Søknadsperiode.Sykdom(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer)
    if (skalSimuleres) håndterSimulering(id, fnr = fnr, orgnummer = orgnummer)
    håndterUtbetalingsgodkjenning(id, true, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    skalSimuleres: Boolean = true
) {
    forlengTilGodkjentVedtak(fom, tom, grad, fnr, orgnummer, skalSimuleres)
    val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
    håndterUtbetalt(id, status = Oppdragstatus.AKSEPTERT, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengPeriode(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) {
    håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    val id: IdInnhenter = { observatør.sisteVedtaksperiode() }
    håndterSøknadMedValidering(id, Søknadsperiode.Sykdom(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.håndterSøknadMedValidering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg perioder: Søknadsperiode,
    andreInntektskilder: List<SendtSøknad.Inntektskilde> = emptyList(),
    sendtTilNav: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
) {
    assertIkkeEtterspurt(
        Søknad::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter,
        AbstractPersonTest.ORGNUMMER
    )
    håndterSøknad(*perioder, andreInntektskilder = andreInntektskilder, sendtTilNav = sendtTilNav, orgnummer = orgnummer, fnr = fnr)
}

internal fun AbstractEndToEndTest.håndterSøknad(
    vararg perioder: Søknadsperiode,
    andreInntektskilder: List<SendtSøknad.Inntektskilde> = emptyList(),
    sendtTilNav: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    id: UUID = UUID.randomUUID(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    sykmeldingSkrevet: LocalDateTime? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    søknad(
        id,
        *perioder,
        andreInntektskilder = andreInntektskilder,
        sendtTilNav = sendtTilNav,
        orgnummer = orgnummer,
        sykmeldingSkrevet = sykmeldingSkrevet,
        fnr = fnr
    ).håndter(Person::håndter)
    søknader[id] = Triple(sendtTilNav, andreInntektskilder, perioder)
    return id
}

internal fun AbstractEndToEndTest.håndterSøknadArbeidsgiver(
    vararg sykdomsperioder: Søknadsperiode.Sykdom,
    arbeidsperiode: Søknadsperiode.Arbeid? = null,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) = søknadArbeidsgiver(*sykdomsperioder, arbeidsperiode = arbeidsperiode, orgnummer = orgnummer).håndter(Person::håndter)

internal fun AbstractEndToEndTest.håndterInntektsmeldingMedValidering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    assertIkkeEtterspurt(Inntektsmelding::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter, orgnummer)
    return håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, beregnetInntekt = beregnetInntekt, refusjon, orgnummer = orgnummer, fnr = fnr)
}

internal fun AbstractEndToEndTest.håndterInntektsmelding(
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    id: UUID = UUID.randomUUID(),
    harOpphørAvNaturalytelser: Boolean = false,
    arbeidsforholdId: String? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    inntektsmelding(
        id,
        arbeidsgiverperioder,
        beregnetInntekt = beregnetInntekt,
        førsteFraværsdag = førsteFraværsdag,
        refusjon = refusjon,
        orgnummer = orgnummer,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
        arbeidsforholdId = arbeidsforholdId,
        fnr = fnr
    ).håndter(Person::håndter)
    return id
}

internal fun AbstractEndToEndTest.håndterInntektsmeldingReplay(
    inntektsmeldingId: UUID,
    vedtaksperiodeId: UUID
) {
    val inntektsmeldinggenerator = inntektsmeldinger[inntektsmeldingId] ?: fail { "Fant ikke inntektsmelding med id $inntektsmeldingId" }
    Assertions.assertTrue(observatør.bedtOmInntektsmeldingReplay(vedtaksperiodeId)) { "Vedtaksperioden har ikke bedt om replay av inntektsmelding" }
    inntektsmeldingReplay(inntektsmeldinggenerator(), vedtaksperiodeId)
        .håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    inntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
        inntekter = inntektperioderForSammenligningsgrunnlag {
            val skjæringstidspunkt = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)
            skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                orgnummer inntekt inntekt
            }
        }
    ),
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
        listOf(
            ArbeidsgiverInntekt(orgnummer.toString(), (0..2).map {
                val yearMonth = YearMonth.from(inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)).minusMonths(3L - it)
                ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                    yearMonth = yearMonth,
                    type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    inntekt = AbstractEndToEndTest.INNTEKT,
                    fordel = "fordel",
                    beskrivelse = "beskrivelse"
                )
            })
        )
    ),
    arbeidsforhold: List<Arbeidsforhold> = finnArbeidsgivere().map { Arbeidsforhold(it.toString(), LocalDate.EPOCH, null) },
    opptjening: Opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
) {
    fun assertEtterspurt(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        assertEtterspurt(Vilkårsgrunnlag::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap)
    vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        medlemskapstatus = medlemskapstatus,
        orgnummer = orgnummer,
        arbeidsforhold = arbeidsforhold,
        opptjening = opptjening,
        inntektsvurdering = inntektsvurdering,
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
        fnr = fnr
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    simuleringOK: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    simuleringsresultat: Simulering.SimuleringResultat? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    simulering(vedtaksperiodeIdInnhenter, simuleringOK, fnr, orgnummer, simuleringsresultat).forEach { simulering -> simulering.håndter(Person::håndter) }
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingId: UUID,
    fagsystemId: String,
    fagområde: Fagområde,
    simuleringOK: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    simuleringsresultat: Simulering.SimuleringResultat? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    Simulering(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer.toString(),
        fagsystemId = fagsystemId,
        fagområde = fagområde.toString(),
        simuleringOK = simuleringOK,
        melding = "",
        utbetalingId = utbetalingId,
        simuleringResultat = simuleringsresultat
    ).apply {
        hendelselogg = this
    }.håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikk(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg utbetalinger: Infotrygdperiode,
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    besvart: LocalDateTime = LocalDateTime.now()
) {
    val bedtOmSykepengehistorikk = inspektør(orgnummer).etterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk)
    if (bedtOmSykepengehistorikk) assertEtterspurt(Utbetalingshistorikk::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingshistorikk(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        utbetalinger = utbetalinger.toList(),
        inntektshistorikk = inntektshistorikk,
        orgnummer = orgnummer,
        besvart = besvart
    ).håndter(Person::håndter)
}

internal fun Inntekt.repeat(antall: Int) = (0.until(antall)).map { this }

private fun AbstractPersonTest.finnArbeidsgivere(): List<String> {
    val arbeidsgivere = mutableListOf<String>()
    person.accept(object : PersonVisitor {
        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            arbeidsgivere.add(organisasjonsnummer)
        }
    })

    return arbeidsgivere
}

internal fun AbstractEndToEndTest.håndterYtelser(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    vararg utbetalinger: Infotrygdperiode,
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    foreldrepenger: Periode? = null,
    pleiepenger: List<Periode> = emptyList(),
    omsorgspenger: List<Periode> = emptyList(),
    opplæringspenger: List<Periode> = emptyList(),
    institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    dødsdato: LocalDate? = null,
    statslønn: Boolean = false,
    arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
    arbeidsavklaringspenger: List<Periode> = emptyList(),
    dagpenger: List<Periode> = emptyList(),
    besvart: LocalDateTime = LocalDateTime.now(),
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
) {
    fun assertEtterspurt(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        assertEtterspurt(Ytelser::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold)
    assertEtterspurt(Aktivitetslogg.Aktivitet.Behov.Behovtype.Dødsinfo)

    ytelser(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        utbetalinger = utbetalinger.toList(),
        inntektshistorikk = inntektshistorikk,
        foreldrepenger = foreldrepenger,
        pleiepenger = pleiepenger,
        omsorgspenger = omsorgspenger,
        opplæringspenger = opplæringspenger,
        institusjonsoppholdsperioder = institusjonsoppholdsperioder,
        orgnummer = orgnummer,
        dødsdato = dødsdato,
        statslønn = statslønn,
        arbeidskategorikoder = arbeidskategorikoder,
        arbeidsavklaringspenger = arbeidsavklaringspenger,
        dagpenger = dagpenger,
        besvart = besvart,
        fnr = fnr
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingpåminnelse(
    utbetalingIndeks: Int,
    status: Utbetalingstatus,
    tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now()
) {
    utbetalingpåminnelse(inspektør.utbetalingId(utbetalingIndeks), status, tilstandsendringstidspunkt).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterPåminnelse(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    påminnetTilstand: TilstandType,
    tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER
) {
    påminnelse(
        vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer),
        påminnetTilstand = påminnetTilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        orgnummer = orgnummer
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingsgodkjenning(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingGodkjent: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    automatiskBehandling: Boolean = false,
    utbetalingId: UUID = UUID.fromString(
        inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()["utbetalingId"]
            ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()}")
    ),
) {
    assertEtterspurt(Utbetalingsgodkjenning::class, Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingsgodkjenning(vedtaksperiodeIdInnhenter, utbetalingGodkjent, fnr, orgnummer, automatiskBehandling, utbetalingId).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalt(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    sendOverførtKvittering: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String = inspektør(orgnummer).fagsystemId(vedtaksperiodeIdInnhenter),
    meldingsreferanseId: UUID = UUID.randomUUID()
): UtbetalingHendelse {
    if (sendOverførtKvittering) {
        UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AbstractPersonTest.AKTØRID,
            fødselsnummer = fnr.toString(),
            orgnummer = orgnummer.toString(),
            fagsystemId = fagsystemId,
            utbetalingId = inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).kontekst()}"),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ).håndter(Person::håndter)
    }
    return utbetaling(
        fagsystemId = fagsystemId,
        status = status,
        fnr = fnr,
        orgnummer = orgnummer,
        meldingsreferanseId = meldingsreferanseId
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterGrunnbeløpsregulering(
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
    gyldighetsdato: LocalDate
) {
    Grunnbeløpsregulering(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = orgnummer.toString(),
        gyldighetsdato = gyldighetsdato,
        fagsystemId = fagsystemId,
        aktivitetslogg = Aktivitetslogg()
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterAnnullerUtbetaling(
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
    opprettet: LocalDateTime = LocalDateTime.now()
) {
    AnnullerUtbetaling(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = orgnummer.toString(),
        fagsystemId = fagsystemId,
        saksbehandlerIdent = "Ola Nordmann",
        saksbehandlerEpost = "tbd@nav.no",
        opprettet = opprettet
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterOverstyrInntekt(
    inntekt: Inntekt = 31000.månedlig,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    skjæringstidspunkt: LocalDate,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    OverstyrInntekt(
        meldingsreferanseId = meldingsreferanseId,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        organisasjonsnummer = orgnummer.toString(),
        inntekt = inntekt,
        skjæringstidspunkt = skjæringstidspunkt
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterOverstyrTidslinje(
    overstyringsdager: List<ManuellOverskrivingDag> = listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag, 100)),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    OverstyrTidslinje(
        meldingsreferanseId = meldingsreferanseId,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        organisasjonsnummer = orgnummer.toString(),
        dager = overstyringsdager,
        opprettet = LocalDateTime.now()
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikkUtenValidering(
    vararg utbetalinger: Infotrygdperiode,
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    besvart: LocalDateTime = LocalDateTime.now()
) {
    utbetalingshistorikk(
        vedtaksperiodeIdInnhenter = { UUID.randomUUID() },
        utbetalinger = utbetalinger.toList(),
        inntektshistorikk = inntektshistorikk,
        orgnummer = orgnummer,
        besvart = besvart
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikkForFeriepenger(
    opptjeningsår: Year,
    utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = listOf(),
    feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = listOf(),
    skalBeregnesManuelt: Boolean = false
) {
    utbetalingshistorikkForFeriepenger(
        opptjeningsår = opptjeningsår,
        utbetalinger = utbetalinger,
        feriepengehistorikk = feriepengehistorikk,
        skalBeregnesManuelt = skalBeregnesManuelt
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterFeriepengerUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    orgnummer: Organisasjonsnummer = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String,
    meldingsreferanseId: UUID = UUID.randomUUID()
): UtbetalingHendelse {
    return feriepengeutbetaling(
        fagsystemId = fagsystemId,
        status = status,
        orgnummer = orgnummer,
        meldingsreferanseId = meldingsreferanseId
    ).håndter(Person::håndter)
}

internal fun TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(førsteAG: Boolean = true): Array<out TilstandType> =
    if (førsteAG) arrayOf(
        START,
        MOTTATT_SYKMELDING_FERDIG_GAP,
        AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
        AVVENTER_ARBEIDSGIVERE,
        AVVENTER_HISTORIKK,
        AVVENTER_VILKÅRSPRØVING,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET
    ) else arrayOf(
        START,
        MOTTATT_SYKMELDING_FERDIG_GAP,
        AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
        AVVENTER_ARBEIDSGIVERE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET
    )

internal fun TIL_AVSLUTTET_FORLENGELSE(førsteAG: Boolean = true) =
    if (førsteAG) arrayOf(
        START,
        MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
        AVVENTER_HISTORIKK,
        AVVENTER_ARBEIDSGIVERE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET,
    ) else arrayOf(
        START,
        MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
        AVVENTER_HISTORIKK,
        AVVENTER_ARBEIDSGIVERE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET,
    )

internal fun AbstractEndToEndTest.håndterOverstyringSykedag(periode: Periode) = håndterOverstyrTidslinje(periode.map { manuellSykedag(it) })

internal fun AbstractEndToEndTest.prosessperiode(periode: Periode, orgnummer: Organisasjonsnummer, sykedagstelling: Int = 0) {
    gapPeriode(periode, orgnummer, sykedagstelling)
    historikk(orgnummer, sykedagstelling)
    betale(orgnummer)
}

internal fun AbstractEndToEndTest.gapPeriode(periode: Periode, orgnummer: Organisasjonsnummer, sykedagstelling: Int = 0) {
    nyPeriode(periode, orgnummer)
    håndterInntektsmelding(
        arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
        førsteFraværsdag = periode.start,
        orgnummer = orgnummer
    )
    historikk(orgnummer, sykedagstelling)
    person.håndter(vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt AbstractEndToEndTest.INNTEKT
                }
            }
        ),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt AbstractEndToEndTest.INNTEKT
                }
            }
        )
    ))
}

internal fun AbstractEndToEndTest.nyPeriode(periode: Periode, orgnummer: Organisasjonsnummer) {
    person.håndter(
        sykmelding(
            UUID.randomUUID(),
            Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
            orgnummer = orgnummer
        )
    )
    person.håndter(
        søknad(
            UUID.randomUUID(),
            Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
            orgnummer = orgnummer
        )
    )
}

internal fun AbstractEndToEndTest.betale(orgnummer: Organisasjonsnummer) {
    simulering(1.vedtaksperiode, orgnummer = orgnummer).forEach { simulering ->
        person.håndter(simulering)
    }
    person.håndter(
        utbetalingsgodkjenning(
            1.vedtaksperiode,
            true,
            orgnummer = orgnummer,
            automatiskBehandling = false
        )
    )
    person.håndter(
        UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AbstractPersonTest.AKTØRID,
            fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
            orgnummer = orgnummer.toString(),
            fagsystemId = inspektør(orgnummer).fagsystemId(1.vedtaksperiode),
            utbetalingId = hendelselogg.behov().first { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )
    )
    person.håndter(
        utbetaling(
            inspektør(orgnummer).fagsystemId(1.vedtaksperiode),
            status = Oppdragstatus.AKSEPTERT,
            orgnummer = orgnummer
        )
    )
}
