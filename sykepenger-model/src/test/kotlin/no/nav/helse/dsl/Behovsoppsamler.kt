package no.nav.helse.dsl

import java.util.UUID
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
    internal fun fjern(behovsdelajer: Behovsdetaljer) {
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
            val fagsystemId: String
        ): Behovsdetaljer

        data class Simulering(
            override val vedtaksperiodeId: UUID,
            override val utbetalingId: UUID,
            val fagsystemId: String,
            val fagområde: String
        ): Behovsdetaljer

        data class Godkjenning(
            val behandlingId: UUID,
            override val utbetalingId: UUID,
            override val vedtaksperiodeId: UUID
        ): Behovsdetaljer

        data class Feriepengeutbetaling(
            override val utbetalingId: UUID
        ): Behovsdetaljer

        data class InitiellHistorikFraInfotrygd(
            override val vedtaksperiodeId: UUID
        ): Behovsdetaljer

        data object OppdatertHistorikkFraInfotrygd: Behovsdetaljer

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
                            null -> Behovsdetaljer.OppdatertHistorikkFraInfotrygd
                            else -> Behovsdetaljer.InitiellHistorikFraInfotrygd(vedtaksperiodeId)
                        }
                        Aktivitet.Behov.Behovtype.Godkjenning -> Behovsdetaljer.Godkjenning(
                            vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                            behandlingId = checkNotNull(behandlingId),
                            utbetalingId = checkNotNull(utbetalingId)
                        )
                        Aktivitet.Behov.Behovtype.Simulering -> Behovsdetaljer.Simulering(
                            vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                            utbetalingId = checkNotNull(utbetalingId),
                            fagsystemId = behovMap.getValue("Simulering").single().getValue("fagsystemId") as String,
                            fagområde = behovMap.getValue("Simulering").single().getValue("fagområde") as String
                        )
                        Aktivitet.Behov.Behovtype.Utbetaling -> Behovsdetaljer.Utbetaling(
                            vedtaksperiodeId = checkNotNull(vedtaksperiodeId),
                            behandlingId = checkNotNull(behandlingId),
                            utbetalingId = checkNotNull(utbetalingId),
                            fagsystemId = behovMap.getValue("Utbetaling").single().getValue("fagsystemId") as String,
                            organisasjonsnummer = meldingMap.getValue("organisasjonsnummer") as String
                        )
                        Aktivitet.Behov.Behovtype.Feriepengeutbetaling -> Behovsdetaljer.Feriepengeutbetaling(checkNotNull(utbetalingId))

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
    }

    class FraEventBus(log: DeferredLog): Behovsoppsamler(log) {
        override fun utbetal(event: EventSubscription.UtbetalingEvent) =
            registrer(Behovsdetaljer.Utbetaling(event.vedtaksperiodeId, event.behandlingId, event.yrkesaktivitetssporing.somOrganisasjonsnummer, event.utbetalingId, event.oppdragsdetaljer.fagsystemId))
        override fun simuler(event: EventSubscription.SimuleringEvent) =
            registrer(Behovsdetaljer.Simulering(event.vedtaksperiodeId, event.utbetalingId, event.oppdragsdetaljer.fagsystemId, event.oppdragsdetaljer.fagområde))
        override fun trengerGodkjenning(event: EventSubscription.GodkjenningEvent) =
            registrer(Behovsdetaljer.Godkjenning(event.behandlingId, event.utbetalingId, event.vedtaksperiodeId,))
        override fun utbetalFeriepenger(event: EventSubscription.UtbetalFeriepengerEvent) =
            registrer(Behovsdetaljer.Feriepengeutbetaling(event.utbetalingId))
        override fun trengerInitiellHistorikkFraInfotrygd(event: EventSubscription.TrengerInitiellHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.InitiellHistorikFraInfotrygd(event.vedtaksperiodeId))
        override fun trengerOppdatertHistorikkFraInfotrygd(event: EventSubscription.TrengerOppdatertHistorikkFraInfotrygdEvent) =
            registrer(Behovsdetaljer.OppdatertHistorikkFraInfotrygd)
        override fun trengerInformasjonTilBeregning(event: EventSubscription.TrengerInformasjonTilBeregningEvent) = when (event.trengerInformasjonOmSelvstendigForsikring) {
            true -> registrer(Behovsdetaljer.InformasjonTilBeregningAvSelvstendig(event.vedtaksperiodeId))
            false -> registrer(Behovsdetaljer.InformasjonTilBeregningAvArbeidstaker(event.vedtaksperiodeId))
        }
        override fun trengerInformasjonTilVilkårsprøving(event: EventSubscription.TrengerInformasjonTilVilkårsprøvingEvent) =
            registrer(Behovsdetaljer.InformasjonTilVilkårsprøving(event.vedtaksperiodeId))
    }
}
