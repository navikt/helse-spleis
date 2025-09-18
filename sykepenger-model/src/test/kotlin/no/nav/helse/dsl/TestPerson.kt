package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.InntekterForOpptjeningsvurdering
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ventetid
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.fail

internal class TestPerson(
    private val observatør: TestObservatør,
    person: Person,
    deferredLog: DeferredLog = DeferredLog()
) {

    constructor(
        observatør: TestObservatør,
        personidentifikator: Personidentifikator = UNG_PERSON_FNR_2018,
        fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO,
        jurist: SubsumsjonsListLog,
        deferredLog: DeferredLog = DeferredLog(),
        regler: ArbeidsgiverRegler = NormalArbeidstaker
    ) : this(observatør, Person(personidentifikator, fødselsdato.alder, jurist, regler), deferredLog)

    internal companion object {
        internal operator fun <R> String.invoke(testPerson: TestPerson, testblokk: TestArbeidsgiver.() -> R) =
            testPerson.arbeidsgiver(this, testblokk)
    }

    private lateinit var forrigeAktivitetslogg: Aktivitetslogg
    internal val personlogg = Aktivitetslogg()
    private val behovsamler = Behovsamler(deferredLog)
    private val varslersamler = Varslersamler()
    private val personHendelsefabrikk = PersonHendelsefabrikk()
    private val vedtaksperiodesamler = Vedtaksperiodesamler(person)
    internal val person = person.also {
        it.addObserver(vedtaksperiodesamler)
        it.addObserver(behovsamler)
        it.addObserver(observatør)
    }
    private val ugyldigeSituasjoner = UgyldigeSituasjonerObservatør(person)

    private val arbeidsgivere = mutableMapOf<String, TestArbeidsgiver>()

    internal fun <INSPEKTØR> inspiser(inspektør: (Person) -> INSPEKTØR) = inspektør(person)
    internal fun view() = person.view()

    internal fun arbeidsgiver(orgnummer: String, behandlingsporing: Behandlingsporing.Yrkesaktivitet = orgnummer.tilYrkesaktivitet()) =
        arbeidsgivere.getOrPut(orgnummer) { TestArbeidsgiver(orgnummer, behandlingsporing) }

    internal fun <R> arbeidsgiver(orgnummer: String, block: TestArbeidsgiver.() -> R) =
        arbeidsgiver(orgnummer)(block)

    internal operator fun <R> String.invoke(testblokk: TestArbeidsgiver.() -> R) =
        arbeidsgiver(this, testblokk)

    private fun String.tilYrkesaktivitet(): Behandlingsporing.Yrkesaktivitet = when (this) {
        selvstendig -> Behandlingsporing.Yrkesaktivitet.Selvstendig
        frilans -> Behandlingsporing.Yrkesaktivitet.Frilans
        arbeidsledig -> Behandlingsporing.Yrkesaktivitet.Arbeidsledig
        else -> Behandlingsporing.Yrkesaktivitet.Arbeidstaker(this)
    }

    private fun <T : Hendelse> T.håndter(håndter: Person.(T, IAktivitetslogg) -> Unit): T {
        forrigeAktivitetslogg = Aktivitetslogg(personlogg)
        try {
            person.håndter(this, forrigeAktivitetslogg)
        } finally {
            varslersamler.registrerVarsler(forrigeAktivitetslogg.varsel)
            behovsamler.registrerBehov(forrigeAktivitetslogg)
        }
        return this
    }

    internal fun bekreftBehovOppfylt(assertetVarsler: Varslersamler.AssertetVarsler) {
        behovsamler.bekreftBehovOppfylt()
        varslersamler.bekreftVarslerAssertet(assertetVarsler)
        ugyldigeSituasjoner.bekreftVarselHarKnytningTilVedtaksperiode(personlogg.varsel)
    }

    internal fun håndterOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: ArbeidsforholdOverstyrt) {
        personHendelsefabrikk.lagOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold)
            .håndter(Person::håndterOverstyrArbeidsforhold)
    }

    internal fun håndterSkjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID, tidsstempel: LocalDateTime) {
        personHendelsefabrikk.lagSkjønnsmessigFastsettelse(skjæringstidspunkt, arbeidsgiveropplysninger, meldingsreferanseId, tidsstempel)
            .håndter(Person::håndterSkjønnsmessigFastsettelse)
    }

    internal fun håndterOverstyrArbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID, tidsstempel: LocalDateTime = LocalDateTime.now()) {
        personHendelsefabrikk.lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt, arbeidsgiveropplysninger, meldingsreferanseId, tidsstempel)
            .håndter(Person::håndterOverstyrArbeidsgiveropplysninger)
    }

    internal fun håndterDødsmelding(dødsdato: LocalDate) {
        personHendelsefabrikk.lagDødsmelding(dødsdato).håndter(Person::håndterDødsmelding)
    }

    operator fun <R> invoke(testblokk: TestPerson.() -> R): R {
        return testblokk(this)
    }

    fun dto(): PersonUtDto {
        return person.dto()
    }

    inner class TestArbeidsgiver(
        internal val orgnummer: String,
        private val behandlingsporing: Behandlingsporing.Yrkesaktivitet
    ) {
        private val arbeidsgiverHendelsefabrikk = ArbeidsgiverHendelsefabrikk(orgnummer, behandlingsporing)

        internal val inspektør get() = TestArbeidsgiverInspektør(person, orgnummer)

        internal val Int.vedtaksperiode get() = vedtaksperiodesamler.vedtaksperiodeId(orgnummer, this - 1)

        internal val sisteVedtaksperiode get() = vedtaksperiodesamler.sisteVedtaksperiode(orgnummer)

        internal fun håndterSykmelding(periode: Periode) = håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))

        internal fun håndterSykmelding(
            vararg sykmeldingsperiode: Sykmeldingsperiode,
            sykmeldingSkrevet: LocalDateTime? = null,
            mottatt: LocalDateTime? = null
        ) = arbeidsgiverHendelsefabrikk.lagSykmelding(*sykmeldingsperiode).håndter(Person::håndterSykmelding)

        internal fun håndterAvbruttSøknad(sykmeldingsperiode: Periode) = arbeidsgiverHendelsefabrikk.lagAvbruttSøknad(sykmeldingsperiode).håndter(Person::håndterAvbruttSøknad)

        internal fun håndterSøknad(
            periode: Periode,
            inntekterFraNyeArbeidsforhold: Boolean = false,
            sendTilGosys: Boolean = false,
            egenmeldinger: List<Periode> = emptyList(),
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null,
            søknadId: UUID = UUID.randomUUID()
        ) = håndterSøknad(
            Sykdom(periode.start, periode.endInclusive, 100.prosent),
            inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
            sendTilGosys = sendTilGosys,
            egenmeldinger = egenmeldinger,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            søknadId = søknadId
        )

        internal fun håndterArbeidsgiveropplysninger(vedtaksperiodeId: UUID, vararg opplysninger: Arbeidsgiveropplysning): UUID {
            val hendelse = arbeidsgiverHendelsefabrikk.lagArbeidsgiveropplysninger(vedtaksperiodeId = vedtaksperiodeId, opplysninger = opplysninger)
            observatør.forsikreForespurteArbeidsgiveropplysninger(vedtaksperiodeId, *hendelse.toTypedArray())
            hendelse.håndter(Person::håndterArbeidsgiveropplysninger)
            return hendelse.metadata.meldingsreferanseId.id
        }

        internal fun håndterKorrigerteArbeidsgiveropplysninger(vedtaksperiodeId: UUID, vararg opplysninger: Arbeidsgiveropplysning): UUID {
            val hendelse = arbeidsgiverHendelsefabrikk.lagKorrigerteArbeidsgiveropplysninger(vedtaksperiodeId = vedtaksperiodeId, opplysninger = opplysninger)
            hendelse.håndter(Person::håndterKorrigerteArbeidsgiveropplysninger)
            return hendelse.metadata.meldingsreferanseId.id
        }

        internal fun håndterSøknad(
            vararg perioder: Søknad.Søknadsperiode,
            egenmeldinger: List<Periode> = emptyList(),
            andreInntektskilder: Boolean = false,
            arbeidUtenforNorge: Boolean = false,
            yrkesskade: Boolean = false,
            sendtTilNAVEllerArbeidsgiver: Temporal? = null,
            sykmeldingSkrevet: LocalDateTime? = null,
            orgnummer: String = "",
            søknadId: UUID = UUID.randomUUID(),
            utenlandskSykmelding: Boolean = false,
            arbeidssituasjon: Søknad.Arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSTAKER,
            sendTilGosys: Boolean = false,
            registrert: LocalDateTime = LocalDateTime.now(),
            merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(),
            inntekterFraNyeArbeidsforhold: Boolean = false,
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null
        ) =
            behovsamler.fangInntektsmeldingReplay({
                vedtaksperiodesamler.fangVedtaksperiode(this.orgnummer) {
                    arbeidsgiverHendelsefabrikk.lagSøknad(
                        *perioder,
                        egenmeldinger = egenmeldinger,
                        andreInntektskilder = andreInntektskilder,
                        arbeidUtenforNorge = arbeidUtenforNorge,
                        sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
                        sykmeldingSkrevet = sykmeldingSkrevet,
                        id = søknadId,
                        yrkesskade = yrkesskade,
                        utenlandskSykmelding = utenlandskSykmelding,
                        arbeidssituasjon = arbeidssituasjon,
                        sendTilGosys = sendTilGosys,
                        registrert = registrert,
                        merknaderFraSykmelding = merknaderFraSykmelding,
                        inntekterFraNyeArbeidsforhold = inntekterFraNyeArbeidsforhold,
                        pensjonsgivendeInntekter = pensjonsgivendeInntekter
                    ).håndter(Person::håndterSøknad)
                }?.also {
                    if (behovsamler.harBehov(it, Sykepengehistorikk)) {
                        arbeidsgiverHendelsefabrikk.lagUtbetalingshistorikk(it).håndter(Person::håndterUtbetalingshistorikk)
                    }
                }
            }) { vedtaksperioderSomHarBedtOmReplay ->
                vedtaksperioderSomHarBedtOmReplay.forEach { forespørsel ->
                    håndterInntektsmeldingReplay(forespørsel)
                }
            }

        internal fun håndterSøknadSelvstendig(
            periode: Periode,
            ventetid: Periode = 1.januar til 16.januar,
            arbeidssituasjon: Søknad.Arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt> = listOf(
                Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INGEN, INGEN, INGEN),
                Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN),
                Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN)
            ),
            sendtTilNAVEllerArbeidsgiver: Temporal? = null
        ) = håndterSøknad(
            Sykdom(periode.start, periode.endInclusive, 100.prosent),
            Ventetid(ventetid),
            arbeidssituasjon = arbeidssituasjon,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver
        )

        internal fun håndterInntektsmelding(
            arbeidsgiverperioder: List<Periode>,
            beregnetInntekt: Inntekt = INNTEKT,
            førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
            begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
            id: UUID = UUID.randomUUID(),
            orgnummer: String = "",
            mottatt: LocalDateTime = LocalDateTime.now()
        ): UUID {
            arbeidsgiverHendelsefabrikk.lagInntektsmelding(
                arbeidsgiverperioder,
                beregnetInntekt,
                førsteFraværsdag,
                refusjon,
                opphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id,
                mottatt = mottatt
            ).håndter(Person::håndterInntektsmelding)
            return id
        }

        internal fun håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder: List<Periode>,
            beregnetInntekt: Inntekt = INNTEKT,
            vedtaksperiodeId: UUID = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.id,
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
            begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
            id: UUID = UUID.randomUUID(),
            mottatt: LocalDateTime = LocalDateTime.now()
        ): UUID {
            val arbeidsgiveropplysninger = Arbeidsgiveropplysninger(
                meldingsreferanseId = MeldingsreferanseId(id),
                innsendt = mottatt,
                registrert = mottatt.plusSeconds(1),
                behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                    organisasjonsnummer = this.orgnummer
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

            arbeidsgiveropplysninger.håndter(Person::håndterArbeidsgiveropplysninger)
            return id
        }

        internal fun håndterForkastSykmeldingsperioder(periode: Periode) =
            arbeidsgiverHendelsefabrikk.lagHåndterForkastSykmeldingsperioder(periode).håndter(Person::håndterForkastSykmeldingsperioder)

        internal fun håndterAnmodningOmForkasting(vedtaksperiodeId: UUID, force: Boolean = false) =
            arbeidsgiverHendelsefabrikk.lagAnmodningOmForkasting(vedtaksperiodeId, force).håndter(Person::håndterAnmodningOmForkasting)

        private fun håndterInntektsmeldingReplay(forespørsel: Forespørsel) {
            val håndterteInntektsmeldinger = behovsamler.håndterteInntektsmeldinger()
            behovsamler.bekreftOgKvitterReplay(forespørsel.vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagInntektsmeldingReplay(forespørsel, håndterteInntektsmeldinger)
                .håndter(Person::håndterInntektsmeldingerReplay)
        }

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            inntekterForOpptjeningsvurdering: List<Pair<String, Inntekt>>? = null,
            orgnummer: String = "aa"
        ) = håndterVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering
        )

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
            inntekterForOpptjeningsvurdering: List<Pair<String, Inntekt>>? = null,
            orgnummer: String = "aa"
        ) = håndterVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            medlemskapstatus = medlemskapstatus,
            skatteinntekter = listOf(this.orgnummer to INNTEKT),
            arbeidsforhold = arbeidsgivere.map { Triple(it.key, LocalDate.EPOCH, null) },
            inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering
        )

        /**
         * lager et vilkårsgrunnlag med samme inntekt for de oppgitte arbeidsgiverne,
         * inkl. arbeidsforhold.
         *
         * snarvei for å få et vilkårsgrunnlag for flere arbeidsgivere uten noe fuzz
         */
        internal fun håndterVilkårsgrunnlagFlereArbeidsgivere(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            vararg orgnumre: String,
            inntekt: Inntekt = INNTEKT,
            orgnummer: String = ""
        ) = håndterVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            skatteinntekter = orgnumre.map { it to inntekt }
        )

        /**
         * lager tre månedsinntekter for hver arbeidsgiver i skatteinntekter, med samme beløp hver måned.
         * arbeidsforhold-listen blir by default fylt ut med alle arbeidsgiverne i inntekt-listen
         */
        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            skatteinntekter: List<Pair<String, Inntekt>>,
            arbeidsforhold: List<Triple<String, LocalDate, LocalDate?>> = skatteinntekter.map { (orgnr, _) -> Triple(orgnr, LocalDate.EPOCH, null) },
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntekterForOpptjeningsvurdering: List<Pair<String, Inntekt>>? = null,
            orgnummer: String = this.orgnummer
        ) {
            val skjæringstidspunkt = inspektør.skjæringstidspunkt(vedtaksperiodeId)
            val opptjeningsinntekter = inntekterForOpptjeningsvurdering?.let {
                lagStandardInntekterForOpptjeningsvurdering(it, skjæringstidspunkt)
            }
            håndterVilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId,
                medlemskapstatus = medlemskapstatus,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(skatteinntekter, skjæringstidspunkt),
                inntekterForOpptjeningsvurdering = opptjeningsinntekter,
                arbeidsforhold = arbeidsforhold.map { (orgnr, fom, tom) ->
                    Vilkårsgrunnlag.Arbeidsforhold(orgnr, fom, tom, type = Arbeidsforholdtype.ORDINÆRT)
                },
                skjæringstidspunkt = skjæringstidspunkt
            )
        }

        internal fun håndterVilkårsgrunnlagSelvstendig(vedtaksperiodeId: UUID = 1.vedtaksperiode) {
            håndterVilkårsgrunnlag(vedtaksperiodeId, skatteinntekter = emptyList())
        }

        /**
         * lager månedsinntekter fra de oppgitte månedene; hver måned har en liste av orgnummer-til-inntekt
         * lager by default arbeidsforhold for alle oppgitte orgnumre i inntekt-listen
         */
        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            månedligeInntekter: Map<YearMonth, List<Pair<String, Inntekt>>>,
            arbeidsforhold: List<Triple<String, LocalDate, LocalDate?>> = månedligeInntekter
                .flatMap { (_, inntekter) -> inntekter.map { (orgnr, _) -> orgnr } }
                .toSet()
                .map { orgnr -> Triple(orgnr, LocalDate.EPOCH, null) },
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            orgnummer: String = ""
        ) {
            val skjæringstidspunkt = inspektør.skjæringstidspunkt(vedtaksperiodeId)
            return håndterVilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId,
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
                                    MånedligInntekt(
                                        yearMonth = måned,
                                        inntekt = inntekt,
                                        type = MånedligInntekt.Inntekttype.LØNNSINNTEKT,
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

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            skatteinntekter: List<Pair<String, Inntekt>>,
            arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
            orgnummer: String = "aa"
        ) {
            val skjæringstidspunkt = inspektør.skjæringstidspunkt(vedtaksperiodeId)
            håndterVilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId,
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(skatteinntekter, skjæringstidspunkt),
                inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering(skatteinntekter, skjæringstidspunkt),
                arbeidsforhold = arbeidsforhold,
                skjæringstidspunkt = skjæringstidspunkt
            )
        }

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
            arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
            inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering? = null,
            skjæringstidspunkt: LocalDate = inspektør.skjæringstidspunkt(vedtaksperiodeId),
            orgnummer: String = "aa"
        ) {
            val inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering ?: run {

                when (behandlingsporing) {
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> lagStandardInntekterForOpptjeningsvurdering(this.orgnummer, INNTEKT, skjæringstidspunkt)

                    Behandlingsporing.Yrkesaktivitet.Frilans,
                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> lagStandardInntekterForOpptjeningsvurdering(this.orgnummer, 0.månedlig, skjæringstidspunkt)
                }
            }

            behovsamler.bekreftBehov(vedtaksperiodeId, InntekterForSykepengegrunnlag, ArbeidsforholdV2, Medlemskap)
            arbeidsgiverHendelsefabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId,
                skjæringstidspunkt,
                medlemskapstatus,
                arbeidsforhold,
                inntektsvurderingForSykepengegrunnlag,
                inntekterForOpptjeningsvurdering
            ).håndter(Person::håndterVilkårsgrunnlag)
        }

        internal fun håndterYtelser(
            vedtaksperiodeId: UUID,
            foreldrepenger: List<GradertPeriode> = emptyList(),
            svangerskapspenger: List<GradertPeriode> = emptyList(),
            pleiepenger: List<GradertPeriode> = emptyList(),
            omsorgspenger: List<GradertPeriode> = emptyList(),
            opplæringspenger: List<GradertPeriode> = emptyList(),
            institusjonsoppholdsperioder: List<no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
            arbeidsavklaringspenger: List<Periode> = emptyList(),
            dagpenger: List<Periode> = emptyList(),
            inntekterForBeregning: List<InntekterForBeregning.Inntektsperiode> = emptyList(),
            orgnummer: String = "aa"
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Dagpenger, Arbeidsavklaringspenger, Institusjonsopphold, Opplæringspenger, Pleiepenger, Omsorgspenger, Foreldrepenger, Aktivitet.Behov.Behovtype.InntekterForBeregning)
            arbeidsgiverHendelsefabrikk.lagYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, arbeidsavklaringspenger, dagpenger, inntekterForBeregning)
                .håndter(Person::håndterYtelser)
        }

        internal fun håndterInntektsendringer(inntektsendringFom: LocalDate) {
            arbeidsgiverHendelsefabrikk.lagInntektsendringer(inntektsendringFom).håndter(Person::håndterInntektsendringer)
        }

        internal fun håndterSimulering(
            vedtaksperiodeId: UUID,
            simuleringOK: Boolean = true,
            simuleringsresultat: SimuleringResultatDto? = standardSimuleringsresultat(orgnummer)
        ) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Simulering)
            behovsamler.detaljerFor(vedtaksperiodeId, Simulering).forEach { (detaljer, kontekst) ->
                val fagsystemId = detaljer.getValue("fagsystemId") as String
                val fagområde = detaljer.getValue("fagområde") as String
                val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))

                arbeidsgiverHendelsefabrikk.lagSimulering(vedtaksperiodeId, utbetalingId, fagsystemId, fagområde, simuleringOK, simuleringsresultat).håndter(Person::håndterSimulering)
            }
        }

        internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true, automatiskBehandling: Boolean = true, godkjenttidspunkt: LocalDateTime = LocalDateTime.now()) {
            behovsamler.bekreftBehov(vedtaksperiodeId, Godkjenning)
            val (_, kontekst) = behovsamler.detaljerFor(vedtaksperiodeId, Godkjenning).last() // TODO: Det er last() istedenfor single() pga dette hacket: 1622276bcd7303fb46037bd6d9e114f071700d44
            val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))
            arbeidsgiverHendelsefabrikk.lagUtbetalingsgodkjenning(vedtaksperiodeId, godkjent, automatiskBehandling, utbetalingId, godkjenttidspunkt)
                .håndter(Person::håndterUtbetalingsgodkjenning)
        }

        internal fun håndterUtbetalingshistorikkEtterInfotrygdendring(vararg utbetalinger: Infotrygdperiode) {
            arbeidsgiverHendelsefabrikk.lagUtbetalingshistorikkEtterInfotrygdendring(utbetalinger.toList())
                .håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)
        }

        internal fun håndterVedtakFattet(vedtaksperiodeId: UUID, utbetalingId: UUID = inspektør.sisteUtbetalingId { vedtaksperiodeId }, automatisert: Boolean = true, vedtakFattetTidspunkt: LocalDateTime = LocalDateTime.now()) {
            arbeidsgiverHendelsefabrikk.lagVedtakFattet(vedtaksperiodeId, utbetalingId, automatisert, vedtakFattetTidspunkt)
                .håndter(Person::håndterVedtakFattet)
        }

        internal fun håndterKanIkkeBehandlesHer(vedtaksperiodeId: UUID, utbetalingId: UUID = inspektør.sisteUtbetalingId { vedtaksperiodeId }, automatisert: Boolean = true) {
            arbeidsgiverHendelsefabrikk.lagKanIkkeBehandlesHer(vedtaksperiodeId, utbetalingId, automatisert)
                .håndter(Person::håndterKanIkkeBehandlesHer)
        }

        internal fun håndterUtbetalt(status: Oppdragstatus = Oppdragstatus.AKSEPTERT) {
            behovsamler.bekreftBehov(orgnummer, Utbetaling)
            behovsamler.detaljerFor(orgnummer, Utbetaling).forEach { (detaljer, kontekst) ->
                val utbetalingId = UUID.fromString(kontekst.getValue("utbetalingId"))
                val fagsystemId = detaljer.getValue("fagsystemId") as String
                arbeidsgiverHendelsefabrikk.lagUtbetalinghendelse(utbetalingId, fagsystemId, status)
                    .håndter(Person::håndterUtbetalingHendelse)
            }
        }

        internal fun håndterAnnullering(utbetalingId: UUID) {
            arbeidsgiverHendelsefabrikk.lagAnnullering(utbetalingId).håndter(Person::håndterAnnulerUtbetaling)
        }

        internal fun håndterIdentOpphørt(nyttFnr: Personidentifikator) {
            arbeidsgiverHendelsefabrikk.lagIdentOpphørt().håndter { it, aktivitetslogg ->
                håndterIdentOpphørt(it, aktivitetslogg, nyttFnr)
            }
        }

        internal fun håndterSykepengegrunnlagForArbeidsgiver(
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            inntekter: List<MånedligInntekt>
        ): UUID {
            val inntektFraOrdningen = arbeidsgiverHendelsefabrikk.lagSykepengegrunnlagForArbeidsgiver(
                skjæringstidspunkt,
                inntekter
            )
            inntektFraOrdningen.håndter(Person::håndterSykepengegrunnlagForArbeidsgiver)
            return inntektFraOrdningen.metadata.meldingsreferanseId.id
        }

        internal fun håndterPåminnelse(
            vedtaksperiodeId: UUID,
            tilstand: TilstandType,
            tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
            nåtidspunkt: LocalDateTime = LocalDateTime.now(),
            flagg: Set<String> = emptySet()
        ) {
            arbeidsgiverHendelsefabrikk.lagPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt, nåtidspunkt, flagg = flagg)
                .håndter(Person::håndterPåminnelse)
        }

        internal fun håndterGrunnbeløpsregulering(skjæringstidspunkt: LocalDate) {
            arbeidsgiverHendelsefabrikk.lagGrunnbeløpsregulering(skjæringstidspunkt)
                .håndter(Person::håndterGrunnbeløpsregulering)
        }

        internal fun håndterPersonPåminnelse() {
            personHendelsefabrikk.lagPåminnelse()
                .håndter(Person::håndterPersonPåminnelse)
        }

        internal fun håndterOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: ArbeidsforholdOverstyrt) {
            personHendelsefabrikk.lagOverstyrArbeidsforhold(skjæringstidspunkt, *overstyrteArbeidsforhold)
                .håndter(Person::håndterOverstyrArbeidsforhold)
        }

        internal fun håndterOverstyrTidslinje(overstyringsdager: List<ManuellOverskrivingDag>) =
            arbeidsgiverHendelsefabrikk.lagHåndterOverstyrTidslinje(overstyringsdager)
                .håndter(Person::håndterOverstyrTidslinje)

        internal fun håndterOverstyrInntekt(
            skjæringstidspunkt: LocalDate,
            inntekt: Inntekt,
            hendelseId: UUID = UUID.randomUUID(),
            organisasjonsnummer: String = orgnummer,
        ) =
            arbeidsgiverHendelsefabrikk.lagOverstyrInntekt(hendelseId, skjæringstidspunkt, inntekt, organisasjonsnummer)
                .håndter(Person::håndterOverstyrArbeidsgiveropplysninger)

        internal fun håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt: LocalDate,
            overstyringer: List<OverstyrtArbeidsgiveropplysning>,
            hendelseId: UUID = UUID.randomUUID(),
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) =
            personHendelsefabrikk.lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt, overstyringer, hendelseId, tidsstempel)
                .håndter(Person::håndterOverstyrArbeidsgiveropplysninger)

        internal fun håndterUtbetalingshistorikkEtterInfotrygdendring(
            utbetalinger: List<Infotrygdperiode> = listOf(),
            besvart: LocalDateTime = LocalDateTime.now(),
            id: UUID = UUID.randomUUID()
        ) =
            arbeidsgiverHendelsefabrikk.lagUtbetalingshistorikkEtterInfotrygdendring(utbetalinger, besvart, id)
                .håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)

        internal fun håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår: Year
        ) =
            personHendelsefabrikk.lagUtbetalingshistorikkForFeriepenger(opptjeningsår)
                .håndter(Person::håndterUtbetalingshistorikkForFeriepenger)

        internal fun antallFeriepengeutbetalingerTilArbeidsgiver() = behovsamler.detaljerFor(orgnummer, Utbetaling)
            .count { (behov, _) ->
                behov["linjer"].toString().contains("SPREFAGFER-IOP")
            }

        operator fun <R> invoke(testblokk: TestArbeidsgiver.() -> R): R {
            return testblokk(this)
        }
    }
}

