package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.Toggle
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingsgrunnlag
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
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.MAKS_INNTEKT_GAP
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.harAvsluttedePerioder
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.håndterInntektsmeldingReplay
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Inntektshistorikk.IkkeRapportert
import no.nav.helse.person.Vedtaksperiode.AvventerArbeidsgivereRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikkRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.AVVENTER_GODKJENT_REVURDERING
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_BEHANDLET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_REVURDERT
import no.nav.helse.person.Vedtaksperiode.Companion.KLAR_TIL_BEHANDLING
import no.nav.helse.person.Vedtaksperiode.Companion.OVERLAPPER_ELLER_FORLENGER
import no.nav.helse.person.Vedtaksperiode.Companion.REVURDERING_IGANGSATT
import no.nav.helse.person.Vedtaksperiode.Companion.SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG
import no.nav.helse.person.Vedtaksperiode.Companion.avventerRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.harOverlappendeUtbetaltePerioder
import no.nav.helse.person.Vedtaksperiode.Companion.harUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.kanStarteRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.lagRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.periode
import no.nav.helse.person.Vedtaksperiode.Companion.senerePerioderPågående
import no.nav.helse.person.Vedtaksperiode.Companion.skjæringstidspunktperiode
import no.nav.helse.person.Vedtaksperiode.Companion.startRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.validerYtelser
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.filter.Utbetalingsfilter
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
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harNærliggendeUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.utbetaltTidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.finn
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.IUtbetalingstidslinjeBuilder
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
    internal val refusjonshistorikk: Refusjonshistorikk,
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

        internal fun List<Arbeidsgiver>.kanOverstyreTidslinje(hendelse: OverstyrTidslinje): Boolean {
            val overlappendePerioder = flatMap { it.overlappendePerioder(hendelse) }
            return when {
                overlappendePerioder.any(KLAR_TIL_BEHANDLING) -> overlappendePerioder.all(KLAR_TIL_BEHANDLING)
                overlappendePerioder.any(REVURDERING_IGANGSATT) -> overlappendePerioder.all(REVURDERING_IGANGSATT)
                else -> true
            }
        }

        private fun Iterable<Arbeidsgiver>.senerePerioderPågående(vedtaksperiode: Vedtaksperiode) =
            any { it.senerePerioderPågående(vedtaksperiode) }

        internal fun List<Arbeidsgiver>.startRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            associateWith { it.vedtaksperioder.toList() }.startRevurdering(vedtaksperiode, hendelse)
        }

        internal fun List<Arbeidsgiver>.kanStarteRevurdering(vedtaksperiode: Vedtaksperiode) =
            flatMap { it.vedtaksperioder }.kanStarteRevurdering(this, vedtaksperiode)

        internal fun List<Arbeidsgiver>.harPeriodeSomBlokkererOverstyring(skjæringstidspunkt: LocalDate) = any { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder
                .filter { vedtaksperiode -> vedtaksperiode.gjelder(skjæringstidspunkt) }
                .any { vedtaksperiode -> vedtaksperiode.blokkererOverstyring() }
        }

        internal fun List<Arbeidsgiver>.håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold): Boolean {
            forEach { arbeidsgiver ->
                if (arbeidsgiver.håndter(overstyrArbeidsforhold)) return true
            }
            return false
        }

        internal fun List<Arbeidsgiver>.håndterOverstyringAvGhostInntekt(overstyrInntekt: OverstyrInntekt): Boolean {
            forEach { arbeidsgiver ->
                if (arbeidsgiver.håndterOverstyringAvGhostInntekt(overstyrInntekt)) return true
            }
            return false
        }

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun Iterable<Arbeidsgiver>.vedtaksperioder(filter: VedtaksperiodeFilter) =
            map { it.vedtaksperioder.filter(filter) }.flatten()

        internal fun Iterable<Arbeidsgiver>.harOverlappendeEllerForlengerForkastetVedtaksperiode(hendelse: SykdomstidslinjeHendelse) =
            any { it.harOverlappendeEllerForlengerForkastetVedtaksperiode(hendelse) }

        internal fun List<Arbeidsgiver>.lagRevurdering(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
            hendelse: ArbeidstakerHendelse
        ) {
            flatMap { it.vedtaksperioder }.lagRevurdering(vedtaksperiode, arbeidsgiverUtbetalinger, hendelse)
        }

        internal fun List<Arbeidsgiver>.beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
            fold(emptyList<ArbeidsgiverInntektsopplysning>()) { inntektsopplysninger, arbeidsgiver ->
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.omregnetÅrsinntekt(
                    skjæringstidspunkt,
                    maxOf(skjæringstidspunkt, periodeStart),
                    arbeidsgiver.finnFørsteFraværsdag(skjæringstidspunkt)

                )
                if (inntektsopplysning == null || inntektsopplysning !is Inntektshistorikk.Infotrygd) inntektsopplysninger
                else inntektsopplysninger + ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, inntektsopplysning)
            }

        internal fun List<Arbeidsgiver>.beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver) =
            mapNotNull { arbeidsgiver ->
                val førsteFraværsdag = arbeidsgiver.finnFørsteFraværsdag(skjæringstidspunkt)
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag)
                inntektsopplysning?.subsumsjon(subsumsjonObserver, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer)
                when {
                    arbeidsgiver.harDeaktivertArbeidsforhold(skjæringstidspunkt) -> null
                    inntektsopplysning == null && arbeidsgiver.arbeidsforholdhistorikk.harIkkeDeaktivertArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP) -> {
                        ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt))
                    }
                    inntektsopplysning != null -> ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, inntektsopplysning)
                    else -> null
                }
            }

        internal fun List<Arbeidsgiver>.beregnOpptjening(skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Opptjening {
            val arbeidsforhold = map { it.organisasjonsnummer to it.arbeidsforholdhistorikk.arbeidsforhold(skjæringstidspunkt) }
                .filter { (_, arbeidsforhold) -> arbeidsforhold.isNotEmpty() }
                .map { Opptjening.ArbeidsgiverOpptjeningsgrunnlag(it.first, it.second) }
            return Opptjening.opptjening(arbeidsforhold, skjæringstidspunkt, subsumsjonObserver)
        }

        internal fun List<Arbeidsgiver>.inntekterForSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { arbeidsgiver ->
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.rapportertInntekt(skjæringstidspunkt)
                when {
                    inntektsopplysning == null && arbeidsgiver.arbeidsforholdhistorikk.harIkkeDeaktivertArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP) -> {
                        ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt))
                    }
                    inntektsopplysning != null -> ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, inntektsopplysning)
                    else -> null
                }
            }

        internal fun Iterable<Arbeidsgiver>.harVedtaksperiodeFor(skjæringstidspunkt: LocalDate) = any { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.gjelder(skjæringstidspunkt) }
        }

        internal fun Iterable<Arbeidsgiver>.harArbeidsgivereMedOverlappendeUtbetaltePerioder(orgnummer: String, periode: Periode) = this
            .filter { it.organisasjonsnummer != orgnummer }
            .any { it.vedtaksperioder.harOverlappendeUtbetaltePerioder(periode) }

        internal fun List<Arbeidsgiver>.deaktiverteArbeidsforhold(skjæringstidspunkt: LocalDate) =
            this.filter { it.arbeidsforholdhistorikk.harDeaktivertArbeidsforhold(skjæringstidspunkt) }

        internal fun kunOvergangFraInfotrygd(
            arbeidsgivere: Iterable<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode
        ) = Vedtaksperiode.kunOvergangFraInfotrygd(vedtaksperiode, arbeidsgivere.flatMap { it.vedtaksperioder })

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

        internal fun Iterable<Arbeidsgiver>.harUtbetaltPeriode(skjæringstidspunkt: LocalDate) =
            flatMap { it.vedtaksperioder }.medSkjæringstidspunkt(skjæringstidspunkt).harUtbetaling()

        internal fun Iterable<Arbeidsgiver>.validerVilkårsgrunnlag(
            aktivitetslogg: IAktivitetslogg,
            vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
            skjæringstidspunkt: LocalDate,
            erForlengelse: Boolean
        ) {
            val vedtaksperioder = flatMap { it.vedtaksperioder }.filter(SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG(skjæringstidspunkt))
            val relevanteArbeidsgivere = filter { arbeidsgiver -> arbeidsgiver.vedtaksperioder.any { vedtaksperiode -> vedtaksperiode in vedtaksperioder } }.distinct().map { it.organisasjonsnummer }
            vilkårsgrunnlag.valider(aktivitetslogg, relevanteArbeidsgivere, erForlengelse)
        }

        internal fun Iterable<Arbeidsgiver>.ghostPeriode(
            skjæringstidspunkt: LocalDate,
            vilkårsgrunnlagHistorikkInnslagId: UUID?,
            deaktivert: Boolean
        ): GhostPeriode? {
            val relevanteVedtaksperioder = flatMap { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt) }
            if (relevanteVedtaksperioder.isEmpty()) return null
            return GhostPeriode(
                fom = relevanteVedtaksperioder.minOf { it.periode().start },
                tom = relevanteVedtaksperioder.maxOf { it.periode().endInclusive },
                skjæringstidspunkt = skjæringstidspunkt,
                vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId,
                deaktivert = deaktivert
            )
        }

        internal fun Iterable<Arbeidsgiver>.beregnFeriepengerForAlleArbeidsgivere(
            aktørId: String,
            fødselsnummer: Fødselsnummer,
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
        ) {
            forEach { it.utbetalFeriepenger(
                aktørId,
                fødselsnummer,
                feriepengeberegner,
                utbetalingshistorikkForFeriepenger
            ) }
        }

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandling(gjenopptaBehandling: IAktivitetslogg) = forEach { arbeidsgiver ->
            arbeidsgiver.gjenopptaBehandling(gjenopptaBehandling)
        }

        /*
            sjekker at vi har inntekt for første fraværsdag for alle arbeidsgivere med sykdom for skjæringstidspunkt
         */
        internal fun Iterable<Arbeidsgiver>.harNødvendigInntekt(skjæringstidspunkt: LocalDate, start: LocalDate) =
                filter {
                    it.vedtaksperioder
                        .medSkjæringstidspunkt(skjæringstidspunkt)
                        .any(IKKE_FERDIG_BEHANDLET)
                }
                .all { it.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt, start) }

        internal fun Iterable<Arbeidsgiver>.trengerSøknadISammeMåned(skjæringstidspunkt: LocalDate) = this
            .filter { !it.harSykdomFor(skjæringstidspunkt) }
            .any { it.sykmeldingsperioder.harSykmeldingsperiodeI(YearMonth.from(skjæringstidspunkt)) }

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandlingNy(aktivitetslogg: IAktivitetslogg) {
            val førstePeriode = nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
                .sortedBy { it.periode().endInclusive }
                .firstOrNull() ?: return

            if (førstePeriode.trengerSøknadISammeMåned(this)) return
            if (!førstePeriode.kanGjenopptaBehandling(this)) return
            if (senerePerioderPågående(førstePeriode)) return

            if (all { it.sykmeldingsperioder.kanFortsetteBehandling(førstePeriode.periode()) }) {
                førstePeriode.gjenopptaBehandlingNy(aktivitetslogg)
            }
        }

        internal fun Iterable<Arbeidsgiver>.slettUtgåtteSykmeldingsperioder(tom: LocalDate) = forEach {
            it.sykmeldingsperioder.fjern(tom.minusDays(1))
        }

        internal fun søppelbøtte(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: IAktivitetslogg,
            filter: VedtaksperiodeFilter
        ) {
            arbeidsgivere.forEach { it.søppelbøtte(hendelse, filter, ForkastetÅrsak.IKKE_STØTTET) }
        }

        internal fun List<Arbeidsgiver>.validerYtelserForSkjæringstidspunkt(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
            forEach { it.vedtaksperioder.validerYtelser(ytelser, skjæringstidspunkt, infotrygdhistorikk) }
        }

        internal fun List<Arbeidsgiver>.skjæringstidspunktperiode(skjæringstidspunkt: LocalDate) =
            flatMap { it.vedtaksperioder }.skjæringstidspunktperiode(skjæringstidspunkt)
    }

    private fun gjenopptaBehandling(gjenopptaBehandling: IAktivitetslogg) {
        gjenopptaBehandling.kontekst(this)
        énHarHåndtert(gjenopptaBehandling, Vedtaksperiode::gjenopptaBehandling)
        Vedtaksperiode.gjenopptaBehandling(
            hendelse = gjenopptaBehandling,
            person = person,
            nåværendeTilstand = AvventerArbeidsgivereRevurdering,
            nesteTilstand = AvventerHistorikkRevurdering,
            filter = IKKE_FERDIG_REVURDERT
        )
    }

    internal fun lagUtbetaling(
        builder: Utbetaling.Builder,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        builder.arbeidsgiver(organisasjonsnummer, sykdomstidslinje(), vilkårsgrunnlagHistorikk, utbetalinger, refusjonshistorikk)
    }

    internal fun avventerRevurdering() = vedtaksperioder.avventerRevurdering()

    internal fun gjenopptaRevurdering(første: Vedtaksperiode, hendelse: IAktivitetslogg) {
        Vedtaksperiode.gjenopptaRevurdering(hendelse, vedtaksperioder, første, this)
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
        aktivitetslogg: IAktivitetslogg,
        fødselsnummer: String,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode,
        forrige: Utbetaling
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
        ).also { nyUtbetaling(aktivitetslogg, it) }
    }

    private fun nyUtbetaling(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling) {
        utbetalinger.add(utbetaling)
        utbetaling.registrer(this)
        utbetaling.opprett(aktivitetslogg)
    }

    internal fun utbetalFeriepenger(
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        feriepengeberegner: Feriepengeberegner,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
    ) {
        utbetalingshistorikkForFeriepenger.kontekst(this)

        val feriepengeutbetaling = Feriepengeutbetaling.Builder(
            aktørId,
            fødselsnummer,
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
        sykmelding.validerAtSykmeldingIkkeErForGammel()
        if (sykmelding.hasErrorsOrWorse()) return
        håndter(sykmelding, Vedtaksperiode::håndter)
        sykmeldingsperioder.lagre(sykmelding.periode())
    }

    private fun harOverlappendeEllerForlengerForkastetVedtaksperiode(hendelse: SykdomstidslinjeHendelse): Boolean {
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, hendelse)
        ForkastetVedtaksperiode.forlengerForkastet(forkastede, hendelse)
        return hendelse.hasErrorsOrWorse()
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        sykmeldingsperioder.fjern(søknad.periode().endInclusive)
        person.slettUtgåtteSykmeldingsperioder(søknad.periode().endInclusive)
        opprettVedtaksperiodeOgHåndter(søknad)
    }

    private fun opprettVedtaksperiodeOgHåndter(søknad: Søknad) {
        val vedtaksperiode = søknad.lagVedtaksperiode(person, this, jurist)
        if (person.harOverlappendeEllerForlengerForkastetVedtaksperiode(søknad)) {
            registrerForkastetVedtaksperiode(vedtaksperiode, søknad)
            person.søppelbøtte(søknad, OVERLAPPER_ELLER_FORLENGER(vedtaksperiode))
            return
        }
        if (noenHarHåndtert(søknad, Vedtaksperiode::håndter)) {
            if (søknad.hasErrorsOrWorse()) {
                person.sendOppgaveEvent(søknad)
                person.emitHendelseIkkeHåndtert(søknad)
            } else {
                person.emitUtsettOppgaveEvent(søknad)
                // TODO: person.emitHendelseHåndtert(søknad, liste av vedtaksperioder som har håndtert)
            }
            return
        }
        registrerNyVedtaksperiode(vedtaksperiode)
        håndter(søknad) { nyPeriodeMedNyFlyt(vedtaksperiode, søknad) }
        vedtaksperiode.håndter(søknad)
        if (søknad.hasErrorsOrWorse()) {
            søknad.info("Forsøkte å opprette en ny vedtaksperiode, men den ble forkastet før den rakk å spørre om inntektsmeldingReplay. " +
                    "Ber om inntektsmeldingReplay så vi kan opprette gosys-oppgaver for inntektsmeldinger som ville ha truffet denne vedtaksperioden")
            vedtaksperiode.trengerInntektsmeldingReplay()
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, vedtaksperiodeId: UUID? = null) {
        inntektsmelding.kontekst(this)
        inntektsmelding.cacheRefusjon(refusjonshistorikk)
        if (vedtaksperiodeId != null) inntektsmelding.info("Replayer inntektsmelding til påfølgende perioder som overlapper.")
        if (!noenHarHåndtert(inntektsmelding) { håndter(inntektsmelding, vedtaksperiodeId, vedtaksperioder.toList()) }) {
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
                    inntektsmelding,
                    PersonObserver.OpprettOppgaveEvent(
                        hendelser = setOf(inntektsmelding.meldingsreferanseId()),
                    )
                )
                inntektsmelding.info("Forkastet vedtaksperiode overlapper med uforventet inntektsmelding")
            } else
                inntektsmelding.info("Ingen forkastede vedtaksperioder overlapper med uforventet inntektsmelding")
        }
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

    internal fun håndter(utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        utbetalingsgrunnlag.kontekst(this)
        håndter(utbetalingsgrunnlag, Vedtaksperiode::håndter)
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
        feriepengeutbetalinger.forEach { it.håndter(utbetalingHendelse, person) }
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

    fun håndterRevurderingFeilet(event: IAktivitetslogg) {
        vedtaksperioder.forEach {
            it.håndterRevurderingFeilet(event)
        }
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
        hendelseskontekst: Hendelseskontekst,
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
            hendelseskontekst,
            PersonObserver.UtbetalingUtbetaltEvent(
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
        hendelseskontekst: Hendelseskontekst,
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
            hendelseskontekst,
            PersonObserver.UtbetalingUtbetaltEvent(
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
        hendelseskontekst: Hendelseskontekst,
        id: UUID,
        type: Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetaling.Tilstand,
        nesteTilstand: Utbetaling.Tilstand
    ) {
        person.utbetalingEndret(
            hendelseskontekst,
            PersonObserver.UtbetalingEndretEvent(
                utbetalingId = id,
                type = type.name,
                forrigeStatus = Utbetalingstatus.fraTilstand(forrigeTilstand).name,
                gjeldendeStatus = Utbetalingstatus.fraTilstand(nesteTilstand).name,
                arbeidsgiverOppdrag = arbeidsgiverOppdrag.toHendelseMap(),
                personOppdrag = personOppdrag.toHendelseMap(),
            )
        )
    }

    override fun utbetalingAnnullert(
        hendelseskontekst: Hendelseskontekst,
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
            hendelseskontekst = hendelseskontekst,
            PersonObserver.UtbetalingAnnullertEvent(
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

    internal fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)
        énHarHåndtert(hendelse, Vedtaksperiode::håndter)
    }

    internal fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold): Boolean {
        overstyrArbeidsforhold.kontekst(this)
        vedtaksperioder.forEach { vedtaksperiode ->
            if (vedtaksperiode.håndter(overstyrArbeidsforhold)) {
                return true
            }
        }
        return false
    }

    internal fun håndterOverstyringAvGhostInntekt(overstyrInntekt: OverstyrInntekt): Boolean {
        overstyrInntekt.kontekst(this)
        vedtaksperioder.forEach { vedtaksperiode ->
            if (vedtaksperiode.håndterOverstyringAvGhostInntekt(overstyrInntekt)) {
                return true
            }
        }
        return false
    }

    internal fun førstePeriodeTilRevurdering(hendelse: PersonHendelse) = vedtaksperioder
        .filter(AVVENTER_GODKJENT_REVURDERING)
        .minOrNull()
        ?: hendelse.severe("Fant ikke periode til revurdering, selv om vi kommer fra en periode til revurdering?!")

    internal fun oppdaterHistorikkRevurdering(hendelse: OverstyrTidslinje) {
        hendelse.info("Oppdaterer sykdomshistorikk med overstyrte dager")
        val overlappendePerioder = overlappendePerioder(hendelse)
        overlappendePerioder.forEach {
            // Vi har hatt en bug der vi opprettet nye elementer i sykdomshistorikken uten å kopiere låser. Derfor er låsene inkonsistente
            // og vi må i revurderingsøyemed sjekke før vi låser opp.
            if (sykdomshistorikk.sykdomstidslinje().erLåst(it.periode())) {
                låsOpp(it.periode())
            }
        }
        oppdaterSykdom(hendelse)
        overlappendePerioder.forEach { lås(it.periode()) }
    }

    internal fun oppdaterSykdom(hendelse: SykdomstidslinjeHendelse) = sykdomshistorikk.håndter(hendelse)

    private fun sykdomstidslinje(): Sykdomstidslinje {
        val sykdomstidslinje = if (sykdomshistorikk.harSykdom()) sykdomshistorikk.sykdomstidslinje() else Sykdomstidslinje()
        return Utbetaling.sykdomstidslinje(utbetalinger, sykdomstidslinje)
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
        .filter { skjæringstidspunkt -> vedtaksperioder.none { it.gjelder(skjæringstidspunkt) } }
        .filter(::erGhost)
        .mapNotNull { skjæringstidspunkt -> person.ghostPeriode(skjæringstidspunkt, arbeidsforholdhistorikk.harDeaktivertArbeidsforhold(skjæringstidspunkt)) }

    private fun erGhost(skjæringstidspunkt: LocalDate): Boolean {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        val inntektsopplysning = inntektshistorikk.omregnetÅrsinntekt(skjæringstidspunkt, førsteFraværsdag)
        val harArbeidsforholdNyereEnnToMåneder = arbeidsforholdhistorikk.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)
        return inntektsopplysning is Inntektshistorikk.SkattComposite || inntektsopplysning is Inntektshistorikk.Saksbehandler || harArbeidsforholdNyereEnnToMåneder
    }

    internal fun utbetalingstidslinje(infotrygdhistorikk: Infotrygdhistorikk) = infotrygdhistorikk.utbetalingstidslinje(organisasjonsnummer)

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

    internal fun harNødvendigInntektForVilkårsprøving(skjæringstidspunkt: LocalDate, periodeStart: LocalDate): Boolean {
        return inntektshistorikk.harNødvendigInntektForVilkårsprøving(
            skjæringstidspunkt,
            periodeStart,
            finnFørsteFraværsdag(skjæringstidspunkt),
            harSykdomFor(skjæringstidspunkt)
        )
    }

    internal fun addInntekt(inntektsmelding: Inntektsmelding, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver) {
        inntektsmelding.addInntekt(inntektshistorikk, skjæringstidspunkt, subsumsjonObserver)
    }

    internal fun finnTidligereInntektsmeldinginfo(skjæringstidspunkt: LocalDate) = inntektsmeldingInfo.finn(skjæringstidspunkt)

    internal fun addInntektsmelding(
        skjæringstidspunkt: LocalDate,
        inntektsmelding: Inntektsmelding,
        subsumsjonObserver: SubsumsjonObserver
    ): InntektsmeldingInfo {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        if (førsteFraværsdag != null) addInntekt(inntektsmelding, førsteFraværsdag, subsumsjonObserver)
        return inntektsmeldingInfo.opprett(skjæringstidspunkt, inntektsmelding)
    }

    internal fun addInntekt(hendelse: OverstyrInntekt) {
        hendelse.addInntekt(inntektshistorikk)
    }

    internal fun lagreOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, overstyring: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) {
        overstyring.lagre(skjæringstidspunkt, arbeidsforholdhistorikk)
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(inntektsopplysninger: List<Inntektsopplysning>, hendelseId: UUID) {
        Inntektsopplysning.lagreInntekter(inntektsopplysninger, inntektshistorikk, hendelseId)
    }

    internal fun lagreOmregnetÅrsinntekt(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
        if (harRelevantArbeidsforhold(skjæringstidspunkt)) {
            arbeidsgiverInntekt.lagreInntekter(inntektshistorikk, skjæringstidspunkt, hendelse.meldingsreferanseId())
        }
    }

    internal fun lagreRapporterteInntekter(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
        arbeidsgiverInntekt.lagreInntekter(inntektshistorikk, skjæringstidspunkt, hendelse.meldingsreferanseId())
    }

    private fun søppelbøtte(
        hendelse: IAktivitetslogg,
        filter: VedtaksperiodeFilter,
        årsak: ForkastetÅrsak
    ) {
        hendelse.kontekst(this)
        val perioder = vedtaksperioder
            .filter(filter)
            .filter { it.forkast(hendelse, utbetalinger) }
        vedtaksperioder.removeAll(perioder)
        forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it, årsak) })
        sykdomshistorikk.fjernDager(perioder.map { it.periode() })
    }

    internal fun startRevurderingForAlleBerørtePerioder(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode) {
        hendelse.kontekst(this)
        håndter(hendelse) { nyRevurderingFør(vedtaksperiode, hendelse) }
        if (hendelse.hasErrorsOrWorse()) {
            hendelse.info("Revurdering blokkeres, gjenopptar behandling")
            return person.gjenopptaBehandling(hendelse)
        }
    }

    private fun harDeaktivertArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harDeaktivertArbeidsforhold(skjæringstidspunkt)

    internal fun kanReberegnes(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.all { it.kanReberegne(vedtaksperiode) }

    internal fun overlappendePerioder(hendelse: SykdomstidslinjeHendelse) = vedtaksperioder.filter { hendelse.erRelevant(it.periode()) }

    private fun registrerNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperioder.sort()
    }

    private fun registrerForkastetVedtaksperiode(vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) {
        hendelse.info("Oppretter forkastet vedtaksperiode ettersom Søknad inneholder errors")
        vedtaksperiode.forkast(hendelse, utbetalinger)
        forkastede.add(ForkastetVedtaksperiode(vedtaksperiode, ForkastetÅrsak.IKKE_STØTTET))
    }

    internal fun finnVedtaksperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erVedtaksperiodeRettFør(vedtaksperiode)
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

    internal fun senerePerioderPågående(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.senerePerioderPågående(vedtaksperiode)

    internal fun harNærliggendeUtbetaling(periode: Periode) =
        utbetalinger.harNærliggendeUtbetaling(periode)

    internal fun alleAndrePerioderErKlare(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.filterNot { it == vedtaksperiode }.none(IKKE_FERDIG_REVURDERT)

    internal fun fordelRevurdertUtbetaling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling) {
        håndter(aktivitetslogg) { håndterRevurdertUtbetaling(vedtaksperiode, utbetaling, aktivitetslogg) }
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

    internal fun harSykdomEllerForventerSøknad() = sykdomshistorikk.harSykdom()
            || sykdomstidslinje().harSykedager()
            || (sykmeldingsperioder.harSykmeldingsperiode())

    internal fun harSpleisSykdom() = sykdomshistorikk.harSykdom()

    internal fun harSykdomFor(skjæringstidspunkt: LocalDate) = vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

    internal fun finnFørsteFraværsdag(skjæringstidspunkt: LocalDate): LocalDate? {
        if (harSykdomFor(skjæringstidspunkt)) {
            return sykdomstidslinje().subset(finnSammenhengendePeriode(skjæringstidspunkt).periode()).sisteSkjæringstidspunkt()
        }
        return null
    }

    internal fun periodetype(periode: Periode): Periodetype {
        return arbeidsgiverperiode(periode, SubsumsjonObserver.NullObserver)?.let { person.periodetype(organisasjonsnummer, it, periode, skjæringstidspunkt(periode)) } ?: Periodetype.FØRSTEGANGSBEHANDLING
    }

    internal fun erFørstegangsbehandling(periode: Periode) = periodetype(periode) == Periodetype.FØRSTEGANGSBEHANDLING
    private fun skjæringstidspunkt(periode: Periode) = person.skjæringstidspunkt(organisasjonsnummer, sykdomstidslinje(), periode)

    internal fun avgrensetPeriode(periode: Periode) =
        Periode(maxOf(periode.start, skjæringstidspunkt(periode)), periode.endInclusive)

    internal fun builder(
        regler: ArbeidsgiverRegler,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
        subsumsjonObserver: SubsumsjonObserver
    ): UtbetalingstidslinjeBuilder {
        val inntekter = Inntekter(
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            regler = regler,
            subsumsjonObserver = subsumsjonObserver,
            organisasjonsnummer = organisasjonsnummer
        )
        return UtbetalingstidslinjeBuilder(inntekter)
    }

    internal fun lagreArbeidsforhold(arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        arbeidsforholdhistorikk.lagre(
            arbeidsforhold
                .filter { it.erRelevant(this) }
                .map { it.tilDomeneobjekt() },
            skjæringstidspunkt
        )
    }

    internal fun build(
        subsumsjonObserver: SubsumsjonObserver,
        infotrygdhistorikk: Infotrygdhistorikk,
        builder: IUtbetalingstidslinjeBuilder,
        kuttdato: LocalDate
    ): Utbetalingstidslinje {
        val sykdomstidslinje = sykdomstidslinje().fremTilOgMed(kuttdato).takeUnless { it.count() == 0 } ?: return Utbetalingstidslinje()
        return infotrygdhistorikk.build(organisasjonsnummer, sykdomstidslinje, builder, subsumsjonObserver)
    }

    internal fun beregn(aktivitetslogg: IAktivitetslogg, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger, periode: Periode, perioder: Map<Periode, Pair<IAktivitetslogg, SubsumsjonObserver>>): Boolean {
        try {
            arbeidsgiverUtbetalinger.beregn(aktivitetslogg, organisasjonsnummer, periode, perioder)
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(aktivitetslogg)
        }
        return !aktivitetslogg.hasErrorsOrWorse()
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

    internal fun fyllUtPeriodeMedForventedeDager(hendelse: PersonHendelse, periode: Periode) {
        sykdomshistorikk.fyllUtPeriodeMedForventedeDager(hendelse, periode)
    }

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harRelevantArbeidsforhold(skjæringstidspunkt)

    internal fun harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        !harRelevantArbeidsforhold(skjæringstidspunkt) && vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

    internal fun loggførHendelsesreferanse(organisasjonsnummer: String, skjæringstidspunkt: LocalDate, overstyrInntekt: OverstyrInntekt) {
        if (this.organisasjonsnummer != organisasjonsnummer) return
        vedtaksperioder.filter { it.gjelder(skjæringstidspunkt) }.forEach { it.loggførHendelsesreferanse(overstyrInntekt) }
    }

    internal fun harFerdigstiltPeriode() = vedtaksperioder.any(ER_ELLER_HAR_VÆRT_AVSLUTTET) || forkastede.harAvsluttedePerioder()

    internal fun <T> arbeidsforhold(
        skjæringstidspunkt: LocalDate,
        creator: (orgnummer: String, ansattFom: LocalDate, ansattTom: LocalDate?, erAktiv: Boolean) -> T
    ) =
        arbeidsforholdhistorikk.sisteArbeidsforhold(skjæringstidspunkt) { ansattFom: LocalDate, ansattTom: LocalDate?, erAktiv: Boolean ->
            creator(organisasjonsnummer, ansattFom, ansattTom, erAktiv)
        }

    internal fun build(filter: Utbetalingsfilter.Builder, inntektsmeldingId: UUID) {
        inntektshistorikk.build(filter, inntektsmeldingId)
    }

    internal fun harSykmeldingsperiodeFør(dato: LocalDate) = sykmeldingsperioder.harSykmeldingsperiodeFør(dato)

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

internal enum class ForkastetÅrsak {
    IKKE_STØTTET,
    UKJENT,
    ERSTATTES,
    ANNULLERING
}
