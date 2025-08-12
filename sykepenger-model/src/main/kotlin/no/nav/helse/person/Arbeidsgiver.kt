package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.UbrukteRefusjonsopplysningerUtDto
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.feriepenger.Arbeidsgiverferiepengegrunnlag
import no.nav.helse.feriepenger.Arbeidsgiverferiepengegrunnlag.Feriepengegrunnlag
import no.nav.helse.feriepenger.Arbeidsgiverferiepengegrunnlag.Feriepengegrunnlag.UtbetaltDag
import no.nav.helse.feriepenger.Feriepengeberegner
import no.nav.helse.feriepenger.Feriepengeutbetaling
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerTomUtbetaling
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsavgjørelse
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.GjenopptaBehandling
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.hendelser.Inntektsendringer
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.KanIkkeBehandlesHer
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Migrate
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrInntektsgrunnlag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.mursteinsperioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingpåminnelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.VedtakFattet
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.erLik
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrArbeidsgiveropplysninger
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.blokkererBehandlingAv
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.perioder
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.trengerArbeidsgiveropplysninger
import no.nav.helse.person.PersonObserver.UtbetalingEndretEvent.OppdragEventDetaljer
import no.nav.helse.person.Vedtaksperiode.Companion.AUU_SOM_VIL_UTBETALES
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.SAMME_ARBEIDSGIVERPERIODE
import no.nav.helse.person.Vedtaksperiode.Companion.aktiv
import no.nav.helse.person.Vedtaksperiode.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.person.Vedtaksperiode.Companion.beregnSkjæringstidspunkter
import no.nav.helse.person.Vedtaksperiode.Companion.checkBareEnPeriodeTilGodkjenningSamtidig
import no.nav.helse.person.Vedtaksperiode.Companion.egenmeldingsperioder
import no.nav.helse.person.Vedtaksperiode.Companion.medSammeUtbetaling
import no.nav.helse.person.Vedtaksperiode.Companion.nestePeriodeSomSkalGjenopptas
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.refusjonstidslinje
import no.nav.helse.person.Vedtaksperiode.Companion.startdatoerPåSammenhengendeVedtaksperioder
import no.nav.helse.person.Vedtaksperiode.Companion.validerTilstand
import no.nav.helse.person.Vedtaksperiode.Companion.venter
import no.nav.helse.person.Vedtaksperiode.VedtaksperiodeForkastetEventBuilder
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsgiveropplysninger for forkastet periode`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsgiveropplysninger for periode som allerede har opplysninger`
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.person.view.ArbeidsgiverView
import no.nav.helse.sykdomstidslinje.Dag.Companion.bareNyeDager
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.kunEnIkkeUtbetalt
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.tillaterOpprettelseAvUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.validerNyUtbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingslinjer.Utbetalingkladd
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeberegner
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat.Companion.finn
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.HundreProsent

