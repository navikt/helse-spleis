package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Arbeidsgiver.Companion.beregnFeriepengerForAlleArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.beregnOpptjening
import no.nav.helse.person.Arbeidsgiver.Companion.beregnSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.deaktiverteArbeidsforhold
import no.nav.helse.person.Arbeidsgiver.Companion.finn
import no.nav.helse.person.Arbeidsgiver.Companion.ghostPeriode
import no.nav.helse.person.Arbeidsgiver.Companion.gjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.Companion.harOverlappendeEllerForlengerForkastetVedtaksperiode
import no.nav.helse.person.Arbeidsgiver.Companion.harPeriodeSomBlokkererOverstyring
import no.nav.helse.person.Arbeidsgiver.Companion.harUferdigPeriodeFør
import no.nav.helse.person.Arbeidsgiver.Companion.harUtbetaltPeriode
import no.nav.helse.person.Arbeidsgiver.Companion.håndter
import no.nav.helse.person.Arbeidsgiver.Companion.håndterOverstyringAvGhostInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.inntekterForSammenligningsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.kanStarteRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.lagRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.nyPeriode
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.relevanteArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.skjæringstidspunktperiode
import no.nav.helse.person.Arbeidsgiver.Companion.slettUtgåtteSykmeldingsperioder
import no.nav.helse.person.Arbeidsgiver.Companion.startRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.validerVilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.validerYtelserForSkjæringstidspunkt
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Alder
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
    internal val aktivitetslogg: Aktivitetslogg,
    private val opprettet: LocalDateTime,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private var dødsdato: LocalDate?,
    private val jurist: MaskinellJurist
) : Aktivitetskontekst {
    internal companion object {
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
            jurist = jurist
        )
    }

    internal constructor(
        aktørId: String,
        personidentifikator: Personidentifikator,
        alder: Alder,
        jurist: MaskinellJurist
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
        jurist.medFødselsnummer(personidentifikator)
    )

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(sykmelding: Sykmelding) = håndter(sykmelding, "sykmelding")

    fun håndter(søknad: Søknad) = håndter(søknad, "søknad") { søknad.forUng(alder) }

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    fun håndter(inntektsmelding: InntektsmeldingReplay) {
        registrer(inntektsmelding, "Behandler replay av inntektsmelding")
        finnArbeidsgiver(inntektsmelding).håndter(inntektsmelding)
        håndterGjenoppta(inntektsmelding)
    }

    private fun håndter(
        hendelse: SykdomstidslinjeHendelse,
        hendelsesmelding: String,
        before: () -> Any = { }
    ) {
        registrer(hendelse, "Behandler $hendelsesmelding")
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(hendelse)
        before()
        hendelse.fortsettÅBehandle(arbeidsgiver)
        håndterGjenoppta(hendelse)
    }

    fun håndter(infotrygdendring: Infotrygdendring) {
        infotrygdendring.kontekst(this)
        håndterGjenoppta(infotrygdendring)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)
        finnArbeidsgiver(utbetalingshistorikk).håndter(utbetalingshistorikk, infotrygdhistorikk)
        håndterGjenoppta(utbetalingshistorikk)
    }

    fun håndter(utbetalingshistorikk: UtbetalingshistorikkForFeriepenger) {
        utbetalingshistorikk.kontekst(this)

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
            arbeidsgivere.finnEllerOpprett(it) {
                utbetalingshistorikk.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", it)
                Arbeidsgiver(this, it, jurist)
            }
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
        ytelser.oppdaterHistorikk(infotrygdhistorikk)
        ytelser.lagreDødsdato(this)

        finnArbeidsgiver(ytelser).håndter(ytelser, infotrygdhistorikk) { subsumsjonObserver ->
            arbeidsgiverUtbetalinger(subsumsjonObserver = subsumsjonObserver)
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

    fun håndter(utbetaling: UtbetalingOverført) {
        registrer(utbetaling, "Behandler utbetaling overført")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
        håndterGjenoppta(utbetaling)
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
            påminnelse.funksjonellFeil("Fikk påminnelse uten at vi fant arbeidsgiver eller vedtaksperiode")
        }
        observers.forEach { påminnelse.vedtaksperiodeIkkeFunnet(it) }
        håndterGjenoppta(påminnelse)
    }

    fun håndter(avstemming: Avstemming) {
        avstemming.kontekst(this)
        avstemming.info("Avstemmer utbetalinger og vedtaksperioder")
        val result = Avstemmer(this).toMap()
        observers.forEach { it.avstemt(avstemming.hendelseskontekst(), result) }
        håndterGjenoppta(avstemming)
    }

    fun håndter(hendelse: OverstyrTidslinje) {
        hendelse.kontekst(this)
        finnArbeidsgiver(hendelse).håndter(hendelse)
        håndterGjenoppta(hendelse)
    }

    fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)

        val erOverstyringAvGhostInntekt = !finnArbeidsgiver(hendelse).harSykdomFor(hendelse.skjæringstidspunkt)
        if (erOverstyringAvGhostInntekt) {
            return håndterOverstyringAvGhostInntekt(hendelse)
        }

        val vilkårsgrunnlag = vilkårsgrunnlagFor(hendelse.skjæringstidspunkt)
        if (vilkårsgrunnlag != null) {
            if (vilkårsgrunnlag.valider(hendelse)) {
                finnArbeidsgiver(hendelse).håndter(hendelse)
            }
        } else {
            hendelse.funksjonellFeil("Kan ikke overstyre inntekt uten at det foreligger et vilkårsgrunnlag")
        }

        if (hendelse.harFunksjonelleFeilEllerVerre()) {
            observers.forEach {
                it.revurderingAvvist(
                    hendelse.hendelseskontekst(),
                    hendelse.tilRevurderingAvvistEvent()
                )
            }
        }
        håndterGjenoppta(hendelse)
    }

    private fun håndterOverstyringAvGhostInntekt(hendelse: OverstyrInntekt) {
        sikkerLogg.info("Ship-o-hoy nå er vi i overstyring av ghost-inntekt-flyten for $aktørId")
        hendelse.valider(arbeidsgivere)
        if (!arbeidsgivere.håndterOverstyringAvGhostInntekt(hendelse)) {
            hendelse.logiskFeil("Kan ikke overstyre ghost-inntekt fordi ingen vedtaksperioder håndterte hendelsen")
        }
        håndterGjenoppta(hendelse)
    }

    fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold) {
        overstyrArbeidsforhold.kontekst(this)
        overstyrArbeidsforhold.valider(arbeidsgivere)

        if (!arbeidsgivere.håndter(overstyrArbeidsforhold)) {
            overstyrArbeidsforhold.logiskFeil("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        }
        håndterGjenoppta(overstyrArbeidsforhold)
    }

    fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(hendelse)
            ?: hendelse.funksjonellFeil("Finner ikke arbeidsgiver")
        håndterGjenoppta(hendelse)
    }

    fun håndter(hendelse: Grunnbeløpsregulering) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(arbeidsgivere, hendelse, vilkårsgrunnlagHistorikk)
            ?: hendelse.funksjonellFeil("Finner ikke arbeidsgiver")
        håndterGjenoppta(hendelse)
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun arbeidsgiverperiodeFor(
        organisasjonsnummer: String,
        sykdomshistorikkId: UUID
    ): List<Arbeidsgiverperiode>? {
        return infotrygdhistorikk.arbeidsgiverperiodeFor(organisasjonsnummer, sykdomshistorikkId)
    }

    internal fun arbeidsgiverperiodeFor(
        orgnummer: String,
        sykdomshistorikkId: UUID,
        sykdomstidslinje: Sykdomstidslinje,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Arbeidsgiverperiode> {
        val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
        infotrygdhistorikk.build(orgnummer, sykdomstidslinje, periodebuilder, subsumsjonObserver)
        return periodebuilder.result().also {
            infotrygdhistorikk.lagreResultat(orgnummer, sykdomshistorikkId, it)
        }
    }

    private fun arbeidsgiverUtbetalinger(
        regler: ArbeidsgiverRegler = NormalArbeidstaker,
        subsumsjonObserver: SubsumsjonObserver
    ): ArbeidsgiverUtbetalinger {
        return ArbeidsgiverUtbetalinger(
            regler = regler,
            alder = alder,
            arbeidsgivere = arbeidsgivereMedSykdom().associateWith {
                it.builder(regler, vilkårsgrunnlagHistorikk, subsumsjonObserver)
            },
            infotrygdhistorikk = infotrygdhistorikk,
            dødsdato = dødsdato,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            subsumsjonObserver = subsumsjonObserver
        )
    }

    internal fun annullert(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(hendelseskontekst, event) }
    }

    internal fun nyInntekt(hendelse: OverstyrInntekt) = finnArbeidsgiver(hendelse).addInntekt(hendelse)

    internal fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(påminnelse.hendelseskontekst(), påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(påminnelse.hendelseskontekst(), tilstandType) }
    }

    internal fun opprettOppgave(aktivitetslogg: IAktivitetslogg, event: PersonObserver.OpprettOppgaveEvent) {
        observers.forEach { it.opprettOppgave(aktivitetslogg.hendelseskontekst(), event) }
    }

    internal fun vedtaksperiodeForkastet(
        aktivitetslogg: IAktivitetslogg,
        event: PersonObserver.VedtaksperiodeForkastetEvent
    ) {
        observers.forEach { it.vedtaksperiodeForkastet(aktivitetslogg.hendelseskontekst(), event) }
    }

    internal fun vedtaksperiodeEndret(
        aktivitetslogg: IAktivitetslogg,
        event: PersonObserver.VedtaksperiodeEndretEvent
    ) {
        observers.forEach {
            it.vedtaksperiodeEndret(aktivitetslogg.hendelseskontekst(), event)
            it.personEndret(aktivitetslogg.hendelseskontekst())
        }
    }

    internal fun inntektsmeldingReplay(vedtaksperiodeId: UUID) {
        observers.forEach {
            it.inntektsmeldingReplay(personidentifikator, vedtaksperiodeId)
        }
    }

    internal fun trengerInntektsmelding(
        hendelseskontekst: Hendelseskontekst,
        orgnr: String,
        event: PersonObserver.ManglendeInntektsmeldingEvent
    ) {
        observers.forEach { it.manglerInntektsmelding(hendelseskontekst, orgnr, event) }
    }

    internal fun trengerIkkeInntektsmelding(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.TrengerIkkeInntektsmeldingEvent
    ) {
        observers.forEach { it.trengerIkkeInntektsmelding(hendelseskontekst, event) }
    }

    internal fun utbetalingUtbetalt(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.UtbetalingUtbetaltEvent
    ) {
        observers.forEach { it.utbetalingUtbetalt(hendelseskontekst, event) }
    }

    internal fun utbetalingUtenUtbetaling(
        hendelseskontekst: Hendelseskontekst,
        event: PersonObserver.UtbetalingUtbetaltEvent
    ) {
        observers.forEach { it.utbetalingUtenUtbetaling(hendelseskontekst, event) }
    }

    internal fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(hendelseskontekst, event) }
    }

    internal fun vedtakFattet(
        hendelseskontekst: Hendelseskontekst,
        vedtakFattetEvent: PersonObserver.VedtakFattetEvent
    ) {
        observers.forEach { it.vedtakFattet(hendelseskontekst, vedtakFattetEvent) }
    }

    internal fun feriepengerUtbetalt(
        hendelseskontekst: Hendelseskontekst,
        feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent
    ) {
        observers.forEach { it.feriepengerUtbetalt(hendelseskontekst, feriepengerUtbetaltEvent) }
    }

    internal fun nyesteIdForVilkårsgrunnlagHistorikk() =
        vilkårsgrunnlagHistorikk.sisteId()

    internal fun skjæringstidspunkt(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, periode: Periode) =
        infotrygdhistorikk.skjæringstidspunkt(orgnummer, periode, sykdomstidslinje)

    internal fun skjæringstidspunkt(periode: Periode) =
        Arbeidsgiver.skjæringstidspunkt(arbeidsgivere, periode, infotrygdhistorikk)

    internal fun skjæringstidspunkter() =
        Arbeidsgiver.skjæringstidspunkter(arbeidsgivere, infotrygdhistorikk)

    internal fun skjæringstidspunkterFraSpleis() = vilkårsgrunnlagHistorikk.skjæringstidspunkterFraSpleis()

    private fun kanOverskriveVilkårsgrunnlag(skjæringstidspunkt: LocalDate) =
        !arbeidsgivere.harUtbetaltPeriode(skjæringstidspunkt)

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, cutoff: LocalDateTime? = null): Boolean {
        val tidligsteDato = arbeidsgivereMedSykdom().minOf { it.tidligsteDato() }
        return infotrygdhistorikk.oppfriskNødvendig(hendelse, tidligsteDato, cutoff)
    }

    internal fun trengerHistorikkFraInfotrygd(
        hendelse: IAktivitetslogg,
        vedtaksperiode: Vedtaksperiode,
        cutoff: LocalDateTime? = null
    ) {
        if (trengerHistorikkFraInfotrygd(hendelse, cutoff)) return hendelse.info("Må oppfriske Infotrygdhistorikken")
        hendelse.info("Trenger ikke oppfriske Infotrygdhistorikken, bruker lagret historikk")
        vedtaksperiode.håndterHistorikkFraInfotrygd(hendelse, infotrygdhistorikk)
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
        arbeidsgivere.finnEllerOpprett(orgnummer) {
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnummer)
            Arbeidsgiver(this, orgnummer, jurist)
        }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finn(orgnr) ?: hendelse.logiskFeil("Finner ikke arbeidsgiver")
        }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, creator: () -> Arbeidsgiver) =
        finn(orgnr) ?: run {
            val newValue = creator()
            add(newValue)
            newValue
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
        arbeidsgivere.nåværendeVedtaksperioder(filter).sorted()

    internal fun vedtaksperioder(filter: VedtaksperiodeFilter) = arbeidsgivere.vedtaksperioder(filter).sorted()

    internal fun lagRevurdering(
        vedtaksperiode: Vedtaksperiode,
        arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
        hendelse: ArbeidstakerHendelse
    ) {
        arbeidsgivere.lagRevurdering(vedtaksperiode, arbeidsgiverUtbetalinger, hendelse)
    }

    internal fun ghostPeriode(skjæringstidspunkt: LocalDate, deaktivert: Boolean) =
        arbeidsgivere.ghostPeriode(skjæringstidspunkt, vilkårsgrunnlagHistorikk.sisteId(), deaktivert)

    private fun harNærliggendeUtbetaling(periode: Periode): Boolean {
        if (infotrygdhistorikk.harBetaltRettFør(periode)) return false
        return arbeidsgivere.any { it.harNærliggendeUtbetaling(periode.oppdaterTom(periode.endInclusive.plusYears(3))) }
    }

    internal fun sendOppgaveEvent(hendelse: SykdomstidslinjeHendelse) {
        sendOppgaveEvent(hendelse, hendelse.sykdomstidslinje().periode(), setOf(hendelse.meldingsreferanseId()))
    }

    internal fun sendOppgaveEvent(hendelse: IAktivitetslogg, periode: Periode?, hendelseIder: Set<UUID>) {
        val harNærliggendeUtbetaling = periode?.let { harNærliggendeUtbetaling(it) } ?: false
        if (harNærliggendeUtbetaling) {
            emitOpprettOppgaveForSpeilsaksbehandlereEvent(hendelse, hendelseIder)
        } else {
            emitOpprettOppgaveEvent(hendelse, hendelseIder)
        }
    }

    internal fun harOverlappendeEllerForlengerForkastetVedtaksperiode(hendelse: SykdomstidslinjeHendelse) =
        arbeidsgivere.harOverlappendeEllerForlengerForkastetVedtaksperiode(hendelse)

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

    internal fun lagreVilkårsgrunnlagFraInfotrygd(
        skjæringstidspunkt: LocalDate,
        periode: Periode,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        infotrygdhistorikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt,
            vilkårsgrunnlagHistorikk,
            ::kanOverskriveVilkårsgrunnlag
        ) {
            beregnSykepengegrunnlagForInfotrygd(it, periode.start, subsumsjonObserver)
        }
    }

    internal fun lagreOverstyrArbeidsforhold(overstyrArbeidsforhold: OverstyrArbeidsforhold, subsumsjonObserver: SubsumsjonObserver) {
        arbeidsgivere.forEach { arbeidsgiver ->
            overstyrArbeidsforhold.lagre(arbeidsgiver, subsumsjonObserver)
        }
    }

    internal fun lagreInntekt(hendelse: OverstyrInntekt) {
        val arbeidsgiver = finnArbeidsgiver(hendelse)
        arbeidsgiver.addInntekt(hendelse)
    }


    internal fun lagreOmregnetÅrsinntekt(
        orgnummer: String,
        arbeidsgiverInntekt: ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        hendelse: PersonHendelse
    ) {
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreOmregnetÅrsinntekt(
            arbeidsgiverInntekt,
            skjæringstidspunkt,
            hendelse
        )
    }

    internal fun lagreRapporterteInntekter(
        orgnummer: String,
        arbeidsgiverInntekt: ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        hendelse: PersonHendelse
    ) {
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreRapporterteInntekter(
            arbeidsgiverInntekt,
            skjæringstidspunkt,
            hendelse
        )
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(
        orgnummer: String,
        inntektsopplysninger: List<Inntektsopplysning>,
        aktivitetslogg: IAktivitetslogg,
        hendelseId: UUID
    ) {
        finnArbeidsgiverForInntekter(orgnummer, aktivitetslogg).lagreSykepengegrunnlagFraInfotrygd(
            inntektsopplysninger,
            hendelseId
        )
    }

    internal fun beregnSykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver,
        forklaring: String?,
        subsumsjon: Subsumsjon?
    ): Sykepengegrunnlag {
        return Sykepengegrunnlag.opprett(
            alder,
            arbeidsgivere.beregnSykepengegrunnlag(skjæringstidspunkt, subsumsjonObserver, forklaring, subsumsjon),
            skjæringstidspunkt,
            subsumsjonObserver,
            arbeidsgivere.deaktiverteArbeidsforhold(skjæringstidspunkt).map { it.organisasjonsnummer() }
        )
    }

    private fun beregnSykepengegrunnlagForInfotrygd(
        skjæringstidspunkt: LocalDate,
        personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ) =
        Sykepengegrunnlag.opprettForInfotrygd(
            alder,
            arbeidsgivere.beregnSykepengegrunnlag(
                skjæringstidspunkt,
                personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden
            ), skjæringstidspunkt, subsumsjonObserver
        )

    internal fun beregnSammenligningsgrunnlag(
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ): Sammenligningsgrunnlag {
        val arbeidsgiverInntektsopplysninger = arbeidsgivere.inntekterForSammenligningsgrunnlag(skjæringstidspunkt)
        val sammenligningsgrunnlag = Sammenligningsgrunnlag(arbeidsgiverInntektsopplysninger)
        subsumsjonObserver.`§ 8-30 ledd 2`(skjæringstidspunkt, sammenligningsgrunnlag.subsumsjonsformat())
        return sammenligningsgrunnlag
    }

    internal fun beregnOpptjening(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
        return arbeidsgivere.beregnOpptjening(skjæringstidspunkt, subsumsjonObserver)
    }

    private fun finnArbeidsgiverForInntekter(arbeidsgiver: String, aktivitetslogg: IAktivitetslogg): Arbeidsgiver {
        return arbeidsgivere.finnEllerOpprett(arbeidsgiver) {
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", arbeidsgiver)
            Arbeidsgiver(this, arbeidsgiver, jurist)
        }
    }

    internal fun harFlereArbeidsgivereMedSykdom() = arbeidsgivere.count(Arbeidsgiver::harSykdomEllerForventerSøknad) > 1

    private fun arbeidsgivereMedSykdom() = arbeidsgivere.filter(Arbeidsgiver::harSykdom)

    internal fun kunOvergangFraInfotrygd(vedtaksperiode: Vedtaksperiode) =
        Arbeidsgiver.kunOvergangFraInfotrygd(arbeidsgivere, vedtaksperiode)

    internal fun ingenUkjenteArbeidsgivere(vedtaksperiode: Vedtaksperiode, skjæringstidspunkt: LocalDate) =
        Arbeidsgiver.ingenUkjenteArbeidsgivere(arbeidsgivere, vedtaksperiode, infotrygdhistorikk, skjæringstidspunkt)

    internal fun sykdomshistorikkEndret(aktivitetslogg: IAktivitetslogg) {
        vilkårsgrunnlagHistorikk.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter())
    }

    internal fun søppelbøtte(hendelse: IAktivitetslogg, filter: VedtaksperiodeFilter) {
        infotrygdhistorikk.tøm()
        Arbeidsgiver.søppelbøtte(arbeidsgivere, hendelse, filter)
        sykdomshistorikkEndret(hendelse)
        gjenopptaBehandling(hendelse)
    }

    private fun emitOpprettOppgaveForSpeilsaksbehandlereEvent(hendelse: IAktivitetslogg, hendelseIder: Set<UUID>) {
        observers.forEach {
            it.opprettOppgaveForSpeilsaksbehandlere(
                hendelse.hendelseskontekst(),
                PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent(
                    hendelseIder
                )
            )
        }
    }

    private fun emitOpprettOppgaveEvent(hendelse: IAktivitetslogg, hendelseIder: Set<UUID>) {
        observers.forEach {
            it.opprettOppgave(
                hendelse.hendelseskontekst(),
                PersonObserver.OpprettOppgaveEvent(
                    hendelseIder
                )
            )
        }
    }

    internal fun emitUtsettOppgaveEvent(hendelse: SykdomstidslinjeHendelse) {
        observers.forEach {
            it.utsettOppgave(
                hendelse.hendelseskontekst(),
                PersonObserver.UtsettOppgaveEvent(
                    hendelse.meldingsreferanseId()
                )
            )
        }
    }

    internal fun emitHendelseIkkeHåndtert(hendelse: SykdomstidslinjeHendelse) {
        val errorMeldinger = mutableListOf<String>()
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitFunksjonellFeil(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitetslogg.Aktivitet.FunksjonellFeil,
                melding: String,
                tidsstempel: String
            ) {
                if (kontekster
                        .mapNotNull { it.kontekstMap["meldingsreferanseId"] }
                        .map(UUID::fromString)
                        .contains(hendelse.meldingsreferanseId())
                ) {
                    errorMeldinger.add(melding)
                }
            }
        })

        observers.forEach {
            it.hendelseIkkeHåndtert(
                hendelse.hendelseskontekst(),
                PersonObserver.HendelseIkkeHåndtertEvent(
                    hendelse.meldingsreferanseId(),
                    errorMeldinger,
                )
            )
        }
    }

    internal fun lagreArbeidsforhold(
        orgnummer: String,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        aktivitetslogg: IAktivitetslogg,
        skjæringstidspunkt: LocalDate
    ) {
        finnEllerOpprettArbeidsgiver(orgnummer, aktivitetslogg).lagreArbeidsforhold(arbeidsforhold, skjæringstidspunkt)
    }

    internal fun brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(
        orgnummerFraAAreg: List<String>,
        skjæringstidspunkt: LocalDate
    ) {
        val arbeidsgivereMedSykdom =
            arbeidsgivere.filter { it.harSykdomFor(skjæringstidspunkt) }.map(Arbeidsgiver::organisasjonsnummer)
        if (arbeidsgivereMedSykdom.containsAll(orgnummerFraAAreg)) {
            sikkerLogg.info("Ingen spøkelser, har sykdom hos alle kjente arbeidsgivere antall=${arbeidsgivereMedSykdom.size}")
        } else {
            sikkerLogg.info("Vi har kontakt med spøkelser, fnr=$personidentifikator, antall=${orgnummerFraAAreg.size}")
        }
    }

    internal fun loggUkjenteOrgnummere(orgnummerFraAAreg: List<String>) {
        val kjenteOrgnummer = arbeidsgivereMedSykdom().map { it.organisasjonsnummer() }
            .filter { it != "0" }
        val orgnummerMedSpleisSykdom = arbeidsgivere.filter { it.harSpleisSykdom() }.map { it.organisasjonsnummer() }

        val manglerIAAReg = kjenteOrgnummer.filter { !orgnummerFraAAreg.contains(it) }
        val spleisOrgnummerManglerIAAreg = kjenteOrgnummer.filter { !orgnummerMedSpleisSykdom.contains(it) }
        val nyeOrgnummer = orgnummerFraAAreg.filter { !kjenteOrgnummer.contains(it) }
        if (spleisOrgnummerManglerIAAreg.isNotEmpty()) {
            sikkerLogg.info("Fant arbeidsgivere i spleis som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $personidentifikator")
        } else if (manglerIAAReg.isNotEmpty()) {
            sikkerLogg.info("Fant arbeidsgivere i IT som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $personidentifikator")
        } else {
            sikkerLogg.info("AAReg kjenner til alle arbeidsgivere i spleis, opprettet (${nyeOrgnummer}) for $personidentifikator")
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

    private fun arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt: LocalDate): List<Arbeidsgiver> =
        arbeidsgivere.filter { it.harRelevantArbeidsforhold(skjæringstidspunkt) }

    internal fun antallArbeidsgivereMedRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) =
        arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt).size

    internal fun harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.any { it.harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt) }

    internal fun relevanteArbeidsgivere(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.relevanteArbeidsgivere(vilkårsgrunnlagFor(skjæringstidspunkt))

    internal fun harFlereArbeidsforholdMedUlikStartdato(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.mapNotNull { it.finnFørsteFraværsdag(skjæringstidspunkt) }.sorted().distinct().count() > 1

    internal fun harKunEttAnnetRelevantArbeidsforholdEnn(skjæringstidspunkt: LocalDate, orgnummer: String): Boolean {
        val aktiveArbeidsforhold = arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt)
        return aktiveArbeidsforhold.size == 1 && aktiveArbeidsforhold.single().organisasjonsnummer() != orgnummer
    }

    internal fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(
        hendelse: PersonHendelse,
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    , forklaring: String?, subsumsjon: Subsumsjon?) {

        val sykepengegrunnlag = beregnSykepengegrunnlag(skjæringstidspunkt, subsumsjonObserver, forklaring, subsumsjon)
        val sammenligningsgrunnlag = beregnSammenligningsgrunnlag(skjæringstidspunkt, subsumsjonObserver)
        val avviksprosent = sammenligningsgrunnlag.avviksprosent(sykepengegrunnlag, subsumsjonObserver)

        val harAkseptabeltAvvik = Inntektsvurdering.sjekkAvvik(avviksprosent, hendelse, IAktivitetslogg::varsel, "Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.")

        val opptjening = beregnOpptjening(skjæringstidspunkt, subsumsjonObserver)
        if (!opptjening.erOppfylt()) {
            hendelse.varsel("Perioden er avslått på grunn av manglende opptjening")
        }

        when (val grunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)) {
            is VilkårsgrunnlagHistorikk.Grunnlagsdata -> {
                val grunnlagselement = grunnlag.kopierGrunnlagsdataMed(
                    hendelse = hendelse,
                    sykepengegrunnlag = sykepengegrunnlag,
                    sammenligningsgrunnlag = sammenligningsgrunnlag,
                    sammenligningsgrunnlagVurdering = harAkseptabeltAvvik,
                    avviksprosent = avviksprosent,
                    nyOpptjening = opptjening,
                    meldingsreferanseId = hendelse.meldingsreferanseId()
                )
                hendelse.kontekst(grunnlagselement)
                vilkårsgrunnlagHistorikk.lagre(grunnlagselement)
            }

            is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag -> hendelse.funksjonellFeil("Vilkårsgrunnlaget ligger i infotrygd. Det er ikke støttet i revurdering eller overstyring.")
            else -> hendelse.funksjonellFeil("Fant ikke vilkårsgrunnlag. Kan ikke vilkårsprøve på nytt etter ny informasjon fra saksbehandler.")
        }
    }

    private var gjenopptaBehandlingNy = false
    internal fun gjenopptaBehandling(hendelse: IAktivitetslogg) {
        gjenopptaBehandlingNy = true
    }

    private fun håndterGjenoppta(hendelse: IAktivitetslogg) {
        while (gjenopptaBehandlingNy) {
            gjenopptaBehandlingNy = false
            arbeidsgivere.gjenopptaBehandling(hendelse)
        }
    }

    internal fun gjenopptaRevurdering(
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
        hendelse: IAktivitetslogg
    ) {
        arbeidsgiver.gjenopptaRevurdering(arbeidsgivere, vedtaksperiode, hendelse)
    }

    internal fun startRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
        arbeidsgivere.startRevurdering(vedtaksperiode, hendelse)
    }

    internal fun kanStarteRevurdering(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.kanStarteRevurdering(vedtaksperiode)

    internal fun slettUtgåtteSykmeldingsperioder(tom: LocalDate) {
        arbeidsgivere.slettUtgåtteSykmeldingsperioder(tom)
    }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        skjæringstidspunkt: LocalDate,
        erForlengelse: Boolean
    ): Boolean {
        arbeidsgivere.validerVilkårsgrunnlag(aktivitetslogg, vilkårsgrunnlag, skjæringstidspunkt, erForlengelse)
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    internal fun validerYtelserForSkjæringstidspunkt(ytelser: Ytelser, skjæringstidspunkt: LocalDate) {
        arbeidsgivere.validerYtelserForSkjæringstidspunkt(ytelser, skjæringstidspunkt, infotrygdhistorikk)
    }

    internal fun skjæringstidspunktperiode(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.skjæringstidspunktperiode(skjæringstidspunkt)

    internal fun harPeriodeSomBlokkererOverstyring(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.harPeriodeSomBlokkererOverstyring(skjæringstidspunkt)

    internal fun harUferdigPeriodeFør(periode: Periode) = arbeidsgivere.harUferdigPeriodeFør(periode)

    internal fun nyPeriode(vedtaksperiode: Vedtaksperiode, søknad: Søknad) =
        arbeidsgivere.nyPeriode(vedtaksperiode, søknad)

    internal fun harSkjæringstidspunktSenereEnn(skjæringstidspunkt: LocalDate) =
        skjæringstidspunkter().any { it.isAfter(skjæringstidspunkt) }
}