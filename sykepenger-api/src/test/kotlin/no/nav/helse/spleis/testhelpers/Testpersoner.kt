package no.nav.helse.spleis.testhelpers

import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.april
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.PensjonsgivendeInntekt
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

private const val ORGNUMMER = "987654322"

internal fun arbeidstakerMedInfotrygdforlengelse(): Person {
    val eventBus = EventBus()
    val fom = 1.april
    val tom = 30.april

    val person = gjenopprettFraJSON("/personer/infotrygdforlengelse.json", skjemaversjon = 312, regelverkslogg = EmptyLog)

    val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER))

    val søknad =
        fabrikk.lagSøknad(
            Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent),
            arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER,
        )
    person.håndterSøknad(eventBus, søknad, Aktivitetslogg())

    val vedtaksperiodeId =
        eventBus.events
            .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
            .single()
            .vedtaksperiodeId

    val arbeidsgiveropplysninger =
        fabrikk.lagArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.april til 16.april),
            beregnetInntekt = 31000.månedlig,
            vedtaksperiodeId = vedtaksperiodeId,
        )
    person.håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, Aktivitetslogg())

    val arbeidsgiverinntekt =
        ArbeidsgiverInntekt(
            arbeidsgiver = ORGNUMMER,
            inntekter =
                listOf(YearMonth.of(2018, 1), YearMonth.of(2018, 2), YearMonth.of(2018, 3)).map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        yearMonth = it,
                        inntekt = 31000.månedlig,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        fordel = "a",
                        beskrivelse = "b",
                    )
                },
        )

    val vilkårsgrunnlag =
        fabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = 1.april,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            arbeidsforhold =
                listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer = ORGNUMMER,
                        ansettelseperiode = 1.april(2017) til LocalDate.MAX,
                        type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT,
                    ),
                ),
            inntektsvurderingForSykepengegrunnlag =
                InntektForSykepengegrunnlag(
                    listOf(arbeidsgiverinntekt),
                ),
            inntekterForOpptjeningsvurdering =
                InntekterForOpptjeningsvurdering(
                    listOf(arbeidsgiverinntekt).map {
                        it.copy(inntekter = listOf(it.inntekter.last()))
                    },
                ),
            forsikringsvurderingId = null,
        )
    person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, Aktivitetslogg())

    val overstyrArbeidsforhold =
        PersonHendelsefabrikk().lagOverstyrArbeidsforhold(
            skjæringstidspunkt = 1.april,
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(ORGNUMMER, deaktivert = true, "test"),
        )
    person.håndterOverstyrArbeidsforhold(eventBus, overstyrArbeidsforhold, Aktivitetslogg())

    return person
}

internal fun selvstendig(fødselsnummer: String): Person {
    val eventBus = EventBus()
    val aktivitetslogg = Aktivitetslogg()
    val fom = 1.april
    val tom = 30.april

    val person = Person(Personidentifikator(fødselsnummer), 1.januar(1990).alder, EmptyLog)

    val fabrikk = YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Selvstendig)

    val søknad =
        fabrikk.lagSøknad(
            Søknad.Søknadsperiode.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = 100.prosent),
            arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            pensjonsgivendeInntekter =
                listOf(
                    PensjonsgivendeInntekt(Year.of(2017), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    PensjonsgivendeInntekt(Year.of(2016), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    PensjonsgivendeInntekt(Year.of(2015), 1_000_000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                ),
        )
    person.håndterSøknad(eventBus, søknad, aktivitetslogg)

    val vedtaksperiodeId =
        eventBus.events
            .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
            .single()
            .vedtaksperiodeId

    person.håndterUtbetalingshistorikk(eventBus, fabrikk.lagUtbetalingshistorikk(vedtaksperiodeId), aktivitetslogg)

    val arbeidsgiverinntekt =
        ArbeidsgiverInntekt(
            arbeidsgiver = ORGNUMMER,
            inntekter =
                listOf(YearMonth.of(2018, 1), YearMonth.of(2018, 2), YearMonth.of(2018, 3)).map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        yearMonth = it,
                        inntekt = 31000.månedlig,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        fordel = "a",
                        beskrivelse = "b",
                    )
                },
        )

    val vilkårsgrunnlag =
        fabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = 1.april,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            arbeidsforhold =
                listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer = ORGNUMMER,
                        ansettelseperiode = 1.april(2017) til LocalDate.MAX,
                        type = Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT,
                    ),
                ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            inntekterForOpptjeningsvurdering = InntekterForOpptjeningsvurdering(emptyList()),
            forsikringsvurderingId = null,
        )
    person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetslogg)

    return person
}