internal fun lagStandardSykepengegrunnlag(orgnummer: String, inntekt: Inntekt, skjæringstidspunkt: LocalDate) =
    lagStandardSykepengegrunnlag(listOf(orgnummer to inntekt), skjæringstidspunkt)

internal fun lagStandardSykepengegrunnlag(arbeidsgivere: List<Pair<String, Inntekt>>, skjæringstidspunkt: LocalDate) =
    InntektForSykepengegrunnlag(
        inntekter = inntektperioderForSykepengegrunnlag {
            val måned = YearMonth.from(skjæringstidspunkt)
            val periode = måned.minusMonths(3L).atDay(1) til måned.minusMonths(1).atDay(1)
            periode inntekter {
                arbeidsgivere.forEach { (orgnummer, inntekt) -> orgnummer inntekt inntekt }
            }
        }
    )

internal fun List<String>.lagStandardSykepengegrunnlag(inntekt: Inntekt, skjæringstidspunkt: LocalDate) =
    lagStandardSykepengegrunnlag(map { it to inntekt }, skjæringstidspunkt)

internal fun lagStandardInntekterForOpptjeningsvurdering(orgnummer: String, inntekt: Inntekt, skjæringstidspunkt: LocalDate) =
    lagStandardInntekterForOpptjeningsvurdering(listOf(orgnummer to inntekt), skjæringstidspunkt)

