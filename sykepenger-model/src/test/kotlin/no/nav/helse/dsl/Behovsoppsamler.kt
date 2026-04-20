package no.nav.helse.dsl

import java.time.LocalDate
import java.time.Year
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person. EventSubscription
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spill_av_im.Forespørsel
import no.nav.helse.spill_av_im.FørsteFraværsdag
import no.nav.helse.spill_av_im.Periode
import org.junit.jupiter.api.Assertions.assertTrue

sealed class Behovsoppsamler(private val log: DeferredLog): EventSubscription {
    private val behovsdetaljer = mutableListOf<Behovsdetaljer>()
    internal inline fun <reified R: Behovsdetaljer>behovsdetaljer() = behovsdetaljer.filterIsInstance<R>()

    protected fun registrer(behovsdetaljer: Behovsdetaljer) {
        this.behovsdetaljer.add(behovsdetaljer)
    }
    internal fun besvart(behovsdelajer: Behovsdetaljer) {
        this.behovsdetaljer.remove(behovsdelajer)
    }

    override fun inntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent) =
        registrer(Behovsdetaljer.InntektsmeldingReplay(event.opplysninger.vedtaksperiodeId, forespørsel = Forespørsel(
            fnr = event.opplysninger.personidentifikator.toString(),
            orgnr = event.opplysninger.arbeidstaker.organisasjonsnummer,
            vedtaksperiodeId = event.opplysninger.vedtaksperiodeId,
            skjæringstidspunkt = event.opplysninger.skjæringstidspunkt,
            førsteFraværsdager = event.opplysninger.førsteFraværsdager.map { FørsteFraværsdag(it.arbeidstaker.organisasjonsnummer, it.førsteFraværsdag) },
            sykmeldingsperioder = event.opplysninger.sykmeldingsperioder.map { Periode(it.start, it.endInclusive) },
            egenmeldinger = event.opplysninger.egenmeldingsperioder.map { Periode(it.start, it.endInclusive) },
            harForespurtArbeidsgiverperiode = EventSubscription.Arbeidsgiverperiode in event.opplysninger.forespurteOpplysninger
        )))

    fun loggUbesvarteBehov() {
        log.log("Etter testen er det ${behovsdetaljer.size} behov uten svar: [${behovsdetaljer.joinToString { "${it::class.simpleName}"  }}]")
    }

    override fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        val vedtaksperiodebehov = behovsdetaljer
            // kvitterer ikke ut utbetalingsbehov som følge av at vedtaksperioden endrer tilstand, det gjøres som følge av utbetaling-events
            .filterNot { behovsdetaljer -> behovsdetaljer is Behovsdetaljer.Utbetaling }
            // kvitterer ikke ut replay forespørsler, det gjøres når det håndteres replay
            .filterNot { behovsdetaljer -> behovsdetaljer is Behovsdetaljer.InntektsmeldingReplay }
            .filter { it.vedtaksperiodeId == event.vedtaksperiodeId }
            // TrengerOppdatertHistorikkFraInfotrygdEvent er bare knagget på person, med kvitterer de ut når vedtaksperioden endrer tilstand
            .plus(behovsdetaljer.filterIsInstance<Behovsdetaljer.OppdatertHistorikkFraInfotrygd>())
            .takeUnless { it.isEmpty() } ?: return

        log.log("Fjerner ${vedtaksperiodebehov.size} behov (${vedtaksperiodebehov.joinToString { "${it::class.simpleName}" }})")
        behovsdetaljer.removeAll(vedtaksperiodebehov)
        log.log(" -> Det er nå ${behovsdetaljer.size} behov (${vedtaksperiodebehov.joinToString { "${it::class.simpleName}" }})")
    }

    override fun utbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent) {
        assertTrue(behovsdetaljer.removeAll { it.utbetalingId == event.utbetalingId }) {
            "Utbetaling ble utbetalt, men ingen behov om utbetaling er registrert"
        }
    }

    override fun annullering(event: EventSubscription.UtbetalingAnnullertEvent) {
        behovsdetaljer.removeAll { it.utbetalingId == event.utbetalingId }
    }

    sealed interface Behovsdetaljer {
        val utbetalingId: UUID? get() = null
        val vedtaksperiodeId: UUID? get() = null

        data class Utbetaling(
            override val vedtaksperiodeId: UUID,
            val behandlingId: UUID,
            val organisasjonsnummer: String,
            override val utbetalingId: UUID,
            val fagsystemId: String,
            val fagområde: String,
            val maksdato: LocalDate?,
            val linjer: List<Linje>
        ): Behovsdetaljer {
            data class Linje(
                val statuskode: String?
            )
        }

        data class Simulering(
            override val vedtaksperiodeId: UUID,
            override val utbetalingId: UUID,
            val fagsystemId: String,
            val fagområde: String
        ): Behovsdetaljer

        data class Godkjenning(
            val behandlingId: UUID,
            override val utbetalingId: UUID,
            override val vedtaksperiodeId: UUID,
            val event: EventSubscription.GodkjenningEvent
        ): Behovsdetaljer

        data class Feriepengeutbetaling(
            override val utbetalingId: UUID,
            val fagsystemId: String
        ): Behovsdetaljer

        data class InitiellHistorikFraInfotrygd(
            override val vedtaksperiodeId: UUID,
            val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
        ): Behovsdetaljer

        data class OppdatertHistorikkFraInfotrygd(
            val periode: no.nav.helse.hendelser.Periode
        ): Behovsdetaljer

        data class InformasjonTilBeregningAvArbeidstaker(
            override val vedtaksperiodeId: UUID
        ): Behovsdetaljer

        data class InformasjonTilBeregningAvSelvstendig(
            override val vedtaksperiodeId: UUID
        ): Behovsdetaljer

        data class InformasjonTilVilkårsprøving(
            override val vedtaksperiodeId: UUID
        ): Behovsdetaljer

        data class InntektsmeldingReplay(
            override val vedtaksperiodeId: UUID,
            val forespørsel: Forespørsel
        ): Behovsdetaljer
    }

    companion object {
        fun opprettBehovsoppsamler(deferredLog: DeferredLog = DeferredLog()) = when (Toggle.BehovFraEventBus.enabled) {
            true -> FraEventBus(deferredLog)
            false -> FraAktivitetslogg(deferredLog)
        }
    }

    class FraAktivitetslogg(log: DeferredLog): Behovsoppsamler(log) {
        internal fun registrerFra(aktivitetslogg: Aktivitetslogg) {
            aktivitetslogg.behov
                .groupBy { it.kontekster }
                .forEach { (kontekster, behovMedSammeKontekster) ->
                    val kontekstMap = kontekster.fold(emptyMap<String, String>()) { result, item -> result + item.kontekstMap }
                    val behovMap = behovMedSammeKontekster.groupBy({ it.type.name }, { it.detaljer() })
                    val meldingMap = kontekstMap + behovMap

                    val vedtaksperiodeId = meldingMap["vedtaksperiodeId"]?.let { UUID.fromString(it.toString()) }
                    val behandlingId = meldingMap["behandlingId"]?.let { UUID.fromString(it.toString()) }
                    val utbetalingId = meldingMap["utbetalingId"]?.let { UUID.fromString(it.toString()) }

                    val behovene = behovMap.keys.map { Aktivitet.Behov.Behovtype.valueOf(it) }
                    val behovsdetaljer = when (behovene.first()) {
                        Aktivitet.Behov.Behovtype.Sykepengehistorikk -> when (vedtaksperiodeId) {
                            null -> Behovsdetaljer.OppdatertHistorikkFraInfotrygd(
                                periode = no.nav.helse.hendelser.Periode(
                                    fom = (behovMap.getValue("Sykepengehistorikk").single()["historikkFom"] as String).let { LocalDate.parse(it) },
                                    tom = (behovMap.getValue("Sykepengehistorikk").single()["historikkTom"] as String).let { LocalDate.parse(it) }
                                ),
                            )
                            else -> Behovsdetaljer.InitiellHistorikFraInfotrygd(
                                vedtaksperiodeId = vedtaksperiodeId,
                                yrkesaktivitetssporing = meldingMap.yrkesaktivitetssporing()
                            )
                        }
                        Aktivitet.Behov.Behovtype.Godkjenning -> {
                            val behovInput = behovMap.getValue("Godkjenning").single()
                            val yrkesaktivitetssporing = meldingMap.yrkesaktivitetssporing()
                            val sykepengegrunnlagsfakta = behovInput.getValue("sykepengegrunnlagsfakta") as Map<String, Any>

                            Behovsdetaljer.Godkjenning(
                                vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                                behandlingId = checkNotNull(behandlingId),
                                utbetalingId = checkNotNull(utbetalingId),
                                event = EventSubscription.GodkjenningEvent(
                                    yrkesaktivitetssporing = yrkesaktivitetssporing,
                                    vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                                    behandlingId = checkNotNull(behandlingId),
                                    utbetalingId = checkNotNull(utbetalingId),
                                    periode = no.nav.helse.hendelser.Periode(
                                        fom = (behovInput["periodeFom"] as String).let { LocalDate.parse(it) },
                                        tom = (behovInput["periodeTom"] as String).let { LocalDate.parse(it) }
                                    ),
                                    vilkårsgrunnlagId = (behovInput.getValue("vilkårsgrunnlagId") as String).let { UUID.fromString(it) },
                                    skjæringstidspunkt = (behovInput.getValue("skjæringstidspunkt") as String).let { LocalDate.parse(it) },
                                    førstegangsbehandling = behovInput.getValue("førstegangsbehandling") as Boolean,
                                    utbetalingtype = behovInput.getValue("utbetalingtype") as String,
                                    inntektskilde = behovInput.getValue("inntektskilde") as String,
                                    periodetype = behovInput.getValue("periodetype") as String,
                                    tags = behovInput.getValue("tags") as Set<String>,
                                    orgnummereMedRelevanteArbeidsforhold = behovInput.getValue("orgnummereMedRelevanteArbeidsforhold") as Set<String>,
                                    kanAvvises = behovInput.getValue("kanAvvises") as Boolean,
                                    relevanteSøknader = (behovInput.getValue("relevanteSøknader") as Set<UUID>),
                                    perioderMedSammeSkjæringstidspunkt = (behovInput.getValue("perioderMedSammeSkjæringstidspunkt") as List<Map<String, String>>).map { EventSubscription.GodkjenningEvent.PeriodeMedSammeSkjæringstidspunkt(
                                        periode = no.nav.helse.hendelser.Periode(
                                            fom = it.getValue("fom").let { LocalDate.parse(it) },
                                            tom = it.getValue("tom").let { LocalDate.parse(it) }
                                        ),
                                        vedtaksperiodeId = it.getValue("vedtaksperiodeId").let { UUID.fromString(it) },
                                        behandlingId = it.getValue("behandlingId").let { UUID.fromString(it) },
                                    )},
                                    forbrukteSykedager = behovInput.getValue("forbrukteSykedager") as Int,
                                    gjenståendeSykedager = behovInput.getValue("gjenståendeSykedager") as Int,
                                    foreløpigBeregnetSluttPåSykepenger = (behovInput.getValue("foreløpigBeregnetSluttPåSykepenger") as String).let { LocalDate.parse(it) },
                                    arbeidssituasjon = behovInput.getValue("arbeidssituasjon") as String,
                                    utbetalingsdager = (behovInput.getValue("utbetalingsdager") as List<Map<String, Any>>).map { EventSubscription.Utbetalingsdag(
                                        dato = (it.getValue("dato") as String).let { LocalDate.parse(it) },
                                        type = (it.getValue("type") as String).let { EventSubscription.Utbetalingsdag.Dagtype.valueOf(it) },
                                        beløpTilArbeidsgiver = it.getValue("beløpTilArbeidsgiver") as Int,
                                        beløpTilBruker = it.getValue("beløpTilBruker") as Int,
                                        sykdomsgrad = it.getValue("sykdomsgrad") as Int,
                                        dekningsgrad = it.getValue("dekningsgrad") as Int,
                                        begrunnelser = (it.getValue("begrunnelser") as List<String>)?.map { EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO.valueOf(it) }
                                    )},
                                    sykepengegrunnlagsfakta = when (yrkesaktivitetssporing) {
                                        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                                        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> when (sykepengegrunnlagsfakta.getValue("fastsatt") as String?) {
                                            "EtterHovedregel" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel(
                                                sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
                                                seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
                                                arbeidsgivere = (sykepengegrunnlagsfakta.getValue("arbeidsgivere") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel.Arbeidsgiver(
                                                    arbeidsgiver = it.getValue("arbeidsgiver") as String,
                                                    omregnetÅrsinntekt = it.getValue("omregnetÅrsinntekt") as Double,
                                                    inntektskilde = it.getValue("inntektskilde") as String
                                                )}
                                            )
                                            "EtterSkjønn" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn(
                                                sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
                                                seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
                                                arbeidsgivere = (sykepengegrunnlagsfakta.getValue("arbeidsgivere") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn.Arbeidsgiver(
                                                    arbeidsgiver = it.getValue("arbeidsgiver") as String,
                                                    omregnetÅrsinntekt = it.getValue("omregnetÅrsinntekt") as Double,
                                                    skjønnsfastsatt = it.getValue("skjønnsfastsatt") as Double
                                                )}
                                            )
                                            "IInfotrygd" -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerFraInfotrygd(
                                                sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
                                                seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
                                            )
                                            else -> error("Ukjent fastsatt-type")
                                        }
                                        Behandlingsporing.Yrkesaktivitet.Selvstendig -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel(
                                            sykepengegrunnlag = sykepengegrunnlagsfakta.getValue("sykepengegrunnlag") as Double,
                                            seksG = sykepengegrunnlagsfakta.getValue("6G") as Double,
                                            pensjonsgivendeInntekter = ((sykepengegrunnlagsfakta.getValue("selvstendig") as Map<String, Any>).getValue("pensjonsgivendeInntekter") as List<Map<String, Any>>).map { EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel.PensjonsgivendeInntekt(
                                                årstall = Year.of(it.getValue("årstall") as Int),
                                                beløp = it.getValue("beløp") as Double
                                            )},
                                            beregningsgrunnlag = (sykepengegrunnlagsfakta.getValue("selvstendig") as Map<String, Any>).getValue("beregningsgrunnlag") as Double
                                        )
                                        Behandlingsporing.Yrkesaktivitet.Frilans -> error("Støtter ikke frilanser ennå")
                                    }
                                )
                            )
                        }
                        Aktivitet.Behov.Behovtype.Simulering -> Behovsdetaljer.Simulering(
                            vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                            utbetalingId = checkNotNull(utbetalingId),
                            fagsystemId = behovMap.getValue("Simulering").single().getValue("fagsystemId") as String,
                            fagområde = behovMap.getValue("Simulering").single().getValue("fagområde") as String
                        )
                        Aktivitet.Behov.Behovtype.Utbetaling -> {
                            val behovInput = behovMap.getValue("Utbetaling").single()
                            Behovsdetaljer.Utbetaling(
                                vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                                behandlingId = checkNotNull(behandlingId),
                                utbetalingId = checkNotNull(utbetalingId),
                                fagområde = behovInput.getValue("fagområde") as String,
                                fagsystemId = behovInput.getValue("fagsystemId") as String,
                                organisasjonsnummer = meldingMap.getValue("organisasjonsnummer") as String,
                                maksdato = (behovInput["maksdato"] as? String)?.let { LocalDate.parse(it) },
                                linjer = ((behovInput.getValue("linjer")) as List<Map<String, *>>).map { Behovsdetaljer.Utbetaling.Linje(it["statuskode"] as? String) }
                            )
                        }
                        Aktivitet.Behov.Behovtype.Feriepengeutbetaling -> Behovsdetaljer.Feriepengeutbetaling(
                            utbetalingId = checkNotNull(utbetalingId),
                            fagsystemId = behovMap.getValue("Feriepengeutbetaling").single().getValue("fagsystemId") as String,
                        )
                        Aktivitet.Behov.Behovtype.Foreldrepenger,
                        Aktivitet.Behov.Behovtype.Pleiepenger,
                        Aktivitet.Behov.Behovtype.Omsorgspenger,
                        Aktivitet.Behov.Behovtype.Opplæringspenger,
                        Aktivitet.Behov.Behovtype.DagpengerV2,
                        Aktivitet.Behov.Behovtype.ArbeidsavklaringspengerV2,
                        Aktivitet.Behov.Behovtype.Institusjonsopphold,
                        Aktivitet.Behov.Behovtype.InntekterForBeregning,
                        Aktivitet.Behov.Behovtype.SelvstendigForsikring -> when (Aktivitet.Behov.Behovtype.SelvstendigForsikring in behovene) {
                            true -> Behovsdetaljer.InformasjonTilBeregningAvSelvstendig(checkNotNull(vedtaksperiodeId))
                            false -> Behovsdetaljer.InformasjonTilBeregningAvArbeidstaker(checkNotNull(vedtaksperiodeId))
                        }

                        Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag,
                        Aktivitet.Behov.Behovtype.InntekterForOpptjeningsvurdering,
                        Aktivitet.Behov.Behovtype.Medlemskap,
                        Aktivitet.Behov.Behovtype.ArbeidsforholdV2 -> Behovsdetaljer.InformasjonTilVilkårsprøving(checkNotNull(vedtaksperiodeId))

                        Aktivitet.Behov.Behovtype.Dødsinfo -> error("Spleis sender ikke ut behov om Dødsinfo")
                        Aktivitet.Behov.Behovtype.SykepengehistorikkForFeriepenger -> error("Spleis sender ikke ut behov om SykepengehistorikkForFeriepenger")
                    }

                    registrer(behovsdetaljer)
                }
        }

        private companion object {
            fun Map<String, Any>.yrkesaktivitetssporing() = when (val yrkesaktivitetstype = get("yrkesaktivitetstype") as String?) {
                "SELVSTENDIG" -> Behandlingsporing.Yrkesaktivitet.Selvstendig
                "FRILANS" -> Behandlingsporing.Yrkesaktivitet.Frilans
                "ARBEIDSLEDIG" -> Behandlingsporing.Yrkesaktivitet.Arbeidsledig
                "ARBEIDSTAKER" -> Behandlingsporing.Yrkesaktivitet.Arbeidstaker(getValue("organisasjonsnummer") as String)
                else -> error("Ukjent yrkesaktivitetstype $yrkesaktivitetstype")
            }
        }
    }

    class FraEventBus(log: DeferredLog): Behovsoppsamler(log) {
        override fun utbetal(event: EventSubscription.UtbetalingEvent) =
            registrer(Behovsdetaljer.Utbetaling(
                vedtaksperiodeId = event.vedtaksperiodeId,
                behandlingId = event.behandlingId,
                organisasjonsnummer = event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                utbetalingId = event.utbetalingId,
                fagområde = event.oppdragsdetaljer.fagområde,
                fagsystemId = event.oppdragsdetaljer.fagsystemId,
                maksdato = event.oppdragsdetaljer.maksdato,
                linjer = event.oppdragsdetaljer.linjer.map { Behovsdetaljer.Utbetaling.Linje(it.statuskode) }
            ))
        override fun simuler(event: EventSubscription.SimuleringEvent) =
            registrer(Behovsdetaljer.Simulering(event.vedtaksperiodeId, event.utbetalingId, event.oppdragsdetaljer.fagsystemId, event.oppdragsdetaljer.fagområde))
        override fun trengerGodkjenning(event: EventSubscription.GodkjenningEvent) =
            registrer(Behovsdetaljer.Godkjenning(event.behandlingId, event.utbetalingId, event.vedtaksperiodeId, event))
        override fun utbetalFeriepenger(event: EventSubscription.UtbetalFeriepengerEvent) =
            registrer(Behovsdetaljer.Feriepengeutbetaling(event.utbetalingId, event.fagsystemId))
        override fun trengerInitiellHistorikkFraInfotrygd(event: EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.InitiellHistorikFraInfotrygd(event.vedtaksperiodeId, event.yrkesaktivitetssporing))
        override fun trengerOppdatertHistorikkFraInfotrygd(event: EventSubscription.TrengerOppdatertHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.OppdatertHistorikkFraInfotrygd(event.periode))
        override fun trengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent) = when (event.trengerInformasjonOmSelvstendigForsikring) {
            true -> registrer(Behovsdetaljer.InformasjonTilBeregningAvSelvstendig(event.vedtaksperiodeId))
            false -> registrer(Behovsdetaljer.InformasjonTilBeregningAvArbeidstaker(event.vedtaksperiodeId))
        }
        override fun trengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent) =
            registrer(Behovsdetaljer.InformasjonTilVilkårsprøving(event.vedtaksperiodeId))
    }
}
