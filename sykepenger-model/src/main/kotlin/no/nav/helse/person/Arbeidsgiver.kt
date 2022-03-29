package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
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
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Inntektshistorikk.IkkeRapportert
import no.nav.helse.person.Vedtaksperiode.AvventerArbeidsgivere
import no.nav.helse.person.Vedtaksperiode.AvventerArbeidsgivereRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikk
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikkRevurdering
import no.nav.helse.person.Vedtaksperiode.Companion.AVVENTER_GODKJENT_REVURDERING
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_BEHANDLET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_REVURDERT
import no.nav.helse.person.Vedtaksperiode.Companion.KLAR_TIL_BEHANDLING
import no.nav.helse.person.Vedtaksperiode.Companion.REVURDERING_IGANGSATT
import no.nav.helse.person.Vedtaksperiode.Companion.harNødvendigInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.harOverlappendeUtbetaltePerioder
import no.nav.helse.person.Vedtaksperiode.Companion.harUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.periode
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
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
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
    private val refusjonOpphører: MutableList<LocalDate?>,
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
        refusjonOpphører = mutableListOf(),
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

        internal fun List<Arbeidsgiver>.harPeriodeSomBlokkererOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate) = any { arbeidsgiver ->
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

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun List<Arbeidsgiver>.beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
            fold(emptyList<ArbeidsgiverInntektsopplysning>()) { inntektsopplysninger, arbeidsgiver ->
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.grunnlagForSykepengegrunnlag(
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
                val inntektsopplysning = arbeidsgiver.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag)
                inntektsopplysning?.subsumsjon(subsumsjonObserver, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer)
                when {
                    arbeidsgiver.harDeaktivertArbeidsforhold(skjæringstidspunkt) -> null
                    inntektsopplysning == null && arbeidsgiver.arbeidsforholdhistorikk.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP) -> {
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

        internal fun List<Arbeidsgiver>.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
            this.mapNotNull { arbeidsgiver ->
                arbeidsgiver.inntektshistorikk.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt)
                    ?.let { ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, it) }
            }

        internal fun List<Arbeidsgiver>.harNødvendigInntekt(skjæringstidspunkt: LocalDate) =
            this.all { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt).harNødvendigInntekt() }

        internal fun Iterable<Arbeidsgiver>.harVedtaksperiodeFor(skjæringstidspunkt: LocalDate) = any { arbeidsgiver ->
            arbeidsgiver.vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.gjelder(skjæringstidspunkt) }
        }

        internal fun List<Arbeidsgiver>.minstEttSykepengegrunnlagSomIkkeKommerFraSkatt(skjæringstidspunkt: LocalDate) =
            any { !it.grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt) }

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
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
        ) {
            filter { it.organisasjonsnummer != "0" }.forEach { it.utbetalFeriepenger(aktørId, feriepengeberegner, utbetalingshistorikkForFeriepenger) }
        }

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandling(gjenopptaBehandling: IAktivitetslogg) = forEach { arbeidsgiver ->
            arbeidsgiver.gjenopptaBehandling(gjenopptaBehandling)
        }

        /*
            sjekker at vi har inntekt for første fraværsdag for alle arbeidsgivere med sykdom for skjæringstidspunkt
         */
        internal fun Iterable<Arbeidsgiver>.harNødvendigInntekt(skjæringstidspunkt: LocalDate) =
            filter { it.harSykdomFor(skjæringstidspunkt) }.all { it.harInntektsmelding(skjæringstidspunkt) }

        internal fun Iterable<Arbeidsgiver>.trengerSøknadISammeMåned(skjæringstidspunkt: LocalDate) = this
            .filter { !it.harSykdomFor(skjæringstidspunkt) }
            .any { it.sykmeldingsperioder.harSykmeldingsperiodeI(YearMonth.from(skjæringstidspunkt)) }

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandlingNy(aktivitetslogg: IAktivitetslogg) {
            val førstePeriode = nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
                .sortedBy { it.periode().endInclusive }
                .firstOrNull() ?: return

            if (førstePeriode.trengerSøknadISammeMåned(this)) return
            if (!førstePeriode.kanGjennoptaBehandling(this)) return

            if (all { it.sykmeldingsperioder.kanFortsetteBehandling(førstePeriode.periode()) }) {
                førstePeriode.gjenopptaBehandlingNy(aktivitetslogg)
            }
        }

        internal fun søppelbøtte(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: IAktivitetslogg,
            filter: VedtaksperiodeFilter
        ) {
            arbeidsgivere.forEach { it.søppelbøtte(hendelse, filter, ForkastetÅrsak.IKKE_STØTTET) }
        }
    }

    private fun gjenopptaBehandling(gjenopptaBehandling: IAktivitetslogg) {
        gjenopptaBehandling.kontekst(this)
        énHarHåndtert(gjenopptaBehandling, Vedtaksperiode::gjenopptaBehandling)
        Vedtaksperiode.gjenopptaBehandling(
            hendelse = gjenopptaBehandling,
            person = person,
            nåværendeTilstand = AvventerArbeidsgivere,
            nesteTilstand = AvventerHistorikk
        )
        Vedtaksperiode.gjenopptaBehandling(
            hendelse = gjenopptaBehandling,
            person = person,
            nåværendeTilstand = AvventerArbeidsgivereRevurdering,
            nesteTilstand = AvventerHistorikkRevurdering,
            filter = IKKE_FERDIG_REVURDERT
        )
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
        visitor.visitRefusjonOpphører(refusjonOpphører)
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
        forrige: List<Utbetaling>
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

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            feriepengeutbetalinger.add(feriepengeutbetaling)

            if (feriepengeutbetaling.sendTilOppdrag) {
                feriepengeutbetaling.overfør(utbetalingshistorikkForFeriepenger)
            }
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
        sykmeldingsperioder.lagre(sykmelding.sykdomstidslinje().periode()!!)
        if (Toggle.NyTilstandsflyt.enabled) return
        val vedtaksperiode = Vedtaksperiode(
            person = person,
            arbeidsgiver = this,
            hendelse = sykmelding,
            jurist = jurist
        )
        if (kanIkkeBehandle(sykmelding)) return registrerForkastetVedtaksperiode(vedtaksperiode, sykmelding)
        if (noenHarHåndtert(sykmelding, Vedtaksperiode::håndter)) return
        registrerNyVedtaksperiode(vedtaksperiode)
        sykmelding.nyVedtaksperiode()
        vedtaksperiode.håndter(sykmelding)
        håndter(sykmelding) { nyPeriode(vedtaksperiode, sykmelding) }
    }

    private fun kanIkkeBehandle(sykmelding: Sykmelding): Boolean {
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, sykmelding)
        ForkastetVedtaksperiode.forlengerForkastet(forkastede, sykmelding)
        return sykmelding.hasErrorsOrWorse()
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        sykmeldingsperioder.fjern(søknad.periode())
        if (Toggle.NyTilstandsflyt.enabled) {
            opprettVedtaksperiodeOgHåndter(søknad)
        } else {
            finnVedtaksperiodeOgHåndter(søknad)
        }
    }

    fun opprettVedtaksperiodeOgHåndter(søknad: Søknad) {
        val vedtaksperiode = Vedtaksperiode(
            person = person,
            arbeidsgiver = this,
            hendelse = søknad,
            jurist = jurist
        )
        if (noenHarHåndtert(søknad, Vedtaksperiode::håndter)) return
        registrerNyVedtaksperiode(vedtaksperiode)
        vedtaksperiode.håndter(søknad)
        håndter(søknad) { nyPeriodeMedNyFlyt(vedtaksperiode, søknad) }
    }

    fun finnVedtaksperiodeOgHåndter(søknad: Søknad) {
        if (vedtaksperioder.any { it.overlapperMenUlikFerieinformasjon(søknad) }) {
            søknad.warn("Det er oppgitt ny informasjon om ferie i søknaden som det ikke har blitt opplyst om tidligere. Tidligere periode må revurderes.")
        }
        noenHarHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke ${søknad.kilde}. Har nok ikke mottatt sykmelding")
        if (søknad.hasErrorsOrWorse()) {
            val søknadsperiode = søknad.sykdomstidslinje().periode()
            val harNærliggendeUtbetaling = søknadsperiode?.let { person.harNærliggendeUtbetaling(it) } ?: false
            if (harNærliggendeUtbetaling) person.emitOpprettOppgaveForSpeilsaksbehandlereEvent(søknad) else person.emitOpprettOppgaveEvent(søknad)
            person.emitHendelseIkkeHåndtert(søknad)
        }
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
        inntektsmelding.cacheRefusjon(refusjonshistorikk)
        if (vedtaksperiodeId != null) inntektsmelding.info("Replayer inntektsmelding til påfølgende perioder som overlapper.")
        if (!noenHarHåndtert(inntektsmelding) { håndter(inntektsmelding, vedtaksperiodeId, vedtaksperioder.toList()) }) {
            if (vedtaksperiodeId != null) return inntektsmelding.info("Vedtaksperiode overlapper ikke med replayet Inntektsmelding")
            inntektsmelding.error("Forventet ikke inntektsmelding. Har nok ikke mottatt sykmelding")
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
        nyUtbetaling(annullering)
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
        nyUtbetaling(etterutbetaling)
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

        MaksimumUtbetaling(arbeidsgivertidslinjer.values.toList(), aktivitetslogg, periode.endInclusive).betal()

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
        vedtaksperioder
            .firstOrNull { it.kanHåndtereOverstyring(hendelse) }
            ?.håndter(hendelse)
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
            ForkastetVedtaksperiode.arbeidsgiverperiodeFor(person, sykdomshistorikk.nyesteId(), forkastede, organisasjonsnummer, sykdomstidslinje(), periode, subsumsjonObserver)
        return arbeidsgiverperioder.finn(periode)
    }

    internal fun ghostPerioder(): List<GhostPeriode> = person.skjæringstidspunkterFraSpleis()
        .filter { skjæringstidspunkt -> vedtaksperioder.none { it.gjelder(skjæringstidspunkt) } }
        .filter(::erGhost)
        .mapNotNull { skjæringstidspunkt -> person.ghostPeriode(skjæringstidspunkt, arbeidsforholdhistorikk.harDeaktivertArbeidsforhold(skjæringstidspunkt)) }

    private fun erGhost(skjæringstidspunkt: LocalDate): Boolean {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        val inntektsopplysning = inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag)
        val harArbeidsforholdNyereEnnToMåneder = arbeidsforholdhistorikk.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)
        return inntektsopplysning is Inntektshistorikk.SkattComposite || harArbeidsforholdNyereEnnToMåneder
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
        val (perioderFør, perioderEtter) = vedtaksperioder.sorted().partition { it < vedtaksperiode }
        val sammenhengendePerioder = mutableListOf(vedtaksperiode)
        perioderFør.reversed().forEach {
            if (it.erSykeperiodeRettFør(sammenhengendePerioder.first()))
                sammenhengendePerioder.add(0, it)
        }
        perioderEtter.forEach {
            if (sammenhengendePerioder.last().erSykeperiodeRettFør(it))
                sammenhengendePerioder.add(it)
        }
        return sammenhengendePerioder
    }

    internal fun finnSammenhengendePeriode(skjæringstidspunkt: LocalDate) = vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt)

    internal fun harInntektsmelding(skjæringstidspunkt: LocalDate): Boolean {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt) ?: return false
        return inntektshistorikk.harInntektsmelding(førsteFraværsdag)
    }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
        inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periodeStart, finnFørsteFraværsdag(skjæringstidspunkt))
            ?.grunnlagForSykepengegrunnlag()

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

    internal fun lagreSykepengegrunnlag(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
        if (harRelevantArbeidsforhold(skjæringstidspunkt)) {
            arbeidsgiverInntekt.lagreInntekter(inntektshistorikk, skjæringstidspunkt, hendelse.meldingsreferanseId())
        }
    }

    internal fun lagreSammenligningsgrunnlag(arbeidsgiverInntekt: ArbeidsgiverInntekt, skjæringstidspunkt: LocalDate, hendelse: PersonHendelse) {
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

    private fun registrerForkastetVedtaksperiode(vedtaksperiode: Vedtaksperiode, hendelse: Sykmelding) {
        hendelse.info("Oppretter forkastet vedtaksperiode ettersom Sykmelding inneholder errors")
        vedtaksperiode.forkast(hendelse, utbetalinger)
        forkastede.add(ForkastetVedtaksperiode(vedtaksperiode, ForkastetÅrsak.IKKE_STØTTET))
    }

    internal fun finnSykeperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erSykeperiodeRettFør(vedtaksperiode)
        }

    internal fun finnSykeperiodeRettEtter(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            vedtaksperiode.erSykeperiodeRettFør(other)
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

    internal fun harNærliggendeUtbetaling(periode: Periode) =
        utbetalinger.harNærliggendeUtbetaling(periode)

    internal fun alleAndrePerioderErKlare(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.filterNot { it == vedtaksperiode }.none(IKKE_FERDIG_REVURDERT)

    internal fun fordelRevurdertUtbetaling(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling) {
        håndter(hendelse) { håndterRevurdertUtbetaling(utbetaling, hendelse) }
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to organisasjonsnummer))
    }

    internal fun lås(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().lås(periode)
    }

    private fun låsOpp(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().låsOpp(periode)
    }

    internal fun harSykdom() = sykdomshistorikk.harSykdom() || sykdomstidslinje().harSykedager()

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
    internal fun erInfotrygdOvergangEllerForlengelse(periode: Periode) =
        periodetype(periode) in arrayOf(Periodetype.OVERGANG_FRA_IT, Periodetype.INFOTRYGDFORLENGELSE)
    internal fun erForlengelse(periode: Periode) = !erFørstegangsbehandling(periode)
    private fun skjæringstidspunkt(periode: Periode) = person.skjæringstidspunkt(organisasjonsnummer, sykdomstidslinje(), periode)

    internal fun avgrensetPeriode(periode: Periode) =
        Periode(maxOf(periode.start, skjæringstidspunkt(periode)), periode.endInclusive)

    internal fun builder(
        regler: ArbeidsgiverRegler,
        skjæringstidspunkter: List<LocalDate>,
        inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver: Map<LocalDate, Map<String, Inntektshistorikk.Inntektsopplysning>>?,
        subsumsjonObserver: SubsumsjonObserver
    ): UtbetalingstidslinjeBuilder {
        val inntekter = Inntekter(
            skjæringstidspunkter = skjæringstidspunkter,
            inntektPerSkjæringstidspunkt = inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver?.mapValues { (_, inntektsopplysningPerArbeidsgiver) ->
                inntektsopplysningPerArbeidsgiver[organisasjonsnummer]
            },
            regler = regler,
            subsumsjonObserver = subsumsjonObserver
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
        periode: Periode
    ): Utbetalingstidslinje {
        val sykdomstidslinje = sykdomstidslinje().fremTilOgMed(periode.endInclusive).takeUnless { it.count() == 0 } ?: return Utbetalingstidslinje()
        return infotrygdhistorikk.build(organisasjonsnummer, sykdomstidslinje, builder, subsumsjonObserver)
    }

    internal fun beregn(aktivitetslogg: IAktivitetslogg, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger, periode: Periode): Boolean {
        try {
            arbeidsgiverUtbetalinger.beregn(aktivitetslogg, organisasjonsnummer, periode)
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(aktivitetslogg)
        }
        return !aktivitetslogg.hasErrorsOrWorse()
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

    internal fun grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) =
        inntektshistorikk.sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt)

    internal fun harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        !harRelevantArbeidsforhold(skjæringstidspunkt) && vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

    internal fun erSykmeldingenDenSistSkrevne(sykmelding: Sykmelding, hendelseIder: Set<UUID>): Boolean =
        sykdomshistorikk.erSykmeldingenDenSistSkrevne(sykmelding, hendelseIder)

    internal fun loggførHendelsesreferanse(organisasjonsnummer: String, skjæringstidspunkt: LocalDate, overstyrInntekt: OverstyrInntekt) {
        if (this.organisasjonsnummer != organisasjonsnummer) return
        vedtaksperioder.filter { it.gjelder(skjæringstidspunkt) }.forEach { it.loggførHendelsesreferanse(overstyrInntekt) }
    }

    internal fun harFerdigstiltPeriode() = vedtaksperioder.any(ER_ELLER_HAR_VÆRT_AVSLUTTET) || forkastede.harAvsluttedePerioder()

    private fun tilstøtendeBak(vedtaksperiode: Vedtaksperiode): Vedtaksperiode? {
        return vedtaksperioder.firstOrNull { it > vedtaksperiode }?.takeIf { vedtaksperiode.erSykeperiodeRettFør(it) }
    }

    internal fun tidligerePeriodeRebehandles(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
        tilstøtendeBak(vedtaksperiode)?.tidligerePeriodeRebehandles(hendelse)
    }

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
                refusjonOpphører: List<LocalDate?>,
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
                refusjonOpphører.toMutableList(),
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
