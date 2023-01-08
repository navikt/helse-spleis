package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingpåminnelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.harAvsluttedePerioder
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.håndterInntektsmeldingReplay
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.HAR_PÅGÅENDE_UTBETALINGER
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_BEHANDLET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_REVURDERT
import no.nav.helse.person.Vedtaksperiode.Companion.KLAR_TIL_BEHANDLING
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.PÅGÅENDE_REVURDERING
import no.nav.helse.person.Vedtaksperiode.Companion.SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG
import no.nav.helse.person.Vedtaksperiode.Companion.TRENGER_REFUSJONSOPPLYSNINGER
import no.nav.helse.person.Vedtaksperiode.Companion.feiletRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.lagRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.skjæringstidspunktperiode
import no.nav.helse.person.Vedtaksperiode.Companion.validerYtelser
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.SkattComposite
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling.Companion.gjelderFeriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harNærliggendeUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.tillaterOpprettelseAvUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.utbetaltTidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.finn
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Inntekter
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning

internal class Arbeidsgiver private constructor(
    private val person: Person,
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntektshistorikk: Inntektshistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val sykmeldingsperioder: Sykmeldingsperioder,
    private val vedtaksperioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<ForkastetVedtaksperiode>,
    private val utbetalinger: MutableList<Utbetaling>,
    private val beregnetUtbetalingstidslinjer: MutableList<Utbetalingstidslinjeberegning>,
    private val feriepengeutbetalinger: MutableList<Feriepengeutbetaling>,
    private val refusjonshistorikk: Refusjonshistorikk,
    private val arbeidsforholdhistorikk: Arbeidsforholdhistorikk,
    private val inntektsmeldingInfo: InntektsmeldingInfoHistorikk,
    private val jurist: MaskinellJurist
) : Aktivitetskontekst, UtbetalingObserver {
    internal constructor(person: Person, organisasjonsnummer: String, jurist: MaskinellJurist) : this(
        person = person,
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntektshistorikk = Inntektshistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        sykmeldingsperioder = Sykmeldingsperioder(),
        vedtaksperioder = mutableListOf(),
        forkastede = mutableListOf(),
        utbetalinger = mutableListOf(),
        beregnetUtbetalingstidslinjer = mutableListOf(),
        feriepengeutbetalinger = mutableListOf(),
        refusjonshistorikk = Refusjonshistorikk(),
        arbeidsforholdhistorikk = Arbeidsforholdhistorikk(),
        inntektsmeldingInfo = InntektsmeldingInfoHistorikk(),
        jurist.medOrganisasjonsnummer(organisasjonsnummer)
    )

    init {
        utbetalinger.forEach { it.registrer(this) }
    }

    internal companion object {
        internal fun List<Arbeidsgiver>.finn(orgnr: String) = find { it.organisasjonsnummer() == orgnr }

        internal fun List<Arbeidsgiver>.relevanteArbeidsgivere(vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?) =
           filter { arbeidsgiver ->
               vilkårsgrunnlag?.erRelevant(arbeidsgiver.organisasjonsnummer) == true
                       || arbeidsgiver.vedtaksperioder.nåværendeVedtaksperiode(KLAR_TIL_BEHANDLING) != null
           }.map { it.organisasjonsnummer }

        internal fun List<Arbeidsgiver>.startRevurdering(hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            forEach { arbeidsgiver ->
                arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                    vedtaksperiode.startRevurdering(hendelse, revurdering)
                }
            }
        }

        internal fun List<Arbeidsgiver>.nekterOpprettelseAvPeriode(vedtaksperiode: Vedtaksperiode, søknad: Søknad): Boolean {
            return harPeriodeSomNekterOpprettelseAvNyPeriode(vedtaksperiode, søknad) || harForkastetVedtaksperiodeSomBlokkerBehandling(søknad)
        }

        private fun Iterable<Arbeidsgiver>.harForkastetVedtaksperiodeSomBlokkerBehandling(hendelse: SykdomstidslinjeHendelse) =
            any { it.harForkastetVedtaksperiodeSomBlokkerBehandling(hendelse) }

        private fun List<Arbeidsgiver>.harPeriodeSomNekterOpprettelseAvNyPeriode(nyVedtaksperiode: Vedtaksperiode, søknad: Søknad) =
            any { arbeidsgiver ->
                arbeidsgiver.vedtaksperioder.any { vedtaksperiode ->
                    vedtaksperiode.nekterOpprettelseAvNyPeriode(nyVedtaksperiode, søknad)
                    søknad.harFunksjonelleFeilEllerVerre()
                }
            }

        internal fun List<Arbeidsgiver>.håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold) =
            any { it.håndter(overstyrArbeidsforhold) }

        internal fun List<Arbeidsgiver>.håndterOverstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger) =
            any { it.håndter(overstyrArbeidsgiveropplysninger) }

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun Iterable<Arbeidsgiver>.vedtaksperioder(filter: VedtaksperiodeFilter) =
            map { it.vedtaksperioder.filter(filter) }.flatten()


        internal fun List<Arbeidsgiver>.lagRevurdering(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
            hendelse: ArbeidstakerHendelse
        ) {
            flatMap { it.vedtaksperioder }.lagRevurdering(vedtaksperiode, arbeidsgiverUtbetalinger, hendelse)
        }

        internal fun List<Arbeidsgiver>.beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
            mapNotNull { arbeidsgiver -> arbeidsgiver.beregnSykepengegrunnlag(skjæringstidspunkt) }

        internal fun List<Arbeidsgiver>.inntekterForSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { arbeidsgiver ->
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.rapportertInntekt(skjæringstidspunkt, arbeidsgiver.arbeidsforholdhistorikk)
                when {
                    inntektsopplysning != null -> ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(arbeidsgiver.organisasjonsnummer, inntektsopplysning)
                    else -> null
                }
            }

        internal fun skjæringstidspunkt(arbeidsgivere: List<Arbeidsgiver>, periode: Periode, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkt(periode, arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))

        internal fun skjæringstidspunkter(arbeidsgivere: List<Arbeidsgiver>, infotrygdhistorikk: Infotrygdhistorikk) =
            infotrygdhistorikk.skjæringstidspunkter(arbeidsgivere.map(Arbeidsgiver::sykdomstidslinje))

        internal fun Iterable<Arbeidsgiver>.validerVilkårsgrunnlag(
            aktivitetslogg: IAktivitetslogg,
            vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
            skjæringstidspunkt: LocalDate,
            erForlengelse: Boolean
        ) {
            val relevanteArbeidsgivere = medSkjæringstidspunkt(skjæringstidspunkt).map { it.organisasjonsnummer }
            vilkårsgrunnlag.valider(aktivitetslogg, relevanteArbeidsgivere, erForlengelse)
        }

        internal fun Iterable<Arbeidsgiver>.ghostPeriode(
            skjæringstidspunkt: LocalDate,
            vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
            arbeidsgiver: Arbeidsgiver
        ): GhostPeriode? {
            val perioder = flatMap { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).map { it.periode() } }
            if (perioder.isEmpty()) return null
            return vilkårsgrunnlagHistorikk.ghostPeriode(skjæringstidspunkt, arbeidsgiver.organisasjonsnummer, perioder.reduce(Periode::plus))
        }

        internal fun Iterable<Arbeidsgiver>.beregnFeriepengerForAlleArbeidsgivere(
            aktørId: String,
            personidentifikator: Personidentifikator,
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
        ) {
            forEach { it.utbetalFeriepenger(
                aktørId,
                personidentifikator,
                feriepengeberegner,
                utbetalingshistorikkForFeriepenger
            ) }
        }

        private fun Iterable<Arbeidsgiver>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) = this
            .filter { arbeidsgiver -> arbeidsgiver.vedtaksperioder.any(SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG(skjæringstidspunkt)) }

        private fun Iterable<Arbeidsgiver>.somTrengerRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode) = this
            .filter { arbeidsgiver -> arbeidsgiver.vedtaksperioder.any(TRENGER_REFUSJONSOPPLYSNINGER(skjæringstidspunkt, periode)) }
        internal fun Iterable<Arbeidsgiver>.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt: LocalDate) = this
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .any { arbeidsgiver -> arbeidsgiver.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt) }

        /* krever inntekt for alle vedtaksperioder som deler skjæringstidspunkt,
            men tillater at det ikke er inntekt for perioder innenfor arbeidsgiverperioden/uten utbetaling
         */
        internal fun Iterable<Arbeidsgiver>.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt: LocalDate) = this
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .all { arbeidsgiver -> arbeidsgiver.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt) }

        internal fun Iterable<Arbeidsgiver>.harNødvendigRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode, hendelse: IAktivitetslogg) = this
            .somTrengerRefusjonsopplysninger(skjæringstidspunkt, periode)
            .all { arbeidsgiver -> arbeidsgiver.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, periode, hendelse) }

        internal fun Iterable<Arbeidsgiver>.harNødvendigOpplysningerFraArbeidsgiver(periode: Periode) = this
            .flatMap { it.vedtaksperioder }
            .filter { it.periode().overlapperMed(periode) }
            .all { vedtaksperiode -> vedtaksperiode.harNødvendigOpplysningerFraArbeidsgiver() }
        internal fun Iterable<Arbeidsgiver>.trengerSøknadISammeMåned(skjæringstidspunkt: LocalDate) = this
            .filter { !it.harSykdomFor(skjæringstidspunkt) }
            .any { it.sykmeldingsperioder.harSykmeldingsperiodeI(YearMonth.from(skjæringstidspunkt)) }
        internal fun Iterable<Arbeidsgiver>.trengerSøknadFør(periode: Periode) = this
            .any { !it.sykmeldingsperioder.kanFortsetteBehandling(periode) }

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandling(aktivitetslogg: IAktivitetslogg) {
            if (nåværendeVedtaksperioder(HAR_PÅGÅENDE_UTBETALINGER).isNotEmpty()) return aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
            val periodeSomSkalGjenopptas = (pågåendeRevurderingsperiode().takeUnless { it.isEmpty() } ?: førsteIkkeFerdigBehandletPeriode()).minOrNull() ?: return
            periodeSomSkalGjenopptas.gjenopptaBehandling(aktivitetslogg, this)
        }

        private fun Iterable<Arbeidsgiver>.pågåendeRevurderingsperiode(): List<Vedtaksperiode> {
            return nåværendeVedtaksperioder(PÅGÅENDE_REVURDERING).takeUnless { it.isEmpty() } ?: nåværendeVedtaksperioder(IKKE_FERDIG_REVURDERT)
        }
        private fun Iterable<Arbeidsgiver>.førsteIkkeFerdigBehandletPeriode() = nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)

        internal fun Iterable<Arbeidsgiver>.slettUtgåtteSykmeldingsperioder(tom: LocalDate) = forEach {
            it.sykmeldingsperioder.fjern(tom.minusDays(1))
        }

        internal fun søppelbøtte(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: IAktivitetslogg,
            filter: VedtaksperiodeFilter
        ) {
            arbeidsgivere.forEach { it.søppelbøtte(hendelse, filter) }
        }

        internal fun List<Arbeidsgiver>.validerYtelserForSkjæringstidspunkt(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
            forEach { it.vedtaksperioder.validerYtelser(ytelser, skjæringstidspunkt, infotrygdhistorikk) }
        }

        internal fun List<Arbeidsgiver>.skjæringstidspunktperiode(skjæringstidspunkt: LocalDate) =
            flatMap { it.vedtaksperioder }.skjæringstidspunktperiode(skjæringstidspunkt)
    }

    /* hvorvidt arbeidsgiver ikke inngår i sykepengegrunnlaget som er på et vilkårsgrunnlag,
        for eksempel i saker hvor man var syk på én arbeidsgiver på skjæringstidspunktet, også blir man
        etterhvert syk fra ny arbeidsgiver (f.eks. jobb-bytte)
     */
    internal fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        harNødvendigInntektITidligereBeregnetSykepengegrunnlag(skjæringstidspunkt) == false

    internal fun harNødvendigInntektForVilkårsprøving(skjæringstidspunkt: LocalDate) : Boolean {
        return harNødvendigInntektITidligereBeregnetSykepengegrunnlag(skjæringstidspunkt) ?: kanBeregneSykepengegrunnlag(skjæringstidspunkt)
    }

    internal fun harNødvendigRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode, hendelse: IAktivitetslogg) : Boolean {
        val arbeidsgiverperiode = arbeidsgiverperiode(periode, NullObserver) ?: return false
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(skjæringstidspunkt)
        val refusjonsopplysninger = when (vilkårsgrunnlag) {
            null -> refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt)
            else -> vilkårsgrunnlag.refusjonsopplysninger(organisasjonsnummer)
        }
        return Arbeidsgiverperiode.harNødvendigeRefusjonsopplysninger(skjæringstidspunkt, periode, refusjonsopplysninger, arbeidsgiverperiode, hendelse, organisasjonsnummer)
    }

    private fun harNødvendigInntektITidligereBeregnetSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        person.vilkårsgrunnlagFor(skjæringstidspunkt)?.harNødvendigInntektForVilkårsprøving(organisasjonsnummer)

    internal fun kanBeregneSykepengegrunnlag(skjæringstidspunkt: LocalDate) = beregnSykepengegrunnlag(skjæringstidspunkt) != null

    private fun beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate) : ArbeidsgiverInntektsopplysning? {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        val inntektsopplysning = inntektshistorikk.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag, arbeidsforholdhistorikk)
        return when {
            inntektsopplysning != null -> ArbeidsgiverInntektsopplysning(organisasjonsnummer, inntektsopplysning, refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt))
            else -> null
        }
    }

    internal fun feiletRevurdering(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.feiletRevurdering(vedtaksperiode)

    internal fun gjenopptaRevurdering(første: Vedtaksperiode, hendelse: IAktivitetslogg) {
        håndter(hendelse) { gjenopptaRevurdering(hendelse, første) }
        vedtaksperioder.last(IKKE_FERDIG_REVURDERT).igangsettRevurdering(hendelse)
    }

    internal fun ferdigstillRevurdering(hendelse: IAktivitetslogg, ferdigstiller: Vedtaksperiode) {
        håndter(hendelse) { ferdigstillRevurdering(hendelse, ferdigstiller) }
    }

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this, id, organisasjonsnummer)
        inntektshistorikk.accept(visitor)
        sykdomshistorikk.accept(visitor)
        sykmeldingsperioder.accept(visitor)
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
        refusjonshistorikk.accept(visitor)
        arbeidsforholdhistorikk.accept(visitor)
        inntektsmeldingInfo.accept(visitor)
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
        ).also { nyUtbetaling(aktivitetslogg, it) }
    }

    internal fun lagRevurdering(
        vedtaksperiode: Vedtaksperiode,
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
        ).also {
            nyUtbetaling(aktivitetslogg, it)
            fordelRevurdertUtbetaling(aktivitetslogg.barn().also { logg -> logg.kontekst(person) }, it, vedtaksperiode)
        }
    }

    private fun nyUtbetaling(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling) {
        utbetalinger.lastOrNull()?.forkast(aktivitetslogg)
        check(utbetalinger.tillaterOpprettelseAvUtbetaling(utbetaling)) { "Har laget en overlappende utbetaling" }
        utbetalinger.add(utbetaling)
        utbetaling.registrer(this)
        utbetaling.opprett(aktivitetslogg)
    }

    internal fun utbetalFeriepenger(
        aktørId: String,
        personidentifikator: Personidentifikator,
        feriepengeberegner: Feriepengeberegner,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
    ) {
        utbetalingshistorikkForFeriepenger.kontekst(this)

        val feriepengeutbetaling = Feriepengeutbetaling.Builder(
            aktørId,
            personidentifikator,
            organisasjonsnummer,
            feriepengeberegner,
            utbetalingshistorikkForFeriepenger,
            feriepengeutbetalinger
        ).build()

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            feriepengeutbetalinger.add(feriepengeutbetaling)
            feriepengeutbetaling.overfør(utbetalingshistorikkForFeriepenger)
        }
    }

    internal fun nåværendeTidslinje() =
        beregnetUtbetalingstidslinjer.lastOrNull()?.utbetalingstidslinje() ?: throw IllegalStateException("mangler utbetalinger")

    internal fun lagreUtbetalingstidslinjeberegning(
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        val sykdomshistorikkId = sykdomshistorikk.nyesteId()
        val inntektshistorikkId = if (inntektshistorikk.isNotEmpty()) {
            inntektshistorikk.nyesteId()
        } else {
            require(!utbetalingstidslinje.harUtbetalinger()) { "Arbeidsgiver har utbetaling, men vi finner ikke inntektshistorikk" }
            Inntektshistorikk.NULLUUID
        }
        val vilkårsgrunnlagHistorikkId = vilkårsgrunnlagHistorikk.sisteId()
        beregnetUtbetalingstidslinjer.add(
            Utbetalingstidslinjeberegning(sykdomshistorikkId, inntektshistorikkId, vilkårsgrunnlagHistorikkId, organisasjonsnummer, utbetalingstidslinje)
        )
    }

    internal fun håndter(sykmelding: Sykmelding) {
        håndter(sykmelding, Vedtaksperiode::håndter)
        sykmeldingsperioder.lagre(sykmelding)
    }

    private fun harForkastetVedtaksperiodeSomBlokkerBehandling(hendelse: SykdomstidslinjeHendelse): Boolean {
        ForkastetVedtaksperiode.forlengerForkastet(forkastede, hendelse) ||
                ForkastetVedtaksperiode.harNyereForkastetPeriode(forkastede, hendelse) ||
                (organisasjonsnummer() == hendelse.organisasjonsnummer() && ForkastetVedtaksperiode.harKortGapTilForkastet(forkastede, hendelse))
        return hendelse.harFunksjonelleFeilEllerVerre()
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        søknad.slettSykmeldingsperioderSomDekkes(sykmeldingsperioder, person)
        opprettVedtaksperiodeOgHåndter(søknad)
    }

    private fun opprettVedtaksperiodeOgHåndter(søknad: Søknad) {
        if (noenHarHåndtert(søknad, Vedtaksperiode::håndter)) {
            if (!søknad.harFunksjonelleFeilEllerVerre()) return person.emitUtsettOppgaveEvent(søknad)
        }
        val vedtaksperiode = søknad.lagVedtaksperiode(person, this, jurist)
        if (søknad.harFunksjonelleFeilEllerVerre() || person.nekterOpprettelseAvPeriode(vedtaksperiode, søknad)) {
            registrerForkastetVedtaksperiode(vedtaksperiode, søknad)
            vedtaksperiode.trengerInntektsmeldingReplay()
            return
        }
        registrerNyVedtaksperiode(vedtaksperiode)
        vedtaksperiode.håndter(søknad)
        if (søknad.harFunksjonelleFeilEllerVerre()) {
            søknad.info("Forsøkte å opprette en ny vedtaksperiode, men den ble forkastet før den rakk å spørre om inntektsmeldingReplay. " +
                    "Ber om inntektsmeldingReplay så vi kan opprette gosys-oppgaver for inntektsmeldinger som ville ha truffet denne vedtaksperioden")
            vedtaksperiode.trengerInntektsmeldingReplay()
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID? = null) {
        inntektsmelding.kontekst(this)
        if (vedtaksperiodeId != null) inntektsmelding.info("Replayer inntektsmelding til påfølgende perioder som overlapper.")
        if (Toggle.HåndterInntektsmeldingOppdelt.enabled) return håndterInntektsmeldingOppdelt(inntektsmelding, vedtaksperiodeId)
        håndterInntektsmelding(inntektsmelding, vedtaksperiodeId)
    }

    private fun håndterInntektsmelding(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID?) {
        if (noenHarHåndtert(inntektsmelding) { håndter(inntektsmelding, vedtaksperiodeId, vedtaksperioder.toList()) }) return
        inntektsmeldingIkkeHåndtert(inntektsmelding, vedtaksperiodeId)
    }

    private fun håndterInntektsmeldingOppdelt(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID?) {
        val dager = inntektsmelding.dager
        val noenHarHåndtertDager = noenHarHåndtert(inntektsmelding) {
            håndter(dager).also { håndtert -> if (håndtert) {
                // Vedtaksperioden har potensielt blitt strukket tilbake og håndtert sin nye periode.
                // så det som gjenstår å håndtere nå er dager som ikke gjenspeiles i noen vedtaksperioder.
                dager.håndterGjenståendeFør(periode(), this@Arbeidsgiver)
            }}
        }

        val noenKommerTilÅHåndtereInntektOgRefusjon = inntektsmelding.inntektOgRefusjon.let { inntektOgRefusjon ->
            noenHarHåndtert(inntektsmelding) {
                inntektOgRefusjon.skalHåndteresAv(periode())
            }
        }

        if (noenHarHåndtertDager || noenKommerTilÅHåndtereInntektOgRefusjon) {
            // Noen av dagene er håndtert, men ikke nødvendigvis alle. For å beholde dagens oppførsel
            // må vi håndtere de gjenstående dagene også.
            dager.håndterGjenstående(this@Arbeidsgiver)
        }

        val inntektOgRefusjon = inntektsmelding.inntektOgRefusjon
        val enHarHåndtertInntektOgRefusjon = énHarHåndtert(inntektsmelding) {
            håndter(inntektOgRefusjon)
        }

        if (noenHarHåndtertDager || enHarHåndtertInntektOgRefusjon) return
        inntektsmeldingIkkeHåndtert(inntektsmelding, vedtaksperiodeId)
    }

    private fun inntektsmeldingIkkeHåndtert(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID?) {
        if (vedtaksperiodeId != null) {
            if (!forkastede.håndterInntektsmeldingReplay(person, inntektsmelding, vedtaksperiodeId)) {
                inntektsmelding.info("Vedtaksperiode overlapper ikke med replayet Inntektsmelding")
            }
            return
        }
        if (sykmeldingsperioder.blirTruffetAv(inntektsmelding)) {
            person.emitUtsettOppgaveEvent(inntektsmelding)
        }
        if (ForkastetVedtaksperiode.sjekkOmOverlapperMedForkastet(forkastede, inntektsmelding)) {
            person.opprettOppgave(
                PersonObserver.OpprettOppgaveEvent(
                    hendelser = setOf(inntektsmelding.meldingsreferanseId()),
                )
            )
            inntektsmelding.info("Forkastet vedtaksperiode overlapper med uforventet inntektsmelding")
        } else
            inntektsmelding.info("Ingen forkastede vedtaksperioder overlapper med uforventet inntektsmelding")
    }

    internal fun håndter(inntektsmelding: InntektsmeldingReplay) {
        inntektsmelding.fortsettÅBehandle(this)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        utbetalingshistorikk.kontekst(this)
        håndter(utbetalingshistorikk) { håndter(utbetalingshistorikk, infotrygdhistorikk) }
    }

    internal fun håndter(
        ytelser: Ytelser,
        infotrygdhistorikk: Infotrygdhistorikk,
        arbeidsgiverUtbetalinger: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
    ) {
        ytelser.kontekst(this)
        håndter(ytelser) { håndter(ytelser, infotrygdhistorikk, arbeidsgiverUtbetalinger) }
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        utbetalingsgodkjenning.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetalingsgodkjenning) }
        håndter(utbetalingsgodkjenning, Vedtaksperiode::håndter)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.kontekst(this)
        håndter(vilkårsgrunnlag, Vedtaksperiode::håndter)
    }

    internal fun håndter(simulering: Simulering) {
        simulering.kontekst(this)
        utbetalinger.forEach { it.håndter(simulering) }
        håndter(simulering, Vedtaksperiode::håndter)
    }

    internal fun håndter(utbetaling: UtbetalingOverført) {
        utbetaling.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetaling) }
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse) {
        utbetalingHendelse.kontekst(this)
        if (feriepengeutbetalinger.gjelderFeriepengeutbetaling(utbetalingHendelse)) return håndterFeriepengeUtbetaling(utbetalingHendelse)
        håndterUtbetaling(utbetalingHendelse)
    }

    private fun håndterFeriepengeUtbetaling(utbetalingHendelse: UtbetalingHendelse) {
        feriepengeutbetalinger.forEach { it.håndter(utbetalingHendelse, organisasjonsnummer, person) }
    }

    private fun håndterUtbetaling(utbetaling: UtbetalingHendelse) {
        utbetalinger.forEach { it.håndter(utbetaling) }
        håndter(utbetaling, Vedtaksperiode::håndter)
    }

    internal fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        hendelse.info("Håndterer annullering")

        val sisteUtbetalte = Utbetaling.finnUtbetalingForAnnullering(utbetalinger, hendelse) ?: return
        val annullering = sisteUtbetalte.annuller(hendelse) ?: return
        nyUtbetaling(hendelse, annullering)
        annullering.håndter(hendelse)
        håndter(hendelse) { håndter(it, annullering) }
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse) {
        påminnelse.kontekst(this)
        utbetalinger.forEach { it.håndter(påminnelse) }
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        påminnelse.kontekst(this)
        return énHarHåndtert(påminnelse, Vedtaksperiode::håndter)
    }

    internal fun håndter(arbeidsgivere: List<Arbeidsgiver>, hendelse: Grunnbeløpsregulering, vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        hendelse.kontekst(this)
        hendelse.info("Håndterer etterutbetaling")

        val sisteUtbetalte = Utbetaling.finnUtbetalingForJustering(
            utbetalinger = utbetalinger,
            hendelse = hendelse
        ) ?: return hendelse.info("Fant ingen utbetalinger å etterutbetale")

        val periode = LocalDate.of(2020, 5, 1).minusMonths(18) til LocalDate.now()

        val reberegnetTidslinje = reberegnUtbetalte(hendelse, arbeidsgivere, periode, vilkårsgrunnlagHistorikk)

        val etterutbetaling = sisteUtbetalte.etterutbetale(hendelse, reberegnetTidslinje)
            ?: return hendelse.info("Utbetalingen for $organisasjonsnummer for perioden $sisteUtbetalte er ikke blitt endret. Grunnbeløpsregulering gjennomføres ikke.")

        hendelse.info("Etterutbetaler for $organisasjonsnummer for perioden $sisteUtbetalte")
        nyUtbetaling(hendelse, etterutbetaling)
        etterutbetaling.håndter(hendelse)
    }

    private fun reberegnUtbetalte(
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        periode: Periode,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ): Utbetalingstidslinje {
        val arbeidsgivertidslinjer = arbeidsgivere
            .map { it to it.utbetalinger.utbetaltTidslinje() }
            .filter { it.second.isNotEmpty() }
            .toMap()

        MaksimumUtbetalingFilter().betal(arbeidsgivertidslinjer.values.toList(), periode, aktivitetslogg, jurist)

        arbeidsgivertidslinjer.forEach { (arbeidsgiver, reberegnetUtbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, reberegnetUtbetalingstidslinje, vilkårsgrunnlagHistorikk)
        }

        return nåværendeTidslinje()
    }

    override fun utbetalingUtbetalt(
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
        ident: String,
    ) {
        val builder = UtbetalingsdagerBuilder(sykdomshistorikk.sykdomstidslinje())
        utbetalingstidslinje.accept(builder)
        person.utbetalingUtbetalt(
            PersonObserver.UtbetalingUtbetaltEvent(
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = id,
                type = type.name,
                korrelasjonsId = korrelasjonsId,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                stønadsdager = stønadsdager,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toHendelseMap(),
                personOppdrag = personOppdrag.toHendelseMap(),
                utbetalingsdager = builder.result(),
                vedtaksperiodeIder = vedtaksperioder.iderMedUtbetaling(id) + forkastede.iderMedUtbetaling(id),
                ident = ident
            )
        )
    }

    override fun utbetalingUtenUtbetaling(
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        personOppdrag: Oppdrag,
        ident: String,
        arbeidsgiverOppdrag: Oppdrag,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
        epost: String,
    ) {
        val builder = UtbetalingsdagerBuilder(sykdomshistorikk.sykdomstidslinje())
        utbetalingstidslinje.accept(builder)
        person.utbetalingUtenUtbetaling(
            PersonObserver.UtbetalingUtbetaltEvent(
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = id,
                type = type.name,
                fom = periode.start,
                tom = periode.endInclusive,
                maksdato = maksdato,
                forbrukteSykedager = forbrukteSykedager,
                gjenståendeSykedager = gjenståendeSykedager,
                stønadsdager = stønadsdager,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling,
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toHendelseMap(),
                personOppdrag = personOppdrag.toHendelseMap(),
                utbetalingsdager = builder.result(),
                vedtaksperiodeIder = vedtaksperioder.iderMedUtbetaling(id) + forkastede.iderMedUtbetaling(id),
                ident = ident,
                korrelasjonsId = korrelasjonsId
            )
        )
    }

    override fun utbetalingEndret(
        id: UUID,
        type: Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetaling.Tilstand,
        nesteTilstand: Utbetaling.Tilstand,
        korrelasjonsId: UUID
    ) {
        person.utbetalingEndret(
            PersonObserver.UtbetalingEndretEvent(
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = id,
                type = type.name,
                forrigeStatus = Utbetalingstatus.fraTilstand(forrigeTilstand).name,
                gjeldendeStatus = Utbetalingstatus.fraTilstand(nesteTilstand).name,
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toHendelseMap(),
                personOppdrag = personOppdrag.toHendelseMap(),
                korrelasjonsId = korrelasjonsId
            )
        )
    }

    override fun nyVedtaksperiodeUtbetaling(utbetalingId: UUID, vedtaksperiodeId: UUID) {
        person.nyVedtaksperiodeUtbetaling(organisasjonsnummer, utbetalingId, vedtaksperiodeId)
    }

    override fun utbetalingAnnullert(
        id: UUID,
        korrelasjonsId: UUID,
        periode: Periode,
        personFagsystemId: String?,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String,
        arbeidsgiverFagsystemId: String?
    ) {
        person.annullert(
            PersonObserver.UtbetalingAnnullertEvent(
                organisasjonsnummer = organisasjonsnummer,
                korrelasjonsId = korrelasjonsId,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
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

    private fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold): Boolean {
        overstyrArbeidsforhold.kontekst(this)
        return énHarHåndtert(overstyrArbeidsforhold) { håndter(it, vedtaksperioder.toList()) }
    }

    private fun håndter(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger): Boolean {
        overstyrArbeidsgiveropplysninger.kontekst(this)
        return énHarHåndtert(overstyrArbeidsgiveropplysninger) { håndter(it, vedtaksperioder.toList()) }
    }

    internal fun oppdaterSykdom(hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
        val sykdomstidslinje = sykdomshistorikk.håndter(hendelse)
        person.sykdomshistorikkEndret(hendelse)
        return sykdomstidslinje
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!sykdomshistorikk.harSykdom()) return Sykdomstidslinje()
        return sykdomshistorikk.sykdomstidslinje()
    }

    internal fun arbeidsgiverperiode(periode: Periode, subsumsjonObserver: SubsumsjonObserver): Arbeidsgiverperiode? {
        val arbeidsgiverperioder = person.arbeidsgiverperiodeFor(organisasjonsnummer, sykdomshistorikk.nyesteId()) ?:
            ForkastetVedtaksperiode.arbeidsgiverperiodeFor(
                person,
                sykdomshistorikk.nyesteId(),
                forkastede,
                organisasjonsnummer,
                sykdomstidslinje(),
                subsumsjonObserver
            )
        return arbeidsgiverperioder.finn(periode)
    }

    internal fun ghostPerioder(): List<GhostPeriode> = person.skjæringstidspunkterFraSpleis()
        .filter { skjæringstidspunkt -> vedtaksperioder.none(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)) }
        .mapNotNull { skjæringstidspunkt -> person.ghostPeriode(skjæringstidspunkt, this) }

    internal fun tidligsteDato(): LocalDate {
        return sykdomstidslinje().førsteDag()
    }

    /**
     * Finner alle vedtaksperioder som tilstøter vedtaksperioden
     * @param vedtaksperiode Perioden vi skal finne alle sammenhengende perioder for. Vi henter alle perioder som
     * tilstøter både foran og bak.
     */
    internal fun finnSammehengendeVedtaksperioder(vedtaksperiode: Vedtaksperiode): List<Vedtaksperiode> {
        val (perioderFør, perioderEtter) = vedtaksperioder.sorted().partition { it før vedtaksperiode }
        val sammenhengendePerioder = mutableListOf(vedtaksperiode)
        perioderFør.reversed().forEach {
            if (it.erVedtaksperiodeRettFør(sammenhengendePerioder.first()))
                sammenhengendePerioder.add(0, it)
        }
        perioderEtter.forEach {
            if (sammenhengendePerioder.last().erVedtaksperiodeRettFør(it))
                sammenhengendePerioder.add(it)
        }
        return sammenhengendePerioder
    }

    internal fun finnSammenhengendePeriode(skjæringstidspunkt: LocalDate) = vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt)

    internal fun finnTidligereInntektsmeldinginfo(skjæringstidspunkt: LocalDate) = inntektsmeldingInfo.finn(skjæringstidspunkt)

    internal fun addInntektsmelding(
        skjæringstidspunkt: LocalDate,
        inntektsmelding: Inntektsmelding,
        subsumsjonObserver: SubsumsjonObserver
    ): InntektsmeldingInfo {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        if (førsteFraværsdag != null) inntektsmelding.addInntekt(inntektshistorikk, førsteFraværsdag, subsumsjonObserver)
        inntektsmelding.cacheRefusjon(refusjonshistorikk)
        return inntektsmeldingInfo.opprett(skjæringstidspunkt, inntektsmelding)
    }

    internal fun lagreInntekter(inntekter: List<SkattComposite>) {
        inntektshistorikk.leggTil(inntekter)
    }

    private fun søppelbøtte(hendelse: IAktivitetslogg, filter: VedtaksperiodeFilter) {
        hendelse.kontekst(this)
        val perioder = vedtaksperioder
            .filter(filter)
            .filter { it.forkast(hendelse, utbetalinger) }
        vedtaksperioder.removeAll(perioder)
        forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it) })
        sykdomshistorikk.fjernDager(perioder.map { it.periode() })
    }

    private fun registrerNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperioder.sort()
    }

    private fun registrerForkastetVedtaksperiode(vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) {
        hendelse.info("Oppretter forkastet vedtaksperiode ettersom Søknad inneholder errors")
        vedtaksperiode.forkast(hendelse, utbetalinger)
        forkastede.add(ForkastetVedtaksperiode(vedtaksperiode))
    }

    internal fun finnVedtaksperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erVedtaksperiodeRettFør(vedtaksperiode)
        }

    internal fun finnVedtaksperiodeRettEtter(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            vedtaksperiode.erVedtaksperiodeRettFør(other)
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

    internal fun harNærliggendeUtbetaling(periode: Periode) =
        utbetalinger.harNærliggendeUtbetaling(periode)

    private fun fordelRevurdertUtbetaling(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling, other: Vedtaksperiode) {
        håndter(aktivitetslogg) { håndterRevurdertUtbetaling(utbetaling, aktivitetslogg, other) }
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

    internal fun harSpleisSykdom() = sykdomshistorikk.harSykdom()

    private fun harSykdomFor(skjæringstidspunkt: LocalDate) =
        vedtaksperioder.any(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))

    internal fun finnFørsteFraværsdag(skjæringstidspunkt: LocalDate): LocalDate? {
        val førstePeriodeMedUtbetaling = vedtaksperioder.firstOrNull(SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG(skjæringstidspunkt))
            ?: vedtaksperioder.firstOrNull(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            ?: return null
        return sykdomstidslinje().subset(førstePeriodeMedUtbetaling.periode().oppdaterFom(skjæringstidspunkt)).sisteSkjæringstidspunkt()
    }

    internal fun periodetype(periode: Periode): Periodetype {
        return arbeidsgiverperiode(periode, NullObserver)?.let { person.periodetype(organisasjonsnummer, it, periode, skjæringstidspunkt(periode)) } ?: Periodetype.FØRSTEGANGSBEHANDLING
    }

    internal fun erFørstegangsbehandling(periode: Periode) = periodetype(periode) == Periodetype.FØRSTEGANGSBEHANDLING
    private fun skjæringstidspunkt(periode: Periode) = person.skjæringstidspunkt(organisasjonsnummer, sykdomstidslinje(), periode)

    internal fun builder(
        regler: ArbeidsgiverRegler,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
        infotrygdhistorikk: Infotrygdhistorikk,
        subsumsjonObserver: SubsumsjonObserver,
        hendelse: IAktivitetslogg
    ): (Periode) -> Utbetalingstidslinje {
        val inntekter = Inntekter(
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            regler = regler,
            subsumsjonObserver = subsumsjonObserver,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperioder = vedtaksperioder
        )
        return { periode ->
            val sykdomstidslinje = sykdomstidslinje().fremTilOgMed(periode.endInclusive).takeUnless { it.count() == 0 }
            if (sykdomstidslinje == null) Utbetalingstidslinje()
            else {
                val builder = UtbetalingstidslinjeBuilder(inntekter, periode, hendelse)
                infotrygdhistorikk.buildUtbetalingstidslinje(organisasjonsnummer, sykdomstidslinje, builder, subsumsjonObserver)
                builder.result()
            }
        }
    }

    internal fun lagreArbeidsforhold(arbeidsforhold: List<Arbeidsforholdhistorikk.Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        arbeidsforholdhistorikk.lagre(arbeidsforhold, skjæringstidspunkt)
    }

    internal fun beregn(aktivitetslogg: IAktivitetslogg, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger, periode: Periode, perioder: Map<Periode, Pair<IAktivitetslogg, SubsumsjonObserver>>): Boolean {
        try {
            arbeidsgiverUtbetalinger.beregn(organisasjonsnummer, periode, perioder)
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(aktivitetslogg)
        }
        return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
    }

    private fun <Hendelse : IAktivitetslogg> håndter(hendelse: Hendelse, håndterer: Vedtaksperiode.(Hendelse) -> Unit) {
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

    internal fun fyllUtPeriodeMedForventedeDager(
        hendelse: PersonHendelse,
        skjæringstidspunkt: LocalDate,
        periode: Periode
    ) {
        sykdomshistorikk.fyllUtPeriodeMedForventedeDager(hendelse, periode.oppdaterFom(skjæringstidspunkt))
    }

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harRelevantArbeidsforhold(skjæringstidspunkt)

    internal fun harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        !harRelevantArbeidsforhold(skjæringstidspunkt) && harSykdomFor(skjæringstidspunkt)

    internal fun harFerdigstiltPeriode() = vedtaksperioder.any(ER_ELLER_HAR_VÆRT_AVSLUTTET) || forkastede.harAvsluttedePerioder()

    internal fun harSykmeldingsperiodeFør(dato: LocalDate) = sykmeldingsperioder.harSykmeldingsperiodeFør(dato)
    internal fun kanForkastes(vedtaksperiodeUtbetalinger: VedtaksperiodeUtbetalinger) =
        vedtaksperiodeUtbetalinger.kanForkastes(utbetalinger)

    fun vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode?): List<Vedtaksperiode> {
        if (arbeidsgiverperiode == null) return emptyList()
        return vedtaksperioder.filter {
            arbeidsgiverperiode.hørerTil(it.periode())
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
                sykmeldingsperioder: Sykmeldingsperioder,
                vedtaksperioder: MutableList<Vedtaksperiode>,
                forkastede: MutableList<ForkastetVedtaksperiode>,
                utbetalinger: List<Utbetaling>,
                beregnetUtbetalingstidslinjer: List<Utbetalingstidslinjeberegning>,
                feriepengeutbetalinger: List<Feriepengeutbetaling>,
                refusjonshistorikk: Refusjonshistorikk,
                arbeidsforholdhistorikk: Arbeidsforholdhistorikk,
                inntektsmeldingInfo: InntektsmeldingInfoHistorikk,
                jurist: MaskinellJurist
            ) = Arbeidsgiver(
                person,
                organisasjonsnummer,
                id,
                inntektshistorikk,
                sykdomshistorikk,
                sykmeldingsperioder,
                vedtaksperioder,
                forkastede,
                utbetalinger.toMutableList(),
                beregnetUtbetalingstidslinjer.toMutableList(),
                feriepengeutbetalinger.toMutableList(),
                refusjonshistorikk,
                arbeidsforholdhistorikk,
                inntektsmeldingInfo,
                jurist
            )
        }
    }
}
