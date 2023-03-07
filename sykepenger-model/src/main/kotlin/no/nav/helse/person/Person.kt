package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Arbeidsgiver.Companion.avklarSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.beregnFeriepengerForAlleArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Arbeidsgiver.Companion.gjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.Companion.håndter
import no.nav.helse.person.Arbeidsgiver.Companion.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.person.Arbeidsgiver.Companion.igangsettOverstyring
import no.nav.helse.person.Arbeidsgiver.Companion.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.nekterOpprettelseAvPeriode
import no.nav.helse.person.Arbeidsgiver.Companion.nestemann
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.relevanteArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.slettUtgåtteSykmeldingsperioder
import no.nav.helse.person.Arbeidsgiver.Companion.sykefraværstilfelle
import no.nav.helse.person.Arbeidsgiver.Companion.validerVilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.Sykefraværstilfelleeventyr.Companion.varsleObservers
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Subaktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AG_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_10
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilderBuilder
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

class Person private constructor(
    private val aktørId: String,
    private val personidentifikator: Personidentifikator,
    private val alder: Alder,
    private val arbeidsgivere: MutableList<Arbeidsgiver>,
    override val aktivitetslogg: Aktivitetslogg,
    private val opprettet: LocalDateTime,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private var dødsdato: LocalDate?,
    private val jurist: MaskinellJurist,
    private val tidligereBehandlinger: List<Person> = emptyList(),
    private val regler: ArbeidsgiverRegler = NormalArbeidstaker
) : Subaktivitetskontekst {
    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        internal fun ferdigPerson(
            aktørId: String,
            personidentifikator: Personidentifikator,
            alder: Alder,
            arbeidsgivere: MutableList<Arbeidsgiver>,
            aktivitetslogg: Aktivitetslogg,
            opprettet: LocalDateTime,
            infotrygdhistorikk: Infotrygdhistorikk,
            vilkårsgrunnlaghistorikk: VilkårsgrunnlagHistorikk,
            dødsdato: LocalDate?,
            tidligereBehandlinger: List<Person> = emptyList(),
            jurist: MaskinellJurist
        ): Person = Person(
            aktørId = aktørId,
            personidentifikator = personidentifikator,
            alder = alder,
            arbeidsgivere = arbeidsgivere,
            aktivitetslogg = aktivitetslogg,
            opprettet = opprettet,
            infotrygdhistorikk = infotrygdhistorikk,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlaghistorikk,
            dødsdato = dødsdato,
            tidligereBehandlinger = tidligereBehandlinger,
            jurist = jurist
        )
    }

    internal constructor(
        aktørId: String,
        personidentifikator: Personidentifikator,
        alder: Alder,
        jurist: MaskinellJurist,
        regler: ArbeidsgiverRegler
    ) : this(
        aktørId,
        personidentifikator,
        alder,
        mutableListOf(),
        Aktivitetslogg(),
        LocalDateTime.now(),
        Infotrygdhistorikk(),
        VilkårsgrunnlagHistorikk(),
        null,
        jurist.medFødselsnummer(personidentifikator),
        emptyList<Person>(),
        regler = regler
    )
    constructor(
        aktørId: String,
        personidentifikator: Personidentifikator,
        alder: Alder,
        jurist: MaskinellJurist
    ) : this(aktørId, personidentifikator, alder, jurist, NormalArbeidstaker)

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(sykmelding: Sykmelding) {
        registrer(sykmelding, "Behandler sykmelding")
        tidligereBehandlinger(sykmelding, sykmelding.periode())
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(sykmelding)
        arbeidsgiver.håndter(sykmelding)
        håndterGjenoppta(sykmelding)
    }

    fun håndter(søknad: Søknad) = håndter(søknad, "søknad") { søknad.forUng(alder) }

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    fun håndter(inntektsmelding: InntektsmeldingReplay) {
        registrer(inntektsmelding, "Behandler replay av inntektsmelding")
        finnArbeidsgiver(inntektsmelding).håndter(inntektsmelding)
        håndterGjenoppta(inntektsmelding)
    }

    fun håndter(inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {
        registrer(inntektsmeldingReplayUtført, "Behandler inntektsmeldingReplayUtført")
        finnArbeidsgiver(inntektsmeldingReplayUtført).håndter(inntektsmeldingReplayUtført)
        håndterGjenoppta(inntektsmeldingReplayUtført)
    }

    private fun håndter(
        hendelse: SykdomstidslinjeHendelse,
        hendelsesmelding: String,
        before: () -> Any = { }
    ) {
        registrer(hendelse, "Behandler $hendelsesmelding")
        tidligereBehandlinger(hendelse, hendelse.periode())
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(hendelse)
        before()
        hendelse.fortsettÅBehandle(arbeidsgiver)
        håndterGjenoppta(hendelse)
    }

    private fun tidligereBehandlinger(hendelse: ArbeidstakerHendelse, periode: Periode) {
        val cutoff = periode.start.minusMonths(6)
        val andreBehandledeVedtaksperioder = tidligereBehandlinger.flatMap { it.vedtaksperioderEtter(cutoff) }
        if (andreBehandledeVedtaksperioder.isNotEmpty()) {
            hendelse.funksjonellFeil(Varselkode.RV_AN_5)
            val msg = andreBehandledeVedtaksperioder.map {
                "vedtaksperiode(${it.periode()})"
            }
            sikkerLogg.info("""hendelse: ${hendelse::class.java.simpleName} ($periode) kaster ut personen aktørid: $aktørId fnr: $personidentifikator 
                | tidligere behandlede identer: ${tidligereBehandlinger.map { it.personidentifikator }}
                | tidligere behandlede perioder: ${msg.joinToString { it }}
                | cutoff: $cutoff""".trimMargin())
        }
    }

    private fun vedtaksperioderEtter(dato: LocalDate) = arbeidsgivere.flatMap { it.vedtaksperioderEtter(dato) }

    fun håndter(infotrygdendring: Infotrygdendring) {
        infotrygdendring.kontekst(this)
        val tidligsteDato = arbeidsgivereMedSykdom().minOfOrNull { it.tidligsteDato() } ?: LocalDate.now()
        infotrygdhistorikk.oppfrisk(infotrygdendring, tidligsteDato)
        håndterGjenoppta(infotrygdendring)
    }

    fun håndter(utbetalingshistorikk: UtbetalingshistorikkEtterInfotrygdendring) {
        utbetalingshistorikk.kontekst(this)
        if(!utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)) return
        arbeidsgivere.håndter(utbetalingshistorikk, infotrygdhistorikk) // diff
        håndterGjenoppta(utbetalingshistorikk)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)
        finnArbeidsgiver(utbetalingshistorikk).håndter(utbetalingshistorikk, infotrygdhistorikk)
        håndterGjenoppta(utbetalingshistorikk)
    }

    fun håndter(utbetalingshistorikk: UtbetalingshistorikkForFeriepenger) {
        utbetalingshistorikk.kontekst(this)

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            utbetalingshistorikk.info("Starter beregning av feriepenger")
        }

        if (utbetalingshistorikk.skalBeregnesManuelt) {
            val msg = "Person er markert for manuell beregning av feriepenger - aktørId: $aktørId"
            sikkerLogg.info(msg)
            utbetalingshistorikk.info(msg)
            return
        }

        val feriepengeberegner = Feriepengeberegner(
            alder = alder,
            opptjeningsår = utbetalingshistorikk.opptjeningsår,
            utbetalingshistorikkForFeriepenger = utbetalingshistorikk,
            person = this
        )

        val feriepengepengebeløpPersonUtbetaltAvInfotrygd = utbetalingshistorikk.utbetalteFeriepengerTilPerson()
        val beregnetFeriepengebeløpPersonInfotrygd =
            feriepengeberegner.beregnFeriepengerForInfotrygdPerson().roundToInt()

        if (beregnetFeriepengebeløpPersonInfotrygd != 0 && beregnetFeriepengebeløpPersonInfotrygd !in feriepengepengebeløpPersonUtbetaltAvInfotrygd) {
            sikkerLogg.info(
                """
                Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
                AktørId: $aktørId
                Faktisk utbetalt beløp: $feriepengepengebeløpPersonUtbetaltAvInfotrygd
                Beregnet beløp: $beregnetFeriepengebeløpPersonInfotrygd
                """.trimIndent()
            )
        }

        utbetalingshistorikk.sikreAtArbeidsgivereEksisterer {
            arbeidsgivere.finnEllerOpprett(it, utbetalingshistorikk)
        }
        arbeidsgivere.beregnFeriepengerForAlleArbeidsgivere(
            aktørId,
            personidentifikator,
            feriepengeberegner,
            utbetalingshistorikk
        )

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            aktivitetslogg.info("Feriepenger er utbetalt")
        }
    }

    fun håndter(ytelser: Ytelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        ytelser.lagreDødsdato(this)

        finnArbeidsgiver(ytelser).håndter(ytelser, infotrygdhistorikk) { subsumsjonObserver ->
            arbeidsgiverUtbetalinger(subsumsjonObserver = subsumsjonObserver, hendelse = ytelser)
        }
        håndterGjenoppta(ytelser)
    }

    fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        registrer(utbetalingsgodkjenning, "Behandler utbetalingsgodkjenning")
        finnArbeidsgiver(utbetalingsgodkjenning).håndter(utbetalingsgodkjenning)
        håndterGjenoppta(utbetalingsgodkjenning)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
        håndterGjenoppta(vilkårsgrunnlag)
    }

    fun håndter(simulering: Simulering) {
        registrer(simulering, "Behandler simulering")
        finnArbeidsgiver(simulering).håndter(simulering)
        håndterGjenoppta(simulering)
    }

    fun håndter(utbetaling: UtbetalingHendelse) {
        registrer(utbetaling, "Behandler utbetaling")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
        håndterGjenoppta(utbetaling)
    }

    fun håndter(påminnelse: Utbetalingpåminnelse) {
        påminnelse.kontekst(this)
        finnArbeidsgiver(påminnelse).håndter(påminnelse)
        håndterGjenoppta(påminnelse)
    }

    fun håndter(påminnelse: PersonPåminnelse) {
        påminnelse.kontekst(this)
        påminnelse.info("Håndterer påminnelse for person")
        håndterGjenoppta(påminnelse)
    }

    fun håndter(påminnelse: Påminnelse) {
        try {
            påminnelse.kontekst(this)
            if (finnArbeidsgiver(påminnelse).håndter(påminnelse)) return håndterGjenoppta(påminnelse)
        } catch (err: Aktivitetslogg.AktivitetException) {
            påminnelse.funksjonellFeil(RV_AG_1)
        }
        observers.forEach { påminnelse.vedtaksperiodeIkkeFunnet(it) }
        håndterGjenoppta(påminnelse)
    }

    fun håndter(avstemming: Avstemming) {
        avstemming.kontekst(this)
        avstemming.info("Avstemmer utbetalinger og vedtaksperioder")
        val result = Avstemmer(this).toMap()
        observers.forEach { it.avstemt(result) }
        håndterGjenoppta(avstemming)
    }

    fun håndter(hendelse: OverstyrTidslinje) {
        hendelse.kontekst(this)
        finnArbeidsgiver(hendelse).håndter(hendelse)
        håndterGjenoppta(hendelse)
    }

    fun håndter(hendelse: OverstyrArbeidsgiveropplysninger) {
        hendelse.kontekst(this)
        check(arbeidsgivere.håndterOverstyrArbeidsgiveropplysninger(hendelse)) {
            "Ingen vedtaksperioder håndterte overstyringen av arbeidsgiveropplysninger"
        }
        håndterGjenoppta(hendelse)
    }

    fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold) {
        overstyrArbeidsforhold.kontekst(this)
        if (!arbeidsgivere.håndter(overstyrArbeidsforhold)) {
            overstyrArbeidsforhold.logiskFeil("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        }
        håndterGjenoppta(overstyrArbeidsforhold)
    }

    fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(hendelse)
            ?: hendelse.funksjonellFeil(RV_AG_1)
        håndterGjenoppta(hendelse)
    }

    fun håndter(hendelse: Grunnbeløpsregulering) {
        hendelse.kontekst(this)
        håndterGjenoppta(hendelse)
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun arbeidsgiverperiodeFor(
        orgnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        subsumsjonObserver: SubsumsjonObserver?
    ): List<Arbeidsgiverperiode> {
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        infotrygdhistorikk.build(orgnummer, sykdomstidslinje, periodebuilder, subsumsjonObserver)
        return periodebuilder.result()
    }

    private fun arbeidsgiverUtbetalinger(
        subsumsjonObserver: SubsumsjonObserver,
        hendelse: IAktivitetslogg
    ): ArbeidsgiverUtbetalinger {
        return ArbeidsgiverUtbetalinger(
            regler = regler,
            alder = alder,
            arbeidsgivere = arbeidsgivereMedSykdom().associateWith {
                it.builder(regler, vilkårsgrunnlagHistorikk, infotrygdhistorikk, subsumsjonObserver, hendelse)
            },
            infotrygdUtbetalingstidslinje = infotrygdhistorikk.utbetalingstidslinje(),
            dødsdato = dødsdato,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
        )
    }

    internal fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

    internal fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(vedtaksperiodeId, organisasjonsnummer, påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(vedtaksperiodeId, organisasjonsnummer, tilstandType) }
    }

    internal fun opprettOppgave(event: PersonObserver.OpprettOppgaveEvent) {
        observers.forEach { it.opprettOppgave(event) }
    }

    internal fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        observers.forEach { it.vedtaksperiodeForkastet(event) }
    }

    internal fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        observers.forEach { it.vedtaksperiodeEndret(event) }
    }

    internal fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        observers.forEach { it.vedtaksperiodeVenter(event) }
    }

    internal fun inntektsmeldingReplay(vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, organisasjonsnummer: String, førsteDagIArbeidsgiverperioden: LocalDate?) {
        observers.forEach {
            it.inntektsmeldingReplay(personidentifikator, aktørId, organisasjonsnummer, vedtaksperiodeId, skjæringstidspunkt, førsteDagIArbeidsgiverperioden)
        }
    }

    internal fun trengerIkkeInntektsmeldingReplay(vedtaksperiodeId: UUID) {
        observers.forEach {
            it.trengerIkkeInntektsmeldingReplay(vedtaksperiodeId)
        }
    }

    internal fun trengerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        observers.forEach { it.manglerInntektsmelding(event) }
    }

    internal fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        observers.forEach { it.trengerIkkeInntektsmelding(event) }
    }

    internal fun håndtertInntektsmelding(event: PersonObserver.HåndtertInntektsmeldingEvent) {
        observers.forEach { it.håndtertInntektsmelding(event) }
    }

    internal fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        observers.forEach { it.trengerArbeidsgiveropplysninger(event) }
    }

    internal fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun vedtakFattet(vedtakFattetEvent: PersonObserver.VedtakFattetEvent) {
        observers.forEach { it.vedtakFattet(vedtakFattetEvent) }
    }

    internal fun emitOverstyringIgangsattEvent(event: PersonObserver.OverstyringIgangsatt) {
        observers.forEach { it.overstyringIgangsatt(event) }
    }

    internal fun emitOverlappendeInfotrygdperiodeEtterInfotrygdendring(
        vedtaksperiodeId: UUID,
        vedtaksperiode: Periode,
        vedtaksperiodetilstand: String,
        organisasjonsnummer: String,
        overlappendeInfotrygdPerioder: List<Infotrygdperiode>,
        hendelseId: UUID?
    ) {
        val event = PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            vedtaksperiodeFom = vedtaksperiode.start,
            vedtaksperiodeTom = vedtaksperiode.endInclusive,
            vedtaksperiodetilstand = vedtaksperiodetilstand,
            infotrygdhistorikkHendelseId = hendelseId.toString(),
            infotrygdperioder = overlappendeInfotrygdPerioder.mapNotNull {
                PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.InfotrygdperiodeBuilder(it).infotrygdperiode
            }
        )

        observers.forEach { it.overlappendeInfotrygdperiodeEtterInfotrygdendring(event) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    internal fun nyesteIdForVilkårsgrunnlagHistorikk() =
        vilkårsgrunnlagHistorikk.sisteId()

    internal fun skjæringstidspunkt(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, periode: Periode) =
        infotrygdhistorikk.skjæringstidspunkt(orgnummer, periode, sykdomstidslinje)

    internal fun skjæringstidspunkt(periode: Periode) =
        Arbeidsgiver.skjæringstidspunkt(arbeidsgivere, periode, infotrygdhistorikk)

    internal fun skjæringstidspunkter() =
        Arbeidsgiver.skjæringstidspunkter(arbeidsgivere, infotrygdhistorikk)

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) {
        if (trengerHistorikkFraInfotrygd(hendelse)) return hendelse.info("Må oppfriske Infotrygdhistorikken")
        hendelse.info("Trenger ikke oppfriske Infotrygdhistorikken, bruker lagret historikk")
        vedtaksperiode.håndterHistorikkFraInfotrygd(hendelse, infotrygdhistorikk)
    }

    private fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg): Boolean {
        val tidligsteDato = arbeidsgivereMedSykdom().minOf { it.tidligsteDato() }
        return infotrygdhistorikk.oppfriskNødvendig(hendelse, tidligsteDato)
    }

    internal fun periodetype(
        orgnummer: String,
        arbeidsgiverperiode: Arbeidsgiverperiode,
        periode: Periode,
        skjæringstidspunkt: LocalDate
    ) =
        arbeidsgiverperiode.periodetype(orgnummer, periode, skjæringstidspunkt, infotrygdhistorikk)

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this, opprettet, aktørId, personidentifikator, dødsdato, vilkårsgrunnlagHistorikk)
        alder.accept(visitor)
        visitor.visitPersonAktivitetslogg(aktivitetslogg)
        aktivitetslogg.accept(visitor)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        infotrygdhistorikk.accept(visitor)
        vilkårsgrunnlagHistorikk.accept(visitor)
        visitor.postVisitPerson(this, opprettet, aktørId, personidentifikator, dødsdato, vilkårsgrunnlagHistorikk)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to personidentifikator.toString(), "aktørId" to aktørId))
    }

    private fun registrer(hendelse: PersonHendelse, melding: String) {
        hendelse.kontekst(this)
        hendelse.info(melding)
    }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        finnEllerOpprettArbeidsgiver(hendelse.organisasjonsnummer(), hendelse)

    private fun finnEllerOpprettArbeidsgiver(orgnummer: String, aktivitetslogg: IAktivitetslogg) =
        arbeidsgivere.finnEllerOpprett(orgnummer, aktivitetslogg)

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finn(orgnr) ?: hendelse.logiskFeil("Finner ikke arbeidsgiver")
        }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, aktivitetslogg: IAktivitetslogg) =
        finn(orgnr) ?: Arbeidsgiver(this@Person, orgnr, jurist).also { arbeidsgiver ->
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnr)
            add(arbeidsgiver)
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
        arbeidsgivere.nåværendeVedtaksperioder(filter).sorted()

    internal fun vedtaksperioder(filter: VedtaksperiodeFilter) = arbeidsgivere.vedtaksperioder(filter).sorted()

    private fun harNærliggendeUtbetaling(periode: Periode): Boolean {
        if (infotrygdhistorikk.harBetaltRettFør(periode)) return false
        return arbeidsgivere.any { it.harNærliggendeUtbetaling(periode.oppdaterTom(periode.endInclusive.plusYears(3))) }
    }

    internal fun sendOppgaveEvent(hendelse: SykdomstidslinjeHendelse) {
        sendOppgaveEvent(hendelse.sykdomstidslinje().periode(), setOf(hendelse.meldingsreferanseId()))
    }

    internal fun sendOppgaveEvent(periode: Periode?, hendelseIder: Set<UUID>) {
        val harNærliggendeUtbetaling = periode?.let { harNærliggendeUtbetaling(it) } ?: false
        if (harNærliggendeUtbetaling) {
            emitOpprettOppgaveForSpeilsaksbehandlereEvent(hendelseIder)
        } else {
            emitOpprettOppgaveEvent(hendelseIder)
        }
    }


    internal fun lagreDødsdato(dødsdato: LocalDate) {
        this.dødsdato = dødsdato
    }

    internal fun build(skjæringstidspunkt: LocalDate, builder: VedtakFattetBuilder) {
        vilkårsgrunnlagHistorikk.build(skjæringstidspunkt, builder)
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun blitt6GBegrensetSidenSist(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlagHistorikk.blitt6GBegrensetSidenSist(skjæringstidspunkt)

    internal fun lagreVilkårsgrunnlag(vilkårsgrunnlag: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }

    internal fun avklarSykepengegrunnlag(hendelse: IAktivitetslogg, skjæringstidspunkt: LocalDate, skatteopplysninger: Map<String, SkattSykepengegrunnlag>, subsumsjonObserver: SubsumsjonObserver): Sykepengegrunnlag {
        skatteopplysninger.keys.forEach { orgnr -> finnEllerOpprettArbeidsgiver(orgnr, hendelse) } // oppretter evt. nye arbeidsgivere
        return Sykepengegrunnlag.opprett(
            alder,
            arbeidsgivere.avklarSykepengegrunnlag(skjæringstidspunkt, skatteopplysninger),
            skjæringstidspunkt,
            subsumsjonObserver
        )
    }

    private fun arbeidsgivereMedSykdom() = arbeidsgivere.filter(Arbeidsgiver::harSykdom)

    internal fun sykdomshistorikkEndret(aktivitetslogg: IAktivitetslogg) {
        val skjæringstidspunkter = skjæringstidspunkter()
        arbeidsgivere.fold(skjæringstidspunkter.map { Sykefraværstilfelleeventyr(it) }) { acc, arbeidsgiver ->
            arbeidsgiver.sykefraværsfortelling(acc)
        }.varsleObservers(observers)
        vilkårsgrunnlagHistorikk.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter)
    }

    internal fun søppelbøtte(hendelse: IAktivitetslogg, filter: VedtaksperiodeFilter) {
        infotrygdhistorikk.tøm()
        Arbeidsgiver.søppelbøtte(arbeidsgivere, hendelse, filter)
        sykdomshistorikkEndret(hendelse)
        gjenopptaBehandling(hendelse)
    }

    private fun emitOpprettOppgaveForSpeilsaksbehandlereEvent(hendelseIder: Set<UUID>) {
        observers.forEach {
            it.opprettOppgaveForSpeilsaksbehandlere(
                PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent(
                    hendelseIder
                )
            )
        }
    }

    private fun emitOpprettOppgaveEvent(hendelseIder: Set<UUID>) {
        observers.forEach {
            it.opprettOppgave(
                PersonObserver.OpprettOppgaveEvent(
                    hendelseIder
                )
            )
        }
    }

    internal fun emitUtsettOppgaveEvent(hendelse: SykdomstidslinjeHendelse) {
        observers.forEach {
            it.utsettOppgave(
                PersonObserver.UtsettOppgaveEvent(
                    hendelse.meldingsreferanseId()
                )
            )
        }
    }

    internal fun fyllUtPeriodeMedForventedeDager(
        hendelse: PersonHendelse,
        periode: Periode,
        skjæringstidspunkt: LocalDate
    ) {
        vilkårsgrunnlagFor(skjæringstidspunkt)!!.also { vilkårsgrunnlagElement ->
            arbeidsgivere
                .filter { vilkårsgrunnlagElement.erRelevant(it.organisasjonsnummer()) }
                .forEach { it.fyllUtPeriodeMedForventedeDager(hendelse, skjæringstidspunkt, periode) }
        }
    }

    internal fun relevanteArbeidsgivere(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.relevanteArbeidsgivere(vilkårsgrunnlagFor(skjæringstidspunkt))

    internal fun nyeArbeidsgiverInntektsopplysninger(
        skjæringstidspunkt: LocalDate,
        inntektsmelding: Inntektsmelding,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        val grunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) ?: return
        val (nyttGrunnlag, eventyr) = (grunnlag.nyeArbeidsgiverInntektsopplysninger(inntektsmelding, subsumsjonObserver) ?: return)
        nyttVilkårsgrunnlag(inntektsmelding, nyttGrunnlag)
        igangsettOverstyring(inntektsmelding, eventyr)
    }

    internal fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(
        hendelse: OverstyrArbeidsgiveropplysninger,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        val grunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) ?: return hendelse.funksjonellFeil(RV_VV_10)
        val (nyttGrunnlag, eventyr) = (grunnlag.overstyrArbeidsgiveropplysninger(hendelse, subsumsjonObserver) ?: return)
        nyttVilkårsgrunnlag(hendelse, nyttGrunnlag)
        igangsettOverstyring(hendelse, eventyr)
    }


    internal fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(
        hendelse: OverstyrArbeidsforhold,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        val grunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) ?: return hendelse.funksjonellFeil(RV_VV_10)
        nyttVilkårsgrunnlag(hendelse, grunnlag.overstyrArbeidsforhold(hendelse, subsumsjonObserver))
        igangsettOverstyring(hendelse, Revurderingseventyr.arbeidsforhold(skjæringstidspunkt))
    }

    private fun nyttVilkårsgrunnlag(hendelse: IAktivitetslogg, vilkårsgrunnlag: VilkårsgrunnlagElement?) {
        if (vilkårsgrunnlag == null) return
        hendelse.kontekst(vilkårsgrunnlag)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }
    private var gjenopptaBehandlingNy = false

    internal fun gjenopptaBehandling(hendelse: IAktivitetslogg) {
        hendelse.info("Forbereder gjenoppta behandling")
        gjenopptaBehandlingNy = true
    }

    private fun håndterGjenoppta(hendelse: IAktivitetslogg) {
        while (gjenopptaBehandlingNy) {
            gjenopptaBehandlingNy = false
            arbeidsgivere.gjenopptaBehandling(hendelse)
        }
    }

    internal fun igangsettOverstyring(hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
        arbeidsgivere.igangsettOverstyring(hendelse, revurdering)
        revurdering.sendOverstyringIgangsattEvent(this)
    }

    internal fun nestemann() = arbeidsgivere.nestemann()

    internal fun slettUtgåtteSykmeldingsperioder(tom: LocalDate) {
        arbeidsgivere.slettUtgåtteSykmeldingsperioder(tom)
    }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        vilkårsgrunnlag: VilkårsgrunnlagElement,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate
    ): Boolean {
        arbeidsgivere.validerVilkårsgrunnlag(aktivitetslogg, vilkårsgrunnlag, organisasjonsnummer, skjæringstidspunkt)
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun sykefraværstilfelle(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.sykefraværstilfelle(skjæringstidspunkt)

    internal fun nekterOpprettelseAvPeriode(vedtaksperiode: Vedtaksperiode, søknad: Søknad): Boolean {
        return arbeidsgivere.nekterOpprettelseAvPeriode(vedtaksperiode, søknad)
    }

    internal fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt)

    internal fun nyVedtaksperiodeUtbetaling(organisasjonsnummer: String, utbetalingId: UUID, vedtaksperiodeId: UUID) {
        observers.forEach { it.nyVedtaksperiodeUtbetaling(personidentifikator, aktørId, organisasjonsnummer, utbetalingId, vedtaksperiodeId) }
    }

    internal fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) = vedtaksperiode.venteårsak(hendelse, arbeidsgivere)
}

