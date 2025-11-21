package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.UbrukteRefusjonsopplysningerUtDto
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.feriepenger.Feriepengeberegner
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Mottaker
import no.nav.helse.feriepenger.Feriepengegrunnlagstidslinje
import no.nav.helse.feriepenger.Feriepengeutbetaling
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsavgjørelse
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Frilans
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Jordbruker
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Selvstendig
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.hendelser.ForkastSykmeldingsperioder
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrInntektsgrunnlag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.mursteinsperioder
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.erLik
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrArbeidsgiveropplysninger
import no.nav.helse.person.EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.blokkererBehandlingAv
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.perioder
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.trengerArbeidsgiveropplysninger
import no.nav.helse.person.Vedtaksperiode.Companion.AUU_SOM_VIL_UTBETALES
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.SAMME_ARBEIDSGIVERPERIODE
import no.nav.helse.person.Vedtaksperiode.Companion.aktiv
import no.nav.helse.person.Vedtaksperiode.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.person.Vedtaksperiode.Companion.checkBareEnPeriodeTilGodkjenningSamtidig
import no.nav.helse.person.Vedtaksperiode.Companion.egenmeldingsperioder
import no.nav.helse.person.Vedtaksperiode.Companion.harFaktaavklartInntekt
import no.nav.helse.person.Vedtaksperiode.Companion.igangsettOverstyring
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
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsgiveropplysninger for forkastet periode`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsgiveropplysninger for periode som allerede har opplysninger`
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.person.view.ArbeidsgiverView
import no.nav.helse.sykdomstidslinje.Dag.Companion.bareNyeDager
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkter
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.kunEnIkkeUtbetalt
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.tillaterOpprettelseAvUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.validerNyUtbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingEventBus
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeberegner
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.PeriodeUtenNavAnsvar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Ventetidberegner
import no.nav.helse.økonomi.Prosentdel.Companion.HundreProsent

