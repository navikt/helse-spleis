package no.nav.helse.person

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Vedtaksperiode.Companion.harInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.OppdragReflect
import no.nav.helse.serde.reflection.UtbetalingsdagerReflect
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.utbetaltTidslinje
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Arbeidsgiver private constructor(
    private val person: Person,
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntektshistorikk: Inntektshistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val vedtaksperioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<ForkastetVedtaksperiode>,
    private val utbetalinger: MutableList<Utbetaling>,
    private val beregnetUtbetalingstidslinjer: MutableList<Utbetalingstidslinjeberegning>,
    private val refusjonOpphører: MutableList<LocalDate?>
) : Aktivitetskontekst, UtbetalingObserver {
    internal constructor(person: Person, organisasjonsnummer: String) : this(
        person = person,
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntektshistorikk = Inntektshistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        vedtaksperioder = mutableListOf(),
        forkastede = mutableListOf(),
        utbetalinger = mutableListOf(),
        beregnetUtbetalingstidslinjer = mutableListOf(),
        refusjonOpphører = mutableListOf()
    )

    init {
        utbetalinger.forEach { it.register(this) }
    }

    internal companion object {
        internal val SENERE_EXCLUSIVE = fun(senereEnnDenne: Vedtaksperiode): VedtaksperioderFilter {
            return fun(vedtaksperiode: Vedtaksperiode) = vedtaksperiode > senereEnnDenne
        }
        internal val SENERE_INCLUSIVE = fun(senereEnnDenne: Vedtaksperiode): VedtaksperioderFilter {
            return fun(vedtaksperiode: Vedtaksperiode) = vedtaksperiode >= senereEnnDenne
        }
        internal val ALLE: VedtaksperioderFilter = { true }

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder() = mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode() }

        internal fun List<Arbeidsgiver>.grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
            this.mapNotNull { it.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, maxOf(skjæringstidspunkt, periodeStart)) }
                .takeIf { it.isNotEmpty() }
                ?.summer()

        internal fun List<Arbeidsgiver>.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { it.inntektshistorikk.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt) }
                .takeIf { it.isNotEmpty() }
                ?.summer()

        internal fun List<Arbeidsgiver>.harNødvendigInntekt(skjæringstidspunkt: LocalDate) =
            this.all { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).harInntekt() }

        /**
         * Brukes i MVP for flere arbeidsgivere. Alle forlengelser hos alle arbeidsgivere må gjelde samme periode
         * */
        internal fun Iterable<Arbeidsgiver>.forlengerSammePeriode(other: Vedtaksperiode): Boolean {
            val arbeidsgivere = filter { it.finnSykeperiodeRettFør(other) != null || it.finnForkastetSykeperiodeRettFør(other) != null }
            if (arbeidsgivere.size == 1) return true
            return arbeidsgivere.all { arbeidsgiver ->
                arbeidsgiver.vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.periode() == other.periode() }
                    && arbeidsgiver.finnSykeperiodeRettFør(other) != null
            }
        }

        internal fun Iterable<Arbeidsgiver>.forlengerIkkeBareAnnenArbeidsgiver(arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode) =
            if (arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode) != null) true
            else none { other -> other.finnSykeperiodeRettFør(vedtaksperiode) != null }

        internal fun Iterable<Arbeidsgiver>.harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode: Vedtaksperiode) = this
            .filter { vedtaksperiode !in it.vedtaksperioder }
            .any { arbeidsgiver ->
                arbeidsgiver
                    .vedtaksperioder
                    .any { it.periode().overlapperMed(vedtaksperiode.periode()) }
            }

        internal fun kunOvergangFraInfotrygd(
            arbeidsgivere: Iterable<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode
        ) = arbeidsgivere
                .flatMap { it.vedtaksperioder }
                .filter { it.periode().overlapperMed(vedtaksperiode.periode()) }
                .all { it.periodetype() == Periodetype.OVERGANG_FRA_IT }

        internal fun ingenUkjenteArbeidsgivere(
            arbeidsgivere: Iterable<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            infotrygdhistorikk: Infotrygdhistorikk,
            skjæringstidspunkt: LocalDate
        ): Boolean {
            val orgnumre = arbeidsgivere
                .filter { it.vedtaksperioder.any { it.periode().overlapperMed(vedtaksperiode.periode()) } }
                .map { it.organisasjonsnummer }
                .distinct()
            return infotrygdhistorikk.ingenUkjenteArbeidsgivere(orgnumre, skjæringstidspunkt)
        }

        internal fun skjæringstidspunkt(arbeidsgivere: List<Arbeidsgiver>, periode: Periode, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkt(periode, arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))

        internal fun skjæringstidspunkter(arbeidsgivere: List<Arbeidsgiver>, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkter(arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))
    }

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this, id, organisasjonsnummer)
        inntektshistorikk.accept(visitor)
        sykdomshistorikk.accept(visitor)
        visitor.preVisitUtbetalinger(utbetalinger)
        utbetalinger.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalinger(utbetalinger)
        visitor.preVisitPerioder(vedtaksperioder)
        vedtaksperioder.forEach { it.accept(visitor) }
        visitor.postVisitPerioder(vedtaksperioder)
        visitor.preVisitForkastedePerioder(forkastede)
        forkastede.forEach { it.accept(visitor) }
        visitor.postVisitForkastedePerioder(forkastede)
        visitor.preVisitUtbetalingstidslinjeberegninger(beregnetUtbetalingstidslinjer)
        beregnetUtbetalingstidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalingstidslinjeberegninger(beregnetUtbetalingstidslinjer)
        visitor.postVisitArbeidsgiver(this, id, organisasjonsnummer)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun utbetaling() = utbetalinger.lastOrNull()

    internal fun lagUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        fødselsnummer: String,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode,
        forrige: Utbetaling?
    ): Utbetaling {
        return Utbetalingstidslinjeberegning.lagUtbetaling(
            beregnetUtbetalingstidslinjer,
            utbetalinger,
            fødselsnummer,
            periode,
            aktivitetslogg,
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager,
            forrige
        ).also { nyUtbetaling(it) }
    }

    internal fun lagRevurdering(
        aktivitetslogg: IAktivitetslogg,
        fødselsnummer: String,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode,
        forrige: Utbetaling?
    ): Utbetaling {
        return Utbetalingstidslinjeberegning.lagRevurdering(
            beregnetUtbetalingstidslinjer,
            utbetalinger,
            fødselsnummer,
            periode,
            aktivitetslogg,
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager,
            forrige
        ).also { nyUtbetaling(it) }
    }

    private fun nyUtbetaling(utbetaling: Utbetaling) {
        utbetalinger.add(utbetaling)
        utbetaling.register(this)
    }

    internal fun nåværendeTidslinje() =
        beregnetUtbetalingstidslinjer.lastOrNull()?.utbetalingstidslinje() ?: throw IllegalStateException("mangler utbetalinger")

    internal fun lagreUtbetalingstidslinjeberegning(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje) {
        beregnetUtbetalingstidslinjer.add(sykdomshistorikk.lagUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje))
    }

    internal fun håndter(sykmelding: Sykmelding) {
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, sykmelding)
        håndterEllerOpprettVedtaksperiode(sykmelding, Vedtaksperiode::håndter)
    }

    internal fun håndter(søknad: Søknad) {
        if (Toggles.OppretteVedtaksperioderVedSøknad.enabled) return håndterEllerOpprettVedtaksperiode(søknad, Vedtaksperiode::håndter)
        søknad.kontekst(this)
        ingenHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke søknad. Har nok ikke mottatt sykmelding")
        finalize(søknad)
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) {
        if (Toggles.OppretteVedtaksperioderVedSøknad.enabled) return håndterEllerOpprettVedtaksperiode(søknad, Vedtaksperiode::håndter)
        søknad.kontekst(this)
        ingenHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke søknad til arbeidsgiver. Har nok ikke mottatt sykmelding")
        finalize(søknad)
    }

    private fun <Hendelse: SykdomstidslinjeHendelse> håndterEllerOpprettVedtaksperiode(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean) {
        hendelse.kontekst(this)
        if (!ingenHåndtert(hendelse, håndterer) && !hendelse.hasErrorsOrWorse()) {
            hendelse.info("Lager ny vedtaksperiode pga. ${hendelse.kilde}")
            val ny = nyVedtaksperiode(hendelse).also { håndterer(it, hendelse) }
            håndter(hendelse) { nyPeriode(ny, hendelse) }
        }
        finalize(hendelse)
    }

    internal fun harRefusjonOpphørt(periodeTom: LocalDate): Boolean {
        return refusjonOpphører.firstOrNull()?.let { it <= periodeTom } ?: false
    }

    internal fun cacheRefusjon(opphørsdato: LocalDate?) {
        if (refusjonOpphører.firstOrNull() != opphørsdato) refusjonOpphører.add(0, opphørsdato)
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID? = null) {
        inntektsmelding.kontekst(this)
        inntektsmelding.cacheRefusjon(this)
        trimTidligereBehandletDager(inntektsmelding)
        if (vedtaksperiodeId != null) {
            if (!énHåndtert(inntektsmelding) { håndter(inntektsmelding, vedtaksperiodeId) })
                return inntektsmelding.info("Vedtaksperiode overlapper ikke med replayet Inntektsmelding")
            inntektsmelding.info("Replayer inntektsmelding til påfølgende perioder som overlapper.")
        }
        if (!ingenHåndtert(inntektsmelding, Vedtaksperiode::håndter) && vedtaksperiodeId == null)
            inntektsmelding.error("Forventet ikke inntektsmelding. Har nok ikke mottatt sykmelding")

        finalize(inntektsmelding)
    }

    internal fun håndter(inntektsmelding: InntektsmeldingReplay) {
        inntektsmelding.fortsettÅBehandle(this)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        utbetalingshistorikk.kontekst(this)
        håndter(utbetalingshistorikk) { håndter(utbetalingshistorikk, infotrygdhistorikk) }
        finalize(utbetalingshistorikk)
    }

    internal fun håndter(ytelser: Ytelser, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger, infotrygdhistorikk: Infotrygdhistorikk) {
        ytelser.kontekst(this)
        håndter(ytelser) { håndter(ytelser, infotrygdhistorikk, arbeidsgiverUtbetalinger) }
        finalize(ytelser)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        utbetalingsgodkjenning.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetalingsgodkjenning) }
        håndter(utbetalingsgodkjenning, Vedtaksperiode::håndter)
        finalize(utbetalingsgodkjenning)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.kontekst(this)
        håndter(vilkårsgrunnlag, Vedtaksperiode::håndter)
        finalize(vilkårsgrunnlag)
    }

    internal fun håndter(simulering: Simulering) {
        simulering.kontekst(this)
        håndter(simulering, Vedtaksperiode::håndter)
        finalize(simulering)
    }

    internal fun håndter(utbetaling: UtbetalingOverført) {
        utbetaling.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetaling) }
        finalize(utbetaling)
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        utbetaling.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetaling) }
        håndter(utbetaling, Vedtaksperiode::håndter)
        finalize(utbetaling)
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse) {
        påminnelse.kontekst(this)
        utbetalinger.forEach { it.håndter(påminnelse) }
        finalize(påminnelse)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        påminnelse.kontekst(this)
        return énHåndtert(påminnelse, Vedtaksperiode::håndter).also { finalize(påminnelse) }
    }

    internal fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        hendelse.info("Håndterer annullering")

        val sisteUtbetalte = Utbetaling.finnUtbetalingForAnnullering(utbetalinger, hendelse) ?: return
        val annullering = sisteUtbetalte.annuller(hendelse) ?: return
        nyUtbetaling(annullering)
        annullering.håndter(hendelse)
        søppelbøtte(hendelse, ALLE, ForkastetÅrsak.ANNULLERING)
        finalize(hendelse)
    }

    internal fun håndter(arbeidsgivere: List<Arbeidsgiver>, hendelse: Grunnbeløpsregulering) {
        hendelse.kontekst(this)
        hendelse.info("Håndterer etterutbetaling")

        val sisteUtbetalte = Utbetaling.finnUtbetalingForJustering(
            utbetalinger = utbetalinger,
            hendelse = hendelse
        ) ?: return hendelse.info("Fant ingen utbetalinger å etterutbetale")

        val periode = LocalDate.of(2020, 5, 1).minusMonths(18) til LocalDate.now()

        val reberegnetTidslinje = reberegnUtbetalte(hendelse, arbeidsgivere, periode)

        val etterutbetaling = sisteUtbetalte.etterutbetale(hendelse, reberegnetTidslinje)
            ?: return hendelse.info("Utbetalingen for $organisasjonsnummer for perioden ${sisteUtbetalte.periode} er ikke blitt endret. Grunnbeløpsregulering gjennomføres ikke.")

        hendelse.info("Etterutbetaler for $organisasjonsnummer for perioden ${sisteUtbetalte.periode}")
        nyUtbetaling(etterutbetaling)
        etterutbetaling.håndter(hendelse)
    }

    private fun reberegnUtbetalte(
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        periode: Periode
    ): Utbetalingstidslinje {
        val arbeidsgivertidslinjer = arbeidsgivere
            .map { it to it.utbetalinger.utbetaltTidslinje() }
            .filter { it.second.isNotEmpty() }
            .toMap()

        MaksimumUtbetaling(arbeidsgivertidslinjer.values.toList(), aktivitetslogg, periode.endInclusive)
            .betal()

        arbeidsgivertidslinjer.forEach { (arbeidsgiver, reberegnetUtbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, reberegnetUtbetalingstidslinje)
        }

        return nåværendeTidslinje()
    }

    override fun utbetalingUtbetalt(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
    ) {
        person.utbetalingUtbetalt(
            PersonObserver.UtbetalingUtbetaltEvent(
                utbetalingId = id,
                type = type.name,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                ident = ident,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = OppdragReflect(arbeidsgiverOppdrag).toMap(),
                personOppdrag = OppdragReflect(personOppdrag).toMap(),
                utbetalingsdager = UtbetalingsdagerReflect(utbetalingstidslinje).toList()
            )
        )
    }

    override fun utbetalingUtenUtbetaling(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
    ) {
        person.utbetalingUtenUtbetaling(
            PersonObserver.UtbetalingUtbetaltEvent(
                utbetalingId = id,
                type = type.name,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                ident = ident,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = OppdragReflect(arbeidsgiverOppdrag).toMap(),
                personOppdrag = OppdragReflect(personOppdrag).toMap(),
                utbetalingsdager = UtbetalingsdagerReflect(utbetalingstidslinje).toList()
            )
        )
    }

    override fun utbetalingEndret(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetaling.Tilstand,
        nesteTilstand: Utbetaling.Tilstand
    ) {
        person.utbetalingEndret(
            PersonObserver.UtbetalingEndretEvent(
                utbetalingId = id,
                type = type.name,
                forrigeStatus = Utbetalingstatus.fraTilstand(forrigeTilstand).name,
                gjeldendeStatus = Utbetalingstatus.fraTilstand(nesteTilstand).name,
                arbeidsgiverOppdrag = OppdragReflect(arbeidsgiverOppdrag).toMap(),
                personOppdrag = OppdragReflect(personOppdrag).toMap(),
            )
        )
    }

    override fun utbetalingAnnullert(
        id: UUID,
        periode: Periode,
        fagsystemId: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String
    ) {
        person.annullert(
            PersonObserver.UtbetalingAnnullertEvent(
                fagsystemId = fagsystemId,
                utbetalingId = id,
                fom = periode.start,
                tom = periode.endInclusive,
                // TODO: gå bort fra å sende linje ettersom det er bare perioden som er interessant for konsumenter
                utbetalingslinjer = listOf(
                    PersonObserver.UtbetalingAnnullertEvent.Utbetalingslinje(
                        fom = periode.start,
                        tom = periode.endInclusive,
                        beløp = 0,
                        grad = 0.0
                    )
                ),
                annullertAvSaksbehandler = godkjenttidspunkt,
                saksbehandlerEpost = saksbehandlerEpost,
                saksbehandlerIdent = saksbehandlerIdent
            )
        )
    }

    internal fun håndter(hendelse: OverstyrTidslinje) {
        hendelse.kontekst(this)
        håndter(hendelse, Vedtaksperiode::håndter)
    }

    internal fun oppdaterSykdom(hendelse: SykdomstidslinjeHendelse) = sykdomshistorikk.håndter(hendelse)

    private fun sykdomstidslinje(): Sykdomstidslinje {
        val sykdomstidslinje = if (sykdomshistorikk.harSykdom()) sykdomshistorikk.sykdomstidslinje() else Sykdomstidslinje()
        return Utbetaling.sykdomstidslinje(utbetalinger, sykdomstidslinje)
    }

    internal fun tidligsteDato(): LocalDate {
        return sykdomstidslinje().førsteDag()
    }

    internal fun fjernDager(periode: Periode) = sykdomshistorikk.fjernDager(periode)

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
        inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periodeStart)

    internal fun addInntekt(inntektsmelding: Inntektsmelding, skjæringstidspunkt: LocalDate) {
        inntektsmelding.addInntekt(inntektshistorikk, skjæringstidspunkt)
    }

    internal fun lagreInntekter(inntektsopplysninger: List<Inntektsopplysning>, hendelseId: UUID) {
        Inntektsopplysning.lagreInntekter(inntektsopplysninger, inntektshistorikk, hendelseId)
    }

    internal fun lagreInntekter(
        arbeidsgiverInntekt: Inntektsvurdering.ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        vilkårsgrunnlag: Vilkårsgrunnlag
    ) {
        arbeidsgiverInntekt.lagreInntekter(
            inntektshistorikk,
            skjæringstidspunkt,
            vilkårsgrunnlag.meldingsreferanseId()
        )
    }

    internal fun søppelbøtte(
        hendelse: ArbeidstakerHendelse,
        filter: VedtaksperioderFilter,
        årsak: ForkastetÅrsak
    ): List<Vedtaksperiode> {
        return forkast(filter, årsak)
            .takeIf { it.isNotEmpty() }
            ?.also { perioder ->
                perioder
                    .forEach {
                        it.ferdig(hendelse, årsak)
                        fjernDager(it.periode())
                    }
                if (vedtaksperioder.isEmpty()) sykdomshistorikk.tøm()
                else sykdomshistorikk.fjernDagerFør(vedtaksperioder.first().periode().start)
                gjenopptaBehandling()
            }
            ?: listOf()
    }

    private fun forkast(filter: VedtaksperioderFilter, årsak: ForkastetÅrsak) = vedtaksperioder
        .filter(filter)
        .also { perioder ->
            vedtaksperioder.removeAll(perioder)
            forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it, årsak) })
        }

    private fun tidligereOgEttergølgende(vedtaksperiode: Vedtaksperiode): MutableList<Vedtaksperiode> {
        var index = vedtaksperioder.indexOf(vedtaksperiode)
        val results = vedtaksperioder.subList(0, index + 1).toMutableList()
        if (results.isEmpty()) return mutableListOf()
        while (vedtaksperioder.last() != results.last()) {
            if (!vedtaksperioder[index].erSykeperiodeRettFør(vedtaksperioder[index + 1])) break
            results.add(vedtaksperioder[index + 1])
            index++
        }
        return results
    }

    //TODO: rename disse
    internal fun revurdering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        val revurdering = Revurdering(hendelse)
        vedtaksperioder.firstOrNull { it.avventerRevurdering(vedtaksperiode, revurdering) }
    }

    internal fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        val sisteUtbetalte = vedtaksperioder.sisteSammenhengedeUtbetaling(vedtaksperiode)
        sisteUtbetalte?.revurder(hendelse)
    }

    private fun List<Vedtaksperiode>.sisteSammenhengedeUtbetaling(vedtaksperiode: Vedtaksperiode) =
        this.filter { it.sammeArbeidsgiverPeriodeOgUtbetalt(vedtaksperiode) }.maxOrNull()

    internal fun blokkeresRevurdering(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.any { it.blokkererRevurdering(vedtaksperiode) }

    internal fun tidligereOgEttergølgende(segSelv: Periode): VedtaksperioderFilter {
        val tidligereOgEttergølgende1 = vedtaksperioder.sorted().firstOrNull { it.periode().overlapperMed(segSelv) }?.let(::tidligereOgEttergølgende)
        return fun(vedtaksperiode: Vedtaksperiode) = tidligereOgEttergølgende1 != null && vedtaksperiode in tidligereOgEttergølgende1
    }

    private fun nyVedtaksperiode(hendelse: SykdomstidslinjeHendelse): Vedtaksperiode {
        return Vedtaksperiode(
            person = person,
            arbeidsgiver = this,
            hendelse = hendelse
        ).also {
            vedtaksperioder.add(it)
            vedtaksperioder.sort()
        }
    }

    internal fun finnSykeperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erSykeperiodeRettFør(vedtaksperiode)
        }

    internal fun finnForkastetSykeperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        ForkastetVedtaksperiode.finnForkastetSykeperiodeRettFør(forkastede, vedtaksperiode)

    internal fun finnSykeperiodeRettEtter(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other -> vedtaksperiode.erSykeperiodeRettFør(other) }

    internal fun tidligerePerioderFerdigBehandlet(vedtaksperiode: Vedtaksperiode) =
        Vedtaksperiode.tidligerePerioderFerdigBehandlet(vedtaksperioder, vedtaksperiode)

    private var skalGjenopptaBehandling = false
    internal fun gjenopptaBehandling() {
        skalGjenopptaBehandling = true
    }

    private fun finalize(hendelse: ArbeidstakerHendelse) {
        while (skalGjenopptaBehandling) {
            skalGjenopptaBehandling = false
            val gjenopptaBehandling = GjenopptaBehandling(hendelse)
            énHåndtert(gjenopptaBehandling, Vedtaksperiode::håndter)
            Vedtaksperiode.gjentaHistorikk(hendelse, person)
        }
    }

    internal class GjenopptaBehandling(private val hendelse: ArbeidstakerHendelse) :
        ArbeidstakerHendelse(hendelse) {
        override fun organisasjonsnummer() = hendelse.organisasjonsnummer()
        override fun aktørId() = hendelse.aktørId()
        override fun fødselsnummer() = hendelse.fødselsnummer()
    }

    internal class Revurdering(private val hendelse: ArbeidstakerHendelse) :
        ArbeidstakerHendelse(hendelse) {
        override fun organisasjonsnummer() = hendelse.organisasjonsnummer()
        override fun aktørId() = hendelse.aktørId()
        override fun fødselsnummer() = hendelse.fødselsnummer()
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to organisasjonsnummer))
    }

    internal fun lås(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().lås(periode)
    }

    internal fun låsOpp(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().låsOpp(periode)
    }

    internal fun harSykdom() = sykdomshistorikk.harSykdom()

    internal fun periodetype(periode: Periode): Periodetype {
        val skjæringstidspunkt = skjæringstidspunkt(periode)
        return when {
            erFørstegangsbehandling(periode) -> Periodetype.FØRSTEGANGSBEHANDLING
            forlengerInfotrygd(periode) -> when {
                Utbetaling.harBetalt(utbetalinger, Periode(skjæringstidspunkt, periode.start.minusDays(1))) -> Periodetype.INFOTRYGDFORLENGELSE
                else -> Periodetype.OVERGANG_FRA_IT
            }
            !Utbetaling.harBetalt(utbetalinger, skjæringstidspunkt) -> Periodetype.FØRSTEGANGSBEHANDLING
            else -> Periodetype.FORLENGELSE
        }
    }

    private fun erFørstegangsbehandling(periode: Periode) =
        skjæringstidspunkt(periode) in periode

    internal fun erForlengelse(periode: Periode) =
        !erFørstegangsbehandling(periode)

    internal fun forlengerInfotrygd(periode: Periode) =
        person.harInfotrygdUtbetalt(organisasjonsnummer, skjæringstidspunkt(periode))

    private fun skjæringstidspunkt(periode: Periode) = person.skjæringstidspunkt(organisasjonsnummer, sykdomstidslinje(), periode)

    internal fun avgrensetPeriode(periode: Periode) =
        Periode(maxOf(periode.start, skjæringstidspunkt(periode)), periode.endInclusive)

    internal fun forrigeSkjæringstidspunktInnenforArbeidsgiverperioden(regler: ArbeidsgiverRegler, nyFørsteSykedag: LocalDate): LocalDate? {
        val sykdomstidslinje = person.historikkFor(organisasjonsnummer, sykdomstidslinje())
        if (sykdomstidslinje.harNyArbeidsgiverperiodeFør(regler, nyFørsteSykedag)) return null
        return sykdomstidslinje.skjæringstidspunkt(nyFørsteSykedag.minusDays(1))
    }

    internal fun builder(regler: ArbeidsgiverRegler, skjæringstidspunkter: List<LocalDate>): UtbetalingstidslinjeBuilder {
        return UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = skjæringstidspunkter,
            inntektshistorikk = inntektshistorikk,
            arbeidsgiverRegler = regler
        )
    }

    internal fun build(builder: IUtbetalingstidslinjeBuilder, periode: Periode) =
        builder.result(sykdomstidslinje(), periode)

    internal fun beregn(aktivitetslogg: IAktivitetslogg, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger, periode: Periode): Boolean {
        try {
            arbeidsgiverUtbetalinger.beregn(aktivitetslogg, organisasjonsnummer, periode)
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(aktivitetslogg)
        }
        return !aktivitetslogg.hasErrorsOrWorse()
    }

    internal fun forrigeAvsluttaPeriodeMedVilkårsvurdering(vedtaksperiode: Vedtaksperiode): Vedtaksperiode? {
        return Vedtaksperiode.finnForrigeAvsluttaPeriode(vedtaksperioder, vedtaksperiode) ?:
        // TODO: leiter frem fra forkasta perioder — vilkårsgrunnlag ol. felles data bør lagres på Arbeidsgivernivå
        ForkastetVedtaksperiode.finnForrigeAvsluttaPeriode(forkastede, vedtaksperiode)
    }

    internal fun opprettReferanseTilInntekt(fra: LocalDate, til: LocalDate) = inntektshistorikk.opprettReferanse(fra, til, UUID.randomUUID())

    private fun trimTidligereBehandletDager(hendelse: Inntektsmelding) {
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, hendelse)
    }

    internal fun harDagUtenSøknad(periode: Periode) =
        sykdomstidslinje().harDagUtenSøknad(periode)

    private fun <Hendelse: ArbeidstakerHendelse> ingenHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean, errortekst: String) {
        if (ingenHåndtert(hendelse, håndterer)) return
        hendelse.error(errortekst)
    }

    private fun <Hendelse: ArbeidstakerHendelse> håndter(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Unit) {
        looper { håndterer(it, hendelse) }
    }

    private fun <Hendelse: ArbeidstakerHendelse> énHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean): Boolean {
        var håndtert = false
        looper { håndtert = håndtert || håndterer(it, hendelse) }
        return håndtert
    }

    private fun <Hendelse: ArbeidstakerHendelse> ingenHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean): Boolean {
        var håndtert = false
        looper { håndtert = håndterer(it, hendelse) || håndtert }
        return håndtert
    }

    // støtter å loope over vedtaksperioder som modifiseres pga. forkasting.
    // dvs. vi stopper å iterere så snart listen har endret seg
    private fun looper(handler: (Vedtaksperiode) -> Unit) {
        val size = vedtaksperioder.size
        var neste = 0
        while (size == vedtaksperioder.size && neste < size) {
            handler(vedtaksperioder[neste])
            neste += 1
        }
    }

    internal class JsonRestorer private constructor() {
        internal companion object {
            internal fun restore(
                person: Person,
                organisasjonsnummer: String,
                id: UUID,
                inntektshistorikk: Inntektshistorikk,
                sykdomshistorikk: Sykdomshistorikk,
                vedtaksperioder: MutableList<Vedtaksperiode>,
                forkastede: MutableList<ForkastetVedtaksperiode>,
                utbetalinger: List<Utbetaling>,
                beregnetUtbetalingstidslinjer: List<Utbetalingstidslinjeberegning>,
                refusjonOpphører: List<LocalDate?>
            ) = Arbeidsgiver(
                person,
                organisasjonsnummer,
                id,
                inntektshistorikk,
                sykdomshistorikk,
                vedtaksperioder,
                forkastede,
                utbetalinger.toMutableList(),
                beregnetUtbetalingstidslinjer.toMutableList(),
                refusjonOpphører.toMutableList()
            )
        }
    }
}

internal enum class ForkastetÅrsak {
    IKKE_STØTTET,
    UKJENT,
    ERSTATTES,
    ANNULLERING
}

internal typealias VedtaksperioderFilter = (Vedtaksperiode) -> Boolean