internal class Arbeidsgiver private constructor(
    private val person: Person,
    private val id: UUID,
    val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    private val inntektshistorikk: Inntektshistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val sykmeldingsperioder: Sykmeldingsperioder,
    private val vedtaksperioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<ForkastetVedtaksperiode>,
    private val utbetalinger: MutableList<Utbetaling>,
    private val feriepengeutbetalinger: MutableList<Feriepengeutbetaling>,
    private val ubrukteRefusjonsopplysninger: Refusjonsservitør,
    private val regelverkslogg: Regelverkslogg
) : Aktivitetskontekst, UtbetalingObserver {
    internal constructor(person: Person, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, regelverkslogg: Regelverkslogg) : this(
        person = person,
        id = UUID.randomUUID(),
        yrkesaktivitetssporing = yrkesaktivitetssporing,
        inntektshistorikk = Inntektshistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        sykmeldingsperioder = Sykmeldingsperioder(),
        vedtaksperioder = mutableListOf(),
        forkastede = mutableListOf(),
        utbetalinger = mutableListOf(),
        feriepengeutbetalinger = mutableListOf(),
        ubrukteRefusjonsopplysninger = Refusjonsservitør(),
        regelverkslogg = regelverkslogg
    )

    val organisasjonsnummer = when (yrkesaktivitetssporing) {
        Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> yrkesaktivitetssporing.organisasjonsnummer
        Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> "SELVSTENDIG_DAGMAMMA"
        Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> "SELVSTENDIG_FISKER"
        Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> "SELVSTENDIG_JORDBRUKER"
    }
    private val yrkesaktivitet = when (yrkesaktivitetssporing) {
        Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> Yrkesaktivitet.Arbeidsledig
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> Yrkesaktivitet.Arbeidstaker
        Behandlingsporing.Yrkesaktivitet.Frilans -> Yrkesaktivitet.Frilans
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> Yrkesaktivitet.Selvstendig
        Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> Yrkesaktivitet.SelvstendigDagmamma
        Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> Yrkesaktivitet.SelvstendigFisker
        Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> Yrkesaktivitet.SelvstendigJordbruker
    }

    init {
        utbetalinger.forEach { it.registrer(this) }
    }

    fun view(): ArbeidsgiverView = ArbeidsgiverView(
        organisasjonsnummer = organisasjonsnummer,
        yrkesaktivitetssporing = yrkesaktivitetssporing,
        sykdomshistorikk = sykdomshistorikk.view(),
        utbetalinger = utbetalinger.map { it.view },
        inntektshistorikk = inntektshistorikk.view(),
        sykmeldingsperioder = sykmeldingsperioder.view(),
        ubrukteRefusjonsopplysninger = ubrukteRefusjonsopplysninger.view(),
        feriepengeutbetalinger = feriepengeutbetalinger.map { it.view() },
        aktiveVedtaksperioder = vedtaksperioder.map { it.view() },
        forkastetVedtaksperioder = forkastede.map { it.view() }
    )

    internal companion object {
        internal fun List<Arbeidsgiver>.finn(behandlingsporing: Behandlingsporing) =
            find { it.yrkesaktivitetssporing.erLik(behandlingsporing) }

        internal fun List<Arbeidsgiver>.tidligsteDato(): LocalDate {
            return mapNotNull { it.sykdomstidslinje().periode()?.start }.minOrNull() ?: LocalDate.now()
        }

        internal fun List<Arbeidsgiver>.igangsettOverstyring(
            revurdering: Revurderingseventyr,
            aktivitetslogg: IAktivitetslogg
        ) {
            forEach { arbeidsgiver ->
                arbeidsgiver.looper {
                    it.igangsettOverstyring(revurdering, aktivitetslogg)
                }
            }
        }

        internal fun List<Arbeidsgiver>.finnAnnulleringskandidater(vedtaksperiode: Vedtaksperiode) = flatMap { it.finnAnnulleringskandidater(vedtaksperiode) }.toSet()

        internal fun List<Arbeidsgiver>.venter(nestemann: Vedtaksperiode) =
            flatMap { arbeidsgiver -> arbeidsgiver.vedtaksperioder.venter(nestemann) }

        internal fun List<Arbeidsgiver>.beregnSkjæringstidspunkt(infotrygdhistorikk: Infotrygdhistorikk): () -> Skjæringstidspunkt =
            {
                infotrygdhistorikk.skjæringstidspunkt(map(Arbeidsgiver::sykdomstidslinje))
            }

        internal fun List<Arbeidsgiver>.beregnSkjæringstidspunkter(infotrygdhistorikk: Infotrygdhistorikk) {
            forEach {
                it.vedtaksperioder.beregnSkjæringstidspunkter(
                    beregnSkjæringstidspunkt(infotrygdhistorikk),
                    it.beregnArbeidsgiverperiode()
                )
            }
        }

        internal fun List<Arbeidsgiver>.aktiveSkjæringstidspunkter(): Set<LocalDate> {
            return flatMap { it.vedtaksperioder }.aktiveSkjæringstidspunkter()
        }

        internal fun List<Arbeidsgiver>.håndter(
            overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag,
            aktivitetslogg: IAktivitetslogg
        ) =
            firstNotNullOfOrNull { it.håndter(overstyrInntektsgrunnlag, aktivitetslogg) }

        internal fun List<Arbeidsgiver>.håndterOverstyringAvInntekt(
            overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger,
            aktivitetslogg: IAktivitetslogg
        ) = firstNotNullOfOrNull { it.håndter(overstyrArbeidsgiveropplysninger, aktivitetslogg) }

        internal fun List<Arbeidsgiver>.håndterOverstyringAvRefusjon(
            hendelse: OverstyrArbeidsgiveropplysninger,
            aktivitetslogg: IAktivitetslogg
        ): Revurderingseventyr? {
            val revurderingseventyr = mapNotNull { arbeidsgiver ->
                val vedtaksperioderPåSkjæringstidspunkt = arbeidsgiver.vedtaksperioder.filter(MED_SKJÆRINGSTIDSPUNKT(hendelse.skjæringstidspunkt))
                val startdatoer = vedtaksperioderPåSkjæringstidspunkt.startdatoerPåSammenhengendeVedtaksperioder()

                val periodendesRefusjonstidslinje = vedtaksperioderPåSkjæringstidspunkt.refusjonstidslinje()
                val ubruktRefusjonstidslinjeEtterPeriodene = vedtaksperioderPåSkjæringstidspunkt.lastOrNull()
                    ?.ubrukteRefusjonsopplysningerEtter(arbeidsgiver.ubrukteRefusjonsopplysninger)
                    ?: Beløpstidslinje()
                val eksisterendeRefusjonstidslinje = periodendesRefusjonstidslinje + ubruktRefusjonstidslinjeEtterPeriodene

                val servitør = hendelse.refusjonsservitør(startdatoer, arbeidsgiver.organisasjonsnummer, eksisterendeRefusjonstidslinje) ?: return@mapNotNull null
                arbeidsgiver.håndterRefusjonsopplysninger(hendelse, overstyrArbeidsgiveropplysninger(hendelse.metadata.meldingsreferanseId), aktivitetslogg, servitør)
            }
            return revurderingseventyr.tidligsteEventyr()
        }

        internal fun Iterable<Arbeidsgiver>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun Iterable<Arbeidsgiver>.vedtaksperioder(filter: VedtaksperiodeFilter) =
            map { it.vedtaksperioder.filter(filter) }.flatten()

        internal fun Iterable<Arbeidsgiver>.mursteinsperioder(utgangspunkt: Vedtaksperiode) = this
            .flatMap { it.vedtaksperioder }
            .mursteinsperioder(utgangspunkt.periode, Vedtaksperiode::periode)

        internal fun List<Arbeidsgiver>.validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            checkBareEnPeriodeTilGodkjenningSamtidig()
            forEach { it.sjekkForUgyldigeSituasjoner(hendelse, aktivitetslogg) }
        }

        internal fun Iterable<Arbeidsgiver>.beregnFeriepengerForAlleArbeidsgivere(
            personidentifikator: Personidentifikator,
            feriepengeberegner: Feriepengeberegner,
            utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
            aktivitetslogg: IAktivitetslogg
        ) {
            forEach {
                it.utbetalFeriepenger(
                    personidentifikator,
                    feriepengeberegner,
                    utbetalingshistorikkForFeriepenger,
                    aktivitetslogg
                )
            }
        }

        internal fun Iterable<Arbeidsgiver>.avventerSøknad(periode: Periode) = this
            .any { it.sykmeldingsperioder.avventerSøknad(periode) }

        internal fun Iterable<Arbeidsgiver>.fjernSykmeldingsperiode(periode: Periode) = this
            .forEach { it.sykmeldingsperioder.fjern(periode) }

        private fun Iterable<Arbeidsgiver>.harPågåeneAnnullering() =
            any { it.utbetalinger.any { utbetaling -> utbetaling.erAnnulleringInFlight() } }

        private fun Iterable<Arbeidsgiver>.førsteAuuSomVilUtbetales() =
            nåværendeVedtaksperioder(AUU_SOM_VIL_UTBETALES).minOrNull()

        internal fun Iterable<Arbeidsgiver>.gjenopptaBehandling(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            if (harPågåeneAnnullering()) return aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
            val periodeSomSkalGjenopptas = periodeSomSkalGjenopptas() ?: return
            periodeSomSkalGjenopptas.gjenopptaBehandling(hendelse, aktivitetslogg)
        }

        internal fun Iterable<Arbeidsgiver>.nestemann() =
            periodeSomSkalGjenopptas() ?: førsteAuuSomVilUtbetales()

        private fun Iterable<Arbeidsgiver>.periodeSomSkalGjenopptas() =
            flatMap { it.vedtaksperioder }.nestePeriodeSomSkalGjenopptas()

        private fun Iterable<Arbeidsgiver>.checkBareEnPeriodeTilGodkjenningSamtidig() =
            flatMap { it.vedtaksperioder }.checkBareEnPeriodeTilGodkjenningSamtidig()

        internal fun søppelbøtte(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg,
            vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>
        ) {
            arbeidsgivere.flatMap { it.søppelbøtte(hendelse, aktivitetslogg, vedtaksperioderSomSkalForkastes) }.forEach { it.buildAndEmit() }
        }

        internal fun gjenopprett(
            person: Person,
            dto: ArbeidsgiverInnDto,
            regelverkslogg: Regelverkslogg,
            grunnlagsdata: Map<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>
        ): Arbeidsgiver {
            val vedtaksperioder = mutableListOf<Vedtaksperiode>()
            val forkastede = mutableListOf<ForkastetVedtaksperiode>()
            val utbetalinger = dto.utbetalinger.fold(emptyList<Utbetaling>()) { result, utbetaling ->
                result.plusElement(Utbetaling.gjenopprett(utbetaling, result))
            }
            val arbeidsgiver = Arbeidsgiver(
                person = person,
                id = dto.id,
                yrkesaktivitetssporing = when (dto.yrkesaktivitetstype) {
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.ARBEIDSLEDIG -> Behandlingsporing.Yrkesaktivitet.Arbeidsledig
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.ARBEIDSTAKER -> Arbeidstaker(dto.organisasjonsnummer)
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.FRILANS -> Behandlingsporing.Yrkesaktivitet.Frilans
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.SELVSTENDIG -> Behandlingsporing.Yrkesaktivitet.Selvstendig
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.SELVSTENDIG_JORDBRUKER -> Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.SELVSTENDIG_FISKER -> Behandlingsporing.Yrkesaktivitet.SelvstendigFisker
                    ArbeidsgiverInnDto.Yrkesaktivitetstype.SELVSTENDIG_DAGMAMMA -> Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma
                },
                inntektshistorikk = Inntektshistorikk.gjenopprett(dto.inntektshistorikk),
                sykdomshistorikk = Sykdomshistorikk.gjenopprett(dto.sykdomshistorikk),
                sykmeldingsperioder = Sykmeldingsperioder.gjenopprett(dto.sykmeldingsperioder),
                vedtaksperioder = vedtaksperioder,
                forkastede = forkastede,
                utbetalinger = utbetalinger.toMutableList(),
                feriepengeutbetalinger = dto.feriepengeutbetalinger.map { Feriepengeutbetaling.gjenopprett(it) }
                    .toMutableList(),
                ubrukteRefusjonsopplysninger = Refusjonsservitør.gjenopprett(dto.ubrukteRefusjonsopplysninger),
                regelverkslogg = regelverkslogg
            )
            val utbetalingerMap = utbetalinger.associateBy(Utbetaling::id)
            vedtaksperioder.addAll(dto.vedtaksperioder.map {
                Vedtaksperiode.gjenopprett(
                    person,
                    arbeidsgiver,
                    it,
                    regelverkslogg,
                    grunnlagsdata,
                    utbetalingerMap
                )
            })
            forkastede.addAll(dto.forkastede.map {
                ForkastetVedtaksperiode.gjenopprett(
                    person,
                    arbeidsgiver,
                    it,
                    regelverkslogg,
                    grunnlagsdata,
                    utbetalingerMap
                )
            })
            return arbeidsgiver
        }
    }

    private fun finnVedtaksperiodeForUtbetaling(utbetalingId: UUID) = vedtaksperioder.firstOrNull { it.behandlinger.harNoenBehandlingUtbetaling(utbetalingId) }

    internal fun kanBeregneSykepengegrunnlag(skjæringstidspunkt: LocalDate, vedtaksperioder: List<Vedtaksperiode>): Boolean {
        return avklarInntekt(skjæringstidspunkt, vedtaksperioder) != null
    }

    // TODO: denne avklaringen må bo på behandlingen; dvs. at inntekt må ligge lagret på vedtaksperiodene
    internal fun avklarInntekt(skjæringstidspunkt: LocalDate, vedtaksperioder: List<Vedtaksperiode>): Inntektsmeldinginntekt? {
        // finner inntektsmelding for en av første fraværsdagene.
        // håndterer det som en liste i tilfelle arbeidsgiveren har auu'er i forkant, og at inntekt kan ha blitt malplassert
        // (og at det er vrient å avgjøre én riktig første fraværsdag i forkant)
        return vedtaksperioder.firstNotNullOfOrNull {
            val førsteFraværsdag = it.førsteFraværsdag
            inntektshistorikk.avklarInntektsgrunnlag(skjæringstidspunkt = skjæringstidspunkt, førsteFraværsdag = førsteFraværsdag)
        }
    }

    internal fun organisasjonsnummer() = organisasjonsnummer
    internal fun utbetaling() = utbetalinger.lastOrNull()
    internal fun grunnlagForFeriepenger(): Arbeidsgiverferiepengegrunnlag {
        val utbetalteDager = fun(oppdrag: Oppdrag): List<UtbetaltDag> {
            return oppdrag
                .linjerUtenOpphør()
                .flatMap { linje ->
                    linje
                        .filterNot { it.erHelg() }
                        .map { UtbetaltDag(it, linje.beløp) }
                }
        }
        return Arbeidsgiverferiepengegrunnlag(
            orgnummer = organisasjonsnummer,
            utbetalinger = utbetalinger
                .aktive()
                .map {
                    Feriepengegrunnlag(
                        arbeidsgiverUtbetalteDager = utbetalteDager(it.arbeidsgiverOppdrag),
                        personUtbetalteDager = utbetalteDager(it.personOppdrag)
                    )
                }
        )
    }

    private fun lagUtbetalingkladd(utbetalingstidslinje: Utbetalingstidslinje): Utbetalingkladd {
        return UtbetalingkladdBuilder(
            tidslinje = utbetalingstidslinje,
            mottakerRefusjon = organisasjonsnummer,
            mottakerBruker = person.fødselsnummer,
            klassekodeBruker = when (yrkesaktivitetssporing) {
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> Klassekode.SykepengerArbeidstakerOrdinær
                Behandlingsporing.Yrkesaktivitet.Selvstendig -> Klassekode.SelvstendigNæringsdrivendeOppgavepliktig
                Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk
                Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> Klassekode.SelvstendigNæringsdrivendeDagmammaOppgavepliktig
                Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> Klassekode.SelvstendigNæringsdrivendeFisker
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                Behandlingsporing.Yrkesaktivitet.Frilans -> error("Forventer ikke å lage utbetaling for $yrkesaktivitetssporing ennå")
            }
        ).build()
    }

    internal fun lagNyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode,
        type: Utbetalingtype
    ): Utbetaling {
        val utbetalingen = Utbetaling.lagUtbetaling(
            utbetalinger = utbetalinger,
            vedtaksperiodekladd = lagUtbetalingkladd(utbetalingstidslinje),
            utbetalingstidslinje = utbetalingstidslinje,
            periode = periode,
            aktivitetslogg = aktivitetslogg,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            type = type
        )
        nyUtbetaling(aktivitetslogg, utbetalingen)
        return utbetalingen
    }

    internal fun lagTomUtbetaling(periode: Periode, type: Utbetalingtype): Utbetaling {
        val tomUtbetaling = Utbetaling.lagTomUtbetaling(
            vedtaksperiodekladd = lagUtbetalingkladd(Utbetalingstidslinje()),
            periode = periode,
            type = type
        )
        check(utbetalinger.tillaterOpprettelseAvUtbetaling(tomUtbetaling)) { "Har laget en overlappende utbetaling" }
        utbetalinger.add(tomUtbetaling)
        return tomUtbetaling
    }

    private fun nyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetaling: Utbetaling
    ) {
        utbetalinger.validerNyUtbetaling(utbetaling)
        check(utbetalinger.tillaterOpprettelseAvUtbetaling(utbetaling)) { "Har laget en overlappende utbetaling" }
        utbetalinger.add(utbetaling)
        utbetaling.registrer(this)
        utbetaling.opprett(aktivitetslogg)
    }

    internal fun utbetalFeriepenger(
        personidentifikator: Personidentifikator,
        feriepengeberegner: Feriepengeberegner,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        aktivitetslogg: IAktivitetslogg
    ) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)

        val feriepengeutbetaling = Feriepengeutbetaling.Builder(
            personidentifikator,
            organisasjonsnummer,
            feriepengeberegner,
            utbetalingshistorikkForFeriepenger,
            feriepengeutbetalinger
        ).build(aktivitetsloggMedArbeidsgiverkontekst)

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            feriepengeutbetalinger.add(feriepengeutbetaling)
            feriepengeutbetaling.overfør(aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndter(sykmelding) }
        yrkesaktivitet.håndter(sykmelding, aktivitetsloggMedArbeidsgiverkontekst, sykmeldingsperioder)
    }

    internal fun håndter(avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        avbruttSøknad.avbryt(sykmeldingsperioder)
    }

    internal fun håndter(forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        forkastSykmeldingsperioder.forkast(sykmeldingsperioder)
    }

    internal fun håndter(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter {
            it.håndter(anmodningOmForkasting, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun vurderOmSøknadIkkeKanHåndteres(
        aktivitetslogg: IAktivitetslogg,
        nyPeriode: Periode,
        arbeidsgivere: List<Arbeidsgiver>
    ) {
        // Sjekker først egen arbeidsgiver
        if (yrkesaktivitet.erYrkesaktivitetenIkkeStøttet(aktivitetslogg)) return
        if (forkastede.blokkererBehandlingAv(nyPeriode, organisasjonsnummer, aktivitetslogg)) return
        // Også alle etterpå
        arbeidsgivere.any { arbeidsgiver ->
            arbeidsgiver.forkastede.blokkererBehandlingAv(nyPeriode, organisasjonsnummer, aktivitetslogg)
        }
    }

    internal fun håndter(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        søknad.slettSykmeldingsperioderSomDekkes(sykmeldingsperioder)
        return behandleSøknad(søknad, aktivitetsloggMedArbeidsgiverkontekst, arbeidsgivere, infotrygdhistorikk)
    }

    private fun behandleSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        return behandleSøknadSomKorrigering(søknad, aktivitetslogg)
            ?: behandleSøknadSomFørstegangs(søknad, aktivitetslogg, arbeidsgivere, infotrygdhistorikk)
    }

    private fun behandleSøknadSomKorrigering(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ): Revurderingseventyr? {
        val noenHarHåndtert = énHåndtert(søknad) { håndterKorrigertSøknad(søknad, aktivitetslogg) } ?: return null
        if (!søknad.delvisOverlappende) return noenHarHåndtert
        // oppretter en forkastet vedtaksperiode hvis det er delvis overlapp
        opprettForkastetVedtaksperiode(søknad, aktivitetslogg)
        return noenHarHåndtert
    }

    private fun opprettForkastetVedtaksperiode(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        val vedtaksperiode = søknad.lagVedtaksperiode(aktivitetslogg, person, this, regelverkslogg)
        registrerForkastetVedtaksperiode(vedtaksperiode, søknad, aktivitetslogg)
    }

    private fun behandleSøknadSomFørstegangs(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        val vedtaksperiode = søknad.lagVedtaksperiode(aktivitetslogg, person, this, regelverkslogg)
        registrerNyVedtaksperiode(vedtaksperiode)
        return vedtaksperiode.håndterSøknadFørsteGang(søknad, aktivitetslogg, arbeidsgivere, infotrygdhistorikk)
    }

    internal fun håndter(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Replayer inntektsmeldinger for vedtaksperiode ${replays.vedtaksperiodeId} og påfølgende som overlapper")
        val revurderingseventyr = håndterReplayAvInntektsmelding(replays.inntektsmeldinger, aktivitetsloggMedArbeidsgiverkontekst, replays.vedtaksperiodeId)
        håndter { it.håndter(replays, aktivitetsloggMedArbeidsgiverkontekst) }
        return revurderingseventyr
    }

    internal fun håndter(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val overstyring = énHåndtert(arbeidsgiveropplysninger) { håndter(arbeidsgiveropplysninger, aktivitetsloggMedArbeidsgiverkontekst, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }
        if (overstyring != null) return overstyring

        person.emitArbeidsgiveropplysningerIkkeHåndtert(arbeidsgiveropplysninger.metadata.meldingsreferanseId, organisasjonsnummer)

        val funksjonellFeil = when (vedtaksperioder.aktiv(arbeidsgiveropplysninger.vedtaksperiodeId)) {
            true -> `Arbeidsgiveropplysninger for periode som allerede har opplysninger`
            false -> `Arbeidsgiveropplysninger for forkastet periode`
        }
        aktivitetsloggMedArbeidsgiverkontekst.funksjonellFeil(funksjonellFeil)
        return null
    }

    internal fun håndter(arbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val overstyring = énHåndtert(arbeidsgiveropplysninger) { håndter(arbeidsgiveropplysninger, aktivitetsloggMedArbeidsgiverkontekst, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }
        if (overstyring != null) return overstyring
        person.emitArbeidsgiveropplysningerIkkeHåndtert(arbeidsgiveropplysninger.metadata.meldingsreferanseId, organisasjonsnummer)
        return null
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, skalBehandleRefusjonsopplysningene: Boolean = true): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val dager = inntektsmelding.dager()

        // 1. nullstille egenmeldingsdager fra søknader som tilhører samme arbeidsgiverperid
        val egenmeldingsoverstyring = dager.førsteOverlappendeVedtaksperiode(vedtaksperioder)?.nullstillEgenmeldingsdagerIArbeidsgiverperiode(inntektsmelding, aktivitetslogg, null)?.tidligsteEventyr()

        // 2. starter håndtering av inntektsmelding på vegne av alle mulige perioder
        val dagoverstyring = håndterDagerFraInntektsmelding(dager, aktivitetsloggMedArbeidsgiverkontekst)

        // 3. starter håndtering av refusjonsopplysninger på vegne av alle mulige perioder
        val refusjonsoverstyring = if (skalBehandleRefusjonsopplysningene)
            håndterRefusjonsopplysninger(inntektsmelding, inntektsmeldingRefusjon(inntektsmelding.metadata.meldingsreferanseId), aktivitetsloggMedArbeidsgiverkontekst, inntektsmelding.refusjonsservitør)
        else null

        // 4. håndterer inntekten fra inntektsmeldingen
        val inntektoverstyring = vedtaksperioder.firstNotNullOfOrNull {
            it.håndterInntektFraInntektsmelding(inntektsmelding, aktivitetsloggMedArbeidsgiverkontekst, inntektshistorikk)
        }

        // 5. ferdigstiller håndtering av inntektsmelding
        inntektsmelding.ferdigstill(aktivitetsloggMedArbeidsgiverkontekst, person, forkastede.perioder(), sykmeldingsperioder)

        // 6. igangsetter
        val tidligsteOverstyring = listOfNotNull(egenmeldingsoverstyring, inntektoverstyring, dagoverstyring, refusjonsoverstyring).tidligsteEventyr()
        // hvis tidligsteOverstyring er null så er verken egenmeldingsdager, dager, refusjon eller inntekt håndtert
        return tidligsteOverstyring
    }

    private fun håndterReplayAvInntektsmelding(inntektsmeldinger: List<Inntektsmelding>, aktivitetslogg: IAktivitetslogg, vedtaksperiodeIdForReplay: UUID): Revurderingseventyr? {
        return vedtaksperioder.firstNotNullOfOrNull { it.håndterReplayAvInntektsmelding(vedtaksperiodeIdForReplay, inntektsmeldinger, aktivitetslogg) }
    }

    private fun håndterDagerFraInntektsmelding(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        håndter { it.håndter(dager, aktivitetslogg) }
        return dager.revurderingseventyr()
    }

    internal fun refusjonstidslinje(vedtaksperiode: Vedtaksperiode): Beløpstidslinje {
        val startdatoPåSammenhengendeVedtaksperioder = startdatoPåSammenhengendeVedtaksperioder(vedtaksperiode)
        return ubrukteRefusjonsopplysninger.servér(startdatoPåSammenhengendeVedtaksperioder, vedtaksperiode.periode)
    }

    internal fun inntektsmeldingFerdigbehandlet(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Inntektsmelding ferdigbehandlet")
        håndter { it.inntektsmeldingFerdigbehandlet(hendelse, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndterHistorikkFraInfotrygd(
        hendelse: Utbetalingshistorikk,
        aktivitetslogg: IAktivitetslogg
    ) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndterHistorikkFraInfotrygd(hendelse, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndter(
        ytelser: Ytelser,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndter(ytelser, aktivitetsloggMedArbeidsgiverkontekst, infotrygdhistorikk) }
    }

    internal fun håndter(utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetalingsavgjørelse, aktivitetsloggMedArbeidsgiverkontekst) }
        håndter {
            it.håndter(utbetalingsavgjørelse, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun lagreInntektFraAOrdningen(
        meldingsreferanseId: MeldingsreferanseId,
        skjæringstidspunkt: LocalDate,
        omregnetÅrsinntekt: Inntekt
    ) {
        val im = Inntektsmeldinginntekt(
            id = UUID.randomUUID(),
            inntektsdata = Inntektsdata(
                hendelseId = meldingsreferanseId,
                dato = skjæringstidspunkt,
                beløp = omregnetÅrsinntekt,
                tidsstempel = LocalDateTime.now()
            ),
            kilde = Inntektsmeldinginntekt.Kilde.AOrdningen
        )

        inntektshistorikk.leggTil(im)
    }

    internal fun håndter(
        sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver,
        aktivitetslogg: IAktivitetslogg
    ) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        énHarHåndtert(sykepengegrunnlagForArbeidsgiver) {
            håndter(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter {
            it.håndter(vilkårsgrunnlag, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun håndter(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(simulering) }
        håndter {
            it.håndter(simulering, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun håndter(utbetalingHendelse: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        feriepengeutbetalinger.forEach { it.håndter(utbetalingHendelse, aktivitetsloggMedArbeidsgiverkontekst, organisasjonsnummer, person) }
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetalingHendelse, aktivitetsloggMedArbeidsgiverkontekst) }
        håndter {
            it.håndter(utbetalingHendelse, aktivitetsloggMedArbeidsgiverkontekst)
        }
        person.gjenopptaBehandling(aktivitetsloggMedArbeidsgiverkontekst)
    }

    // gammel annulleringsfunksjonalitet. Slettes når ny er igang
    internal fun nyAnnullering(
        hendelse: AnnullerUtbetaling,
        aktivitetslogg: IAktivitetslogg,
        utbetalingSomSkalAnnulleres: Utbetaling
    ): Utbetaling? {
        val annullering =
            utbetalingSomSkalAnnulleres.annuller(hendelse, aktivitetslogg, utbetalinger.toList()) ?: return null
        nyUtbetaling(aktivitetslogg, annullering)
        annullering.håndter(hendelse, aktivitetslogg)
        looper { vedtaksperiode -> vedtaksperiode.nyAnnullering(aktivitetslogg) }
        return annullering
    }

    internal fun lagAnnulleringsutbetaling(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, utbetalingSomSkalAnnulleres: Utbetaling): Utbetaling {
        val annullering = utbetalingSomSkalAnnulleres.lagAnnulleringsutbetaling(aktivitetslogg)
        checkNotNull(annullering) { "Klarte ikke lage annullering for utbetaling ${utbetalingSomSkalAnnulleres.id}. Det er litt rart, eller?" }
        val vurdering = lagAnnulleringsvurdering(hendelse)
        annullering.leggTilVurdering(vurdering)
        nyUtbetaling(aktivitetslogg, annullering)
        return annullering
    }

    private fun lagAnnulleringsvurdering(hendelse: Hendelse): Utbetaling.Vurdering {
        return when (hendelse) {
            is AnnullerUtbetaling -> hendelse.vurdering

            is AnmodningOmForkasting,
            is Arbeidsgiveropplysninger,
            is AvbruttSøknad,
            is KanIkkeBehandlesHer,
            is Utbetalingsgodkjenning,
            is VedtakFattet,
            is Dødsmelding,
            is FeriepengeutbetalingHendelse,
            is ForkastSykmeldingsperioder,
            is GjenopptaBehandling,
            is Grunnbeløpsregulering,
            is IdentOpphørt,
            is Infotrygdendring,
            is Inntektsendringer,
            is Inntektsmelding,
            is InntektsmeldingerReplay,
            is KorrigerteArbeidsgiveropplysninger,
            is Migrate,
            is MinimumSykdomsgradsvurderingMelding,
            is OverstyrArbeidsforhold,
            is OverstyrArbeidsgiveropplysninger,
            is SkjønnsmessigFastsettelse,
            is OverstyrTidslinje,
            is PersonPåminnelse,
            is Påminnelse,
            is Simulering,
            is SykepengegrunnlagForArbeidsgiver,
            is Sykmelding,
            is Søknad,
            is UtbetalingHendelse,
            is Utbetalingpåminnelse,
            is Utbetalingshistorikk,
            is UtbetalingshistorikkEtterInfotrygdendring,
            is UtbetalingshistorikkForFeriepenger,
            is Vilkårsgrunnlag,
            is AnnullerTomUtbetaling,
            is Ytelser -> Utbetaling.Vurdering(true, "Automatisk behandlet", "tbd@nav.no", LocalDateTime.now(), true)

        }
    }

    internal fun sisteAktiveUtbetalingMedSammeKorrelasjonsId(utbetaling: Utbetaling) = utbetaling.sisteAktiveMedSammeKorrelasjonsId(utbetalinger)

    internal fun finnAnnulleringskandidater(vedtaksperiodeSomForsøkesAnnullert: Vedtaksperiode): Set<Vedtaksperiode> {
        if (vedtaksperioder.none { it === vedtaksperiodeSomForsøkesAnnullert }) return emptySet()
        // senereVedtaksperioderMedSammeAgp() burde funnet alle vedtaksperioder uavhengig av utbetalingsrigg
        // MEN vi føler oss litt usikre på om denne klarer å finne alle perioder som ville linket seg på en annen utbetaling på tidligere utbetalingsrigg
        return senereVedtaksperioderMedFattetVedtakMedSammeAgp(vedtaksperiodeSomForsøkesAnnullert) + senereVedtaksperioderMedSammeUtbetaling(vedtaksperiodeSomForsøkesAnnullert)
    }

    // Utbetalinger gjort på "gammel rigg" (gruppert på samme agp)
    private fun senereVedtaksperioderMedSammeUtbetaling(vedtaksperiodeSomForsøkesAnnullert: Vedtaksperiode): Set<Vedtaksperiode> {
        return vedtaksperioder.medSammeUtbetaling(vedtaksperiodeSomForsøkesAnnullert).filter { it.periode.start >= vedtaksperiodeSomForsøkesAnnullert.periode.start }.toSet()
    }

    // Utbetalinger gjort på "ny rigg" (én utbetaling per vedtaksperiode)
    private fun senereVedtaksperioderMedFattetVedtakMedSammeAgp(vedtaksperiodeSomForsøkesAnnullert: Vedtaksperiode): Set<Vedtaksperiode> {
        val arbeidsgiverperiode = vedtaksperiodeSomForsøkesAnnullert.behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.periode() ?: return setOf(vedtaksperiodeSomForsøkesAnnullert)
        val vedtaksperioder = vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).filter { it.periode.start >= vedtaksperiodeSomForsøkesAnnullert.periode.start }
        return vedtaksperioder.filter { it.behandlinger.harVærtUtbetalt() }.toSet()
    }

    internal fun finnSisteVedtaksperiodeFørMedSammenhengendeUtbetaling(vedtaksperiode: Vedtaksperiode): Vedtaksperiode? {
        return vedtaksperioder.medSammeUtbetaling(vedtaksperiode).filterNot { it.periode.start >= vedtaksperiode.periode.start }.maxByOrNull { it.periode.start }
    }

    internal fun håndter(hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Håndterer annullering")
        if (Toggle.NyAnnulleringsløype.enabled || hendelse.saksbehandlerIdent in listOf("S161635", "A148751", "V149621", "H160235")) {
            val utbetalingId = hendelse.utbetalingId
            val vedtaksperiodeSomSkalAnnulleres = finnVedtaksperiodeForUtbetaling(utbetalingId) ?: error("Fant ikke vedtaksperiode for utbetaling $utbetalingId")
            val annulleringskandidater = finnAnnulleringskandidater(vedtaksperiodeSomSkalAnnulleres)
            return håndter { it.håndter(hendelse, aktivitetsloggMedArbeidsgiverkontekst, annulleringskandidater.toList()) }.tidligsteEventyr()
        } else {
            return håndter { it.håndter(hendelse, aktivitetsloggMedArbeidsgiverkontekst, vedtaksperioder.toList()) }.tidligsteEventyr()
        }
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(påminnelse, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndter(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return énHåndtert(påminnelse) { håndter(it, aktivitetsloggMedArbeidsgiverkontekst) }
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
        val builder = UtbetalingsdagerBuilder(sykdomshistorikk.sykdomstidslinje(), utbetalingstidslinje)
        person.utbetalingUtbetalt(
            PersonObserver.UtbetalingUtbetaltEvent(
                yrkesaktivitetssporing = yrkesaktivitetssporing,
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
                arbeidsgiverOppdrag = PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(
                    arbeidsgiverOppdrag
                ),
                personOppdrag = PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = builder.result(),
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
        val builder = UtbetalingsdagerBuilder(sykdomshistorikk.sykdomstidslinje(), utbetalingstidslinje)
        person.utbetalingUtenUtbetaling(
            PersonObserver.UtbetalingUtbetaltEvent(
                yrkesaktivitetssporing = yrkesaktivitetssporing,
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
                arbeidsgiverOppdrag = PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(
                    arbeidsgiverOppdrag
                ),
                personOppdrag = PersonObserver.UtbetalingUtbetaltEvent.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = builder.result(),
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
        forrigeTilstand: Utbetalingstatus,
        nesteTilstand: Utbetalingstatus,
        korrelasjonsId: UUID
    ) {
        person.utbetalingEndret(
            PersonObserver.UtbetalingEndretEvent(
                yrkesaktivitetssporing = yrkesaktivitetssporing,
                utbetalingId = id,
                type = type.name,
                forrigeStatus = forrigeTilstand.name,
                gjeldendeStatus = nesteTilstand.name,
                arbeidsgiverOppdrag = OppdragEventDetaljer.mapOppdrag(arbeidsgiverOppdrag),
                personOppdrag = OppdragEventDetaljer.mapOppdrag(personOppdrag),
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
        personFagsystemId: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String,
        arbeidsgiverFagsystemId: String
    ) {
        person.annullert(
            PersonObserver.UtbetalingAnnullertEvent(
                yrkesaktivitetssporing = yrkesaktivitetssporing,
                korrelasjonsId = korrelasjonsId,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                utbetalingId = id,
                fom = periode.start,
                tom = periode.endInclusive,
                annullertAvSaksbehandler = godkjenttidspunkt,
                saksbehandlerEpost = saksbehandlerEpost,
                saksbehandlerIdent = saksbehandlerIdent
            )
        )
    }

    internal fun håndter(hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return håndter {
            it.håndter(hendelse, aktivitetsloggMedArbeidsgiverkontekst)
        }.tidligsteEventyr()
    }

    private fun håndter(overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return vedtaksperioder.firstNotNullOfOrNull { it.håndter(overstyrInntektsgrunnlag, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    private fun håndter(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        var revurderingseventyr: Revurderingseventyr? = null
        énHarHåndtert(overstyrArbeidsgiveropplysninger) {
            revurderingseventyr = håndter(it, aktivitetsloggMedArbeidsgiverkontekst)
            revurderingseventyr != null
        }
        return revurderingseventyr
    }

    internal fun håndterRefusjonsopplysninger(hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val revurderingseventyr = håndter {
            it.håndterRefusjon(hendelse, dokumentsporing, aktivitetsloggMedArbeidsgiverkontekst, servitør)
        }.tidligsteEventyr()
        servitør.servér(ubrukteRefusjonsopplysninger, aktivitetsloggMedArbeidsgiverkontekst)
        return revurderingseventyr
    }

    internal fun oppdaterSykdom(meldingsreferanseId: MeldingsreferanseId, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        return sykdomshistorikk.håndter(meldingsreferanseId, sykdomstidslinje)
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!sykdomshistorikk.harSykdom()) return Sykdomstidslinje()
        return sykdomshistorikk.sykdomstidslinje()
    }

    private fun arbeidsgiverperiodeFor(): List<Arbeidsgiverperioderesultat> {
        val teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        val arbeidsgiverperiodeberegner = Arbeidsgiverperiodeberegner(teller)
        return arbeidsgiverperiodeberegner.resultat(
            sykdomstidslinje = sykdomstidslinjeHensyntattEgenmeldinger(),
            infotrygdBetalteDager = person.infotrygdhistorikk.betaltePerioder(organisasjonsnummer),
            infotrygdFerieperioder = person.infotrygdhistorikk.friperioder()
        )
    }

    internal fun beregnArbeidsgiverperiode() = { vedtaksperiode: Periode ->
        when (yrkesaktivitetssporing) {
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> arbeidsgiverperiodeFor()
                .finn(vedtaksperiode)
                ?.arbeidsgiverperiode
                ?.grupperSammenhengendePerioder()
                ?: emptyList()

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans,
            Behandlingsporing.Yrkesaktivitet.Selvstendig,
            Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker,
            Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma,
            Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> emptyList()
        }
    }

    internal fun arbeidsgiverperiode(periode: Periode): Arbeidsgiverperiode? {
        val arbeidsgiverperioder = arbeidsgiverperiodeFor()
        return arbeidsgiverperioder.finn(periode)?.somArbeidsgiverperiode()
    }

    private fun sykdomstidslinjeHensyntattEgenmeldinger(): Sykdomstidslinje {
        val egenmeldingsperioder = vedtaksperioder.egenmeldingsperioder()
        if (egenmeldingsperioder.isEmpty()) return sykdomstidslinje()

        val tøyseteKilde = Hendelseskilde(Søknad::class, MeldingsreferanseId(UUID.randomUUID()), LocalDateTime.now())
        val egenmeldingstidslinje = egenmeldingsperioder
            .map { Sykdomstidslinje.arbeidsgiverdager(it.start, it.endInclusive, HundreProsent, tøyseteKilde) }
            .merge()

        return sykdomstidslinje().merge(egenmeldingstidslinje, bareNyeDager)
    }

    /**
     * Finner alle vedtaksperioder som tilstøter vedtaksperioden
     * @param vedtaksperiode Perioden vi skal finne alle sammenhengende perioder for. Vi henter alle perioder som
     * tilstøter både foran og bak.
     */
    internal fun finnSammenhengendeVedtaksperioder(vedtaksperiode: Vedtaksperiode): List<Vedtaksperiode> {
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

    internal fun startdatoPåSammenhengendeVedtaksperioder(vedtaksperiode: Vedtaksperiode) =
        finnSammenhengendeVedtaksperioder(vedtaksperiode).first().periode.start

    internal fun lagreTidsnærInntektsmelding(
        skjæringstidspunkt: LocalDate,
        orgnummer: String,
        arbeidsgiverinntekt: ArbeidstakerFaktaavklartInntekt,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean
    ) {
        if (this.organisasjonsnummer != orgnummer) return
        setOfNotNull(finnFørsteFraværsdag(skjæringstidspunkt), skjæringstidspunkt).forEach { dato ->
            arbeidsgiverinntekt.kopierTidsnærOpplysning(dato, aktivitetslogg, nyArbeidsgiverperiode, inntektshistorikk)
        }
    }

    private fun søppelbøtte(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>
    ): List<VedtaksperiodeForkastetEventBuilder> {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val perioder: List<Pair<Vedtaksperiode, VedtaksperiodeForkastetEventBuilder>> = vedtaksperioderSomSkalForkastes
            .filter { it.arbeidsgiver === this }
            .map { vedtaksperiode ->
                vedtaksperiode to vedtaksperiode.utførForkasting(hendelse, aktivitetsloggMedArbeidsgiverkontekst)
            }

        vedtaksperioder.removeAll(perioder.map { it.first })
        forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it.first, organisasjonsnummer, it.first.periode) })
        sykdomshistorikk.fjernDager(perioder.map { it.first.periode })
        return perioder.map { it.second }
    }

    private fun registrerNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperioder.sort()
    }

    private fun registrerForkastetVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.info("Oppretter forkastet vedtaksperiode ettersom Søknad inneholder errors")
        val vedtaksperiodeForkastetEventBuilder = vedtaksperiode.utførForkasting(hendelse, aktivitetslogg)
        vedtaksperiodeForkastetEventBuilder.buildAndEmit()
        forkastede.add(ForkastetVedtaksperiode(vedtaksperiode, organisasjonsnummer, vedtaksperiode.periode))
    }

    internal fun finnVedtaksperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erVedtaksperiodeRettFør(vedtaksperiode)
        }

    internal fun finnVedtaksperiodeRettEtter(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            vedtaksperiode.erVedtaksperiodeRettFør(other)
        }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst(
            "Arbeidsgiver", mapOf(
            "organisasjonsnummer" to organisasjonsnummer,
            "yrkesaktivitetstype" to when (yrkesaktivitetssporing) {
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> "ARBEIDSTAKER"
                Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
                Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
                Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> "SELVSTENDIG_DAGMAMMA"
                Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> "SELVSTENDIG_FISKER"
                Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> "SELVSTENDIG_JORDBRUKER"
            }
        )
        )
    }

    internal fun lås(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().lås(periode)
    }

    internal fun låsOpp(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().låsOpp(periode)
    }

    internal fun bekreftErLåst(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().bekreftErLåst(periode)
    }

    internal fun bekreftErÅpen(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().bekreftErÅpen(periode)
    }

    private fun sjekkForUgyldigeSituasjoner(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        utbetalinger.kunEnIkkeUtbetalt()
        vedtaksperioder.validerTilstand(hendelse, aktivitetslogg)
    }

    private fun finnFørsteFraværsdag(skjæringstidspunkt: LocalDate): LocalDate? {
        return vedtaksperioder
            .filter(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .asReversed()
            .firstNotNullOfOrNull { it.førsteFraværsdag }
    }

    internal fun finnFørsteFraværsdag(vedtaksperiode: Periode): LocalDate? {
        return Skjæringstidspunkt(sykdomstidslinje()).sisteOrNull(vedtaksperiode)
    }

    private fun <R> håndter(håndterer: (Vedtaksperiode) -> R?): List<R> {
        return looper { håndterer(it) }
    }

    private fun <Hendelsetype : Hendelse> énHarHåndtert(
        hendelse: Hendelsetype,
        håndterer: Vedtaksperiode.(Hendelsetype) -> Boolean
    ): Boolean {
        var håndtert = false
        looper { håndtert = håndtert || håndterer(it, hendelse) }
        return håndtert
    }

    private fun <R, Hendelsetype : Hendelse> énHåndtert(
        hendelse: Hendelsetype,
        håndterer: Vedtaksperiode.(Hendelsetype) -> R?
    ): R? {
        var result: R? = null
        looper {
            if (result == null) result = håndterer(it, hendelse)
        }
        return result
    }

    // støtter å loope over vedtaksperioder som modifiseres pga. forkasting.
    // dvs. vi stopper å iterere så snart listen har endret seg
    private fun <R> looper(handler: (Vedtaksperiode) -> R?) = buildList<R> {
        val size = vedtaksperioder.size
        var neste = 0
        while (size == vedtaksperioder.size && neste < size) {
            handler(vedtaksperioder[neste])?.also {
                add(it)
            }
            neste += 1
        }
    }

    internal fun kanForkastes(vedtaksperiode: Vedtaksperiode) =
        vedtaksperiode.tillaterBehandlingForkasting(vedtaksperioder.toList())

    fun vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode: Periode): List<Vedtaksperiode> {
        return vedtaksperioder.filter(SAMME_ARBEIDSGIVERPERIODE(this, arbeidsgiverperiode))
    }

    internal fun vedtaksperioderEtter(dato: LocalDate) = vedtaksperioder.filter { it.slutterEtter(dato) }
    internal fun dto(nestemann: Vedtaksperiode?): ArbeidsgiverUtDto {
        val vedtaksperioderDto = vedtaksperioder.map { it.dto(nestemann) }
        val refusjonsopplysningerPåSisteBehandling = vedtaksperioder.lastOrNull()?.let { sisteVedtaksperiode ->
            val sisteBehandlingId = vedtaksperioderDto.last().behandlinger.behandlinger.last().id
            val sisteRefusjonstidslinje =
                sisteVedtaksperiode.hensyntattUbrukteRefusjonsopplysninger(ubrukteRefusjonsopplysninger)
            sisteBehandlingId to sisteRefusjonstidslinje
        }
        return ArbeidsgiverUtDto(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            yrkesaktivitetstype = when (yrkesaktivitetssporing) {
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> ArbeidsgiverUtDto.Yrkesaktivitetstype.ARBEIDSLEDIG
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> ArbeidsgiverUtDto.Yrkesaktivitetstype.ARBEIDSTAKER
                Behandlingsporing.Yrkesaktivitet.Frilans -> ArbeidsgiverUtDto.Yrkesaktivitetstype.FRILANS
                Behandlingsporing.Yrkesaktivitet.Selvstendig -> ArbeidsgiverUtDto.Yrkesaktivitetstype.SELVSTENDIG
                Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> ArbeidsgiverUtDto.Yrkesaktivitetstype.SELVSTENDIG_FISKER
                Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> ArbeidsgiverUtDto.Yrkesaktivitetstype.SELVSTENDIG_JORDBRUKER
                Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> ArbeidsgiverUtDto.Yrkesaktivitetstype.SELVSTENDIG_DAGMAMMA
            },
            inntektshistorikk = inntektshistorikk.dto(),
            sykdomshistorikk = sykdomshistorikk.dto(),
            sykmeldingsperioder = sykmeldingsperioder.dto(),
            vedtaksperioder = vedtaksperioderDto,
            forkastede = forkastede.map { it.dto() },
            utbetalinger = utbetalinger.map { it.dto() },
            feriepengeutbetalinger = feriepengeutbetalinger.map { it.dto() },
            ubrukteRefusjonsopplysninger = UbrukteRefusjonsopplysningerUtDto(
                ubrukteRefusjonsopplysninger = ubrukteRefusjonsopplysninger.dto(),
                sisteRefusjonstidslinje = refusjonsopplysningerPåSisteBehandling?.second?.dto(),
                sisteBehandlingId = refusjonsopplysningerPåSisteBehandling?.first
            )
        )
    }

    internal fun trengerArbeidsgiveropplysninger(periode: Periode, trengerArbeidsgiveropplysninger: (historiskeSykmeldingsperioder: List<Periode>) -> Unit) {
        forkastede.trengerArbeidsgiveropplysninger(periode, vedtaksperioder.map { it.periode }, trengerArbeidsgiveropplysninger)
    }
}
