package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.desember
import no.nav.helse.etterspurteBehov
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sisteBehov
import no.nav.helse.testhelpers.Inntektperioder
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail


internal fun AbstractEndToEndTest.håndterSykmelding(
    vararg sykeperioder: Sykmeldingsperiode,
    sykmeldingSkrevet: LocalDateTime? = null,
    mottatt: LocalDateTime? = null,
    id: UUID = UUID.randomUUID(),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
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

internal fun AbstractEndToEndTest.tilGodkjenning(fom: LocalDate, tom: LocalDate, vararg organisasjonsnummere: String, inntekterBlock: Inntektperioder.() -> Unit = {
    fom.minusYears(1) til fom.minusMonths(1) inntekter {
        organisasjonsnummere.forEach {
            it inntekt 20000.månedlig
        }
    }
}) {
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    nyPeriode(fom til tom, *organisasjonsnummere)
    organisasjonsnummere.forEach {
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            beregnetInntekt = 20000.månedlig,
            orgnummer = it
        )
    }

    organisasjonsnummere.first().let { organisasjonsnummer ->
        val vedtaksperiode = observatør.sisteVedtaksperiode()
        håndterYtelser(vedtaksperiodeIdInnhenter = vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = vedtaksperiode,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                inntekterBlock()
            })
        )
        håndterYtelser(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(vedtaksperiode, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyeVedtak(
    fom: LocalDate,
    tom: LocalDate,
    vararg organisasjonsnummere: String,
    inntekterBlock: Inntektperioder.() -> Unit = {
        organisasjonsnummere.forEach {
            lagInntektperioder(it, fom, 20000.månedlig)
        }
    }
) {
    require(organisasjonsnummere.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    tilGodkjenning(fom, tom, *organisasjonsnummere, inntekterBlock = inntekterBlock)
    val (første, resten) = organisasjonsnummere.first() to organisasjonsnummere.drop(1)

    første.let { organisasjonsnummer ->
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }

    resten.forEach { organisasjonsnummer ->
        håndterYtelser(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.førstegangTilGodkjenning(
    fom: LocalDate,
    tom: LocalDate,
    vararg organisasjonsnummere: String,
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

    val vedtaksperiode = observatør.sisteVedtaksperiode()

    organisasjonsnummere.first().let { organisasjonsnummer ->
        håndterYtelser(vedtaksperiodeIdInnhenter = vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = vedtaksperiode,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                inntekterBlock()
            })
        )
        håndterYtelser(vedtaksperiode, orgnummer = organisasjonsnummer)
        håndterSimulering(vedtaksperiode, orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.forlengelseTilGodkjenning(fom: LocalDate, tom: LocalDate, vararg organisasjonsnumre: String) {
    require(organisasjonsnumre.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    nyPeriode(fom til tom, *organisasjonsnumre)
    håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnumre.first())
    håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnumre.first())
}

internal fun AbstractEndToEndTest.forlengVedtak(fom: LocalDate, tom: LocalDate, vararg organisasjonsnumre: String) {
    require(organisasjonsnumre.isNotEmpty()) { "Må inneholde minst ett organisasjonsnummer" }
    organisasjonsnumre.forEach { håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = it) }
    organisasjonsnumre.forEach { håndterSøknad(Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = it) }
    organisasjonsnumre.forEach { organisasjonsnummer ->
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = organisasjonsnummer)
        håndterUtbetalt(orgnummer = organisasjonsnummer)
    }
}

internal fun AbstractEndToEndTest.nyttVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    førsteFraværsdag: LocalDate = fom,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    arbeidsgiverperiode: List<Periode>? = null,
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) }
) {
    tilGodkjent(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock, arbeidsgiverperiode = arbeidsgiverperiode)
    håndterUtbetalt(status = Oppdragstatus.AKSEPTERT, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.tilGodkjent(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
    arbeidsgiverperiode: List<Periode>? = null
): IdInnhenter {
    val id = tilGodkjenning(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock, arbeidsgiverperiode = arbeidsgiverperiode)
    håndterUtbetalingsgodkjenning(id, true, fnr = fnr, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilGodkjenning(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
    arbeidsgiverperiode: List<Periode>? = null
): IdInnhenter {
    val id = tilSimulering(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock, arbeidsgiverperiode = arbeidsgiverperiode)
    håndterSimulering(id, fnr = fnr, orgnummer = orgnummer)
    return id
}

internal fun AbstractEndToEndTest.tilSimulering(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
    arbeidsgiverperiode: List<Periode>? = null
): IdInnhenter {
    return tilYtelser(fom, tom, grad, førsteFraværsdag, fnr = fnr, orgnummer = orgnummer, refusjon = refusjon, inntekterBlock = inntekterBlock, arbeidsgiverperiode = arbeidsgiverperiode)
}

internal fun AbstractEndToEndTest.tilYtelser(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel,
    førsteFraværsdag: LocalDate,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(AbstractEndToEndTest.INNTEKT, null, emptyList()),
    inntekterBlock: Inntektperioder.() -> Unit = { lagInntektperioder(orgnummer, fom) },
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
        inntekter = inntektperioderForSykepengegrunnlag {
            fom.minusMonths(3) til fom.minusMonths(1) inntekter {
                orgnummer inntekt AbstractEndToEndTest.INNTEKT
            }
        }, arbeidsforhold = emptyList()
    ),
    arbeidsgiverperiode: List<Periode>? = null
): IdInnhenter {
    håndterSykmelding(Sykmeldingsperiode(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    håndterSøknad(Søknadsperiode.Sykdom(fom, tom, grad), fnr = fnr, orgnummer = orgnummer)
    håndterInntektsmelding(
        arbeidsgiverperiode ?: listOf(Periode(fom, fom.plusDays(15))),
        førsteFraværsdag = førsteFraværsdag,
        fnr = fnr,
        orgnummer = orgnummer,
        refusjon = refusjon
    )
    val id = observatør.sisteVedtaksperiode()

    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer, besvart = LocalDate.EPOCH.atStartOfDay())
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
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer, besvart = LocalDate.EPOCH.atStartOfDay())
    return id
}

internal fun AbstractEndToEndTest.forlengTilGodkjentVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    forlengTilGodkjenning(fom, tom, grad, fnr, orgnummer)
    håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), true, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengTilSimulering(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    nyPeriode(fom til tom, orgnummer, grad = grad, fnr = fnr)
    val id: IdInnhenter = observatør.sisteVedtaksperiode()
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer)
    assertTrue(person.personLogg.etterspurteBehov(id, Behovtype.Simulering)) { "Forventet at simulering er etterspurt" }
}

internal fun AbstractEndToEndTest.forlengTilGodkjenning(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    nyPeriode(fom til tom, orgnummer, grad = grad, fnr = fnr)
    val id: IdInnhenter = observatør.sisteVedtaksperiode()
    håndterYtelser(id, fnr = fnr, orgnummer = orgnummer)
    if (person.personLogg.etterspurteBehov(id, Behovtype.Simulering)) håndterSimulering(id, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengVedtak(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    forlengTilGodkjentVedtak(fom, tom, grad, fnr, orgnummer)
    håndterUtbetalt(status = Oppdragstatus.AKSEPTERT, fnr = fnr, orgnummer = orgnummer)
}

internal fun AbstractEndToEndTest.forlengPeriode(
    fom: LocalDate,
    tom: LocalDate,
    grad: Prosentdel = 100.prosent,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER
) {
    nyPeriode(fom til tom, orgnummer, grad = grad, fnr = fnr)
}

internal fun AbstractEndToEndTest.håndterSøknadMedValidering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg perioder: Søknadsperiode,
    andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
    sendtTilNAVEllerArbeidsgiver: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
) {
    assertIkkeEtterspurt(
        Søknad::class, Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter,
        AbstractPersonTest.ORGNUMMER
    )
    håndterSøknad(
        *perioder,
        andreInntektskilder = andreInntektskilder,
        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
        orgnummer = orgnummer,
        fnr = fnr
    )
}

internal fun AbstractEndToEndTest.håndterSøknad(
    vararg perioder: Søknadsperiode,
    andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
    sendtTilNAVEllerArbeidsgiver: LocalDate = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
    id: UUID = UUID.randomUUID(),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    sykmeldingSkrevet: LocalDateTime? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    søknad(
        id,
        *perioder,
        andreInntektskilder = andreInntektskilder,
        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
        orgnummer = orgnummer,
        sykmeldingSkrevet = sykmeldingSkrevet,
        fnr = fnr
    ).håndter(Person::håndter)
    søknader[id] = Triple(sendtTilNAVEllerArbeidsgiver, andreInntektskilder, perioder)
    return id
}

internal fun AbstractEndToEndTest.håndterInntektsmeldingMedValidering(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
): UUID {
    assertIkkeEtterspurt(Inntektsmelding::class, Behovtype.InntekterForSammenligningsgrunnlag, vedtaksperiodeIdInnhenter, orgnummer)
    return håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag, beregnetInntekt = beregnetInntekt, refusjon, orgnummer = orgnummer, fnr = fnr)
}

internal fun AbstractEndToEndTest.håndterInntektsmelding(
    arbeidsgiverperioder: List<Periode>,
    førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
    beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    id: UUID = UUID.randomUUID(),
    harOpphørAvNaturalytelser: Boolean = false,
    arbeidsforholdId: String? = null,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
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
        fnr = fnr,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt
    ).håndter(Person::håndter)
    return id
}

internal fun AbstractEndToEndTest.håndterInntektsmeldingReplay(
    inntektsmeldingId: UUID,
    vedtaksperiodeId: UUID
) {
    val inntektsmeldinggenerator = inntektsmeldinger[inntektsmeldingId] ?: fail { "Fant ikke inntektsmelding med id $inntektsmeldingId" }
    assertTrue(observatør.bedtOmInntektsmeldingReplay(vedtaksperiodeId)) { "Vedtaksperioden har ikke bedt om replay av inntektsmelding" }
    inntektsmeldingReplay(inntektsmeldinggenerator(), vedtaksperiodeId)
        .håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterVilkårsgrunnlag(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    inntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
    medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
        inntekter = inntektperioderForSammenligningsgrunnlag {
            val skjæringstidspunkt = inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)
            skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                orgnummer inntekt inntekt
            }
        }
    ),
    inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
        inntekter = listOf(
            ArbeidsgiverInntekt(orgnummer, (0..2).map {
                val yearMonth = YearMonth.from(inspektør(orgnummer).skjæringstidspunkt(vedtaksperiodeIdInnhenter)).minusMonths(3L - it)
                ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                    yearMonth = yearMonth,
                    type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    inntekt = AbstractEndToEndTest.INNTEKT,
                    fordel = "fordel",
                    beskrivelse = "beskrivelse"
                )
            })
        ), arbeidsforhold = emptyList()
    ),
    arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = finnArbeidsgivere().map { Vilkårsgrunnlag.Arbeidsforhold(it, LocalDate.EPOCH, null) },
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
) {
    fun assertEtterspurt(behovtype: Behovtype) =
        assertEtterspurt(Vilkårsgrunnlag::class, behovtype, vedtaksperiodeIdInnhenter, orgnummer)

    assertEtterspurt(Behovtype.InntekterForSammenligningsgrunnlag)
    assertEtterspurt(Behovtype.InntekterForSykepengegrunnlag)
    assertEtterspurt(Behovtype.ArbeidsforholdV2)
    assertEtterspurt(Behovtype.Medlemskap)
    vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        medlemskapstatus = medlemskapstatus,
        orgnummer = orgnummer,
        arbeidsforhold = arbeidsforhold,
        inntektsvurdering = inntektsvurdering,
        inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
        fnr = fnr
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    simuleringOK: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    simuleringsresultat: Simulering.SimuleringResultat? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    simulering(vedtaksperiodeIdInnhenter, simuleringOK, fnr, orgnummer, simuleringsresultat).forEach { simulering -> simulering.håndter(Person::håndter) }
}