internal fun lagStandardInntekterForOpptjeningsvurdering(arbeidsgivere: List<Pair<String, Inntekt>>, skjæringstidspunkt: LocalDate) =
    InntekterForOpptjeningsvurdering(inntekter = arbeidsgivere.map { arbeidsgiver ->
        val orgnummer = arbeidsgiver.first
        val inntekt = arbeidsgiver.second
        val måned = skjæringstidspunkt.minusMonths(1L)
        ArbeidsgiverInntekt(
            arbeidsgiver = orgnummer,
            inntekter = listOf(
                MånedligInntekt(
                    YearMonth.from(måned),
                    inntekt,
                    MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    "kontantytelse",
                    "fastloenn"
                )
            )
        )
    })

internal fun standardSimuleringsresultat(orgnummer: String) = SimuleringResultatDto(
    totalbeløp = 2000,
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

internal fun TestPerson.TestArbeidsgiver.tilGodkjenning(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    førsteFraværsdag: LocalDate = periode.start,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode> = emptyList(),
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    ghosts: List<String> = emptyList()
): UUID {
    val arbeidsgivere = listOf(this.orgnummer) + ghosts
    val vedtaksperiode = nyPeriode(periode, grad)
    håndterInntektsmelding(arbeidsgiverperiode, beregnetInntekt, førsteFraværsdag, refusjon)
    håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiode, *arbeidsgivere.toTypedArray())
    håndterYtelser(vedtaksperiode)
    håndterSimulering(vedtaksperiode)
    return vedtaksperiode
}

