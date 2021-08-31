package no.nav.helse.person

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.*
import no.nav.helse.person.Vedtaksperiode.Companion.ALLE
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_REVURDERT
import no.nav.helse.person.Vedtaksperiode.Companion.harInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.harNødvendigInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.harOverlappendeUtbetaltePerioder
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling.Companion.gjelderFeriepengeutbetaling
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
    private val feriepengeutbetalinger: MutableList<Feriepengeutbetaling>,
    private val refusjonOpphører: MutableList<LocalDate?>,
    private val arbeidsforholdhistorikk: Arbeidsforholdhistorikk
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
        feriepengeutbetalinger = mutableListOf(),
        refusjonOpphører = mutableListOf(),
        arbeidsforholdhistorikk = Arbeidsforholdhistorikk()
    )

    init {
        utbetalinger.forEach { it.registrer(this) }
    }

    internal companion object {

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun List<Arbeidsgiver>.grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
            this.mapNotNull { it.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, maxOf(skjæringstidspunkt, periodeStart)) }
                .takeIf { it.isNotEmpty() }
                ?.summer()

        internal fun List<Arbeidsgiver>.grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { it.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt) }
                .takeIf { it.isNotEmpty() }
                ?.summer()

        internal fun List<Arbeidsgiver>.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { it.inntektshistorikk.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt) }
                .takeIf { it.isNotEmpty() }
                ?.summer()

        internal fun List<Arbeidsgiver>.harGrunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
            filter { it.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt) != null }

        internal fun List<Arbeidsgiver>.harNødvendigInntekt(skjæringstidspunkt: LocalDate) =
            this.all { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).harNødvendigInntekt() }
                && any { !it.grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt) }

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

        internal fun Iterable<Arbeidsgiver>.antallArbeidsgivereMedOverlappendeVedtaksperioder(vedtaksperiode: Vedtaksperiode) = this
            .count { arbeidsgiver ->
                arbeidsgiver
                    .vedtaksperioder
                    .any { it.periode().overlapperMed(vedtaksperiode.periode()) }
            }

        internal fun Iterable<Arbeidsgiver>.harArbeidsgivereMedOverlappendeUtbetaltePerioder(orgnummer: String, periode: Periode) = this
            .filter { it.organisasjonsnummer != orgnummer }
            .any { it.vedtaksperioder.harOverlappendeUtbetaltePerioder(periode) }

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
                .filter { arbeidsgiver -> arbeidsgiver.vedtaksperioder.any { it.periode().overlapperMed(vedtaksperiode.periode()) } }
                .map { it.organisasjonsnummer }
                .distinct()
            return infotrygdhistorikk.ingenUkjenteArbeidsgivere(orgnumre, skjæringstidspunkt)
        }

        internal fun skjæringstidspunkt(arbeidsgivere: List<Arbeidsgiver>, periode: Periode, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkt(periode, arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))

        internal fun skjæringstidspunkter(arbeidsgivere: List<Arbeidsgiver>, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkter(arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))

        internal fun Iterable<Arbeidsgiver>.beregnFeriepengerForAlleArbeidsgivere(
            aktørId: String,
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
        ) {
            filter { it.organisasjonsnummer != "0" }.forEach { it.utbetalFeriepenger(aktørId, feriepengeberegner, utbetalingshistorikkForFeriepenger) }
        }

        internal fun Iterable<Arbeidsgiver>.harRelevanteArbeidsforholdForFlereArbeidsgivere(skjæringstidspunkt: LocalDate) =
            this.relevanteArbeidsforhold(skjæringstidspunkt).size > 1

        internal fun Iterable<Arbeidsgiver>.relevanteArbeidsforhold(skjæringstidspunkt: LocalDate) =
            filter {(it.arbeidsforholdhistorikk.harAktivtArbeidsforhold(skjæringstidspunkt) && !it.arbeidsforholdhistorikk.arbeidsforholdErEldreEnnTreMåneder(skjæringstidspunkt)) || it.harGrunnlagForSykepengegrunnlag(skjæringstidspunkt)}
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
        visitor.preVisitFeriepengeutbetalinger(feriepengeutbetalinger)
        feriepengeutbetalinger.forEach { it.accept(visitor) }
        visitor.postVisitFeriepengeutbetalinger(feriepengeutbetalinger)
        arbeidsforholdhistorikk.accept(visitor)
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
            forrige,
            organisasjonsnummer
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
            forrige,
            organisasjonsnummer
        ).also { nyUtbetaling(it) }
    }

    private fun nyUtbetaling(utbetaling: Utbetaling) {
        utbetalinger.add(utbetaling)
        utbetaling.registrer(this)
    }

    internal fun utbetalFeriepenger(
        aktørId: String,
        feriepengeberegner: Feriepengeberegner,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
    ) {
        utbetalingshistorikkForFeriepenger.kontekst(this)

        val feriepengeutbetaling = Feriepengeutbetaling.Builder(
            aktørId,
            organisasjonsnummer,
            feriepengeberegner,
            utbetalingshistorikkForFeriepenger,
            feriepengeutbetalinger
        ).build()

        if (Toggles.SendFeriepengeOppdrag.enabled) {
            feriepengeutbetalinger.add(feriepengeutbetaling)
        }

        feriepengeutbetaling.registrer(this)

        if (Toggles.SendFeriepengeOppdrag.enabled && feriepengeutbetaling.sendTilOppdrag) {
            feriepengeutbetaling.overfør(utbetalingshistorikkForFeriepenger)
        }
    }

    internal fun nåværendeTidslinje() =
        beregnetUtbetalingstidslinjer.lastOrNull()?.utbetalingstidslinje() ?: throw IllegalStateException("mangler utbetalinger")

    internal fun lagreUtbetalingstidslinjeberegning(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje) {
        val sykdomshistorikkId = sykdomshistorikk.nyesteId()
        val inntektshistorikkId = inntektshistorikk.nyesteId()
        val vilkårsgrunnlagHistorikkId = person.nyesteIdForVilkårsgrunnlagHistorikk()
        beregnetUtbetalingstidslinjer.add(
            Utbetalingstidslinjeberegning(sykdomshistorikkId, inntektshistorikkId, vilkårsgrunnlagHistorikkId, organisasjonsnummer, utbetalingstidslinje)
        )
    }

    internal fun håndter(sykmelding: Sykmelding) {
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, sykmelding)
        håndterEllerOpprettVedtaksperiode(sykmelding, Vedtaksperiode::håndter)
    }

    internal fun håndter(søknad: Søknad) {
        if (Toggles.OppretteVedtaksperioderVedSøknad.enabled) return håndterEllerOpprettVedtaksperiode(søknad, Vedtaksperiode::håndter)
        søknad.kontekst(this)
        noenHarHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke søknad. Har nok ikke mottatt sykmelding")
        finalize(søknad)
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) {
        if (Toggles.OppretteVedtaksperioderVedSøknad.enabled) return håndterEllerOpprettVedtaksperiode(søknad, Vedtaksperiode::håndter)
        søknad.kontekst(this)
        noenHarHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke søknad til arbeidsgiver. Har nok ikke mottatt sykmelding")
        finalize(søknad)
    }

    private fun <Hendelse : SykdomstidslinjeHendelse> håndterEllerOpprettVedtaksperiode(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean) {
        hendelse.kontekst(this)
        if (!noenHarHåndtert(hendelse, håndterer)) {
            if (hendelse.forGammel()) hendelse.error("Forventet ikke ${hendelse.kilde}. Oppretter ikke vedtaksperiode.")
            if (Utbetaling.harBetalt(utbetalinger, hendelse.periode())) hendelse.error("Periode overlapper med tidligere utbetaling.")
            if (!hendelse.hasErrorsOrWorse()) {
                hendelse.info("Lager ny vedtaksperiode pga. ${hendelse.kilde}")
                val ny = nyVedtaksperiode(hendelse).also { håndterer(it, hendelse) }
                håndter(hendelse) { nyPeriode(ny, hendelse) }
            }
        }
        if (hendelse.hasErrorsOrWorse()) person.førerIkkeTilVidereBehandling(hendelse)
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
            if (!énHarHåndtert(inntektsmelding) { håndter(inntektsmelding, vedtaksperiodeId) })
                return inntektsmelding.info("Vedtaksperiode overlapper ikke med replayet Inntektsmelding")
            inntektsmelding.info("Replayer inntektsmelding til påfølgende perioder som overlapper.")
        }
        if (!noenHarHåndtert(inntektsmelding, Vedtaksperiode::håndter) && vedtaksperiodeId == null)
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

    internal fun håndter(utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        utbetalingsgrunnlag.kontekst(this)
        håndter(utbetalingsgrunnlag, Vedtaksperiode::håndter)
        finalize(utbetalingsgrunnlag)
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

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse) {
        utbetalingHendelse.kontekst(this)
        if (feriepengeutbetalinger.gjelderFeriepengeutbetaling(utbetalingHendelse)) return håndterFeriepengeUtbetaling(utbetalingHendelse)
        håndterUtbetaling(utbetalingHendelse)
    }

    private fun håndterFeriepengeUtbetaling(utbetalingHendelse: UtbetalingHendelse) {
        feriepengeutbetalinger.map { it.håndter(utbetalingHendelse, person) }
    }

    private fun håndterUtbetaling(utbetaling: UtbetalingHendelse) {
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
        return énHarHåndtert(påminnelse, Vedtaksperiode::håndter).also { finalize(påminnelse) }
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
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toMap(),
                personOppdrag = personOppdrag.toMap(),
                utbetalingsdager = utbetalingstidslinje.toList(),
                vedtaksperiodeIder = vedtaksperioder.iderMedUtbetaling(id) + forkastede.iderMedUtbetaling(id)
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
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toMap(),
                personOppdrag = personOppdrag.toMap(),
                utbetalingsdager = utbetalingstidslinje.toList(),
                vedtaksperiodeIder = vedtaksperioder.iderMedUtbetaling(id) + forkastede.iderMedUtbetaling(id)
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
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toMap(),
                personOppdrag = personOppdrag.toMap(),
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
        finalize(hendelse)
    }

    internal fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)
        vedtaksperioder.firstOrNull { it.gjelder(hendelse.skjæringstidspunkt) }
            ?.håndter(hendelse)
        finalize(hendelse)
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

    internal fun addInntekt(hendelse: OverstyrInntekt) {
        hendelse.addInntekt(inntektshistorikk)
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(inntektsopplysninger: List<Inntektsopplysning>, hendelseId: UUID) {
        Inntektsopplysning.lagreInntekter(inntektsopplysninger, inntektshistorikk, hendelseId)
    }

    internal fun lagreSykepengegrunnlag(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
        if (harAktivtArbeidsforhold(skjæringstidspunkt)) {
            arbeidsgiverInntekt.lagreInntekter(inntektshistorikk, skjæringstidspunkt, hendelse.meldingsreferanseId())
        }
    }

    internal fun lagreSammenligningsgrunnlag(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
        arbeidsgiverInntekt.lagreInntekter(inntektshistorikk, skjæringstidspunkt, hendelse.meldingsreferanseId())
    }

    internal fun søppelbøtte(
        hendelse: IAktivitetslogg,
        filter: VedtaksperiodeFilter,
        årsak: ForkastetÅrsak
    ): List<Vedtaksperiode> {
        return forkast(filter, årsak)
            .takeIf { it.isNotEmpty() }
            ?.also { perioder ->
                perioder
                    .forEach {
                        it.forkast(hendelse, årsak)
                        fjernDager(it.periode())
                    }
                if (vedtaksperioder.isEmpty()) sykdomshistorikk.tøm()
                gjenopptaBehandling()
            }
            ?: listOf()
    }

    private fun forkast(filter: VedtaksperiodeFilter, årsak: ForkastetÅrsak) = vedtaksperioder
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

    internal fun startRevurderingForAlleBerørtePerioder(hendelse: OverstyrTidslinje, vedtaksperiode: Vedtaksperiode) {
        håndter(hendelse) { nyRevurderingFør(vedtaksperiode, hendelse) }
        if (hendelse.hasErrorsOrWorse()) {
            hendelse.info("Revurdering blokkeres, gjenopptar behandling")
            return gjenopptaBehandling()
        }

        if (!Toggles.RevurderTidligerePeriode.enabled) {
            vedtaksperioder.sisteSammenhengedeUtbetaling(vedtaksperiode)?.revurder(hendelse, vedtaksperiode)
        }
    }

    internal fun startRevurderingForAlleBerørtePerioder(hendelse: OverstyrInntekt, vedtaksperiode: Vedtaksperiode) {
        håndter(hendelse) { nyRevurderingFør(vedtaksperiode, hendelse) }
        if (hendelse.hasErrorsOrWorse()) {
            hendelse.info("Revurdering blokkeres, gjenopptar behandling")
            return gjenopptaBehandling()
        }

        if(Toggles.RevurderTidligerePeriode.enabled) {
            inntektshistorikk {
                addSaksbehandler(hendelse.skjæringstidspunkt, hendelse.meldingsreferanseId(), hendelse.inntekt)
            }
        }
    }

    private fun List<Vedtaksperiode>.sisteSammenhengedeUtbetaling(vedtaksperiode: Vedtaksperiode) =
        this.filter { it.sammeArbeidsgiverPeriodeOgUtbetalt(vedtaksperiode) }.maxOrNull()

    internal fun tidligereOgEttergølgende(segSelv: Periode): VedtaksperiodeFilter {
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

    internal fun finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode: Vedtaksperiode) =
        finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode, emptyList())


    private fun finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode: Vedtaksperiode, perioderFør: List<Vedtaksperiode>): List<Vedtaksperiode> {
        vedtaksperioder.firstOrNull { other ->
            other.erSykeperiodeAvsluttetUtenUtbetalingRettFør(vedtaksperiode)
        }?.also {
            return finnSykeperioderAvsluttetUtenUtbetalingRettFør(it, perioderFør + listOf(it))
        }
        return perioderFør
    }

    internal fun finnForkastetSykeperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        ForkastetVedtaksperiode.finnForkastetSykeperiodeRettFør(forkastede, vedtaksperiode)

    internal fun tidligerePerioderFerdigBehandlet(vedtaksperiode: Vedtaksperiode) =
        Vedtaksperiode.tidligerePerioderFerdigBehandlet(vedtaksperioder, vedtaksperiode)

    internal fun alleAndrePerioderErKlare(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.filterNot { it == vedtaksperiode }.none(IKKE_FERDIG_REVURDERT)

    internal fun fordelRevurdertUtbetaling(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling) {
        fordelRevurdertUtbetaling(hendelse) { håndterRevurdertUtbetaling(utbetaling, hendelse) }
    }

    private var skalGjenopptaBehandling = false
    internal fun gjenopptaBehandling() {
        skalGjenopptaBehandling = true
    }

    private var skalGjenopptaRevurdering = false
    internal fun gjenopptaRevurdering() {
        skalGjenopptaRevurdering = true
    }

    private fun finalize(hendelse: ArbeidstakerHendelse) {
        while (skalGjenopptaBehandling) {
            skalGjenopptaBehandling = false
            val gjenopptaBehandling = GjenopptaBehandling(hendelse)
            énHarHåndtert(gjenopptaBehandling, Vedtaksperiode::håndter)
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivere, AvventerUtbetalingsgrunnlag)
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering, IKKE_FERDIG_REVURDERT)
        }

        while (skalGjenopptaRevurdering) {
            skalGjenopptaRevurdering = false
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering, IKKE_FERDIG_REVURDERT)
        }
    }

    internal class GjenopptaBehandling(private val hendelse: ArbeidstakerHendelse) :
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

    internal fun harSykdom() = sykdomshistorikk.harSykdom() || sykdomstidslinje().harSykedager()

    internal fun harSpleisSykdom() = !sykdomshistorikk.isEmpty()

    internal fun harSykdomFor(skjæringstidspunkt: LocalDate) = vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

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

    internal fun builder(regler: ArbeidsgiverRegler, skjæringstidspunkter: List<LocalDate>): UtbetalingstidslinjeBuilder {
        return UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = skjæringstidspunkter,
            inntektshistorikk = inntektshistorikk,
            arbeidsgiverRegler = regler
        )
    }

    internal fun lagreArbeidsforhold(arbeidsforhold: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        arbeidsforholdhistorikk.lagre(arbeidsforhold.filter { it.erRelevant(this) }, skjæringstidspunkt)
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

    private fun trimTidligereBehandletDager(hendelse: Inntektsmelding) {
        ForkastetVedtaksperiode.overlapperMedForkastet(vedtaksperioder, forkastede, hendelse)
    }

    internal fun harDagUtenSøknad(periode: Periode) =
        sykdomstidslinje().harDagUtenSøknad(periode)

    private fun <Hendelse : IAktivitetslogg> noenHarHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean, errortekst: String) {
        if (noenHarHåndtert(hendelse, håndterer)) return
        hendelse.error(errortekst)
    }

    private fun <Hendelse : IAktivitetslogg> håndter(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Unit) {
        looper { håndterer(it, hendelse) }
    }

    private fun <Hendelse : IAktivitetslogg> fordelRevurdertUtbetaling(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Unit) {
        looper { håndterer(it, hendelse) }
    }

    private fun <Hendelse : IAktivitetslogg> énHarHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean): Boolean {
        var håndtert = false
        looper { håndtert = håndtert || håndterer(it, hendelse) }
        return håndtert
    }

    private fun <Hendelse : IAktivitetslogg> noenHarHåndtert(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Boolean): Boolean {
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

    internal fun fyllUtPeriodeMedForventedeDager(hendelse: PersonHendelse, periode: Periode) {
        sykdomshistorikk.fyllUtPeriodeMedForventedeDager(hendelse, periode)
    }

    internal fun harAktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harAktivtArbeidsforhold(skjæringstidspunkt)

    internal fun grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) =
        inntektshistorikk.sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt)

    internal fun harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        !harAktivtArbeidsforhold(skjæringstidspunkt) && vedtaksperioder.any { it.harUferdigFørstegangsbehandling(skjæringstidspunkt) }

    internal fun harGrunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        inntektshistorikk.harGrunnlagForSykepengegrunnlag(skjæringstidspunkt) || vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).harInntekt()

    internal fun harGrunnlagForSykepengegrunnlagEllerSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
        inntektshistorikk.harGrunnlagForSykepengegrunnlagEllerSammenligningsgrunnlag(skjæringstidspunkt) || vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).harInntekt()

    internal fun toMap(): Map<String, Any?> = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "id" to id,
        "beregnetUtbetalingstidslinjer" to beregnetUtbetalingstidslinjer.map { Utbetalingstidslinjeberegning.save(it) },
        "refusjonOpphører" to refusjonOpphører
    )

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
                feriepengeutbetalinger: List<Feriepengeutbetaling>,
                refusjonOpphører: List<LocalDate?>,
                arbeidsforholdhistorikk: Arbeidsforholdhistorikk
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
                feriepengeutbetalinger.toMutableList(),
                refusjonOpphører.toMutableList(),
                arbeidsforholdhistorikk
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
