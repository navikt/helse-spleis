package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.UbrukteRefusjonsopplysningerUtDto
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.AvbruttSøknad
import no.nav.helse.hendelser.Behandlingsavgjørelse
import no.nav.helse.hendelser.DagerFraInntektsmelding
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
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.mursteinsperioder
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingpåminnelse
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrArbeidsgiveropplysninger
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.slåSammenSykdomstidslinjer
import no.nav.helse.person.PersonObserver.UtbetalingEndretEvent.OppdragEventDetaljer
import no.nav.helse.person.Vedtaksperiode.Companion.AUU_SOM_VIL_UTBETALES
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.person.Vedtaksperiode.Companion.arbeidsgiverperioder
import no.nav.helse.person.Vedtaksperiode.Companion.beregnSkjæringstidspunkter
import no.nav.helse.person.Vedtaksperiode.Companion.checkBareEnPeriodeTilGodkjenningSamtidig
import no.nav.helse.person.Vedtaksperiode.Companion.egenmeldingsperioder
import no.nav.helse.person.Vedtaksperiode.Companion.nestePeriodeSomSkalGjenopptas
import no.nav.helse.person.Vedtaksperiode.Companion.nåværendeVedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.refusjonstidslinje
import no.nav.helse.person.Vedtaksperiode.Companion.sendOppdatertForespørselOmArbeidsgiveropplysningerForNestePeriode
import no.nav.helse.person.Vedtaksperiode.Companion.startdatoerPåSammenhengendeVedtaksperioder
import no.nav.helse.person.Vedtaksperiode.Companion.validerTilstand
import no.nav.helse.person.Vedtaksperiode.Companion.venter
import no.nav.helse.person.Yrkesaktivitet.Companion.tilYrkesaktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_26
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.builders.UtbetalingsdagerBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.inntekt.FaktaavklartInntekt
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.person.view.ArbeidsgiverView
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.utbetalingslinjer.Arbeidsgiverferiepengegrunnlag
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling.Companion.gjelderFeriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.grunnlagForFeriepenger
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.tillaterOpprettelseAvUtbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingObserver
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeberegner
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperioderesultat.Companion.finn
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Arbeidsgiver private constructor(
    private val person: Person,
    val organisasjonsnummer: String,
    private val id: UUID,
    private val inntektshistorikk: Inntektshistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val sykmeldingsperioder: Sykmeldingsperioder,
    private val vedtaksperioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<ForkastetVedtaksperiode>,
    private val utbetalinger: MutableList<Utbetaling>,
    private val feriepengeutbetalinger: MutableList<Feriepengeutbetaling>,
    private val ubrukteRefusjonsopplysninger: Refusjonsservitør,
    private val yrkesaktivitet: Yrkesaktivitet,
    private val regelverkslogg: Regelverkslogg
) : Aktivitetskontekst, UtbetalingObserver {
    internal constructor(person: Person, yrkesaktivitet: Yrkesaktivitet, regelverkslogg: Regelverkslogg) : this(
        person = person,
        organisasjonsnummer = yrkesaktivitet.identifikator(),
        id = UUID.randomUUID(),
        inntektshistorikk = Inntektshistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        sykmeldingsperioder = Sykmeldingsperioder(),
        vedtaksperioder = mutableListOf(),
        forkastede = mutableListOf(),
        utbetalinger = mutableListOf(),
        feriepengeutbetalinger = mutableListOf(),
        ubrukteRefusjonsopplysninger = Refusjonsservitør(),
        yrkesaktivitet = yrkesaktivitet,
        regelverkslogg = regelverkslogg
    )

    init {
        utbetalinger.forEach { it.registrer(this) }
    }

    fun view(): ArbeidsgiverView = ArbeidsgiverView(
        organisasjonsnummer = organisasjonsnummer,
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
        internal fun List<Arbeidsgiver>.finn(yrkesaktivitet: Yrkesaktivitet) =
            find { it.erSammeYrkesaktivitet(yrkesaktivitet) }

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

        internal fun List<Arbeidsgiver>.håndterHistorikkFraInfotrygd(
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            forEach { arbeidsgiver ->
                arbeidsgiver.håndterHistorikkFraInfotrygd(hendelse, aktivitetslogg, infotrygdhistorikk)
            }
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

        internal fun List<Arbeidsgiver>.validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) =
            forEach { it.vedtaksperioder.validerTilstand(hendelse, aktivitetslogg) }

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
            checkBareEnPeriodeTilGodkjenningSamtidig(periodeSomSkalGjenopptas)
            periodeSomSkalGjenopptas.gjenopptaBehandling(hendelse, aktivitetslogg)
        }

        internal fun Iterable<Arbeidsgiver>.nestemann() =
            periodeSomSkalGjenopptas() ?: førsteAuuSomVilUtbetales()

        private fun Iterable<Arbeidsgiver>.periodeSomSkalGjenopptas() =
            flatMap { it.vedtaksperioder }.nestePeriodeSomSkalGjenopptas()

        private fun Iterable<Arbeidsgiver>.checkBareEnPeriodeTilGodkjenningSamtidig(periodeSomSkalGjenopptas: Vedtaksperiode) =
            flatMap { it.vedtaksperioder }.checkBareEnPeriodeTilGodkjenningSamtidig(periodeSomSkalGjenopptas)

        internal fun søppelbøtte(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg,
            filter: VedtaksperiodeFilter
        ) {
            arbeidsgivere.flatMap { it.søppelbøtte(hendelse, aktivitetslogg, filter) }.forEach { it.buildAndEmit() }
        }

        internal fun gjenopprett(
            person: Person,
            alder: Alder,
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
                organisasjonsnummer = dto.organisasjonsnummer,
                inntektshistorikk = Inntektshistorikk.gjenopprett(dto.inntektshistorikk),
                sykdomshistorikk = Sykdomshistorikk.gjenopprett(dto.sykdomshistorikk),
                sykmeldingsperioder = Sykmeldingsperioder.gjenopprett(dto.sykmeldingsperioder),
                vedtaksperioder = vedtaksperioder,
                forkastede = forkastede,
                utbetalinger = utbetalinger.toMutableList(),
                feriepengeutbetalinger = dto.feriepengeutbetalinger.map { Feriepengeutbetaling.gjenopprett(alder, it) }
                    .toMutableList(),
                ubrukteRefusjonsopplysninger = Refusjonsservitør.gjenopprett(dto.ubrukteRefusjonsopplysninger),
                yrkesaktivitet = dto.organisasjonsnummer.tilYrkesaktivitet(),
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

    private fun erSammeYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet) = this.yrkesaktivitet == yrkesaktivitet

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
    internal fun grunnlagForFeriepenger() = Arbeidsgiverferiepengegrunnlag(
        orgnummer = organisasjonsnummer,
        utbetalinger = utbetalinger.grunnlagForFeriepenger()
    )

    internal fun lagUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode
    ) = lagNyUtbetaling(
        aktivitetslogg,
        utbetalingstidslinje,
        maksdato,
        forbrukteSykedager,
        gjenståendeSykedager,
        periode,
        Utbetalingtype.UTBETALING
    )

    internal fun lagRevurdering(
        aktivitetslogg: IAktivitetslogg,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode
    ): Utbetaling {
        return lagNyUtbetaling(
            aktivitetslogg,
            utbetalingstidslinje,
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager,
            periode,
            Utbetalingtype.REVURDERING
        )
    }

    private fun lagNyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        periode: Periode,
        type: Utbetalingtype
    ): Utbetaling {
        val utbetalingsaker = UtbetalingsakerBuilder(
            vedtaksperioder.arbeidsgiverperioder(),
            person.infotrygdhistorikk.betaltePerioder()
        ).lagUtbetalingsaker()

        val (utbetalingen, annulleringer) = Utbetaling.lagUtbetaling(
            utbetalinger = utbetalinger,
            fødselsnummer = person.fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingstidslinje = utbetalingstidslinje,
            periode = periode,
            utbetalingsaker = utbetalingsaker,
            aktivitetslogg = aktivitetslogg,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            type = type
        )
        nyUtbetaling(aktivitetslogg, utbetalingen, annulleringer)
        return utbetalingen
    }

    private fun nyUtbetaling(
        aktivitetslogg: IAktivitetslogg,
        utbetalingen: Utbetaling,
        annulleringer: List<Utbetaling> = emptyList()
    ) {
        utbetalinger.lastOrNull()?.forkast(aktivitetslogg)
        annulleringer.plus(utbetalingen).forEach { utbetaling ->
            check(utbetalinger.tillaterOpprettelseAvUtbetaling(utbetaling)) { "Har laget en overlappende utbetaling" }
            utbetalinger.add(utbetaling)
            utbetaling.registrer(this)
            utbetaling.opprett(aktivitetslogg)
        }
    }

    internal fun utbetalFeriepenger(
        personidentifikator: Personidentifikator,
        feriepengeberegner: Feriepengeberegner,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.kontekst(this)

        val feriepengeutbetaling = Feriepengeutbetaling.Builder(
            personidentifikator,
            organisasjonsnummer,
            feriepengeberegner,
            utbetalingshistorikkForFeriepenger,
            feriepengeutbetalinger
        ).build(aktivitetslogg)

        if (Toggle.SendFeriepengeOppdrag.enabled) {
            feriepengeutbetalinger.add(feriepengeutbetaling)
            feriepengeutbetaling.overfør(aktivitetslogg)
        }
    }

    internal fun håndter(sykmelding: Sykmelding, aktivitetslogg: IAktivitetslogg) {
        håndter { it.håndter(sykmelding) }
        yrkesaktivitet.håndter(sykmelding, aktivitetslogg, sykmeldingsperioder)
    }

    internal fun håndter(avbruttSøknad: AvbruttSøknad, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        avbruttSøknad.avbryt(sykmeldingsperioder)
    }

    internal fun håndter(forkastSykmeldingsperioder: ForkastSykmeldingsperioder, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        forkastSykmeldingsperioder.forkast(sykmeldingsperioder)
    }

    internal fun håndter(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        håndter {
            it.håndter(anmodningOmForkasting, aktivitetslogg)
        }
    }

    internal fun vurderOmSøknadIkkeKanHåndteres(
        aktivitetslogg: IAktivitetslogg,
        vedtaksperiode: Vedtaksperiode,
        arbeidsgivere: List<Arbeidsgiver>
    ): Boolean {
        // sjekker først egen arbeidsgiver først
        return yrkesaktivitet.erYrkesaktivitetenIkkeStøttet(aktivitetslogg) || this.harForkastetVedtaksperiodeSomBlokkererBehandling(
            aktivitetslogg,
            vedtaksperiode
        )
            || arbeidsgivere.any {
            it !== this && it.harForkastetVedtaksperiodeSomBlokkererBehandling(
                aktivitetslogg,
                vedtaksperiode
            )
        }
            || ForkastetVedtaksperiode.harKortGapTilForkastet(forkastede, aktivitetslogg, vedtaksperiode)
    }

    private fun harForkastetVedtaksperiodeSomBlokkererBehandling(
        aktivitetslogg: IAktivitetslogg,
        vedtaksperiode: Vedtaksperiode
    ): Boolean {
        return ForkastetVedtaksperiode.forlengerForkastet(forkastede, aktivitetslogg, vedtaksperiode)
            || ForkastetVedtaksperiode.harOverlappendeForkastetPeriode(forkastede, vedtaksperiode, aktivitetslogg)
            || ForkastetVedtaksperiode.harNyereForkastetPeriode(forkastede, vedtaksperiode, aktivitetslogg)
    }

    internal fun håndter(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr? {
        aktivitetslogg.kontekst(this)
        søknad.slettSykmeldingsperioderSomDekkes(sykmeldingsperioder)
        return behandleSøknad(søknad, aktivitetslogg, arbeidsgivere, infotrygdhistorikk)
    }

    private fun behandleSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr? {
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
    ): Revurderingseventyr? {
        val vedtaksperiode = søknad.lagVedtaksperiode(aktivitetslogg, person, this, regelverkslogg)
        registrerNyVedtaksperiode(vedtaksperiode)
        return vedtaksperiode.håndterSøknadFørsteGang(søknad, aktivitetslogg, arbeidsgivere, infotrygdhistorikk)
    }

    internal fun håndter(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        aktivitetslogg.kontekst(this)
        aktivitetslogg.info("Replayer inntektsmeldinger for vedtaksperiode ${replays.vedtaksperiodeId} og påfølgende som overlapper")
        val revurderingseventyr = håndterReplayAvInntektsmelding(replays.inntektsmeldinger, aktivitetslogg, replays.vedtaksperiodeId)
        håndter { it.håndter(replays, aktivitetslogg) }
        return revurderingseventyr
    }

    internal fun håndter(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        if (énHarHåndtert(arbeidsgiveropplysninger) { håndter(arbeidsgiveropplysninger, aktivitetslogg, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }) return
        person.emitInntektsmeldingIkkeHåndtert(arbeidsgiveropplysninger, organisasjonsnummer, true)
        aktivitetslogg.funksjonellFeil(RV_IM_26)
    }

    internal fun håndter(arbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        if (énHarHåndtert(arbeidsgiveropplysninger) { håndter(arbeidsgiveropplysninger, aktivitetslogg, vedtaksperioder.toList(), inntektshistorikk, ubrukteRefusjonsopplysninger) }) return
        person.emitInntektsmeldingIkkeHåndtert(arbeidsgiveropplysninger, organisasjonsnummer, true)
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, skalBehandleRefusjonsopplysningene: Boolean = true): Revurderingseventyr? {
        aktivitetslogg.kontekst(this)

        // 1. starter håndtering av inntektsmelding på vegne av alle mulige perioder
        val dagoverstyring = håndterDagerFraInntektsmelding(inntektsmelding.dager(), aktivitetslogg)

        // 2. starter håndtering av refusjonsopplysninger på vegne av alle mulige perioder
        val refusjonsoverstyring = if (skalBehandleRefusjonsopplysningene)
            håndterRefusjonsopplysninger(inntektsmelding, inntektsmeldingRefusjon(inntektsmelding.metadata.meldingsreferanseId), aktivitetslogg, inntektsmelding.refusjonsservitør)
        else null

        // 3. håndterer inntekten fra inntektsmeldingen
        val inntektoverstyring = vedtaksperioder.firstNotNullOfOrNull {
            it.håndterInntektFraInntektsmelding(inntektsmelding, aktivitetslogg, inntektshistorikk)
        }

        // 4. ferdigstiller håndtering av inntektsmelding
        inntektsmelding.ferdigstill(aktivitetslogg, person, vedtaksperioder, forkastede, sykmeldingsperioder)

        // 5. igangsetter
        val tidligsteOverstyring = listOfNotNull(inntektoverstyring, dagoverstyring, refusjonsoverstyring).tidligsteEventyr()
        // hvis tidligsteOverstyring er null så er verken dager, refusjon eller inntekt håndtert
        return tidligsteOverstyring
    }

    internal fun håndterReplayAvInntektsmelding(inntektsmeldinger: List<Inntektsmelding>, aktivitetslogg: IAktivitetslogg, vedtaksperiodeIdForReplay: UUID): Revurderingseventyr? {
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
        aktivitetslogg.kontekst(this)
        aktivitetslogg.info("Inntektsmelding ferdigbehandlet")
        håndter { it.inntektsmeldingFerdigbehandlet(hendelse, aktivitetslogg) }
    }

    internal fun håndterHistorikkFraInfotrygd(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        aktivitetslogg.kontekst(this)
        håndter { it.håndterHistorikkFraInfotrygd(hendelse, aktivitetslogg, infotrygdhistorikk) }
    }

    internal fun håndter(
        ytelser: Ytelser,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        aktivitetslogg.kontekst(this)
        håndter { it.håndter(ytelser, aktivitetslogg, infotrygdhistorikk) }
    }

    internal fun håndter(utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(utbetalingsavgjørelse, aktivitetslogg) }
        håndter {
            it.håndter(utbetalingsavgjørelse, aktivitetslogg)
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
        aktivitetslogg.kontekst(this)
        énHarHåndtert(sykepengegrunnlagForArbeidsgiver) {
            håndter(sykepengegrunnlagForArbeidsgiver, aktivitetslogg)
        }
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        håndter {
            it.håndter(vilkårsgrunnlag, aktivitetslogg)
        }
    }

    internal fun håndter(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(simulering) }
        håndter {
            it.håndter(simulering, aktivitetslogg)
        }
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        if (feriepengeutbetalinger.gjelderFeriepengeutbetaling(utbetalingHendelse)) return håndterFeriepengeUtbetaling(
            utbetalingHendelse,
            aktivitetslogg
        )
        håndterUtbetaling(utbetalingHendelse, aktivitetslogg)
    }

    private fun håndterFeriepengeUtbetaling(utbetalingHendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        feriepengeutbetalinger.forEach { it.håndter(utbetalingHendelse, aktivitetslogg, organisasjonsnummer, person) }
    }

    private fun håndterUtbetaling(utbetaling: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        utbetalinger.forEach { it.håndter(utbetaling, aktivitetslogg) }
        håndter {
            it.håndter(utbetaling, aktivitetslogg)
        }
        person.gjenopptaBehandling(aktivitetslogg)
    }

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

    internal fun håndter(hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        aktivitetslogg.info("Håndterer annullering")
        håndter { it.håndter(hendelse, aktivitetslogg, vedtaksperioder.toList()) }
    }

    internal fun håndter(påminnelse: Utbetalingpåminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        utbetalinger.forEach { it.håndter(påminnelse, aktivitetslogg) }
    }

    internal fun håndter(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Boolean {
        aktivitetslogg.kontekst(this)
        return énHarHåndtert(påminnelse) { håndter(it, aktivitetslogg) }
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
                organisasjonsnummer = organisasjonsnummer,
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
                organisasjonsnummer = organisasjonsnummer,
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

    internal fun håndter(hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.kontekst(this)
        håndter {
            it.håndter(hendelse, aktivitetslogg)
        }
    }

    private fun håndter(overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        aktivitetslogg.kontekst(this)
        return vedtaksperioder.firstNotNullOfOrNull { it.håndter(overstyrInntektsgrunnlag, aktivitetslogg) }
    }

    private fun håndter(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        aktivitetslogg.kontekst(this)
        var revurderingseventyr: Revurderingseventyr? = null
        énHarHåndtert(overstyrArbeidsgiveropplysninger) {
            revurderingseventyr = håndter(it, aktivitetslogg)
            revurderingseventyr != null
        }
        return revurderingseventyr
    }

    internal fun håndterRefusjonsopplysninger(hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val revurderingseventyr = håndter {
            it.håndterRefusjon(hendelse, dokumentsporing, aktivitetslogg, servitør)
        }.tidligsteEventyr()
        aktivitetslogg.kontekst(this)
        servitør.servér(ubrukteRefusjonsopplysninger, aktivitetslogg)
        return revurderingseventyr
    }

    internal fun oppdaterSykdom(meldingsreferanseId: MeldingsreferanseId, sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        return sykdomshistorikk.håndter(meldingsreferanseId, sykdomstidslinje)
    }

    private fun sykdomstidslinje(): Sykdomstidslinje {
        if (!sykdomshistorikk.harSykdom()) return Sykdomstidslinje()
        return sykdomshistorikk.sykdomstidslinje()
    }

    private fun sykdomstidslinjeInkludertForkastet(sykdomstidslinje: Sykdomstidslinje): Sykdomstidslinje {
        return forkastede
            .slåSammenSykdomstidslinjer(sykdomstidslinje)
            .merge(sykdomstidslinje(), replace)
    }

    private fun arbeidsgiverperiodeFor(sykdomstidslinje: Sykdomstidslinje): List<Arbeidsgiverperioderesultat> {
        val teller = Arbeidsgiverperiodeteller.NormalArbeidstaker
        val arbeidsgiverperiodeberegner = Arbeidsgiverperiodeberegner(teller)
        // hensyntar historikk fra infotrygd, men spleis-tidslinjen overskriver eventuell overlappende info
        val samletTidslinje = person.infotrygdhistorikk
            .sykdomstidslinje(organisasjonsnummer)
            .merge(sykdomstidslinje, replace)
        return arbeidsgiverperiodeberegner.resultat(
            samletTidslinje,
            person.infotrygdhistorikk.betaltePerioder(organisasjonsnummer)
        )
    }

    internal fun beregnArbeidsgiverperiode() = { vedtaksperiode: Periode ->
        arbeidsgiverperiodeFor(sykdomstidslinje())
            .finn(vedtaksperiode)
            ?.arbeidsgiverperiode
            ?.grupperSammenhengendePerioder()
            ?: emptyList()
    }

    private fun arbeidsgiverperiode(periode: Periode, sykdomstidslinje: Sykdomstidslinje): Arbeidsgiverperiode? {
        val arbeidsgiverperioder = arbeidsgiverperiodeFor(sykdomstidslinje)
        return arbeidsgiverperioder.finn(periode)?.somArbeidsgiverperiode()
    }

    internal fun arbeidsgiverperiode(periode: Periode) =
        arbeidsgiverperiode(periode, sykdomstidslinje())

    internal fun arbeidsgiverperiodeInkludertForkastet(periode: Periode, sykdomstidslinje: Sykdomstidslinje) =
        arbeidsgiverperiode(periode, sykdomstidslinjeInkludertForkastet(sykdomstidslinje))

    internal fun arbeidsgiverperiodeHensyntattEgenmeldinger(periode: Periode): Arbeidsgiverperiode? {
        val egenmeldingsperioder = vedtaksperioder.egenmeldingsperioder()
        if (egenmeldingsperioder.isEmpty()) return arbeidsgiverperiode(periode)

        val tøyseteKilde = Hendelseskilde(Søknad::class, MeldingsreferanseId(UUID.randomUUID()), LocalDateTime.now())
        val egenmeldingstidslinje = egenmeldingsperioder
            .map { Sykdomstidslinje.arbeidsgiverdager(it.start, it.endInclusive, 100.prosent, tøyseteKilde) }
            .merge()
            .fremTilOgMed(periode.endInclusive)

        val sykdomstidslinjeMedEgenmeldinger = egenmeldingstidslinje.merge(sykdomstidslinje(), replace)
        return arbeidsgiverperiode(periode, sykdomstidslinjeMedEgenmeldinger)
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
        arbeidsgiverinntekt: FaktaavklartInntekt,
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
        filter: VedtaksperiodeFilter
    ): List<Vedtaksperiode.VedtaksperiodeForkastetEventBuilder> {
        aktivitetslogg.kontekst(this)
        val perioder: List<Pair<Vedtaksperiode, Vedtaksperiode.VedtaksperiodeForkastetEventBuilder>> = vedtaksperioder
            .filter(filter)
            .mapNotNull { vedtaksperiode ->
                vedtaksperiode.forkast(hendelse, aktivitetslogg, utbetalinger)?.let { vedtaksperiode to it }
            }

        vedtaksperioder.removeAll(perioder.map { it.first })
        forkastede.addAll(perioder.map { ForkastetVedtaksperiode(it.first) })
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
        val vedtaksperiodeForkastetEventBuilder = vedtaksperiode.forkast(hendelse, aktivitetslogg, utbetalinger)
        vedtaksperiodeForkastetEventBuilder!!.buildAndEmit()
        forkastede.add(ForkastetVedtaksperiode(vedtaksperiode))
    }

    internal fun finnVedtaksperiodeRettFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            other.erVedtaksperiodeRettFør(vedtaksperiode)
        }

    internal fun finnVedtaksperiodeFør(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.indexOf(vedtaksperiode)
            .takeUnless { index -> index == 0 }
            ?.let { vedtaksperioder[it - 1] }

    internal fun finnVedtaksperiodeRettEtter(vedtaksperiode: Vedtaksperiode) =
        vedtaksperioder.firstOrNull { other ->
            vedtaksperiode.erVedtaksperiodeRettFør(other)
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

    internal fun bekreftErLåst(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().bekreftErLåst(periode)
    }

    internal fun bekreftErÅpen(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().bekreftErÅpen(periode)
    }

    internal fun finnFørsteFraværsdag(skjæringstidspunkt: LocalDate): LocalDate? {
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

    internal fun kanForkastes(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) =
        vedtaksperiode.kanForkastes(utbetalinger, aktivitetslogg)

    fun vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode): List<Vedtaksperiode> {
        return vedtaksperioder.filter { it.periode in arbeidsgiverperiode }
    }

    internal fun sendOppdatertForespørselOmArbeidsgiveropplysningerForNestePeriode(vedtaksperiode: Vedtaksperiode) {
        vedtaksperioder.sendOppdatertForespørselOmArbeidsgiveropplysningerForNestePeriode(vedtaksperiode)
    }

    fun vedtaksperioderKnyttetTilArbeidsgiverperiodeInkludertForkastede(arbeidsgiverperiode: Arbeidsgiverperiode?): List<Vedtaksperiode> {
        if (arbeidsgiverperiode == null) return emptyList()
        return ForkastetVedtaksperiode.hørerTilArbeidsgiverperiode(forkastede, vedtaksperioder, arbeidsgiverperiode)
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
}