internal class Yrkesaktivitet private constructor(
    private val person: Person,
    private val id: UUID,
    val yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet,
    private val inntektshistorikk: Inntektshistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val sykmeldingsperioder: Sykmeldingsperioder,
    perioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar>,
    private val vedtaksperioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<ForkastetVedtaksperiode>,
    private val _utbetalinger: MutableList<Utbetaling>,
    private val feriepengeutbetalinger: MutableList<Feriepengeutbetaling>,
    private val ubrukteRefusjonsopplysninger: Refusjonsservitør,
    private val regelverkslogg: Regelverkslogg
) : Aktivitetskontekst {
    internal constructor(person: Person, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, regelverkslogg: Regelverkslogg) : this(
        person = person,
        id = UUID.randomUUID(),
        yrkesaktivitetstype = yrkesaktivitetssporing,
        inntektshistorikk = Inntektshistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        sykmeldingsperioder = Sykmeldingsperioder(),
        perioderUtenNavAnsvar = emptyList(),
        vedtaksperioder = mutableListOf(),
        forkastede = mutableListOf(),
        _utbetalinger = mutableListOf(),
        feriepengeutbetalinger = mutableListOf(),
        ubrukteRefusjonsopplysninger = Refusjonsservitør(),
        regelverkslogg = regelverkslogg
    )

    val organisasjonsnummer = when (yrkesaktivitetstype) {
        Arbeidsledig -> "ARBEIDSLEDIG"
        is Arbeidstaker -> yrkesaktivitetstype.organisasjonsnummer
        Frilans -> "FRILANS"
        Selvstendig -> "SELVSTENDIG"
        Jordbruker -> "JORDBRUKER"
    }

    internal var perioderUtenNavAnsvar: List<PeriodeUtenNavAnsvar> = perioderUtenNavAnsvar
        private set

    internal val utbetalinger get() = _utbetalinger.toList()

    fun view(): ArbeidsgiverView = ArbeidsgiverView(
        organisasjonsnummer = organisasjonsnummer,
        yrkesaktivitetssporing = yrkesaktivitetstype,
        sykdomshistorikk = sykdomshistorikk.view(),
        utbetalinger = _utbetalinger.map { it.view },
        inntektshistorikk = inntektshistorikk.view(),
        sykmeldingsperioder = sykmeldingsperioder.view(),
        ubrukteRefusjonsopplysninger = ubrukteRefusjonsopplysninger.view(),
        feriepengeutbetalinger = feriepengeutbetalinger.map { it.view() },
        aktiveVedtaksperioder = vedtaksperioder.map { it.view() },
        forkastetVedtaksperioder = forkastede.map { it.view() }
    )

    internal companion object {
        internal fun List<Yrkesaktivitet>.finn(behandlingsporing: Behandlingsporing) =
            find { it.yrkesaktivitetstype.erLik(behandlingsporing) }

        internal fun List<Yrkesaktivitet>.tidligsteDato(): LocalDate {
            return mapNotNull { it.sykdomstidslinje().periode()?.start }.minOrNull() ?: LocalDate.now()
        }

        internal fun List<Yrkesaktivitet>.igangsettOverstyring(
            eventBus: EventBus,
            revurdering: Revurderingseventyr,
            aktivitetslogg: IAktivitetslogg
        ) {
            flatMap { it.vedtaksperioder }.igangsettOverstyring(eventBus, revurdering, aktivitetslogg)
        }

        internal fun List<Yrkesaktivitet>.venter() =
            flatMap { yrkesaktivitet -> yrkesaktivitet.vedtaksperioder.venter() }

        internal fun List<Yrkesaktivitet>.beregnSkjæringstidspunkt(infotrygdhistorikk: Infotrygdhistorikk): Skjæringstidspunkter =
                infotrygdhistorikk.skjæringstidspunkt(map(Yrkesaktivitet::sykdomstidslinje))

        internal fun List<Yrkesaktivitet>.aktiveSkjæringstidspunkter(): Set<LocalDate> {
            return flatMap { it.vedtaksperioder }.aktiveSkjæringstidspunkter()
        }

        internal fun List<Yrkesaktivitet>.håndterOverstyrInntektsgrunnlag(
            overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag,
            aktivitetslogg: IAktivitetslogg
        ) =
            firstNotNullOfOrNull { it.håndterOverstyrInntektsgrunnlag(overstyrInntektsgrunnlag, aktivitetslogg) }

        internal fun List<Yrkesaktivitet>.håndterOverstyringAvInntekt(
            eventBus: EventBus,
            overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger,
            aktivitetslogg: IAktivitetslogg
        ): Revurderingseventyr? {
            val vilkårsgrunnlageventyr = firstNotNullOfOrNull { it.håndterOverstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger, aktivitetslogg) }

            if (vilkårsgrunnlageventyr != null) {
                overstyrArbeidsgiveropplysninger.arbeidsgiveropplysninger.mapNotNull { korrigering ->
                    finn(Arbeidstaker(korrigering.organisasjonsnummer))?.håndterKorrigertInntekt(eventBus, overstyrArbeidsgiveropplysninger, korrigering.korrigertInntekt, aktivitetslogg)
                }
            }

            return vilkårsgrunnlageventyr
        }

        internal fun List<Yrkesaktivitet>.håndterOverstyringAvRefusjon(
            eventBus: EventBus,
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
                arbeidsgiver.håndterRefusjonsopplysninger(eventBus, hendelse, overstyrArbeidsgiveropplysninger(hendelse.metadata.meldingsreferanseId), aktivitetslogg, servitør)
            }
            return revurderingseventyr.tidligsteEventyr()
        }

        internal fun Iterable<Yrkesaktivitet>.nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) =
            mapNotNull { it.vedtaksperioder.nåværendeVedtaksperiode(filter) }

        internal fun Iterable<Yrkesaktivitet>.vedtaksperioder(filter: VedtaksperiodeFilter) =
            map { it.vedtaksperioder.filter(filter) }.flatten()

        internal fun Iterable<Yrkesaktivitet>.mursteinsperioder(utgangspunkt: Vedtaksperiode) = this
            .flatMap { it.vedtaksperioder }
            .mursteinsperioder(utgangspunkt.periode, Vedtaksperiode::periode)

        internal fun List<Yrkesaktivitet>.validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            checkBareEnPeriodeTilGodkjenningSamtidig()
            forEach { it.sjekkForUgyldigeSituasjoner(hendelse, aktivitetslogg) }
        }

        internal fun Iterable<Yrkesaktivitet>.beregnFeriepengerForAlleArbeidsgivere(
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

        internal fun Iterable<Yrkesaktivitet>.avventerSøknad(periode: Periode) = this
            .any { it.sykmeldingsperioder.avventerSøknad(periode) }

        internal fun Iterable<Yrkesaktivitet>.fjernSykmeldingsperiode(periode: Periode) = this
            .forEach { it.sykmeldingsperioder.fjern(periode) }

        private fun Iterable<Yrkesaktivitet>.harPågåeneAnnullering() =
            any { it._utbetalinger.any { utbetaling -> utbetaling.erAnnulleringInFlight() } }

        private fun Iterable<Yrkesaktivitet>.førsteAuuSomVilUtbetales() =
            nåværendeVedtaksperioder(AUU_SOM_VIL_UTBETALES).minOrNull()

        internal fun Iterable<Yrkesaktivitet>.gjenopptaBehandling(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            if (harPågåeneAnnullering()) return aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
            val periodeSomSkalGjenopptas = periodeSomSkalGjenopptas() ?: return
            periodeSomSkalGjenopptas.gjenopptaBehandling(eventBus, hendelse, aktivitetslogg)
        }

        internal fun Iterable<Yrkesaktivitet>.nestemann() =
            periodeSomSkalGjenopptas() ?: førsteAuuSomVilUtbetales()

        private fun Iterable<Yrkesaktivitet>.periodeSomSkalGjenopptas() =
            flatMap { it.vedtaksperioder }.nestePeriodeSomSkalGjenopptas()

        private fun Iterable<Yrkesaktivitet>.checkBareEnPeriodeTilGodkjenningSamtidig() =
            flatMap { it.vedtaksperioder }.checkBareEnPeriodeTilGodkjenningSamtidig()

        internal fun søppelbøtte(
            eventBus: EventBus,
            yrkesaktiviteter: List<Yrkesaktivitet>,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg,
            vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>
        ) {
            yrkesaktiviteter.flatMap { it.søppelbøtte(eventBus, hendelse, aktivitetslogg, vedtaksperioderSomSkalForkastes) }.forEach { it.buildAndEmit(eventBus) }
        }

        internal fun gjenopprett(
            person: Person,
            dto: ArbeidsgiverInnDto,
            regelverkslogg: Regelverkslogg,
            grunnlagsdata: Map<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>
        ): Yrkesaktivitet {
            val vedtaksperioder = mutableListOf<Vedtaksperiode>()
            val forkastede = mutableListOf<ForkastetVedtaksperiode>()
            val utbetalinger = dto.utbetalinger.fold(emptyList<Utbetaling>()) { result, utbetaling ->
                result.plusElement(Utbetaling.gjenopprett(utbetaling, result))
            }
            val yrkesaktivitetType = Yrkesaktivitet(
                person = person,
                id = dto.id,
                yrkesaktivitetstype = when (dto.yrkesaktivitetstype) {
                    YrkesaktivitetstypeDto.ARBEIDSLEDIG -> Arbeidsledig
                    YrkesaktivitetstypeDto.ARBEIDSTAKER -> Arbeidstaker(dto.organisasjonsnummer)
                    YrkesaktivitetstypeDto.FRILANS -> Frilans
                    YrkesaktivitetstypeDto.SELVSTENDIG -> Selvstendig
                    YrkesaktivitetstypeDto.JORDBRUKER -> Jordbruker
                },
                inntektshistorikk = Inntektshistorikk.gjenopprett(dto.inntektshistorikk),
                sykdomshistorikk = Sykdomshistorikk.gjenopprett(dto.sykdomshistorikk),
                sykmeldingsperioder = Sykmeldingsperioder.gjenopprett(dto.sykmeldingsperioder),
                perioderUtenNavAnsvar = dto.perioderUtenNavAnsvar.map { PeriodeUtenNavAnsvar.gjenopprett(it) },
                vedtaksperioder = vedtaksperioder,
                forkastede = forkastede,
                _utbetalinger = utbetalinger.toMutableList(),
                feriepengeutbetalinger = dto.feriepengeutbetalinger.map { Feriepengeutbetaling.gjenopprett(it) }
                    .toMutableList(),
                ubrukteRefusjonsopplysninger = Refusjonsservitør.gjenopprett(dto.ubrukteRefusjonsopplysninger),
                regelverkslogg = regelverkslogg
            )
            val utbetalingerMap = utbetalinger.associateBy(Utbetaling::id)
            vedtaksperioder.addAll(dto.vedtaksperioder.map {
                Vedtaksperiode.gjenopprett(
                    person,
                    yrkesaktivitetType,
                    it,
                    regelverkslogg,
                    grunnlagsdata,
                    utbetalingerMap
                )
            })
            forkastede.addAll(dto.forkastede.map {
                ForkastetVedtaksperiode.gjenopprett(
                    person,
                    yrkesaktivitetType,
                    it,
                    regelverkslogg,
                    grunnlagsdata,
                    utbetalingerMap
                )
            })
            return yrkesaktivitetType
        }
    }

    internal fun kanBeregneSykepengegrunnlag(skjæringstidspunkt: LocalDate, vedtaksperioder: List<Vedtaksperiode>): Boolean {
        return vedtaksperioder.harFaktaavklartInntekt() || (avklarInntektFraInntektshistorikk(skjæringstidspunkt, vedtaksperioder) != null)
    }

    internal fun avklarInntektFraInntektshistorikk(skjæringstidspunkt: LocalDate, vedtaksperioder: List<Vedtaksperiode>): ArbeidstakerFaktaavklartInntekt? {
        // finner inntektsmelding for en av første fraværsdagene.
        // håndterer det som en liste i tilfelle arbeidsgiveren har auu'er i forkant, og at inntekt kan ha blitt malplassert
        // (og at det er vrient å avgjøre én riktig første fraværsdag i forkant)
        val inntektsmeldinginntekt = vedtaksperioder.firstNotNullOfOrNull {
            val førsteFraværsdag = it.førsteFraværsdag
            inntektshistorikk.avklarInntektsgrunnlag(skjæringstidspunkt = skjæringstidspunkt, førsteFraværsdag = førsteFraværsdag)
        } ?: return null

        return ArbeidstakerFaktaavklartInntekt(
            id = inntektsmeldinginntekt.id,
            inntektsdata = inntektsmeldinginntekt.inntektsdata,
            inntektsopplysningskilde = when (inntektsmeldinginntekt.kilde) {
                Inntektsmeldinginntekt.Kilde.Arbeidsgiver -> Arbeidstakerinntektskilde.Arbeidsgiver
                Inntektsmeldinginntekt.Kilde.AOrdningen -> Arbeidstakerinntektskilde.AOrdningen(emptyList())
            }
        )
    }

    internal fun organisasjonsnummer() = organisasjonsnummer
    internal fun grunnlagForFeriepenger(): Feriepengegrunnlagstidslinje {
        val feriepengetidslinje = fun(oppdrag: Oppdrag, mottaker: Mottaker): Feriepengegrunnlagstidslinje {
            return Feriepengegrunnlagstidslinje.Builder().apply {
                oppdrag
                    .linjerUtenOpphør()
                    .forEach { linje ->
                        linje
                            .filterNot { it.erHelg() }
                            .forEach {
                                leggTilUtbetaling(it, organisasjonsnummer, mottaker, Feriepengegrunnlagsdag.Kilde.SPLEIS, linje.beløp)
                            }
                    }
            }.build()
        }
        return utbetalinger
            .aktive()
            .map { feriepengetidslinje(it.arbeidsgiverOppdrag, Mottaker.ARBEIDSGIVER) + feriepengetidslinje(it.personOppdrag, Mottaker.PERSON) }
            .fold(Feriepengegrunnlagstidslinje(emptyList()), Feriepengegrunnlagstidslinje::plus)
    }

    internal val EventBus.utbetalingEventBus get() =
        utbetalingEventBus(yrkesaktivitetstype, UtbetalingsdagerBuilder(sykdomshistorikk.sykdomstidslinje()))

    internal fun leggTilNyUtbetaling(
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        utbetaling: Utbetaling
    ) {
        _utbetalinger.validerNyUtbetaling(utbetaling)
        check(_utbetalinger.tillaterOpprettelseAvUtbetaling(utbetaling)) { "Har laget en overlappende utbetaling" }
        _utbetalinger.add(utbetaling)

        utbetaling.opprett(eventBus.utbetalingEventBus, aktivitetslogg)
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

    internal fun håndterSykmelding(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedYrkesaktivitetkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndterSykmelding(sykmelding) }

        when (yrkesaktivitetstype) {
            Arbeidsledig -> aktivitetsloggMedYrkesaktivitetkontekst.info("Lagrer _ikke_ sykmeldingsperiode ${sykmelding.periode()} ettersom det er en sykmelding som arbeidsledig.")
            is Arbeidstaker,
            Frilans,
            Selvstendig,
            Jordbruker -> sykmeldingsperioder.lagre(sykmelding, aktivitetsloggMedYrkesaktivitetkontekst)
        }
    }

    internal fun håndterAvbruttSøknad(avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        avbruttSøknad.avbryt(sykmeldingsperioder)
    }

    internal fun håndterForkastSykmeldingsperioder(forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        forkastSykmeldingsperioder.forkast(sykmeldingsperioder)
    }

    internal fun håndterAnmodningOmForkasting(eventBus: EventBus, anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedYrkesaktivitetkontekst = aktivitetslogg.kontekst(this)
        return énHåndtert(anmodningOmForkasting) {
            håndterAnmodningOmForkasting(eventBus, anmodningOmForkasting, aktivitetsloggMedYrkesaktivitetkontekst)
        }
    }

    internal fun vurderOmSøknadIkkeKanHåndteres(
        aktivitetslogg: IAktivitetslogg,
        nyPeriode: Periode,
        yrkesaktiviteter: List<Yrkesaktivitet>
    ) {
        // Sjekker først egen arbeidsgiver
        when (yrkesaktivitetstype) {
            is Arbeidstaker,
            Selvstendig -> {} // :)

            Jordbruker -> {
                if (Toggle.Jordbruker.disabled) return aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
            }

            Arbeidsledig,
            Frilans -> return aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_39)
        }
        if (forkastede.blokkererBehandlingAv(nyPeriode, organisasjonsnummer, aktivitetslogg)) return
        // Også alle etterpå
        yrkesaktiviteter.any { arbeidsgiver ->
            arbeidsgiver.forkastede.blokkererBehandlingAv(nyPeriode, organisasjonsnummer, aktivitetslogg)
        }
    }

    internal fun håndterSøknad(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        søknad.slettSykmeldingsperioderSomDekkes(sykmeldingsperioder)
        return behandleSøknad(eventBus, søknad, aktivitetsloggMedArbeidsgiverkontekst, yrkesaktiviteter, infotrygdhistorikk)
    }

    private fun behandleSøknad(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        return behandleSøknadSomKorrigering(eventBus, søknad, aktivitetslogg)
            ?: behandleSøknadSomFørstegangs(eventBus, søknad, aktivitetslogg, yrkesaktiviteter, infotrygdhistorikk)
    }

    private fun behandleSøknadSomKorrigering(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ): Revurderingseventyr? {
        val noenHarHåndtert = énHåndtert(søknad) { håndterKorrigertSøknad(eventBus, søknad, aktivitetslogg) } ?: return null
        if (!søknad.delvisOverlappende) return noenHarHåndtert
        // oppretter en forkastet vedtaksperiode hvis det er delvis overlapp
        opprettForkastetVedtaksperiode(eventBus, søknad, aktivitetslogg)
        return noenHarHåndtert
    }

    private fun opprettForkastetVedtaksperiode(eventBus: EventBus, søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        val vedtaksperiode = søknad.lagVedtaksperiode(eventBus, person, this, regelverkslogg)
        registrerForkastetVedtaksperiode(eventBus, vedtaksperiode, søknad, aktivitetslogg)
    }

    private fun behandleSøknadSomFørstegangs(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        val vedtaksperiode = søknad.lagVedtaksperiode(eventBus, person, this, regelverkslogg)
        registrerNyVedtaksperiode(vedtaksperiode)
        return vedtaksperiode.håndterSøknadFørsteGang(eventBus, søknad, aktivitetslogg, yrkesaktiviteter, infotrygdhistorikk)
    }

    internal fun håndterInntektsmeldingerReplay(eventBus: EventBus, replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Replayer inntektsmeldinger for vedtaksperiode ${replays.vedtaksperiodeId} og påfølgende som overlapper")
        val revurderingseventyr = håndterReplayAvInntektsmelding(eventBus, replays.inntektsmeldinger, aktivitetsloggMedArbeidsgiverkontekst, replays.vedtaksperiodeId)
        håndter { it.håndterInntektsmeldingerReplay(eventBus, replays, aktivitetsloggMedArbeidsgiverkontekst) }
        return revurderingseventyr
    }

    internal fun håndterArbeidsgiveropplysninger(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val overstyring = énHåndtert(arbeidsgiveropplysninger) { håndterArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedArbeidsgiverkontekst, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }
        if (overstyring != null) return overstyring

        eventBus.emitArbeidsgiveropplysningerIkkeHåndtert(arbeidsgiveropplysninger.metadata.meldingsreferanseId, organisasjonsnummer)

        val funksjonellFeil = when (vedtaksperioder.aktiv(arbeidsgiveropplysninger.vedtaksperiodeId)) {
            true -> `Arbeidsgiveropplysninger for periode som allerede har opplysninger`
            false -> `Arbeidsgiveropplysninger for forkastet periode`
        }
        aktivitetsloggMedArbeidsgiverkontekst.funksjonellFeil(funksjonellFeil)
        return null
    }

    internal fun håndterKorrigerteArbeidsgiveropplysninger(eventBus: EventBus, arbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val overstyring = énHåndtert(arbeidsgiveropplysninger) { håndterKorrigerteArbeidsgiveropplysninger(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedArbeidsgiverkontekst, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }
        if (overstyring != null) return overstyring
        eventBus.emitArbeidsgiveropplysningerIkkeHåndtert(arbeidsgiveropplysninger.metadata.meldingsreferanseId, organisasjonsnummer)
        return null
    }

    internal fun håndterInntektsmelding(eventBus: EventBus, inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val dager = inntektsmelding.dager()

        // 1. nullstille egenmeldingsdager fra søknader som tilhører samme arbeidsgiverperid
        val egenmeldingsoverstyring = dager.førsteOverlappendeVedtaksperiode(vedtaksperioder)?.nullstillEgenmeldingsdagerIArbeidsgiverperiode(eventBus, inntektsmelding, aktivitetslogg, null)?.tidligsteEventyr()

        // 2. starter håndtering av inntektsmelding på vegne av alle mulige perioder
        val dagoverstyring = håndterDagerFraInntektsmelding(eventBus, dager, aktivitetsloggMedArbeidsgiverkontekst)

        // 3. starter håndtering av refusjonsopplysninger på vegne av alle mulige perioder
        val refusjonsoverstyring = håndterRefusjonsopplysninger(eventBus, inntektsmelding, inntektsmeldingRefusjon(inntektsmelding.metadata.meldingsreferanseId), aktivitetsloggMedArbeidsgiverkontekst, inntektsmelding.refusjonsservitør)

        // 4. håndterer inntekten fra inntektsmeldingen
        val inntektoverstyring = vedtaksperioder.firstNotNullOfOrNull {
            it.håndterInntektFraInntektsmelding(eventBus, inntektsmelding, aktivitetsloggMedArbeidsgiverkontekst, inntektshistorikk)
        }

        val inntektPåPeriode = vedtaksperioder.firstNotNullOfOrNull {
            it.håndterInntektFraInntektsmeldingPåPerioden(eventBus, inntektsmelding, aktivitetslogg)
        }

        // 5. ferdigstiller håndtering av inntektsmelding
        inntektsmelding.ferdigstill(eventBus, aktivitetsloggMedArbeidsgiverkontekst, person, forkastede.perioder(), sykmeldingsperioder)

        // 6. igangsetter
        val tidligsteOverstyring = listOfNotNull(egenmeldingsoverstyring, inntektoverstyring, dagoverstyring, refusjonsoverstyring, inntektPåPeriode).tidligsteEventyr()
        // hvis tidligsteOverstyring er null så er verken egenmeldingsdager, dager, refusjon eller inntekt håndtert
        return tidligsteOverstyring
    }

    private fun håndterReplayAvInntektsmelding(eventBus: EventBus, inntektsmeldinger: List<Inntektsmelding>, aktivitetslogg: IAktivitetslogg, vedtaksperiodeIdForReplay: UUID): Revurderingseventyr? {
        return vedtaksperioder.firstNotNullOfOrNull { it.håndterReplayAvInntektsmelding(eventBus, vedtaksperiodeIdForReplay, inntektsmeldinger, aktivitetslogg) }
    }

    private fun håndterDagerFraInntektsmelding(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        håndter { it.håndterDagerFraInntektsmelding(eventBus, dager, aktivitetslogg) }
        return dager.revurderingseventyr()
    }

    internal fun refusjonstidslinje(vedtaksperiode: Vedtaksperiode): Beløpstidslinje {
        val startdatoPåSammenhengendeVedtaksperioder = startdatoPåSammenhengendeVedtaksperioder(vedtaksperiode)
        return ubrukteRefusjonsopplysninger.servér(startdatoPåSammenhengendeVedtaksperioder, vedtaksperiode.periode)
    }

    internal fun inntektsmeldingFerdigbehandlet(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Inntektsmelding ferdigbehandlet")
        håndter { it.inntektsmeldingFerdigbehandlet(eventBus, hendelse, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndterHistorikkFraInfotrygd(
        eventBus: EventBus,
        hendelse: Utbetalingshistorikk,
        aktivitetslogg: IAktivitetslogg
    ) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndterHistorikkFraInfotrygd(eventBus, hendelse, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndterYtelser(
        eventBus: EventBus,
        ytelser: Ytelser,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        val aktivitetsloggMedYrkesaktivitetkontekst = aktivitetslogg.kontekst(this)
        håndter { it.håndter(eventBus, ytelser, aktivitetsloggMedYrkesaktivitetkontekst, infotrygdhistorikk) }
    }

    internal fun håndterBehandlingsavgjørelse(eventBus: EventBus, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return énHåndtert(utbetalingsavgjørelse) {
            håndterUtbetalingsavgjørelse(eventBus, utbetalingsavgjørelse, aktivitetsloggMedArbeidsgiverkontekst)
        }
    }

    internal fun håndterVilkårsgrunnlag(eventBus: EventBus, vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedYrkesaktivitetkontekst = aktivitetslogg.kontekst(this)
        håndter {
            it.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetsloggMedYrkesaktivitetkontekst)
        }
    }

    internal fun håndterSimulering(eventBus: EventBus, simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedYrkesaktivitetkontekst = aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndterSimuleringHendelse(simulering) }
        håndter {
            it.håndterSimulering(eventBus, simulering, aktivitetsloggMedYrkesaktivitetkontekst)
        }
    }

    internal fun håndterFeriepengeutbetalingHendelse(eventBus: EventBus, utbetalingHendelse: FeriepengeutbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        feriepengeutbetalinger.forEach { it.håndter(eventBus, utbetalingHendelse, aktivitetsloggMedArbeidsgiverkontekst, organisasjonsnummer) }
    }

    internal fun håndterUtbetalingHendelse(eventBus: EventBus, utbetalingHendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        håndter {
            it.håndterUtbetalingHendelse(eventBus, utbetalingHendelse, aktivitetsloggMedArbeidsgiverkontekst)
        }
        person.gjenopptaBehandling(aktivitetsloggMedArbeidsgiverkontekst)
    }

    internal fun aktiveUtbetalingerForPeriode(vedtaksperiode: Periode) =
        utbetalinger.aktive(vedtaksperiode)

    internal fun finnAnnulleringskandidater(vedtaksperiodeIdSomForsøkesAnnullert: UUID): Set<Vedtaksperiode> {
        val vedtaksperiodeSomForsøkesAnnullert = vedtaksperioder.firstOrNull { it.id == vedtaksperiodeIdSomForsøkesAnnullert } ?: return emptySet()
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
        val arbeidsgiverperiode = vedtaksperiodeSomForsøkesAnnullert.behandlinger.ventedager().dagerUtenNavAnsvar.periode ?: return setOf(vedtaksperiodeSomForsøkesAnnullert)
        val vedtaksperioder = vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).filter { it.periode.start >= vedtaksperiodeSomForsøkesAnnullert.periode.start }
        return vedtaksperioder.filter { it.behandlinger.harFattetVedtak() }.toSet()
    }

    internal fun finnSisteVedtaksperiodeFørMedSammenhengendeUtbetaling(vedtaksperiode: Vedtaksperiode): Vedtaksperiode? {
        return vedtaksperioder.medSammeUtbetaling(vedtaksperiode).filterNot { it.periode.start >= vedtaksperiode.periode.start }.maxByOrNull { it.periode.start }
    }

    internal fun håndterAnnullerUtbetaling(eventBus: EventBus, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        aktivitetsloggMedArbeidsgiverkontekst.info("Håndterer annullering")

        val annulleringskandidater = finnAnnulleringskandidater(hendelse.vedtaksperiodeId)

        if (annulleringskandidater.isEmpty()) return null
        val planlagtAnnullering = EventSubscription.PlanlagtAnnulleringEvent(
            yrkesaktivitetssporing = hendelse.behandlingsporing,
            vedtaksperioder = annulleringskandidater.map { it.id },
            fom = annulleringskandidater.minOf { it.periode.start },
            tom = annulleringskandidater.maxOf { it.periode.endInclusive },
            saksbehandlerIdent = hendelse.saksbehandlerIdent,
            årsaker = hendelse.årsaker,
            begrunnelse = hendelse.begrunnelse
        )
        eventBus.planlagtAnnullering(planlagtAnnullering)
        return håndter { it.håndterAnnullerUtbetaling(eventBus, hendelse, aktivitetsloggMedArbeidsgiverkontekst, annulleringskandidater.toList()) }.tidligsteEventyr()
    }

    internal fun håndterPåminnelse(eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return énHåndtert(påminnelse) { håndterPåminnelse(eventBus, it, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    internal fun håndterOverstyrTidslinje(eventBus: EventBus, hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return håndter {
            it.håndterOverstyrTidslinje(eventBus, hendelse, aktivitetsloggMedArbeidsgiverkontekst)
        }.tidligsteEventyr()
    }

    internal fun håndterKorrigertInntekt(eventBus: EventBus, hendelse: OverstyrArbeidsgiveropplysninger, korrigertInntekt: Saksbehandler, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return håndter {
            it.håndterKorrigertInntekt(eventBus, hendelse, korrigertInntekt, aktivitetsloggMedArbeidsgiverkontekst)
        }.tidligsteEventyr()
    }

    private fun håndterOverstyrInntektsgrunnlag(overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        return vedtaksperioder.firstNotNullOfOrNull { it.håndterOverstyrInntektsgrunnlag(overstyrInntektsgrunnlag, aktivitetsloggMedArbeidsgiverkontekst) }
    }

    private fun håndterOverstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        var revurderingseventyr: Revurderingseventyr? = null
        énHarHåndtert(overstyrArbeidsgiveropplysninger) {
            revurderingseventyr = håndterOverstyrArbeidsgiveropplysninger(it, aktivitetsloggMedArbeidsgiverkontekst)
            revurderingseventyr != null
        }
        return revurderingseventyr
    }

    internal fun håndterRefusjonsopplysninger(eventBus: EventBus, hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val revurderingseventyr = håndter {
            it.håndterRefusjonLPSEllerOverstyring(eventBus, hendelse, dokumentsporing, aktivitetsloggMedArbeidsgiverkontekst, servitør)
        }.tidligsteEventyr()
        servitør.servér(ubrukteRefusjonsopplysninger, aktivitetsloggMedArbeidsgiverkontekst)
        return revurderingseventyr
    }

    internal fun beregnPerioderUtenNavAnsvar(egenmeldingsperioder: List<Periode> = vedtaksperioder.egenmeldingsperioder()): List<PeriodeUtenNavAnsvar> {
        when (yrkesaktivitetstype) {
            is Arbeidstaker -> {
                perioderUtenNavAnsvar = arbeidsgiverperiodeFor(egenmeldingsperioder)
            }
            Selvstendig,
            Jordbruker -> {
                val beregner = Ventetidberegner()
                perioderUtenNavAnsvar = beregner.result(sykdomstidslinje())
            }
            Arbeidsledig,
            Frilans -> {}
        }
        return perioderUtenNavAnsvar
    }

    internal fun oppdaterSykdom(
        meldingsreferanseId: MeldingsreferanseId,
        sykdomstidslinje: Sykdomstidslinje?,
        egenmeldingsperioder: List<Periode>
    ): Triple<Sykdomstidslinje, Skjæringstidspunkter, List<PeriodeUtenNavAnsvar>> {
        val nyTidslinje = sykdomstidslinje?.let { sykdomshistorikk.håndter(meldingsreferanseId, sykdomstidslinje) } ?: sykdomstidslinje()
        val nyeArbeidsgiverperioder = beregnPerioderUtenNavAnsvar(egenmeldingsperioder)
        val nyeSkjæringstidspunkter = person.beregnSkjæringstidspunkter()
        return Triple(nyTidslinje, nyeSkjæringstidspunkter, nyeArbeidsgiverperioder)
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!sykdomshistorikk.harSykdom()) return Sykdomstidslinje()
        return sykdomshistorikk.sykdomstidslinje()
    }

    private fun arbeidsgiverperiodeFor(egenmeldingsperioder: List<Periode>): List<PeriodeUtenNavAnsvar> {
        val teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        val arbeidsgiverperiodeberegner = Arbeidsgiverperiodeberegner(teller)
        return arbeidsgiverperiodeberegner.resultat(
            sykdomstidslinje = sykdomstidslinjeHensyntattEgenmeldinger(egenmeldingsperioder),
            infotrygdBetalteDager = person.infotrygdhistorikk.betaltePerioder(organisasjonsnummer),
            infotrygdFerieperioder = person.infotrygdhistorikk.friperioder()
        )
    }

    internal fun egenmeldingsperioderUnntatt(unntatt: Vedtaksperiode) =
        vedtaksperioder.filterNot { it === unntatt }.egenmeldingsperioder()

    private fun sykdomstidslinjeHensyntattEgenmeldinger(egenmeldingsperioder: List<Periode>): Sykdomstidslinje {
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
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        vedtaksperioderSomSkalForkastes: List<Vedtaksperiode>
    ): List<VedtaksperiodeForkastetEventBuilder> {
        val aktivitetsloggMedArbeidsgiverkontekst = aktivitetslogg.kontekst(this)
        val perioder: List<Pair<Vedtaksperiode, VedtaksperiodeForkastetEventBuilder>> = vedtaksperioderSomSkalForkastes
            .filter { it.yrkesaktivitet === this }
            .map { vedtaksperiode ->
                vedtaksperiode to vedtaksperiode.utførForkasting(eventBus, hendelse, aktivitetsloggMedArbeidsgiverkontekst)
            }

        vedtaksperioder.removeAll(perioder.map { it.first })
        forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it.first, organisasjonsnummer, it.first.periode) })
        sykdomshistorikk.fjernDager(perioder.map { it.first.periode })
        beregnPerioderUtenNavAnsvar()
        return perioder.map { it.second }
    }

    private fun registrerNyVedtaksperiode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
        vedtaksperioder.sort()
    }

    private fun registrerForkastetVedtaksperiode(
        eventBus: EventBus,
        vedtaksperiode: Vedtaksperiode,
        hendelse: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.info("Oppretter forkastet vedtaksperiode ettersom Søknad inneholder errors")
        val vedtaksperiodeForkastetEventBuilder = vedtaksperiode.utførForkasting(eventBus, hendelse, aktivitetslogg)
        vedtaksperiodeForkastetEventBuilder.buildAndEmit(eventBus)
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
            kontekstType = "Arbeidsgiver",
            kontekstMap = mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "yrkesaktivitetstype" to when (yrkesaktivitetstype) {
                    Arbeidsledig -> "ARBEIDSLEDIG"
                    is Arbeidstaker -> "ARBEIDSTAKER"
                    Frilans -> "FRILANS"
                    Selvstendig -> "SELVSTENDIG"
                    Jordbruker -> "JORDBRUKER"
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
        return Skjæringstidspunkt(sykdomstidslinje()).alle().sisteOrNull(vedtaksperiode)
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
            yrkesaktivitetstype = when (yrkesaktivitetstype) {
                Arbeidsledig -> YrkesaktivitetstypeDto.ARBEIDSLEDIG
                is Arbeidstaker -> YrkesaktivitetstypeDto.ARBEIDSTAKER
                Frilans -> YrkesaktivitetstypeDto.FRILANS
                Selvstendig -> YrkesaktivitetstypeDto.SELVSTENDIG
                Jordbruker -> YrkesaktivitetstypeDto.JORDBRUKER
            },
            inntektshistorikk = inntektshistorikk.dto(),
            sykdomshistorikk = sykdomshistorikk.dto(),
            sykmeldingsperioder = sykmeldingsperioder.dto(),
            perioderUtenNavAnsvar = perioderUtenNavAnsvar.map { it.dto() },
            vedtaksperioder = vedtaksperioderDto,
            forkastede = forkastede.map { it.dto() },
            utbetalinger = _utbetalinger.map { it.dto() },
            feriepengeutbetalinger = feriepengeutbetalinger.map { it.dto() },
            ubrukteRefusjonsopplysninger = UbrukteRefusjonsopplysningerUtDto(
                ubrukteRefusjonsopplysninger = ubrukteRefusjonsopplysninger.dto(),
                sisteRefusjonstidslinje = refusjonsopplysningerPåSisteBehandling?.second?.dto(),
                sisteBehandlingId = refusjonsopplysningerPåSisteBehandling?.first
            )
        )
    }

    internal fun trengerArbeidsgiveropplysninger(periode: Periode): List<Periode> {
        return forkastede.trengerArbeidsgiveropplysninger(periode, vedtaksperioder.map { it.periode })
    }
}