internal fun TestPerson.TestArbeidsgiver.forlengelseTilGodkjenning(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
): UUID {
    val vedtaksperiode = nyPeriode(periode, grad)
    håndterYtelser(vedtaksperiode)
    håndterSimulering(vedtaksperiode)
    return vedtaksperiode
}

internal fun TestPerson.TestArbeidsgiver.nyttVedtak(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    førsteFraværsdag: LocalDate = periode.start,
    beregnetInntekt: Inntekt = INNTEKT,
    refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
    arbeidsgiverperiode: List<Periode> = emptyList(),
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT,
    ghosts: List<String> = emptyList()
) {
    val vedtaksperiode = tilGodkjenning(periode, grad, førsteFraværsdag, beregnetInntekt, refusjon, arbeidsgiverperiode, status, ghosts)
    håndterUtbetalingsgodkjenning(vedtaksperiode)
    håndterUtbetalt(status)
}

internal fun TestPerson.TestArbeidsgiver.forlengVedtak(
    periode: Periode,
    grad: Prosentdel = 100.prosent,
    status: Oppdragstatus = Oppdragstatus.AKSEPTERT
): UUID {
    val vedtaksperiode = nyPeriode(periode, grad)
    håndterYtelser(vedtaksperiode)
    håndterSimulering(vedtaksperiode)
    håndterUtbetalingsgodkjenning(vedtaksperiode)
    håndterUtbetalt(status)
    return vedtaksperiode
}

internal fun TestPerson.TestArbeidsgiver.nyPeriode(periode: Periode, grad: Prosentdel = 100.prosent, søknadId: UUID = UUID.randomUUID()): UUID {
    håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
    return håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad), søknadId = søknadId) ?: fail { "Det ble ikke opprettet noen vedtaksperiode." }
}

internal fun TestPerson.nyPeriode(periode: Periode, vararg orgnummer: String, grad: Prosentdel = 100.prosent) {
    orgnummer.forEach { it { håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive)) } }
    orgnummer.forEach { it { håndterSøknad(Sykdom(periode.start, periode.endInclusive, grad)) } }
}

