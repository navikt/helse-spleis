package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.Arbeidsgiverperiode
import no.nav.helse.person.EventSubscription.Inntekt
import no.nav.helse.person.EventSubscription.Refusjon
import no.nav.helse.person.EventSubscription.Utbetalingsdag.Dagtype
import no.nav.helse.person.EventSubscription.Utbetalingsdag.EksternBegrunnelseDTO
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class PersonMediator(
    private val message: HendelseMessage
) {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun ferdigstill(context: MessageContext, eventBus: EventBus) {
        eventBus
            .events
            .map { event ->
                when (event) {
                    is EventSubscription.AnalytiskDatapakkeEvent -> mapAnalytiskDatapakke(event)
                    is EventSubscription.AvsluttetMedVedtakEvent -> mapAvsluttetMedVedtak(event)
                    is EventSubscription.AvsluttetUtenVedtakEvent -> mapAvsluttetUtenVedtak(event)
                    is EventSubscription.BehandlingForkastetEvent -> mapBehandlingForkastet(event)
                    is EventSubscription.BehandlingLukketEvent -> mapBehandlingLukket(event)
                    is EventSubscription.BehandlingOpprettetEvent -> mapBehandlingOpprettet(event)
                    is EventSubscription.FeriepengerUtbetaltEvent -> mapFeriepengerUtbetalt(event)
                    is EventSubscription.InntektsmeldingFørSøknadEvent -> mapInntektsmeldingFørSøknad(event)
                    is EventSubscription.InntektsmeldingHåndtertEvent -> mapInntektsmeldingHåndtert(event)
                    is EventSubscription.InntektsmeldingIkkeHåndtertEvent -> mapInntektsmeldingIkkeHåndtert(event)
                    is EventSubscription.OverlappendeInfotrygdperioder -> mapOverlappendeInfotrygdperioder(event)
                    is EventSubscription.OverstyringIgangsatt -> mapOverstyringIgangsatt(event)
                    is EventSubscription.PlanlagtAnnulleringEvent -> mapPlanlagtAnnullering(event)
                    is EventSubscription.SkatteinntekterLagtTilGrunnEvent -> mapSkatteinntekterLagtTilGrunn(event)
                    is EventSubscription.SykefraværstilfelleIkkeFunnet -> mapSykefraværstilfelleIkkeFunnet(event)
                    is EventSubscription.SøknadHåndtertEvent -> mapSøknadHåndtert(event)
                    is EventSubscription.TrengerArbeidsgiveropplysningerEvent -> mapTrengerArbeidsgiveropplysninger(event)
                    is EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent -> mapTrengerIkkeArbeidsgiveropplysninger(event)
                    is EventSubscription.TrengerInntektsmeldingReplayEvent -> mapTrengerInntektsmeldingReplay(event)
                    is EventSubscription.UtbetalingAnnullertEvent -> mapUtbetalingAnnullert(event)
                    is EventSubscription.UtbetalingEndretEvent -> mapUtbetalingEndret(event)
                    is EventSubscription.UtbetalingUtbetaltEvent -> mapUtbetalingUtbetalt(event)
                    is EventSubscription.UtbetalingUtenUtbetalingEvent -> mapUtbetalingUtenUtbetaling(event)
                    is EventSubscription.UtkastTilVedtakEvent -> mapUtkastTilVedtak(event)
                    is EventSubscription.VedtaksperiodeAnnullertEvent -> mapVedtaksperiodeAnnullert(event)
                    is EventSubscription.VedtaksperiodeEndretEvent -> mapVedtaksperiodeEndret(event)
                    is EventSubscription.VedtaksperiodeForkastetEvent -> mapVedtaksperiodeForkastet(event)
                    is EventSubscription.VedtaksperiodeIkkePåminnetEvent -> mapVedtaksperiodeIkkePåminnet(event)
                    is EventSubscription.VedtaksperiodeNyUtbetalingEvent -> mapVedtaksperiodeNyUtbetaling(event)
                    is EventSubscription.VedtaksperiodeOpprettet -> mapVedtaksperiodeOpprettet(event)
                    is EventSubscription.VedtaksperiodePåminnetEvent -> mapVedtaksperiodePåminnet(event)
                    is EventSubscription.VedtaksperioderVenterEvent -> mapVedtaksperioderVenter(event)
                }
            }
            .map { jsonMessage -> mapTilPakke(jsonMessage) }
            .sendUtgåendeMeldinger(context)
    }

    private fun mapTilPakke(jsonMessage: JsonMessage): Pakke {
        val outgoingMessage = jsonMessage.apply {
            this["fødselsnummer"] = message.meldingsporing.fødselsnummer
        }.toJson()
        val eventName = objectMapper.readTree(outgoingMessage).path("@event_name").asText()
        return Pakke(message.meldingsporing.fødselsnummer, eventName, outgoingMessage)
    }

    private fun List<Pakke>.sendUtgåendeMeldinger(context: MessageContext) {
        if (this.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, this.size)
        val outgoingMessages = this.map { it.tilUtgåendeMelding() }
        val (ok, failed) = context.publish(outgoingMessages)

        if (failed.isEmpty()) return
        val førsteFeil = failed.first().error
        val feilmelding = "Feil ved sending av ${failed.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
            "Disse meldingene feilet:\n" +
            failed.joinToString(separator = "\n") { "#${it.index}: ${it.error.message}\n\t${it.message}" }
        throw RuntimeException(feilmelding, førsteFeil)
    }

    private val Behandlingsporing.Yrkesaktivitet.somOrganisasjonsnummer
        get() = when (this) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> organisasjonsnummer
            Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        }

    private val Behandlingsporing.Yrkesaktivitet.somYrkesaktivitetstype
        get() = when (this) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> "ARBEIDSTAKER"
            Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        }

    private fun mapInntektsmeldingFørSøknad(event: EventSubscription.InntektsmeldingFørSøknadEvent): JsonMessage {
        return JsonMessage.newMessage(
            "inntektsmelding_før_søknad",
            mapOf(
                "inntektsmeldingId" to event.inntektsmeldingId,
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER"
            )
        )
    }

    private fun mapInntektsmeldingIkkeHåndtert(event: EventSubscription.InntektsmeldingIkkeHåndtertEvent): JsonMessage {
        return JsonMessage.newMessage(
            "inntektsmelding_ikke_håndtert",
            mapOf(
                "inntektsmeldingId" to event.meldingsreferanseId,
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "speilrelatert" to event.speilrelatert
            )
        )
    }

    private fun mapInntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent): JsonMessage {
        return JsonMessage.newMessage(
            "inntektsmelding_håndtert", mapOf(
            "inntektsmeldingId" to event.meldingsreferanseId,
            "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
            "yrkesaktivitetstype" to "ARBEIDSTAKER",
            "vedtaksperiodeId" to event.vedtaksperiodeId
        )
        )
    }

    private fun mapSøknadHåndtert(event: EventSubscription.SøknadHåndtertEvent): JsonMessage {
        return JsonMessage.newMessage("søknad_håndtert", byggMedYrkesaktivitet(event.yrkesaktivitetssporing) {
            put("søknadId", event.meldingsreferanseId)
            put("vedtaksperiodeId", event.vedtaksperiodeId)
        })
    }

    private fun mapVedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: EventSubscription.VedtaksperiodeAnnullertEvent): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperiode_annullert",
            mapOf(
                "fom" to vedtaksperiodeAnnullertEvent.fom,
                "tom" to vedtaksperiodeAnnullertEvent.tom,
                "vedtaksperiodeId" to vedtaksperiodeAnnullertEvent.vedtaksperiodeId,
                "behandlingId" to vedtaksperiodeAnnullertEvent.behandlingId,
                "organisasjonsnummer" to vedtaksperiodeAnnullertEvent.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to vedtaksperiodeAnnullertEvent.yrkesaktivitetssporing.somYrkesaktivitetstype
            )
        )
    }

    private fun mapOverlappendeInfotrygdperioder(event: EventSubscription.OverlappendeInfotrygdperioder): JsonMessage {
        return JsonMessage.newMessage(
            "overlappende_infotrygdperioder",
            mutableMapOf(
                "infotrygdhistorikkHendelseId" to event.infotrygdhistorikkHendelseId,
                "vedtaksperioder" to event.overlappendeInfotrygdperioder.map {
                    mapOf(
                        "organisasjonsnummer" to it.yrkesaktivitetssporing.somOrganisasjonsnummer,
                        "yrkesaktivitetstype" to it.yrkesaktivitetssporing.somYrkesaktivitetstype,
                        "vedtaksperiodeId" to it.vedtaksperiodeId,
                        "vedtaksperiodeFom" to it.vedtaksperiodeFom,
                        "vedtaksperiodeTom" to it.vedtaksperiodeTom,
                        "vedtaksperiodetilstand" to it.vedtaksperiodetilstand,
                        "kanForkastes" to it.kanForkastes,
                        "infotrygdperioder" to it.infotrygdperioder.map { infotrygdperiode ->
                            mapOf(
                                "fom" to infotrygdperiode.fom,
                                "tom" to infotrygdperiode.tom,
                                "type" to infotrygdperiode.type,
                                "organisasjonsnummer" to infotrygdperiode.orgnummer
                            )
                        }
                    )
                },
            )
        )
    }

    private fun mapVedtaksperiodePåminnet(event: EventSubscription.VedtaksperiodePåminnetEvent): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperiode_påminnet",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "tilstand" to event.tilstand,
                "antallGangerPåminnet" to event.antallGangerPåminnet,
                "tilstandsendringstidspunkt" to event.tilstandsendringstidspunkt,
                "påminnelsestidspunkt" to event.påminnelsestidspunkt,
                "nestePåminnelsestidspunkt" to event.nestePåminnelsestidspunkt
            )
        )
    }

    private fun mapVedtaksperiodeIkkePåminnet(event: EventSubscription.VedtaksperiodeIkkePåminnetEvent): JsonMessage {
        return JsonMessage.newMessage("vedtaksperiode_ikke_påminnet", byggMedYrkesaktivitet(event.yrkesaktivitetssporing) {
            put("vedtaksperiodeId", event.vedtaksperiodeId)
            put("tilstand", event.nåværendeTilstand)
        })
    }

    private fun mapUtbetalingAnnullert(event: EventSubscription.UtbetalingAnnullertEvent): JsonMessage {
        return JsonMessage.newMessage(
            "utbetaling_annullert",
            mutableMapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "utbetalingId" to event.utbetalingId,
                "korrelasjonsId" to event.korrelasjonsId,
                "fom" to event.fom,
                "tom" to event.tom,
                "tidspunkt" to event.annullertAvSaksbehandler,
                "epost" to event.saksbehandlerEpost,
                "ident" to event.saksbehandlerIdent,
                "arbeidsgiverFagsystemId" to event.arbeidsgiverFagsystemId,
                "personFagsystemId" to event.personFagsystemId
            )
        )
    }


    private fun mapPlanlagtAnnullering(event: EventSubscription.PlanlagtAnnulleringEvent): JsonMessage {
        return JsonMessage.newMessage("planlagt_annullering", byggMedYrkesaktivitet(event.yrkesaktivitetssporing) {
            put("vedtaksperioder", event.vedtaksperioder)
            put("fom", event.fom)
            put("tom", event.tom)
            put("ident", event.saksbehandlerIdent)
            put("årsaker", event.årsaker)
            put("begrunnelse", event.begrunnelse)
        })
    }

    private fun mapUtbetalingEndret(event: EventSubscription.UtbetalingEndretEvent): JsonMessage {
        return JsonMessage.newMessage(
            "utbetaling_endret",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "utbetalingId" to event.utbetalingId,
                "type" to event.type,
                "forrigeStatus" to event.forrigeStatus,
                "gjeldendeStatus" to event.gjeldendeStatus,
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap(),
                "korrelasjonsId" to event.korrelasjonsId
            )
        )
    }

    private fun EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "nettoBeløp" to this.nettoBeløp,
            "linjer" to this.linjer.map {
                it.tilJsonMap()
            }
        )

    private fun EventSubscription.UtbetalingEndretEvent.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "totalbeløp" to this.totalbeløp
        )

    private fun mapVedtaksperiodeNyUtbetaling(event: EventSubscription.VedtaksperiodeNyUtbetalingEvent): JsonMessage {
        return JsonMessage.newMessage("vedtaksperiode_ny_utbetaling", byggMedYrkesaktivitet(event.yrkesaktivitetssporing) {
            put("vedtaksperiodeId", event.vedtaksperiodeId)
            put("utbetalingId", event.utbetalingId)
        })
    }

    private fun mapOverstyringIgangsatt(event: EventSubscription.OverstyringIgangsatt): JsonMessage {
        return JsonMessage.newMessage(
            "overstyring_igangsatt", mapOf(
            "revurderingId" to UUID.randomUUID(),
            "kilde" to message.meldingsporing.id,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "periodeForEndringFom" to event.periodeForEndring.start,
            "periodeForEndringTom" to event.periodeForEndring.endInclusive,
            "årsak" to event.årsak,
            "typeEndring" to event.typeEndring,
            "berørtePerioder" to event.berørtePerioder.map {
                mapOf(
                    "vedtaksperiodeId" to it.vedtaksperiodeId,
                    "skjæringstidspunkt" to it.skjæringstidspunkt,
                    "periodeFom" to it.periode.start,
                    "periodeTom" to it.periode.endInclusive,
                    "orgnummer" to it.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to it.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "typeEndring" to it.typeEndring,
                )
            }
        ))
    }

    private fun mapUtbetalingUtbetalt(event: EventSubscription.UtbetalingUtbetaltEvent): JsonMessage {
        return JsonMessage.newMessage(
            "utbetaling_utbetalt",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "utbetalingId" to event.utbetalingId,
                "korrelasjonsId" to event.korrelasjonsId,
                "type" to event.type,
                "fom" to event.fom,
                "tom" to event.tom,
                "maksdato" to event.maksdato,
                "forbrukteSykedager" to event.forbrukteSykedager,
                "gjenståendeSykedager" to event.gjenståendeSykedager,
                "stønadsdager" to event.stønadsdager,
                "ident" to event.ident,
                "epost" to event.epost,
                "tidspunkt" to event.tidspunkt,
                "automatiskBehandling" to event.automatiskBehandling,
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap(),
                "utbetalingsdager" to event.utbetalingsdager.map {
                    it.tilJsonMap()
                }
            )
        )
    }

    private fun mapUtbetalingUtenUtbetaling(event: EventSubscription.UtbetalingUtenUtbetalingEvent): JsonMessage {
        return JsonMessage.newMessage(
            "utbetaling_uten_utbetaling",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "utbetalingId" to event.utbetalingId,
                "korrelasjonsId" to event.korrelasjonsId,
                "type" to event.type,
                "fom" to event.fom,
                "tom" to event.tom,
                "maksdato" to event.maksdato,
                "forbrukteSykedager" to event.forbrukteSykedager,
                "gjenståendeSykedager" to event.gjenståendeSykedager,
                "stønadsdager" to event.stønadsdager,
                "ident" to event.ident,
                "epost" to event.epost,
                "tidspunkt" to event.tidspunkt,
                "automatiskBehandling" to event.automatiskBehandling,
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap(),
                "utbetalingsdager" to event.utbetalingsdager.map {
                    it.tilJsonMap()
                }
            )
        )
    }

    private fun EventSubscription.OppdragEventDetaljer.OppdragEventLinjeDetaljer.tilJsonMap() =
        mapOf(
            "fom" to this.fom,
            "tom" to this.tom,
            "sats" to this.sats,
            "grad" to this.grad,
            "stønadsdager" to this.stønadsdager,
            "totalbeløp" to this.totalbeløp,
            "statuskode" to this.statuskode,
        )
    private fun EventSubscription.OppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "fagområde" to this.fagområde,
            "mottaker" to this.mottaker,
            "nettoBeløp" to this.nettoBeløp,
            "stønadsdager" to this.stønadsdager,
            "fom" to this.fom,
            "tom" to this.tom,
            "linjer" to this.linjer.map {
                it.tilJsonMap()
            }
        )

    private fun EventSubscription.Utbetalingsdag.tilJsonMap() =
        mapOf(
            "dato" to this.dato,
            "type" to when (this.type) {
                Dagtype.ArbeidsgiverperiodeDag -> "ArbeidsgiverperiodeDag"
                Dagtype.NavDag -> "NavDag"
                Dagtype.NavHelgDag -> "NavHelgDag"
                Dagtype.Arbeidsdag -> "Arbeidsdag"
                Dagtype.Fridag -> "Fridag"
                Dagtype.AvvistDag -> "AvvistDag"
                Dagtype.UkjentDag -> "UkjentDag"
                Dagtype.ForeldetDag -> "ForeldetDag"
                Dagtype.Permisjonsdag -> "Permisjonsdag"
                Dagtype.Feriedag -> "Feriedag"
                Dagtype.ArbeidIkkeGjenopptattDag -> "ArbeidIkkeGjenopptattDag"
                Dagtype.AndreYtelser -> "AndreYtelser"
                Dagtype.Ventetidsdag -> "Ventetidsdag"
            },
            "beløpTilArbeidsgiver" to this.beløpTilArbeidsgiver,
            "beløpTilBruker" to this.beløpTilBruker,
            "sykdomsgrad" to this.sykdomsgrad,
            "begrunnelser" to this.begrunnelser?.map {
                when (it) {
                    EksternBegrunnelseDTO.SykepengedagerOppbrukt -> "SykepengedagerOppbrukt"
                    EksternBegrunnelseDTO.SykepengedagerOppbruktOver67 -> "SykepengedagerOppbruktOver67"
                    EksternBegrunnelseDTO.MinimumInntekt -> "MinimumInntekt"
                    EksternBegrunnelseDTO.MinimumInntektOver67 -> "MinimumInntektOver67"
                    EksternBegrunnelseDTO.EgenmeldingUtenforArbeidsgiverperiode -> "EgenmeldingUtenforArbeidsgiverperiode"
                    EksternBegrunnelseDTO.AndreYtelserAap -> "AndreYtelserAap"
                    EksternBegrunnelseDTO.AndreYtelserDagpenger -> "AndreYtelserDagpenger"
                    EksternBegrunnelseDTO.AndreYtelserForeldrepenger -> "AndreYtelserForeldrepenger"
                    EksternBegrunnelseDTO.AndreYtelserOmsorgspenger -> "AndreYtelserOmsorgspenger"
                    EksternBegrunnelseDTO.AndreYtelserOpplaringspenger -> "AndreYtelserOpplaringspenger"
                    EksternBegrunnelseDTO.AndreYtelserPleiepenger -> "AndreYtelserPleiepenger"
                    EksternBegrunnelseDTO.AndreYtelserSvangerskapspenger -> "AndreYtelserSvangerskapspenger"
                    EksternBegrunnelseDTO.MinimumSykdomsgrad -> "MinimumSykdomsgrad"
                    EksternBegrunnelseDTO.EtterDødsdato -> "EtterDødsdato"
                    EksternBegrunnelseDTO.ManglerMedlemskap -> "ManglerMedlemskap"
                    EksternBegrunnelseDTO.ManglerOpptjening -> "ManglerOpptjening"
                    EksternBegrunnelseDTO.Over70 -> "Over70"
                }
            }
        )

    private fun mapFeriepengerUtbetalt(event: EventSubscription.FeriepengerUtbetaltEvent): JsonMessage =
        JsonMessage.newMessage(
            "feriepenger_utbetalt",
            mapOf(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "fom" to event.fom,
                "tom" to event.tom,
                "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag.tilJsonMap(),
                "personOppdrag" to event.personOppdrag.tilJsonMap()
            )
        )

    private fun EventSubscription.FeriepengerUtbetaltEvent.FeriepengeoppdragEventDetaljer.tilJsonMap() =
        mapOf(
            "fagsystemId" to this.fagsystemId,
            "mottaker" to this.mottaker,
            "totalbeløp" to this.totalbeløp
        )

    private fun mapVedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperiode_endret",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "gjeldendeTilstand" to event.gjeldendeTilstand,
                "forrigeTilstand" to event.forrigeTilstand,
                "hendelser" to event.hendelser,
                "makstid" to event.makstid,
                "fom" to event.fom,
                "tom" to event.tom,
                "skjæringstidspunkt" to event.skjæringstidspunkt
            )
        )
    }

    private fun mapVedtaksperioderVenter(event: EventSubscription.VedtaksperioderVenterEvent): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperioder_venter", mapOf(
            "vedtaksperioder" to event.vedtaksperioder.map { event ->
                mapOf(
                    "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                    "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "behandlingId" to event.behandlingId,
                    "skjæringstidspunkt" to event.skjæringstidspunkt,
                    "hendelser" to event.hendelser,
                    "ventetSiden" to event.ventetSiden,
                    "venterTil" to event.venterTil,
                    "venterPå" to mapOf(
                        "vedtaksperiodeId" to event.venterPå.vedtaksperiodeId,
                        "skjæringstidspunkt" to event.venterPå.skjæringstidspunkt,
                        "organisasjonsnummer" to event.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer,
                        "yrkesaktivitetstype" to event.venterPå.yrkesaktivitetssporing.somYrkesaktivitetstype,
                        "venteårsak" to mapOf(
                            "hva" to event.venterPå.venteårsak.hva,
                            "hvorfor" to event.venterPå.venteårsak.hvorfor
                        )
                    )
                )
            }
        ))
    }

    private fun mapVedtaksperiodeOpprettet(event: EventSubscription.VedtaksperiodeOpprettet): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperiode_opprettet",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive
            )
        )
    }

    private fun mapVedtaksperiodeForkastet(event: EventSubscription.VedtaksperiodeForkastetEvent): JsonMessage {
        return JsonMessage.newMessage(
            "vedtaksperiode_forkastet",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "tilstand" to event.gjeldendeTilstand,
                "hendelser" to event.hendelser,
                "fom" to event.fom,
                "tom" to event.tom,
                "trengerArbeidsgiveropplysninger" to event.trengerArbeidsgiveropplysninger,
                "speilrelatert" to event.speilrelatert,
                "sykmeldingsperioder" to event.sykmeldingsperioder.map {
                    mapOf(
                        "fom" to it.start,
                        "tom" to it.endInclusive
                    )
                }
            ))
    }

    private fun mapBehandlingOpprettet(event: EventSubscription.BehandlingOpprettetEvent): JsonMessage {
        return JsonMessage.newMessage(
            "behandling_opprettet",
            mutableMapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "søknadIder" to event.søknadIder,
                "type" to event.type,
                "fom" to event.fom,
                "tom" to event.tom,
                "kilde" to mapOf(
                    "meldingsreferanseId" to event.kilde.meldingsreferanseId,
                    "innsendt" to event.kilde.innsendt,
                    "registrert" to event.kilde.registert,
                    "avsender" to event.kilde.avsender
                )
            )
        )
    }

    private fun mapBehandlingForkastet(event: EventSubscription.BehandlingForkastetEvent): JsonMessage {
        return JsonMessage.newMessage(
            "behandling_forkastet",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "automatiskBehandling" to event.automatiskBehandling
            )
        )
    }

    private fun mapBehandlingLukket(event: EventSubscription.BehandlingLukketEvent): JsonMessage {
        return JsonMessage.newMessage(
            "behandling_lukket",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId
            )
        )
    }

    private fun mapAvsluttetUtenVedtak(event: EventSubscription.AvsluttetUtenVedtakEvent): JsonMessage {
        return JsonMessage.newMessage(
            "avsluttet_uten_vedtak",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "hendelser" to event.hendelseIder,
                "avsluttetTidspunkt" to event.avsluttetTidspunkt
            )
        )
    }

    private fun mapAvsluttetMedVedtak(event: EventSubscription.AvsluttetMedVedtakEvent): JsonMessage {
        return JsonMessage.newMessage(
            "avsluttet_med_vedtak",
            mapOf(
                "organisasjonsnummer" to event.yrkesaktivitetssporing.somOrganisasjonsnummer,
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive,
                "hendelser" to event.hendelseIder,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "sykepengegrunnlag" to event.sykepengegrunnlag,
                "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt,
                "utbetalingId" to event.utbetalingId,
                "sykepengegrunnlagsfakta" to when (val fakta = event.sykepengegrunnlagsfakta) {
                    is EventSubscription.UtkastTilVedtakEvent.FastsattEtterHovedregel -> mapOf(
                        "fastsatt" to fakta.fastsatt,
                        "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                        "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt,
                        "sykepengegrunnlag" to fakta.sykepengegrunnlag,
                        "6G" to fakta.`6G`,
                        "arbeidsgivere" to fakta.arbeidsgivere.map { arbeidsgiver ->
                            mapOf(
                                "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                                "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                                "inntektskilde" to arbeidsgiver.inntektskilde
                            )
                        }
                    )

                    is EventSubscription.UtkastTilVedtakEvent.FastsattEtterSkjønn -> mutableMapOf(
                        "fastsatt" to fakta.fastsatt,
                        "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                        "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt,
                        "skjønnsfastsatt" to fakta.skjønnsfastsatt,
                        "sykepengegrunnlag" to fakta.sykepengegrunnlag,
                        "6G" to fakta.`6G`,
                        "arbeidsgivere" to fakta.arbeidsgivere.map { arbeidsgiver ->
                            mapOf(
                                "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                                "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt,
                                "skjønnsfastsatt" to arbeidsgiver.skjønnsfastsatt,
                                "inntektskilde" to arbeidsgiver.inntektskilde
                            )
                        }
                    )

                    is EventSubscription.UtkastTilVedtakEvent.FastsattIInfotrygd -> mapOf(
                        "fastsatt" to fakta.fastsatt,
                        "omregnetÅrsinntekt" to fakta.omregnetÅrsinntekt,
                        "omregnetÅrsinntektTotalt" to fakta.omregnetÅrsinntekt
                    )
                }
            )
        )
    }

    private fun mapAnalytiskDatapakke(event: EventSubscription.AnalytiskDatapakkeEvent): JsonMessage {
        return JsonMessage.newMessage(
            "analytisk_datapakke",
            mapOf(
                "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "beløpTilBruker" to mapOf(
                    "totalBeløp" to event.beløpTilBruker.totalBeløp,
                    "nettoBeløp" to event.beløpTilBruker.nettoBeløp
                ),
                "beløpTilArbeidsgiver" to event.beløpTilArbeidsgiver,
                "fom" to event.fom,
                "tom" to event.tom,
                "antallForbrukteSykedagerEtterPeriode" to mapOf(
                    "antallDager" to event.antallForbrukteSykedagerEtterPeriode.antallDager,
                    "nettoDager" to event.antallForbrukteSykedagerEtterPeriode.nettoDager
                ),
                "antallGjenståendeSykedagerEtterPeriode" to mapOf(
                    "antallDager" to event.antallGjenståendeSykedagerEtterPeriode.antallDager,
                    "nettoDager" to event.antallGjenståendeSykedagerEtterPeriode.nettoDager
                ),
                "harAndreInntekterIBeregning" to event.harAndreInntekterIBeregning
            )
        )
    }

    private fun mapSykefraværstilfelleIkkeFunnet(event: EventSubscription.SykefraværstilfelleIkkeFunnet): JsonMessage {
        return JsonMessage.newMessage(
            "sykefraværstilfelle_ikke_funnet", mapOf(
            "skjæringstidspunkt" to event.skjæringstidspunkt,
        )
        )
    }

    private fun mapSkatteinntekterLagtTilGrunn(event: EventSubscription.SkatteinntekterLagtTilGrunnEvent): JsonMessage {
        return JsonMessage.newMessage(
            "skatteinntekter_lagt_til_grunn",
            mapOf(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "behandlingId" to event.behandlingId,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "omregnetÅrsinntekt" to event.omregnetÅrsinntekt,
                "skatteinntekter" to event.skatteinntekter.map {
                    mapOf<String, Any>(
                        "måned" to it.måned,
                        "beløp" to it.beløp
                    )
                }
            ))
    }

    private fun mapTrengerInntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent): JsonMessage {
        return JsonMessage.newMessage("trenger_inntektsmelding_replay", event.opplysninger.tilJsonMap())
    }

    private fun mapTrengerArbeidsgiveropplysninger(event: EventSubscription.TrengerArbeidsgiveropplysningerEvent): JsonMessage {
        return JsonMessage.newMessage("trenger_opplysninger_fra_arbeidsgiver", event.opplysninger.tilJsonMap())
    }

    private fun EventSubscription.TrengerArbeidsgiveropplysninger.tilJsonMap(): Map<String, Any> {
        return mapOf(
            "organisasjonsnummer" to this.arbeidstaker.organisasjonsnummer,
            "yrkesaktivitetstype" to "ARBEIDSTAKER",
            "vedtaksperiodeId" to this.vedtaksperiodeId,
            "skjæringstidspunkt" to this.skjæringstidspunkt,
            "sykmeldingsperioder" to this.sykmeldingsperioder.map {
                mapOf(
                    "fom" to it.start,
                    "tom" to it.endInclusive
                )
            },
            "egenmeldingsperioder" to this.egenmeldingsperioder.map {
                mapOf(
                    "fom" to it.start,
                    "tom" to it.endInclusive
                )
            },
            "førsteFraværsdager" to this.førsteFraværsdager.map {
                mapOf<String, Any>(
                    "organisasjonsnummer" to it.arbeidstaker.organisasjonsnummer,
                    "yrkesaktivitetstype" to "ARBEIDSTAKER",
                    "førsteFraværsdag" to it.førsteFraværsdag
                )
            },
            "forespurteOpplysninger" to this.forespurteOpplysninger.map { forespurtOpplysning ->
                when (forespurtOpplysning) {
                    is Arbeidsgiverperiode -> mapOf(
                        "opplysningstype" to "Arbeidsgiverperiode"
                    )

                    is Inntekt -> mapOf(
                        "opplysningstype" to "Inntekt",
                        "forslag" to mapOf(
                            "forrigeInntekt" to null
                        )
                    )

                    is Refusjon -> mapOf(
                        "opplysningstype" to "Refusjon",
                        "forslag" to emptyList<Nothing>()
                    )
                }
            }
        )
    }

    private fun mapTrengerIkkeArbeidsgiveropplysninger(event: EventSubscription.TrengerIkkeArbeidsgiveropplysningerEvent): JsonMessage {
        return JsonMessage.newMessage(
            "trenger_ikke_opplysninger_fra_arbeidsgiver",
            mapOf<String, Any>(
                "organisasjonsnummer" to event.arbeidstaker.organisasjonsnummer,
                "yrkesaktivitetstype" to "ARBEIDSTAKER",
                "vedtaksperiodeId" to event.vedtaksperiodeId
            )
        )
    }

    private fun mapUtkastTilVedtak(event: EventSubscription.UtkastTilVedtakEvent): JsonMessage {
        val utkastTilVedtak = mutableMapOf(
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "tags" to event.tags,
            "yrkesaktivitetstype" to event.yrkesaktivitetssporing.somYrkesaktivitetstype,
        )
        if (event.`6G` != null) utkastTilVedtak["sykepengegrunnlagsfakta"] = mapOf("6G" to event.`6G`)
        return JsonMessage.newMessage("utkast_til_vedtak", utkastTilVedtak.toMap())
    }

    /** Legger alltid til yrkesaktivitetstype, men legger kun til organisasjonsnummer for Arbeidstaker **/
    private fun byggMedYrkesaktivitet(yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, block: MutableMap<String, Any>.() -> Unit) =
        buildMap {
            put("yrkesaktivitetstype", yrkesaktivitetssporing.somYrkesaktivitetstype)
            compute("organisasjonsnummer") { _, _ ->
                (yrkesaktivitetssporing as? Behandlingsporing.Yrkesaktivitet.Arbeidstaker)?.organisasjonsnummer
            }
            this.block()
        }

    private data class Pakke(
        private val fødselsnummer: String,
        private val eventName: String,
        private val blob: String,
    ) {
        fun tilUtgåendeMelding(): OutgoingMessage {
            sikkerLogg.info("sender $eventName: $blob")
            return OutgoingMessage(
                body = blob,
                key = fødselsnummer
            )
        }
    }
}