internal fun AbstractEndToEndTest.håndterSimulering(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingId: UUID,
    fagsystemId: String,
    fagområde: Fagområde,
    simuleringOK: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    simuleringsresultat: Simulering.SimuleringResultat? = standardSimuleringsresultat(orgnummer)
) {
    assertEtterspurt(Simulering::class, Behovtype.Simulering, vedtaksperiodeIdInnhenter, orgnummer)
    Simulering(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer).toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
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

internal fun AbstractEndToEndTest.håndterInfotrygdendring() {
    Infotrygdendring(UUID.randomUUID(), AbstractPersonTest.UNG_PERSON_FNR_2018.toString(), AbstractPersonTest.AKTØRID)
        .håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikk(
    vedtaksperiodeIdInnhenter: IdInnhenter,
    vararg utbetalinger: Infotrygdperiode,
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    statslønn: Boolean = false,
    besvart: LocalDateTime = LocalDateTime.now()
) {
    val bedtOmSykepengehistorikk = person.personLogg.etterspurteBehov(vedtaksperiodeIdInnhenter, Behovtype.Sykepengehistorikk, orgnummer)
    if (bedtOmSykepengehistorikk) assertEtterspurt(Utbetalingshistorikk::class, Behovtype.Sykepengehistorikk, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingshistorikk(
        vedtaksperiodeIdInnhenter = vedtaksperiodeIdInnhenter,
        utbetalinger = utbetalinger.toList(),
        inntektshistorikk = inntektshistorikk,
        orgnummer = orgnummer,
        harStatslønn = statslønn,
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
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    dødsdato: LocalDate? = null,
    statslønn: Boolean = false,
    arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
    arbeidsavklaringspenger: List<Periode> = emptyList(),
    dagpenger: List<Periode> = emptyList(),
    besvart: LocalDateTime = LocalDateTime.now(),
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018
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
    assertEtterspurt(Behovtype.Dødsinfo)

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
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    antallGangerPåminnet: Int = 1
) {
    påminnelse(
        vedtaksperiodeId = vedtaksperiodeIdInnhenter.id(orgnummer),
        påminnetTilstand = påminnetTilstand,
        tilstandsendringstidspunkt = tilstandsendringstidspunkt,
        orgnummer = orgnummer,
        antallGangerPåminnet = antallGangerPåminnet
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingsgodkjenning(
    vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
    utbetalingGodkjent: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    automatiskBehandling: Boolean = false,
    utbetalingId: UUID = UUID.fromString(
        person.personLogg.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"]
            ?: throw IllegalStateException("Finner ikke utbetalingId i: ${person.personLogg.sisteBehov(Behovtype.Godkjenning).kontekst()}")
    ),
) {
    assertEtterspurt(Utbetalingsgodkjenning::class, Behovtype.Godkjenning, vedtaksperiodeIdInnhenter, orgnummer)
    utbetalingsgodkjenning(vedtaksperiodeIdInnhenter, utbetalingGodkjent, fnr, orgnummer, automatiskBehandling, utbetalingId).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    sendOverførtKvittering: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String,
    utbetalingId: UUID? = null,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    val faktiskUtbetalingId = utbetalingId?.toString() ?: person.personLogg.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId")
    if (sendOverførtKvittering) {
        UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AbstractPersonTest.AKTØRID,
            fødselsnummer = fnr.toString(),
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = faktiskUtbetalingId,
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ).håndter(Person::håndter)
    }
    utbetaling(
        fagsystemId = fagsystemId,
        status = status,
        fnr = fnr,
        orgnummer = orgnummer,
        meldingsreferanseId = meldingsreferanseId,
        utbetalingId = UUID.fromString(faktiskUtbetalingId)
    ).håndter(Person::håndter)
}

private fun Oppdrag.fagsytemIdOrNull() = if (harUtbetalinger()) inspektør.fagsystemId() else null

private fun AbstractEndToEndTest.førsteUhåndterteUtbetalingsbehov(orgnummer: String): Pair<UUID, List<String>>? {
    val utbetalingsbehovUtbetalingIder = person.personLogg.behov()
        .filter { it.type == Behovtype.Utbetaling }
        .map { UUID.fromString(it.kontekst().getValue("utbetalingId")) }

    return inspektør(orgnummer).utbetalinger
        .filter { it.inspektør.tilstand in setOf(Utbetaling.Sendt, Utbetaling.Overført) }
        .also { require(it.size < 2) { "For mange utbetalinger i spill! Er sendt ut godkjenningsbehov for periodene ${it.map { utbetaling -> utbetaling.inspektør.periode }}" } }
        .firstOrNull { it.inspektør.utbetalingId in utbetalingsbehovUtbetalingIder }
        ?.let {
            it.inspektør.utbetalingId to listOfNotNull(
                it.inspektør.arbeidsgiverOppdrag.fagsytemIdOrNull(),
                it.inspektør.personOppdrag.fagsytemIdOrNull()
            )
        }
}

internal fun AbstractEndToEndTest.håndterUtbetalt(
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    sendOverførtKvittering: Boolean = true,
    fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    førsteUhåndterteUtbetalingsbehov(orgnummer)?.also { (utbetalingId, fagsystemIder) ->
        fagsystemIder.forEach { fagsystemId ->
            håndterUtbetalt(status, sendOverførtKvittering, fnr, orgnummer, fagsystemId, utbetalingId, meldingsreferanseId)
        }
    }
}

internal fun AbstractEndToEndTest.håndterGrunnbeløpsregulering(
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
    gyldighetsdato: LocalDate
) {
    Grunnbeløpsregulering(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = orgnummer,
        gyldighetsdato = gyldighetsdato,
        fagsystemId = fagsystemId,
        aktivitetslogg = Aktivitetslogg()
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterAnnullerUtbetaling(
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    fagsystemId: String = inspektør.arbeidsgiverOppdrag.last().fagsystemId(),
    opprettet: LocalDateTime = LocalDateTime.now()
) {
    AnnullerUtbetaling(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AbstractPersonTest.AKTØRID,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = orgnummer,
        fagsystemId = fagsystemId,
        saksbehandlerIdent = "Ola Nordmann",
        saksbehandlerEpost = "tbd@nav.no",
        opprettet = opprettet
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterOverstyrInntekt(
    inntekt: Inntekt = 31000.månedlig,
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    skjæringstidspunkt: LocalDate,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    OverstyrInntekt(
        meldingsreferanseId = meldingsreferanseId,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        organisasjonsnummer = orgnummer,
        inntekt = inntekt,
        skjæringstidspunkt = skjæringstidspunkt
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterOverstyrTidslinje(
    overstyringsdager: List<ManuellOverskrivingDag> = listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag, 100)),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
    meldingsreferanseId: UUID = UUID.randomUUID()
) {
    OverstyrTidslinje(
        meldingsreferanseId = meldingsreferanseId,
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        organisasjonsnummer = orgnummer,
        dager = overstyringsdager,
        opprettet = LocalDateTime.now()
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterOverstyrArbeidsforhold(
    skjæringstidspunkt: LocalDate,
    overstyrteArbeidsforhold: List<OverstyrArbeidsforhold.ArbeidsforholdOverstyrt>
) {
    OverstyrArbeidsforhold(
        meldingsreferanseId = UUID.randomUUID(),
        fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
        aktørId = AbstractPersonTest.AKTØRID,
        skjæringstidspunkt = skjæringstidspunkt,
        overstyrteArbeidsforhold = overstyrteArbeidsforhold
    ).håndter(Person::håndter)
}

internal fun AbstractEndToEndTest.håndterUtbetalingshistorikkUtenValidering(
    vararg utbetalinger: Infotrygdperiode,
    inntektshistorikk: List<Inntektsopplysning> = emptyList(),
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
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
    orgnummer: String = AbstractPersonTest.ORGNUMMER,
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
        AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
        AVVENTER_BLOKKERENDE_PERIODE,
        AVVENTER_HISTORIKK,
        AVVENTER_VILKÅRSPRØVING,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET
    ) else arrayOf(
        START,
        AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
        AVVENTER_BLOKKERENDE_PERIODE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET
    )

internal fun TIL_AVSLUTTET_FORLENGELSE(førsteAG: Boolean = true) =
    if (førsteAG) arrayOf(
        START,
        AVVENTER_BLOKKERENDE_PERIODE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET,
    ) else arrayOf(
        START,
        AVVENTER_BLOKKERENDE_PERIODE,
        AVVENTER_HISTORIKK,
        AVVENTER_SIMULERING,
        AVVENTER_GODKJENNING,
        TIL_UTBETALING,
        AVSLUTTET,
    )

internal fun AbstractEndToEndTest.håndterOverstyringSykedag(periode: Periode) = håndterOverstyrTidslinje(periode.map { manuellSykedag(it) })

internal fun AbstractEndToEndTest.prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
    gapPeriode(periode, orgnummer, sykedagstelling)
    historikk(orgnummer, sykedagstelling)
    betale(orgnummer)
}

internal fun AbstractEndToEndTest.gapPeriode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
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
                }, arbeidsforhold = emptyList()
            )
    )
    )
}

internal fun AbstractEndToEndTest.nyPeriode(periode: Periode, vararg orgnummer: String = arrayOf(AbstractPersonTest.ORGNUMMER), grad: Prosentdel = 100.prosent, fnr: Fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018) {
    orgnummer.forEach { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, grad), fnr = fnr, orgnummer = it) }
    orgnummer.forEach {
        håndterSøknad(Søknadsperiode.Sykdom(periode.start, periode.endInclusive, grad), fnr = fnr, orgnummer = it)
    }
}

internal fun AbstractEndToEndTest.betale(orgnummer: String) {
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
            orgnummer = orgnummer,
            fagsystemId = inspektør(orgnummer).fagsystemId(1.vedtaksperiode),
            utbetalingId = person.personLogg.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId"),
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
