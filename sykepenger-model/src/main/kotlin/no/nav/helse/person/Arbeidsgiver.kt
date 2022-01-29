package no.nav.helse.person

import no.nav.helse.Toggle
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.utbetaling.*
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.harAvsluttedePerioder
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Inntektshistorikk.IkkeRapportert
import no.nav.helse.person.Vedtaksperiode.*
import no.nav.helse.person.Vedtaksperiode.Companion.ALLE
import no.nav.helse.person.Vedtaksperiode.Companion.AVVENTER_GODKJENT_REVURDERING
import no.nav.helse.person.Vedtaksperiode.Companion.ER_ELLER_HAR_VÆRT_AVSLUTTET
import no.nav.helse.person.Vedtaksperiode.Companion.FERDIG_BEHANDLET
import no.nav.helse.person.Vedtaksperiode.Companion.IKKE_FERDIG_REVURDERT
import no.nav.helse.person.Vedtaksperiode.Companion.KLAR_TIL_BEHANDLING
import no.nav.helse.person.Vedtaksperiode.Companion.REVURDERING_IGANGSATT
import no.nav.helse.person.Vedtaksperiode.Companion.UTEN_UTBETALING
import no.nav.helse.person.Vedtaksperiode.Companion.harNødvendigInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.harOverlappendeUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.harOverlappendeUtbetaltePerioder
import no.nav.helse.person.Vedtaksperiode.Companion.harUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.iderMedUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.medSkjæringstidspunkt
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.periode
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
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
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingstidslinje.*
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

        internal fun List<Arbeidsgiver>.antallMedVedtaksperioder(skjæringstidspunkt: LocalDate) =
            this.count { arbeidsgiver -> arbeidsgiver.vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.gjelder(skjæringstidspunkt) } }

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

        internal fun List<Arbeidsgiver>.beregnSykepengegrunnlag(skjæringstidspunkt: LocalDate) = mapNotNull { arbeidsgiver ->
            val førsteFraværsdag = arbeidsgiver.finnFørsteFraværsdag(skjæringstidspunkt)
            val inntektsopplysning = arbeidsgiver.inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag)
            when {
                arbeidsgiver.harInaktivtArbeidsforhold(skjæringstidspunkt) -> null
                inntektsopplysning == null && arbeidsgiver.harArbeidsforholdNyereEnnTreMåneder(skjæringstidspunkt) -> {
                    ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt))
                }
                inntektsopplysning != null -> ArbeidsgiverInntektsopplysning(arbeidsgiver.organisasjonsnummer, inntektsopplysning)
                else -> null
            }
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

        internal fun Iterable<Arbeidsgiver>.ghostPeriode(skjæringstidspunkt: LocalDate): GhostPerioder.GhostPeriode? {
            val relevanteVedtaksperioder = flatMap { it.vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt) }
            if (relevanteVedtaksperioder.isEmpty()) return null
            return GhostPerioder.GhostPeriode(
                fom = relevanteVedtaksperioder.minOf { it.periode().start },
                tom = relevanteVedtaksperioder.maxOf { it.periode().endInclusive },
                skjæringstidspunkt = skjæringstidspunkt
            )
        }


        internal fun Iterable<Arbeidsgiver>.beregnFeriepengerForAlleArbeidsgivere(
            aktørId: String,
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
        ) {
            filter { it.organisasjonsnummer != "0" }.forEach { it.utbetalFeriepenger(aktørId, feriepengeberegner, utbetalingshistorikkForFeriepenger) }
        }
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
        ForkastetVedtaksperiode.overlapperMedForkastet(forkastede, sykmelding)
        if (!sykmelding.forGammel() && !sykmelding.hasErrorsOrWorse()) {
            if (!noenHarHåndtert(sykmelding, Vedtaksperiode::håndter)) {
                if (!sykmelding.hasErrorsOrWorse()) {
                    sykmelding.info("Lager ny vedtaksperiode")
                    val ny = nyVedtaksperiode(sykmelding).also { it.håndter(sykmelding) }
                    håndter(sykmelding) { nyPeriode(ny, sykmelding) }
                }
            }
        }
        if (sykmelding.hasErrorsOrWorse()) person.emitHendelseIkkeHåndtert(sykmelding)
        finalize(sykmelding)
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        if (vedtaksperioder.any { it.overlapperMenUlikFerieinformasjon(søknad) }) {
            søknad.warn("Det er oppgitt ny informasjon om ferie i søknaden som det ikke har blitt opplyst om tidligere. Tidligere periode må revurderes.")
        }
        noenHarHåndtert(søknad, Vedtaksperiode::håndter, "Forventet ikke ${søknad.kilde}. Har nok ikke mottatt sykmelding")
        if (søknad.hasErrorsOrWorse()) {
            val harNærliggendeUtbetaling = søknad.sykdomstidslinje().periode()?.let(person::harNærliggendeUtbetaling) ?: false
            if (harNærliggendeUtbetaling) person.emitOpprettOppgaveForSpeilsaksbehandlereEvent(søknad) else person.emitOpprettOppgaveEvent(søknad)
            person.emitHendelseIkkeHåndtert(søknad)
        }
        finalize(søknad)
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
            }
            else
                inntektsmelding.info("Ingen forkastede vedtaksperioder overlapper med uforventet inntektsmelding")
        }

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

    internal fun håndter(
        ytelser: Ytelser,
        infotrygdhistorikk: Infotrygdhistorikk,
        arbeidsgiverUtbetalinger: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
    ) {
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
        utbetalinger.forEach { it.håndter(simulering) }
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
        feriepengeutbetalinger.forEach { it.håndter(utbetalingHendelse, person) }
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
            ?: return hendelse.info("Utbetalingen for $organisasjonsnummer for perioden ${sisteUtbetalte.periode} er ikke blitt endret. Grunnbeløpsregulering gjennomføres ikke.")

        hendelse.info("Etterutbetaler for $organisasjonsnummer for perioden ${sisteUtbetalte.periode}")
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
        finalize(hendelse)
    }

    internal fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)
        vedtaksperioder
            .firstOrNull { it.kanHåndtereOverstyring(hendelse) }
            ?.håndter(hendelse)
        finalize(hendelse)
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

    internal fun arbeidsgiverperiode(periode: Periode): Arbeidsgiverperiode? {
        val sykdomstidslinje = sykdomstidslinje()
        return ForkastetVedtaksperiode.arbeidsgiverperiodeFor(person, forkastede, organisasjonsnummer, sykdomstidslinje, periode)
    }

    internal fun ghostPerioder(): GhostPerioder? {
        val perioder = person.skjæringstidspunkterFraSpleis(organisasjonsnummer)
            .filter { skjæringstidspunkt -> vedtaksperioder.none { it.gjelder(skjæringstidspunkt) } }
            .mapNotNull { skjæringstidspunkt -> person.ghostPeriode(skjæringstidspunkt) }
        if (perioder.isEmpty()) return null
        return GhostPerioder(
            historikkInnslagId = person.nyesteIdForVilkårsgrunnlagHistorikk(),
            ghostPerioder = perioder
        )
    }

    internal fun infotrygdUtbetalingstidslinje() = person.infotrygdUtbetalingstidslinje(organisasjonsnummer)

    internal fun tidligsteDato(): LocalDate {
        return sykdomstidslinje().førsteDag()
    }

    internal fun finnSammenhengendePeriode(skjæringstidspunkt: LocalDate) = vedtaksperioder.medSkjæringstidspunkt(skjæringstidspunkt)

    private fun fjernDager(periode: Periode) = sykdomshistorikk.fjernDager(periode)

    internal fun harInntektsmelding(skjæringstidspunkt: LocalDate): Boolean {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt) ?: return false
        return inntektshistorikk.harInntektsmelding(skjæringstidspunkt, førsteFraværsdag)
    }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, periodeStart: LocalDate) =
        inntektshistorikk.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periodeStart, finnFørsteFraværsdag(skjæringstidspunkt))?.grunnlagForSykepengegrunnlag()

    internal fun addInntekt(inntektsmelding: Inntektsmelding, skjæringstidspunkt: LocalDate) {
        inntektsmelding.addInntekt(inntektshistorikk, skjæringstidspunkt)
    }

    internal fun finnTidligereInntektsmeldinginfo(skjæringstidspunkt: LocalDate) = inntektsmeldingInfo.finn(skjæringstidspunkt)

    internal fun addInntektsmelding(skjæringstidspunkt: LocalDate, inntektsmelding: Inntektsmelding): InntektsmeldingInfo {
        val førsteFraværsdag = finnFørsteFraværsdag(skjæringstidspunkt)
        if (førsteFraværsdag != null) addInntekt(inntektsmelding, førsteFraværsdag)
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

    internal fun søppelbøtte(
        hendelse: IAktivitetslogg,
        filter: VedtaksperiodeFilter,
        årsak: ForkastetÅrsak
    ) {
        forkast(filter, årsak)
            .takeIf { it.isNotEmpty() }
            ?.also { perioder ->
                hendelse.kontekst(this)
                perioder
                    .forEach {
                        it.forkast(hendelse, årsak)
                        fjernDager(it.periode())
                    }
                if (vedtaksperioder.isEmpty()) sykdomshistorikk.tøm()
                gjenopptaBehandling()
            }
    }

    private fun forkast(filter: VedtaksperiodeFilter, årsak: ForkastetÅrsak) = vedtaksperioder
        .filter { periode -> periode.kanForkastes(utbetalinger) }
        .filter(filter)
        .also { perioder ->
            vedtaksperioder.removeAll(perioder)
            forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it, årsak) })
        }

    // Fredet funksjonsnavn
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

    internal fun startRevurderingForAlleBerørtePerioder(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode) {
        hendelse.kontekst(this)
        håndter(hendelse) { nyRevurderingFør(vedtaksperiode, hendelse) }
        if (hendelse.hasErrorsOrWorse()) {
            hendelse.info("Revurdering blokkeres, gjenopptar behandling")
            return gjenopptaBehandling()
        }
    }

    private fun harInaktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harInaktivtArbeidsforhold(skjæringstidspunkt)

    internal fun kanReberegnes(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.all { it.kanReberegne(vedtaksperiode) }

    internal fun periodeReberegnet(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode) {
        håndter(hendelse) { periodeReberegnetFør(vedtaksperiode, hendelse) }
        if (!hendelse.hasErrorsOrWorse()) return
        hendelse.info("Reberegning blokkeres, gjenopptar behandling")
        gjenopptaBehandling()
    }

    // Fredet funksjonsnavn
    internal fun tidligereOgEttergølgende(segSelv: Periode): VedtaksperiodeFilter {
        val tidligereOgEttergølgende1 = vedtaksperioder.sorted().firstOrNull { it.periode().overlapperMed(segSelv) }?.let(::tidligereOgEttergølgende)
        return fun(vedtaksperiode: Vedtaksperiode) = tidligereOgEttergølgende1 != null && vedtaksperiode in tidligereOgEttergølgende1
    }

    internal fun overlappendePerioder(hendelse: SykdomstidslinjeHendelse) = vedtaksperioder.filter { hendelse.erRelevant(it.periode()) }

    private fun nyVedtaksperiode(hendelse: Sykmelding): Vedtaksperiode {
        return Vedtaksperiode(
            person = person,
            arbeidsgiver = this,
            hendelse = hendelse,
            jurist = jurist
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

    internal fun harOverlappendeUtbetaling(periode: Periode) =
        vedtaksperioder.harOverlappendeUtbetaling(periode)

    internal fun alleAndrePerioderErKlare(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.filterNot { it == vedtaksperiode }.none(IKKE_FERDIG_REVURDERT)

    internal fun fordelRevurdertUtbetaling(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling) {
        håndter(hendelse) { håndterRevurdertUtbetaling(utbetaling, hendelse) }
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
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivere, AvventerHistorikk)
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering, IKKE_FERDIG_REVURDERT)
        }

        while (skalGjenopptaRevurdering) {
            skalGjenopptaRevurdering = false
            Vedtaksperiode.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering, IKKE_FERDIG_REVURDERT)
        }
    }

    internal class GjenopptaBehandling(private val hendelse: ArbeidstakerHendelse) : ArbeidstakerHendelse(hendelse) {
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

    private fun låsOpp(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().låsOpp(periode)
    }

    internal fun harSykdom() = sykdomshistorikk.harSykdom() || sykdomstidslinje().harSykedager()

    internal fun harSpleisSykdom() = sykdomshistorikk.harSykdom()

    internal fun harSykdomFor(skjæringstidspunkt: LocalDate) = vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

    fun finnFørsteFraværsdag(skjæringstidspunkt: LocalDate): LocalDate? {
        if (harSykdomFor(skjæringstidspunkt)) {
            return sykdomstidslinje().subset(finnSammenhengendePeriode(skjæringstidspunkt).periode()).sisteSkjæringstidspunkt()
        }
        return null
    }

    internal fun periodetype(periode: Periode): Periodetype {
        val skjæringstidspunkt = skjæringstidspunkt(periode)
        return when {
            erFørstegangsbehandling(periode, skjæringstidspunkt) -> Periodetype.FØRSTEGANGSBEHANDLING
            forlengerInfotrygd(periode, skjæringstidspunkt) -> when {
                Utbetaling.harBetalt(utbetalinger, Periode(skjæringstidspunkt, periode.start.minusDays(1))) -> Periodetype.INFOTRYGDFORLENGELSE
                else -> Periodetype.OVERGANG_FRA_IT
            }
            !Utbetaling.harBetalt(utbetalinger, skjæringstidspunkt) -> Periodetype.FØRSTEGANGSBEHANDLING
            else -> Periodetype.FORLENGELSE
        }
    }

    internal fun erFørstegangsbehandling(periode: Periode, skjæringstidspunkt: LocalDate) =
        skjæringstidspunkt in periode

    internal fun erForlengelse(periode: Periode, skjæringstidspunkt: LocalDate = skjæringstidspunkt(periode)) =
        !erFørstegangsbehandling(periode, skjæringstidspunkt)

    internal fun forlengerInfotrygd(periode: Periode, skjæringstidspunkt: LocalDate = skjæringstidspunkt(periode)) =
        person.harInfotrygdUtbetalt(organisasjonsnummer, skjæringstidspunkt)

    private fun skjæringstidspunkt(periode: Periode) = person.skjæringstidspunkt(organisasjonsnummer, sykdomstidslinje(), periode)

    internal fun avgrensetPeriode(periode: Periode) =
        Periode(maxOf(periode.start, skjæringstidspunkt(periode)), periode.endInclusive)

    internal fun builder(
        regler: ArbeidsgiverRegler,
        skjæringstidspunkter: List<LocalDate>,
        inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver: Map<LocalDate, Map<String, Inntektshistorikk.Inntektsopplysning>>?,
        subsumsjonObserver: SubsumsjonObserver
    ): UtbetalingstidslinjeBuilder {
        return UtbetalingstidslinjeBuilder(
            skjæringstidspunkter = skjæringstidspunkter,
            inntektPerSkjæringstidspunkt = inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver?.mapValues { (_, inntektsopplysningPerArbeidsgiver) ->
                inntektsopplysningPerArbeidsgiver[organisasjonsnummer]
            },
            arbeidsgiverRegler = regler,
            subsumsjonObserver = subsumsjonObserver
        )
    }

    internal fun lagreArbeidsforhold(arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        arbeidsforholdhistorikk.lagre(
            arbeidsforhold
                .filter { it.erRelevant(this) }
                .map { it.tilDomeneobjekt() },
            skjæringstidspunkt
        )
    }

    internal fun build(builder: IArbeidsgiverperiodetelling, periode: Periode) {
        builder.build(sykdomstidslinje(), periode)
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

    internal fun harArbeidsforholdNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) = arbeidsforholdhistorikk.harArbeidsforholdNyereEnnTreMåneder(skjæringstidspunkt)

    internal fun grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) =
        inntektshistorikk.sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt)

    internal fun harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        !harRelevantArbeidsforhold(skjæringstidspunkt) && vedtaksperioder.any { it.gjelder(skjæringstidspunkt) }

    internal fun erSykmeldingenDenSistSkrevne(sykmelding: Sykmelding, hendelseIder: Set<UUID>): Boolean =
        sykdomshistorikk.erSykmeldingenDenSistSkrevne(sykmelding, hendelseIder)

    internal fun søknadsperioder(hendelseIder: Set<UUID>) = sykdomshistorikk.søknadsperioder(hendelseIder)

    internal fun loggførHendelsesreferanse(organisasjonsnummer: String, skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) {
        if (this.organisasjonsnummer != organisasjonsnummer) return
        vedtaksperioder.filter { it.gjelder(skjæringstidspunkt) }.forEach { it.loggførHendelsesreferanse(meldingsreferanseId) }
    }


    fun harFerdigstiltPeriode() = vedtaksperioder.any(ER_ELLER_HAR_VÆRT_AVSLUTTET) || forkastede.harAvsluttedePerioder()

    internal fun tilstøtendeBak(vedtaksperiode: Vedtaksperiode): Vedtaksperiode? {
        return vedtaksperioder.firstOrNull { it > vedtaksperiode }?.takeIf { vedtaksperiode.erSykeperiodeRettFør(it) }
    }

    internal fun harPeriodeBak(vedtaksperiode: Vedtaksperiode) = vedtaksperioder.any { it > vedtaksperiode }
    internal fun erAlleFerdigbehandletBak(vedtaksperiode: Vedtaksperiode) = vedtaksperioder
        .filter { it > vedtaksperiode }
        .all(FERDIG_BEHANDLET)
    internal fun erAlleUtenUtbetaling(vedtaksperiode: Vedtaksperiode) = vedtaksperioder
        .filter { it > vedtaksperiode }
        .all(UTEN_UTBETALING)

    internal fun tidligerePeriodeRebehandles(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
        tilstøtendeBak(vedtaksperiode)?.tidligerePeriodeRebehandles(hendelse)
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