internal fun EventBus.utbetalingEventBus(yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet, builder: UtbetalingsdagerBuilder): UtbetalingEventBus {
    val utbetalingEventBus = UtbetalingEventBus()
    val formidler = Utbetalingseventformidler(this, yrkesaktivitetstype, builder)
    utbetalingEventBus.register(formidler)
    return utbetalingEventBus
}

private class Utbetalingseventformidler(
    private val eventBus: EventBus,
    private val yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet,
    private val builder: UtbetalingsdagerBuilder
) : UtbetalingObserver {
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
        eventBus.utbetalingUtbetalt(
            EventSubscription.UtbetalingUtbetaltEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
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
                arbeidsgiverOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(
                    arbeidsgiverOppdrag
                ),
                personOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = builder.result(utbetalingstidslinje),
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
        eventBus.utbetalingUtenUtbetaling(
            EventSubscription.UtbetalingUtenUtbetalingEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
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
                arbeidsgiverOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(
                    arbeidsgiverOppdrag
                ),
                personOppdrag = EventSubscription.OppdragEventDetaljer.mapOppdrag(personOppdrag),
                utbetalingsdager = builder.result(utbetalingstidslinje),
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
        eventBus.utbetalingEndret(
            EventSubscription.UtbetalingEndretEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
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
        eventBus.annullert(
            EventSubscription.UtbetalingAnnullertEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
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
}
