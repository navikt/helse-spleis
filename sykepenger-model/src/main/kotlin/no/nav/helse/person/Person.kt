package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Fødselsnummer
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
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingsgrunnlag
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
import no.nav.helse.person.Arbeidsgiver.Companion.gjenopptaBehandlingNy
import no.nav.helse.person.Arbeidsgiver.Companion.harArbeidsgivereMedOverlappendeUtbetaltePerioder
import no.nav.helse.person.Arbeidsgiver.Companion.harOverlappendeEllerForlengerForkastetVedtaksperiode
import no.nav.helse.person.Arbeidsgiver.Companion.harUtbetaltPeriode
import no.nav.helse.person.Arbeidsgiver.Companion.harVedtaksperiodeFor
import no.nav.helse.person.Arbeidsgiver.Companion.håndter
import no.nav.helse.person.Arbeidsgiver.Companion.håndterOverstyringAvGhostInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.inntekterForSammenligningsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.kanOverstyreTidslinje
import no.nav.helse.person.Arbeidsgiver.Companion.kanStarteRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.lagRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.skjæringstidspunktperiode
import no.nav.helse.person.Arbeidsgiver.Companion.slettUtgåtteSykmeldingsperioder
import no.nav.helse.person.Arbeidsgiver.Companion.startRevurdering
import no.nav.helse.person.Arbeidsgiver.Companion.validerVilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.validerYtelserForSkjæringstidspunkt
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.Vedtaksperiode.Companion.ALLE
import no.nav.helse.person.Vedtaksperiode.Companion.lagUtbetalinger
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilderBuilder
import no.nav.helse.utbetalingstidslinje.AvvisInngangsvilkårfilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: Fødselsnummer,
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
            fødselsnummer: Fødselsnummer,
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
            fødselsnummer = fødselsnummer,
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
        fødselsnummer: Fødselsnummer,
        alder: Alder,
        jurist: MaskinellJurist
    ) : this(
        aktørId,
        fødselsnummer,
        alder,
        mutableListOf(),
        Aktivitetslogg(),
        LocalDateTime.now(),
        Infotrygdhistorikk(),
        VilkårsgrunnlagHistorikk(),
        null,
        jurist.medFødselsnummer(fødselsnummer)
    )

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(sykmelding: Sykmelding) = håndter(sykmelding, "sykmelding")

    fun håndter(søknad: Søknad) = håndter(søknad, "søknad") { søknad.forUng(alder) }

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    fun håndter(inntektsmelding: InntektsmeldingReplay) {
        registrer(inntektsmelding, "Behandler replay av inntektsmelding")
        finnArbeidsgiver(inntektsmelding).håndter(inntektsmelding)
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
    }

    fun håndter(infotrygdendring: Infotrygdendring) {
        infotrygdendring.kontekst(this)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)
        finnArbeidsgiver(utbetalingshistorikk).håndter(utbetalingshistorikk, infotrygdhistorikk)
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
        val beregnetFeriepengebeløpPersonInfotrygd = feriepengeberegner.beregnFeriepengerForInfotrygdPerson().roundToInt()

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
        arbeidsgivere.beregnFeriepengerForAlleArbeidsgivere(aktørId, fødselsnummer, feriepengeberegner, utbetalingshistorikk)

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
    }

    fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        registrer(utbetalingsgodkjenning, "Behandler utbetalingsgodkjenning")
        finnArbeidsgiver(utbetalingsgodkjenning).håndter(utbetalingsgodkjenning)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
    }

    fun håndter(utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        registrer(utbetalingsgrunnlag, "Behandler utbetalingsgrunnlag")
        finnArbeidsgiver(utbetalingsgrunnlag).håndter(utbetalingsgrunnlag)
    }

    fun håndter(simulering: Simulering) {
        registrer(simulering, "Behandler simulering")
        finnArbeidsgiver(simulering).håndter(simulering)
    }

    fun håndter(utbetaling: UtbetalingOverført) {
        registrer(utbetaling, "Behandler utbetaling overført")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
    }

    fun håndter(utbetaling: UtbetalingHendelse) {
        registrer(utbetaling, "Behandler utbetaling")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
    }

    fun håndter(påminnelse: Utbetalingpåminnelse) {
        påminnelse.kontekst(this)
        finnArbeidsgiver(påminnelse).håndter(påminnelse)
    }

    fun håndter(påminnelse: PersonPåminnelse) {
        påminnelse.kontekst(this)
        påminnelse.info("Håndterer påminnelse for person")
    }

    fun håndter(påminnelse: Påminnelse) {
        try {
            påminnelse.kontekst(this)
            if (finnArbeidsgiver(påminnelse).håndter(påminnelse)) return
        } catch (err: Aktivitetslogg.AktivitetException) {
            påminnelse.error("Fikk påminnelse uten at vi fant arbeidsgiver eller vedtaksperiode")
        }
        observers.forEach { påminnelse.vedtaksperiodeIkkeFunnet(it) }
    }

    fun håndter(avstemming: Avstemming) {
        avstemming.kontekst(this)
        avstemming.info("Avstemmer utbetalinger og vedtaksperioder")
        val result = Avstemmer(this).toMap()
        observers.forEach { it.avstemt(avstemming.hendelseskontekst(), result) }
    }

    fun håndter(hendelse: OverstyrTidslinje) {
        if (Toggle.NyRevurdering.enabled) return håndterNy(hendelse)
        hendelse.kontekst(this)
        if (arbeidsgivere.kanOverstyreTidslinje(hendelse)) {
            finnArbeidsgiver(hendelse).håndter(hendelse)
        } else {
            hendelse.error("Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
        }
        if (hendelse.hasErrorsOrWorse()) {
            observers.forEach { it.revurderingAvvist(hendelse.hendelseskontekst(), hendelse.tilRevurderingAvvistEvent()) }
        }
    }

    fun håndterNy(hendelse: OverstyrTidslinje) {
        check(Toggle.NyRevurdering.enabled)
        hendelse.kontekst(this)
        finnArbeidsgiver(hendelse).håndter(hendelse)
    }

    fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)

        val erOverstyringAvGhostInntekt = !finnArbeidsgiver(hendelse).harSykdomFor(hendelse.skjæringstidspunkt)
        if (erOverstyringAvGhostInntekt) {
            return håndterOverstyringAvGhostInntekt(hendelse)
        }

        finnArbeidsgiver(hendelse).håndter(hendelse)
        if (hendelse.hasErrorsOrWorse()) {
            observers.forEach { it.revurderingAvvist(hendelse.hendelseskontekst(), hendelse.tilRevurderingAvvistEvent()) }
        }
    }

    private fun håndterOverstyringAvGhostInntekt(hendelse: OverstyrInntekt) {
        sikkerLogg.info("Ship-o-hoy nå er vi i overstyring av ghost-inntekt-flyten for $aktørId")
        hendelse.valider(arbeidsgivere)
        if (!arbeidsgivere.håndterOverstyringAvGhostInntekt(hendelse)) {
            hendelse.severe("Kan ikke overstyre ghost-inntekt fordi ingen vedtaksperioder håndterte hendelsen")
        }
    }

    fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold) {
        overstyrArbeidsforhold.kontekst(this)
        overstyrArbeidsforhold.valider(arbeidsgivere)

        if (!arbeidsgivere.håndter(overstyrArbeidsforhold)) {
            overstyrArbeidsforhold.severe("Kan ikke overstyre arbeidsforhold fordi ingen vedtaksperioder håndterte hendelsen")
        }
    }

    fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(hendelse)
            ?: hendelse.error("Finner ikke arbeidsgiver")
    }

    fun håndter(hendelse: Grunnbeløpsregulering) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(arbeidsgivere, hendelse, vilkårsgrunnlagHistorikk)
            ?: hendelse.error("Finner ikke arbeidsgiver")
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun arbeidsgiverperiodeFor(organisasjonsnummer: String, sykdomshistorikkId: UUID): List<Arbeidsgiverperiode>? {
        return infotrygdhistorikk.arbeidsgiverperiodeFor(organisasjonsnummer, sykdomshistorikkId)
    }

    internal fun arbeidsgiverperiodeFor(orgnummer: String, sykdomshistorikkId: UUID, sykdomstidslinje: Sykdomstidslinje, subsumsjonObserver: SubsumsjonObserver): List<Arbeidsgiverperiode> {
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

    internal fun gjenopptaBehandling(hendelse: IAktivitetslogg) {
        arbeidsgivere.gjenopptaBehandling(hendelse)
    }

    internal fun annullert(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(hendelseskontekst, event) }
    }

    internal fun nyInntekt(hendelse: OverstyrInntekt) = finnArbeidsgiver(hendelse).addInntekt(hendelse)

    internal fun overstyrUtkastRevurdering(hendelse: OverstyrTidslinje) {
        val førstePeriode = finnArbeidsgiver(hendelse).førstePeriodeTilRevurdering(hendelse)
        igangsettRevurdering(hendelse, førstePeriode)
    }

    internal fun overstyrUtkastRevurdering(hendelse: Påminnelse) {
        val førstePeriode = finnArbeidsgiver(hendelse).førstePeriodeTilRevurdering(hendelse)
        igangsettRevurdering(hendelse, førstePeriode)
    }

    internal fun overstyrUtkastRevurdering(hendelse: OverstyrInntekt) {
        val førstePeriode = finnArbeidsgiver(hendelse).førstePeriodeTilRevurdering(hendelse)
        igangsettRevurdering(hendelse, førstePeriode)
    }

    internal fun igangsettRevurdering(hendelse: OverstyrTidslinje, vedtaksperiode: Vedtaksperiode) {
        arbeidsgivere.forEach { it.startRevurderingForAlleBerørtePerioder(hendelse, vedtaksperiode) }
        if (hendelse.hasErrorsOrWorse()) return
        vedtaksperiode.revurder(hendelse)
    }

    private fun igangsettRevurdering(hendelse: Påminnelse, vedtaksperiode: Vedtaksperiode) {
        arbeidsgivere.forEach { it.startRevurderingForAlleBerørtePerioder(hendelse, vedtaksperiode) }
        if (hendelse.hasErrorsOrWorse()) return
        vedtaksperiode.revurder(hendelse)
    }

    internal fun igangsettRevurdering(hendelse: OverstyrInntekt, vedtaksperiode: Vedtaksperiode) {
        arbeidsgivere.forEach { it.startRevurderingForAlleBerørtePerioder(hendelse, vedtaksperiode) }
        if (hendelse.hasErrorsOrWorse()) return
        vedtaksperiode.revurder(hendelse)
    }

    internal fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(påminnelse.hendelseskontekst(), påminnelse) }
    }

    internal fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(påminnelse.hendelseskontekst(), tilstandType) }
    }

    internal fun opprettOppgave(aktivitetslogg: IAktivitetslogg, event: PersonObserver.OpprettOppgaveEvent) {
        observers.forEach { it.opprettOppgave(aktivitetslogg.hendelseskontekst(), event) }
    }

    internal fun vedtaksperiodeForkastet(aktivitetslogg: IAktivitetslogg, event: PersonObserver.VedtaksperiodeForkastetEvent) {
        observers.forEach { it.vedtaksperiodeForkastet(aktivitetslogg.hendelseskontekst(), event) }
    }

    internal fun vedtaksperiodeEndret(aktivitetslogg: IAktivitetslogg, event: PersonObserver.VedtaksperiodeEndretEvent) {
        observers.forEach {
            it.vedtaksperiodeEndret(aktivitetslogg.hendelseskontekst(), event)
            it.personEndret(aktivitetslogg.hendelseskontekst())
        }
    }

    internal fun inntektsmeldingReplay(vedtaksperiodeId: UUID) {
        observers.forEach {
            it.inntektsmeldingReplay(fødselsnummer, vedtaksperiodeId)
        }
    }

    internal fun trengerInntektsmelding(hendelseskontekst: Hendelseskontekst, orgnr: String, event: PersonObserver.ManglendeInntektsmeldingEvent) {
        observers.forEach { it.manglerInntektsmelding(hendelseskontekst, orgnr, event) }
    }

    internal fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst, event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        observers.forEach { it.trengerIkkeInntektsmelding(hendelseskontekst, event) }
    }

    internal fun utbetalingUtbetalt(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(hendelseskontekst, event) }
    }

    internal fun utbetalingUtenUtbetaling(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(hendelseskontekst, event) }
    }

    internal fun utbetalingEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(hendelseskontekst, event) }
    }

    internal fun vedtakFattet(hendelseskontekst: Hendelseskontekst, vedtakFattetEvent: PersonObserver.VedtakFattetEvent) {
        observers.forEach { it.vedtakFattet(hendelseskontekst, vedtakFattetEvent) }
    }

    internal fun feriepengerUtbetalt(hendelseskontekst: Hendelseskontekst, feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
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

    private fun kanOverskriveVilkårsgrunnlag(skjæringstidspunkt: LocalDate) = !arbeidsgivere.harUtbetaltPeriode(skjæringstidspunkt)

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, cutoff: LocalDateTime? = null): Boolean {
        val tidligsteDato = arbeidsgivereMedSykdom().minOf { it.tidligsteDato() }
        return infotrygdhistorikk.oppfriskNødvendig(hendelse, tidligsteDato, cutoff)
    }

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, cutoff: LocalDateTime? = null) {
        if (trengerHistorikkFraInfotrygd(hendelse, cutoff)) return hendelse.info("Må oppfriske Infotrygdhistorikken")
        hendelse.info("Trenger ikke oppfriske Infotrygdhistorikken, bruker lagret historikk")
        vedtaksperiode.håndterHistorikkFraInfotrygd(hendelse, infotrygdhistorikk)
    }

    internal fun periodetype(orgnummer: String, arbeidsgiverperiode: Arbeidsgiverperiode, periode: Periode, skjæringstidspunkt: LocalDate) =
        arbeidsgiverperiode.periodetype(orgnummer, periode, skjæringstidspunkt, infotrygdhistorikk)

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this, opprettet, aktørId, fødselsnummer, dødsdato, vilkårsgrunnlagHistorikk)
        alder.accept(visitor)
        visitor.visitPersonAktivitetslogg(aktivitetslogg)
        aktivitetslogg.accept(visitor)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        infotrygdhistorikk.accept(visitor)
        vilkårsgrunnlagHistorikk.accept(visitor)
        visitor.postVisitPerson(this, opprettet, aktørId, fødselsnummer, dødsdato, vilkårsgrunnlagHistorikk)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to fødselsnummer.toString(), "aktørId" to aktørId))
    }

    private fun registrer(hendelse: PersonHendelse, melding: String) {
        hendelse.kontekst(this)
        hendelse.info(melding)
    }

    internal fun revurderingHarFeilet(event: IAktivitetslogg) {
        arbeidsgivere.forEach { it.håndterRevurderingFeilet(event) }
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
            arbeidsgivere.finn(orgnr) ?: hendelse.severe("Finner ikke arbeidsgiver")
        }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, creator: () -> Arbeidsgiver) =
        finn(orgnr) ?: run {
            val newValue = creator()
            add(newValue)
            newValue
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) = arbeidsgivere.nåværendeVedtaksperioder(filter).sorted()
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

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun lagreVilkårsgrunnlag(vilkårsgrunnlag: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag)
    }

    internal fun lagreVilkårsgrunnlagFraInfotrygd(skjæringstidspunkt: LocalDate, periode: Periode, subsumsjonObserver: SubsumsjonObserver) {
        infotrygdhistorikk.lagreVilkårsgrunnlag(skjæringstidspunkt, vilkårsgrunnlagHistorikk, ::kanOverskriveVilkårsgrunnlag) {
            beregnSykepengegrunnlagForInfotrygd(it, periode.start, subsumsjonObserver)
        }
    }

    internal fun lagreOverstyrArbeidsforhold(overstyrArbeidsforhold: OverstyrArbeidsforhold) {
        arbeidsgivere.forEach { arbeidsgiver ->
            overstyrArbeidsforhold.lagre(arbeidsgiver)
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
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreOmregnetÅrsinntekt(arbeidsgiverInntekt, skjæringstidspunkt, hendelse)
    }

    internal fun lagreRapporterteInntekter(
        orgnummer: String,
        arbeidsgiverInntekt: ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        hendelse: PersonHendelse
    ) {
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreRapporterteInntekter(arbeidsgiverInntekt, skjæringstidspunkt, hendelse)
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(
        orgnummer: String,
        inntektsopplysninger: List<Inntektsopplysning>,
        aktivitetslogg: IAktivitetslogg,
        hendelseId: UUID
    ) {
        finnArbeidsgiverForInntekter(orgnummer, aktivitetslogg).lagreSykepengegrunnlagFraInfotrygd(inntektsopplysninger, hendelseId)
    }

    internal fun beregnSykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        subsumsjonObserver: SubsumsjonObserver
    ): Sykepengegrunnlag {
        return Sykepengegrunnlag.opprett(
            alder,
            arbeidsgivere.beregnSykepengegrunnlag(skjæringstidspunkt, subsumsjonObserver),
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

    internal fun beregnSammenligningsgrunnlag(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Sammenligningsgrunnlag {
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

    internal fun kanRevurdereInntekt(skjæringstidspunkt: LocalDate) = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) != null

    internal fun harVedtaksperiodeForAnnenArbeidsgiver(arbeidsgiver: Arbeidsgiver, skjæringstidspunkt: LocalDate) = arbeidsgivere
        .filter { it != arbeidsgiver }
        .harVedtaksperiodeFor(skjæringstidspunkt)

    internal fun harFlereArbeidsgivereMedSykdom() = arbeidsgivere.count(Arbeidsgiver::harSykdomEllerForventerSøknad) > 1

    private fun arbeidsgivereMedSykdom() = arbeidsgivere.filter(Arbeidsgiver::harSykdom)

    internal fun kunOvergangFraInfotrygd(vedtaksperiode: Vedtaksperiode) =
        Arbeidsgiver.kunOvergangFraInfotrygd(arbeidsgivere, vedtaksperiode)

    internal fun ingenUkjenteArbeidsgivere(vedtaksperiode: Vedtaksperiode, skjæringstidspunkt: LocalDate) =
        Arbeidsgiver.ingenUkjenteArbeidsgivere(arbeidsgivere, vedtaksperiode, infotrygdhistorikk, skjæringstidspunkt)

    internal fun invaliderAllePerioder(hendelse: IAktivitetslogg, feilmelding: String?) {
        feilmelding?.also(hendelse::error)
        søppelbøtte(hendelse, ALLE)
    }

    internal fun søppelbøtte(hendelse: IAktivitetslogg, filter: VedtaksperiodeFilter) {
        infotrygdhistorikk.tøm()
        Arbeidsgiver.søppelbøtte(arbeidsgivere, hendelse, filter)
        gjenopptaBehandling(hendelse)
        gjenopptaBehandlingNy(hendelse)
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
            override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
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

    internal fun harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer: String, periode: Periode) =
        arbeidsgivere.harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer, periode)

    internal fun lagreArbeidsforhold(
        orgnummer: String,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        aktivitetslogg: IAktivitetslogg,
        skjæringstidspunkt: LocalDate
    ) {
        finnEllerOpprettArbeidsgiver(orgnummer, aktivitetslogg).lagreArbeidsforhold(arbeidsforhold, skjæringstidspunkt)
    }

    internal fun brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(orgnummerFraAAreg: List<String>, skjæringstidspunkt: LocalDate) {
        val arbeidsgivereMedSykdom = arbeidsgivere.filter { it.harSykdomFor(skjæringstidspunkt) }.map(Arbeidsgiver::organisasjonsnummer)
        if (arbeidsgivereMedSykdom.containsAll(orgnummerFraAAreg)) {
            sikkerLogg.info("Ingen spøkelser, har sykdom hos alle kjente arbeidsgivere antall=${arbeidsgivereMedSykdom.size}")
        } else {
            sikkerLogg.info("Vi har kontakt med spøkelser, fnr=$fødselsnummer, antall=${orgnummerFraAAreg.size}")
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
            sikkerLogg.info("Fant arbeidsgivere i spleis som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $fødselsnummer")
        } else if (manglerIAAReg.isNotEmpty()) {
            sikkerLogg.info("Fant arbeidsgivere i IT som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $fødselsnummer")
        } else {
            sikkerLogg.info("AAReg kjenner til alle arbeidsgivere i spleis, opprettet (${nyeOrgnummer}) for $fødselsnummer")
        }
    }

    internal fun fyllUtPeriodeMedForventedeDager(hendelse: PersonHendelse, periode: Periode, skjæringstidspunkt: LocalDate) {
        vilkårsgrunnlagFor(skjæringstidspunkt)!!.sykepengegrunnlag().inntektsopplysningPerArbeidsgiver().keys
            .map { arbeidsgivere.finn(it)!! }
            .forEach { it.fyllUtPeriodeMedForventedeDager(hendelse, periode) }
    }

    private fun arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt: LocalDate): List<Arbeidsgiver> =
        arbeidsgivere.filter { it.harRelevantArbeidsforhold(skjæringstidspunkt) }

    internal fun antallArbeidsgivereMedRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt).size

    internal fun harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.any { it.harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt) }

    internal fun orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsgivere
        .filter { vilkårsgrunnlagHistorikk.erRelevant(it.organisasjonsnummer(), listOf(skjæringstidspunkt)) }
        .map { it.organisasjonsnummer() }

    internal fun harFlereArbeidsforholdMedUlikStartdato(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.mapNotNull { it.finnFørsteFraværsdag(skjæringstidspunkt) }.sorted().distinct().count() > 1

    internal fun harKunEttAnnetRelevantArbeidsforholdEnn(skjæringstidspunkt: LocalDate, orgnummer: String): Boolean {
        val aktiveArbeidsforhold = arbeidsgivereMedRelevanteArbeidsforhold(skjæringstidspunkt)
        return aktiveArbeidsforhold.size == 1 && aktiveArbeidsforhold.single().organisasjonsnummer() != orgnummer
    }

    internal fun vilkårsprøvEtterNyInformasjonFraSaksbehandler(hendelse: PersonHendelse, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver) {
        val sykepengegrunnlag = beregnSykepengegrunnlag(skjæringstidspunkt, subsumsjonObserver)
        val sammenligningsgrunnlag = beregnSammenligningsgrunnlag(skjæringstidspunkt, subsumsjonObserver)
        val avviksprosent = sammenligningsgrunnlag.avviksprosent(sykepengegrunnlag, subsumsjonObserver)

        val harAkseptabeltAvvik = Inntektsvurdering.sjekkAvvik(avviksprosent, hendelse) { _, maksimaltTillattAvvik ->
            warn("Har mer enn %.0f %% avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.", maksimaltTillattAvvik)
        }

        val opptjening = beregnOpptjening(skjæringstidspunkt, subsumsjonObserver)
        if (!opptjening.erOppfylt()) {
            hendelse.warn("Perioden er avslått på grunn av manglende opptjening")
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
            is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag -> hendelse.error("Vilkårsgrunnlaget ligger i infotrygd. Det er ikke støttet i revurdering eller overstyring.")
            else -> hendelse.error("Fant ikke vilkårsgrunnlag. Kan ikke vilkårsprøve på nytt etter ny informasjon fra saksbehandler.")
        }
    }

    internal fun loggførHendelsesreferanse(orgnummer: String, skjæringstidspunkt: LocalDate, hendelse: OverstyrInntekt) =
        arbeidsgivere.forEach { it.loggførHendelsesreferanse(orgnummer, skjæringstidspunkt, hendelse) }

    internal fun gjenopptaBehandlingNy(hendelse: IAktivitetslogg) {
        arbeidsgivere.gjenopptaBehandlingNy(hendelse)
    }

    internal fun startRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
        arbeidsgivere.startRevurdering(vedtaksperiode, hendelse)
    }

    internal fun kanStarteRevurdering(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.kanStarteRevurdering(vedtaksperiode)

    internal fun slettUtgåtteSykmeldingsperioder(tom: LocalDate) {
        arbeidsgivere.slettUtgåtteSykmeldingsperioder(tom)
    }

    internal fun lagUtbetalinger(aktivitetslogg: IAktivitetslogg, periode: Periode, subsumsjonObserver: SubsumsjonObserver, vedtaksperioder: List<Vedtaksperiode>) =
        Utbetaling.Builder(
            fødselsnummer = fødselsnummer,
            alder = alder,
            aktivitetslogg = aktivitetslogg,
            periode = periode,
            subsumsjonObserver = subsumsjonObserver,
            dødsdato = dødsdato,
            infotrygdhistorikk = infotrygdhistorikk,
            regler = NormalArbeidstaker
        ).apply {
            avvisInngangsvilkårfilter(AvvisInngangsvilkårfilter(vilkårsgrunnlagHistorikk))
            vedtaksperioder.lagUtbetalinger(this, vilkårsgrunnlagHistorikk)
        }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        skjæringstidspunkt: LocalDate,
        erForlengelse: Boolean
    ): Boolean {
        arbeidsgivere.validerVilkårsgrunnlag(aktivitetslogg, vilkårsgrunnlag, skjæringstidspunkt, erForlengelse)
        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun validerYtelserForSkjæringstidspunkt(ytelser: Ytelser, skjæringstidspunkt: LocalDate) {
        arbeidsgivere.validerYtelserForSkjæringstidspunkt(ytelser, skjæringstidspunkt, infotrygdhistorikk)
    }

    internal fun skjæringstidspunktperiode(skjæringstidspunkt: LocalDate) = arbeidsgivere.skjæringstidspunktperiode(skjæringstidspunkt)
}