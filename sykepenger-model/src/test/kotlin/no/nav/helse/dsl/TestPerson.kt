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
import no.nav.helse.hendelser.AndreYtelser
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
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
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.fail

internal class TestPerson(
    private val observatør: TestObservatør,
    internal val person: Person,
    deferredLog: DeferredLog = DeferredLog()
) {

    constructor(
        observatør: TestObservatør,
        personidentifikator: Personidentifikator = UNG_PERSON_FNR_2018,
        fødselsdato: LocalDate = UNG_PERSON_FØDSELSDATO,
        jurist: SubsumsjonsListLog,
        deferredLog: DeferredLog = DeferredLog(),
        regler: MaksimumSykepengedagerregler = NormalArbeidstaker
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
    private val ugyldigeSituasjoner = UgyldigeSituasjonerObservatør(person)
    private val eventBus = EventBus().apply {
        register(ugyldigeSituasjoner)
        register(vedtaksperiodesamler)
        register(behovsamler)
        register(observatør)
    }

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

    private fun <T : Hendelse> T.håndter(håndter: Person.(EventBus, T, IAktivitetslogg) -> Unit): T {
        forrigeAktivitetslogg = Aktivitetslogg(personlogg)
        try {
            person.håndter(eventBus, this, forrigeAktivitetslogg)
        } finally {
            varslersamler.registrerVarsler(forrigeAktivitetslogg.varsel)
            behovsamler.registrerBehov(forrigeAktivitetslogg)
        }
        return this
    }

    internal fun validerState(assertetVarsler: Varslersamler.AssertetVarsler) {
        behovsamler.loggUbesvarteBehov()
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

    internal fun håndterUtbetalingshistorikkForFeriepenger(
        opptjeningsår: Year,
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = emptyList(),
        feriepengehistorikk: List<UtbetalingshistorikkForFeriepenger.Feriepenger> = emptyList(),
        datoForSisteFeriepengekjøringIInfotrygd: LocalDate,
        skalBeregnesManuelt: Boolean = false
    ) = personHendelsefabrikk.lagUtbetalingshistorikkForFeriepenger(
        opptjeningsår, utbetalinger, feriepengehistorikk, datoForSisteFeriepengekjøringIInfotrygd, skalBeregnesManuelt
    ).håndter(Person::håndterUtbetalingshistorikkForFeriepenger)

    internal fun håndterFeriepengerUtbetalt(
        fagsystemId: String,
        orgnummer: String,
        status: Oppdragstatus = Oppdragstatus.AKSEPTERT
    ) {
        val (utbetalingId) = behovsamler.feriepengerutbetalingsdetaljer().last()

        FeriepengeutbetalingHendelse(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer),
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            status = status,
            melding = "hey",
            avstemmingsnøkkel = 654321L,
            overføringstidspunkt = LocalDateTime.now()
        ).håndter(Person::håndterFeriepengeutbetalingHendelse)
    }

    operator fun <R> invoke(testblokk: TestPerson.() -> R): R {
        return testblokk(this)
    }

    fun dto(): PersonUtDto {
        return person.dto()
    }

    fun events(filter: EventFilter = { true }) = eventBus
        .events
        .filter(filter)

    fun behandlingevents() = events(behandlingerEventFilter)

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
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null,
            fraværFørSykmelding: Boolean? = null,
            harOppgittAvvikling: Boolean? = null,
            harOppgittVarigEndring: Boolean? = null,
            harOppgittNyIArbeidslivet: Boolean? = null,
            harOppgittOpprettholdtInntekt: Boolean? = null,
            harOppgittOppholdIUtlandet: Boolean? = null
        ) =
            behovsamler.håndterForespørslerOmReplayAvInntektsmeldingSomFølgeAv(
                operasjon = {
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
                            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
                            fraværFørSykmelding = fraværFørSykmelding,
                            harOppgittAvvikling = harOppgittAvvikling,
                            harOppgittVarigEndring = harOppgittVarigEndring,
                            harOppgittNyIArbeidslivet = harOppgittNyIArbeidslivet,
                            harOppgittOpprettholdtInntekt = harOppgittOpprettholdtInntekt,
                            harOppgittOppholdIUtlandet = harOppgittOppholdIUtlandet
                        ).håndter(Person::håndterSøknad)
                    }?.also {
                        if (behovsamler.harForespurtHistorikkFraInfotrygd(it)) {
                            arbeidsgiverHendelsefabrikk.lagUtbetalingshistorikk(it).håndter(Person::håndterUtbetalingshistorikk)
                        }
                    }
                },
                håndterForespørsel = ::håndterInntektsmeldingReplay
            )

        internal fun håndterFørstegangssøknadSelvstendig(
            periode: Periode,
            sykdomsgrad: Prosentdel = 100.prosent,
            arbeidssituasjon: Søknad.Arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt> = listOf(
                Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
            ),
            sendtTilNAVEllerArbeidsgiver: Temporal? = null,
            fraværFørSykmelding: Boolean? = false,
            harOppgittAvvikling: Boolean? = null,
            harOppgittVarigEndring: Boolean? = null,
            harOppgittNyIArbeidslivet: Boolean? = null,
            harOppgittOpprettholdtInntekt: Boolean? = null,
            harOppgittOppholdIUtlandet: Boolean? = null,
            søknadId: UUID = UUID.randomUUID()
        ) = håndterSøknad(
            Sykdom(periode.start, periode.endInclusive, sykdomsgrad),
            arbeidssituasjon = arbeidssituasjon,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
            fraværFørSykmelding = fraværFørSykmelding,
            harOppgittAvvikling = harOppgittAvvikling,
            harOppgittVarigEndring = harOppgittVarigEndring,
            harOppgittNyIArbeidslivet = harOppgittNyIArbeidslivet,
            harOppgittOpprettholdtInntekt = harOppgittOpprettholdtInntekt,
            harOppgittOppholdIUtlandet = harOppgittOppholdIUtlandet,
            søknadId = søknadId
        )

        internal fun håndterForlengelsessøknadSelvstendig(
            periode: Periode,
            sykdomsgrad: Prosentdel = 100.prosent,
            arbeidssituasjon: Søknad.Arbeidssituasjon = Søknad.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt> = listOf(
                Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
            ),
            sendtTilNAVEllerArbeidsgiver: Temporal? = null,
            fraværFørSykmelding: Boolean? = null,
            harOppgittAvvikling: Boolean? = null,
            harOppgittVarigEndring: Boolean? = null,
            harOppgittNyIArbeidslivet: Boolean? = null
        ) = håndterSøknad(
            Sykdom(periode.start, periode.endInclusive, sykdomsgrad),
            arbeidssituasjon = arbeidssituasjon,
            pensjonsgivendeInntekter = pensjonsgivendeInntekter,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver,
            fraværFørSykmelding = fraværFørSykmelding,
            harOppgittAvvikling = harOppgittAvvikling,
            harOppgittVarigEndring = harOppgittVarigEndring,
            harOppgittNyIArbeidslivet = harOppgittNyIArbeidslivet
        )

        internal fun håndterInntektsopplysningerFraLagretInnteksmelding(
            meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            inntektssmeldingMeldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            inntektsmeldingMottatt: LocalDateTime = LocalDateTime.now(),
            vedtaksperiodeId: UUID,
            inntekt: Inntekt,
            refusjon: Inntekt
        ): MeldingsreferanseId {
            arbeidsgiverHendelsefabrikk.lagInntektsopplysningerFraLagretInnteksmelding(
                meldingsreferanseId = meldingsreferanseId,
                inntektsmeldingMeldingsreferanseId = inntektssmeldingMeldingsreferanseId,
                inntektsmeldingMottatt = inntektsmeldingMottatt,
                vedtaksperiodeId = vedtaksperiodeId,
                inntekt = inntekt,
                refusjon = refusjon
            )
                .håndter(Person::håndterInntektsopplysningerFraLagretInntektsmelding)
            return meldingsreferanseId
        }

        internal fun håndterInntektsmelding(
            arbeidsgiverperioder: List<Periode>,
            beregnetInntekt: Inntekt = INNTEKT,
            førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOf { it.start },
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            opphørAvNaturalytelser: List<Inntektsmelding.OpphørAvNaturalytelse> = emptyList(),
            begrunnelseForReduksjonEllerIkkeUtbetalt: String? = null,
            id: UUID = UUID.randomUUID(),
            orgnummer: String = "",
            mottatt: LocalDateTime = LocalDateTime.now(),
            arbeidsforholdId: String? = null,
        ): UUID {
            arbeidsgiverHendelsefabrikk.lagInntektsmelding(
                arbeidsgiverperioder,
                beregnetInntekt,
                førsteFraværsdag,
                refusjon,
                opphørAvNaturalytelser,
                begrunnelseForReduksjonEllerIkkeUtbetalt,
                id,
                mottatt = mottatt,
                arbeidsforholdId = arbeidsforholdId
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

        private fun håndterInntektsmeldingReplay(forespørsel: Forespørsel, alleredeHåndterteInntektsmeldinger: Set<UUID>) {
            val fabrikk = arbeidsgivere[forespørsel.orgnr]?.arbeidsgiverHendelsefabrikk ?: arbeidsgiverHendelsefabrikk
            fabrikk.lagInntektsmeldingReplay(forespørsel, alleredeHåndterteInntektsmeldinger)
                .håndter(Person::håndterInntektsmeldingerReplay)
        }

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            inntekterForOpptjeningsvurdering: List<Pair<String, Inntekt>>? = null,
            skatteinntekt: Inntekt? = INNTEKT,
            orgnummer: String = "aa"
        ) = håndterVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntekterForOpptjeningsvurdering = inntekterForOpptjeningsvurdering,
            skatteinntekt = skatteinntekt
        )

        internal fun håndterVilkårsgrunnlag(
            vedtaksperiodeId: UUID = 1.vedtaksperiode,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
            inntekterForOpptjeningsvurdering: List<Pair<String, Inntekt>>? = null,
            skatteinntekt: Inntekt? = INNTEKT,
            orgnummer: String = "aa"
        ) = håndterVilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            medlemskapstatus = medlemskapstatus,
            skatteinntekter = skatteinntekt?.let { listOf(this.orgnummer to it) } ?: emptyList(),
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

        internal fun håndterVilkårsgrunnlagSelvstendig(vedtaksperiodeId: UUID = 1.vedtaksperiode, skatteInntekter: List<Pair<String, Inntekt>> = emptyList()) {
            håndterVilkårsgrunnlag(vedtaksperiodeId, skatteinntekter = skatteInntekter)
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

            behovsamler.bekreftForespurtVilkårsprøving(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId,
                skjæringstidspunkt,
                medlemskapstatus,
                arbeidsforhold,
                inntektsvurderingForSykepengegrunnlag,
                inntekterForOpptjeningsvurdering
            ).håndter(Person::håndterVilkårsgrunnlag)
        }

        internal fun håndterYtelserSelvstendig(
            vedtaksperiodeId: UUID,
            foreldrepenger: List<GradertPeriode> = emptyList(),
            svangerskapspenger: List<GradertPeriode> = emptyList(),
            pleiepenger: List<GradertPeriode> = emptyList(),
            omsorgspenger: List<GradertPeriode> = emptyList(),
            opplæringspenger: List<GradertPeriode> = emptyList(),
            institusjonsoppholdsperioder: List<no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
            arbeidsavklaringspengerV2: List<Periode> = emptyList(),
            dagpenger: List<Periode> = emptyList(),
            inntekterForBeregning: List<InntekterForBeregning.Inntektsperiode> = emptyList(),
            selvstendigForsikring: SelvstendigForsikring? = null,
            andreYtelser: List<AndreYtelser.PeriodeMedAnnenYtelse> = emptyList(),
            orgnummer: String = "aa"
        ) {
            behovsamler.bekreftForespurtBeregningAvSelvstendig(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, arbeidsavklaringspengerV2, dagpenger, inntekterForBeregning, selvstendigForsikring, andreYtelser)
                .håndter(Person::håndterYtelser)
        }

        internal fun håndterYtelser(
            vedtaksperiodeId: UUID,
            foreldrepenger: List<GradertPeriode> = emptyList(),
            svangerskapspenger: List<GradertPeriode> = emptyList(),
            pleiepenger: List<GradertPeriode> = emptyList(),
            omsorgspenger: List<GradertPeriode> = emptyList(),
            opplæringspenger: List<GradertPeriode> = emptyList(),
            institusjonsoppholdsperioder: List<no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
            arbeidsavklaringspengerV2: List<Periode> = emptyList(),
            dagpenger: List<Periode> = emptyList(),
            inntekterForBeregning: List<InntekterForBeregning.Inntektsperiode> = emptyList(),
            andreYtelser: List<AndreYtelser.PeriodeMedAnnenYtelse> = emptyList(),
            orgnummer: String = "aa"
        ) {
            behovsamler.bekreftForespurtBeregningAvArbeidstaker(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagYtelser(vedtaksperiodeId, foreldrepenger, svangerskapspenger, pleiepenger, omsorgspenger, opplæringspenger, institusjonsoppholdsperioder, arbeidsavklaringspengerV2, dagpenger, inntekterForBeregning, null, andreYtelser)
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
            behovsamler.simuleringsdetaljer(vedtaksperiodeId).forEach { (vedtaksperiodeId, utbetalingId, fagsystemId, fagområde) ->
                arbeidsgiverHendelsefabrikk.lagSimulering(vedtaksperiodeId, utbetalingId, fagsystemId, fagområde, simuleringOK, simuleringsresultat)
                    .håndter(Person::håndterSimulering)
            }
        }

        internal fun håndterUtbetalingsgodkjenning(vedtaksperiodeId: UUID, godkjent: Boolean = true, automatiskBehandling: Boolean = true, godkjenttidspunkt: LocalDateTime = LocalDateTime.now()) {
            val (behandlingId, utbetalingId) = behovsamler.godkjenningsdetaljer(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagUtbetalingsgodkjenning(vedtaksperiodeId, behandlingId, godkjent, automatiskBehandling, utbetalingId, godkjenttidspunkt)
                .håndter(Person::håndterUtbetalingsgodkjenning)
        }

        internal fun håndterUtbetalingshistorikkEtterInfotrygdendring(vararg utbetalinger: Infotrygdperiode) {
            behovsamler.håndterForespørslerOmReplayAvInntektsmeldingSomFølgeAv(
                operasjon = {
                    arbeidsgiverHendelsefabrikk.lagUtbetalingshistorikkEtterInfotrygdendring(utbetalinger.toList())
                        .håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)
                },
                håndterForespørsel = ::håndterInntektsmeldingReplay
            )
        }

        internal fun håndterVedtakFattet(vedtaksperiodeId: UUID, automatisert: Boolean = true, vedtakFattetTidspunkt: LocalDateTime = LocalDateTime.now()) {
            val (behandlingId, utbetalingId) = behovsamler.godkjenningsdetaljer(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagVedtakFattet(vedtaksperiodeId, behandlingId, utbetalingId, automatisert, vedtakFattetTidspunkt)
                .håndter(Person::håndterVedtakFattet)
        }

        internal fun håndterKanIkkeBehandlesHer(vedtaksperiodeId: UUID, automatisert: Boolean = true) {
            val (behandlingId, utbetalingId) = behovsamler.godkjenningsdetaljer(vedtaksperiodeId)
            arbeidsgiverHendelsefabrikk.lagKanIkkeBehandlesHer(vedtaksperiodeId, behandlingId, utbetalingId, automatisert)
                .håndter(Person::håndterKanIkkeBehandlesHer)
        }

        internal fun håndterVedtakFattet(vedtaksperiodeId: UUID, behandlingId: UUID, utbetalingId: UUID, automatisert: Boolean = true, vedtakFattetTidspunkt: LocalDateTime = LocalDateTime.now()) {
            arbeidsgiverHendelsefabrikk.lagVedtakFattet(vedtaksperiodeId, behandlingId, utbetalingId, automatisert, vedtakFattetTidspunkt)
                .håndter(Person::håndterVedtakFattet)
        }

        internal fun håndterKanIkkeBehandlesHer(vedtaksperiodeId: UUID, behandlingId: UUID, utbetalingId: UUID, automatisert: Boolean = true) {
            arbeidsgiverHendelsefabrikk.lagKanIkkeBehandlesHer(vedtaksperiodeId, behandlingId, utbetalingId, automatisert)
                .håndter(Person::håndterKanIkkeBehandlesHer)
        }

        internal fun håndterUtbetalt(status: Oppdragstatus, fagsystemId: String) {
            behovsamler.utbetalingsdetaljer(orgnummer).lastOrNull { it.fagsystemId == fagsystemId }?.also { (vedtaksperiodeId, behandlingId, utbetalingId) ->
                arbeidsgiverHendelsefabrikk.lagUtbetalinghendelse(vedtaksperiodeId, behandlingId, utbetalingId, fagsystemId, status)
                    .håndter(Person::håndterUtbetalingHendelse)
            }
        }

        internal fun håndterUtbetalt(status: Oppdragstatus = Oppdragstatus.AKSEPTERT) {
            behovsamler.utbetalingsdetaljer(orgnummer).forEach { utbetalingsdetaljer ->
                arbeidsgiverHendelsefabrikk.lagUtbetalinghendelse(utbetalingsdetaljer.vedtaksperiodeId, utbetalingsdetaljer.behandlingId, utbetalingsdetaljer.utbetalingId, utbetalingsdetaljer.fagsystemId, status)
                    .håndter(Person::håndterUtbetalingHendelse)
            }
        }

        internal fun håndterAnnullering(vedtaksperiodeId: UUID) {
            arbeidsgiverHendelsefabrikk.lagAnnullering(vedtaksperiodeId).håndter(Person::håndterAnnulerUtbetaling)
        }

        internal fun håndterIdentOpphørt(nyttFnr: Personidentifikator) {
            arbeidsgiverHendelsefabrikk.lagIdentOpphørt().håndter { bus, it, aktivitetslogg ->
                håndterIdentOpphørt(bus, it, aktivitetslogg, nyttFnr)
            }
        }

        internal fun håndterPåminnelse(
            vedtaksperiodeId: UUID,
            tilstand: TilstandType,
            tilstandsendringstidspunkt: LocalDateTime = LocalDateTime.now(),
            nåtidspunkt: LocalDateTime = LocalDateTime.now(),
            flagg: Set<String> = emptySet()
        ) {
            behovsamler.håndterForespørslerOmReplayAvInntektsmeldingSomFølgeAv(
                operasjon = {
                    arbeidsgiverHendelsefabrikk.lagPåminnelse(vedtaksperiodeId, tilstand, tilstandsendringstidspunkt, nåtidspunkt, flagg = flagg)
                        .håndter(Person::håndterPåminnelse)
                },
                håndterForespørsel = ::håndterInntektsmeldingReplay
            )
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

        internal fun håndterMinimumSykdomsgradVurdert(
            perioderMedMinimumSykdomsgradVurdertOK: List<Periode>,
            perioderMedMinimumSykdomsgradVurdertIkkeOK: List<Periode> = emptyList()
        ) = personHendelsefabrikk.lagMinimumSykdomsgradsvurderingMelding(
            perioderMedMinimumSykdomsgradVurdertOK.toSet(),
            perioderMedMinimumSykdomsgradVurdertIkkeOK.toSet()
        ).håndter(Person::håndterMinimumSykdomsgradsvurderingMelding)

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

typealias EventFilter = (EventSubscription.Event) -> Boolean

val behandlingerEventFilter: EventFilter = { it: EventSubscription.Event ->
    it is EventSubscription.VedtaksperiodeOpprettet || it is EventSubscription.VedtaksperiodeAnnullertEvent || it is EventSubscription.VedtaksperiodeForkastetEvent
        || it is EventSubscription.BehandlingOpprettetEvent || it is EventSubscription.BehandlingLukketEvent || it is EventSubscription.BehandlingForkastetEvent
        || it is EventSubscription.AvsluttetMedVedtakEvent
}
