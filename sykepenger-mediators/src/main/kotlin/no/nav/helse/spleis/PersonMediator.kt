package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.PersonMediator.Pakke.Companion.fjernVedtaksperiodeVenter
import no.nav.helse.spleis.PersonMediator.Pakke.Companion.loggAntallVedtaksperioderVenter
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.asPeriode
import org.slf4j.LoggerFactory

internal class PersonMediator(
    private val message: HendelseMessage,
    private val hendelse: PersonHendelse,
    private val hendelseRepository: HendelseRepository
) : PersonObserver {
    private val meldinger = mutableListOf<Pakke>()
    private val replays = mutableListOf<Pair<UUID, String>>()
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(PersonMediator::class.java)
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun fjernUnødvendigeVedtaksperiodeVenter() {
        if (replays.isEmpty()) return
        // Ettersom det kommer til å bli replayet inntektsmelding(er)
        // fjernes eventene vedtaksperiode_venter som er køet opp
        // ettersom vi vil sende ut ferskere eventer etter InntektsmeldingReplayUtført
        meldinger.fjernVedtaksperiodeVenter()
    }

    fun finalize(rapidsConnection: RapidsConnection, context: MessageContext) {
        fjernUnødvendigeVedtaksperiodeVenter()
        sendUtgåendeMeldinger(context)
        sendReplays(rapidsConnection)
    }

    private fun sendUtgåendeMeldinger(context: MessageContext) {
        if (meldinger.isEmpty()) return
        meldinger.loggAntallVedtaksperioderVenter()
        message.logOutgoingMessages(sikkerLogg, meldinger.size)
        meldinger.forEach { pakke -> pakke.publish(context) }
    }

    private fun sendReplays(rapidsConnection: RapidsConnection) {
        if (replays.isEmpty()) return
        message.logReplays(sikkerLogg, replays.size)
        replays.forEach { (_, melding) ->
            rapidsConnection.queueReplayMessage(hendelse.fødselsnummer(), melding)
        }
    }

    private fun queueMessage(fødselsnummer: String, message: String) {
        val eventName = objectMapper.readTree(message).path("@event_name").asText()
        meldinger.add(Pakke(fødselsnummer, eventName, message))
    }

    override fun inntektsmeldingReplay(personidentifikator: Personidentifikator, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, sammenhengendePeriode: Periode) {
        hendelseRepository.finnInntektsmeldinger(personidentifikator)
            .filter { it.path("virksomhetsnummer").asText() == organisasjonsnummer }
            .filter { inntektsmelding ->
                val førsteFraværsdag = inntektsmelding.path("foersteFravaersdag").asOptionalLocalDate()
                val redusertUtbetaling = inntektsmelding.path("begrunnelseForReduksjonEllerIkkeUtbetalt").asText().isNotBlank()
                val arbeidsgiverperioder = inntektsmelding.path("arbeidsgiverperioder").map(::asPeriode).periode()
                Inntektsmelding.aktuellForReplay(sammenhengendePeriode, førsteFraværsdag, arbeidsgiverperioder, redusertUtbetaling)
            }
            .forEach { inntektsmelding -> createReplayMessage(inntektsmelding, vedtaksperiodeId) }
        createReplayUtførtMessage(personidentifikator, aktørId, organisasjonsnummer, vedtaksperiodeId)
    }

    override fun trengerIkkeInntektsmeldingReplay(vedtaksperiodeId: UUID) {
        replays.removeIf { it.first == vedtaksperiodeId }
    }

    private fun createReplayUtførtMessage(fødselsnummer: Personidentifikator, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) {
        replays.add(vedtaksperiodeId to JsonMessage.newMessage("inntektsmelding_replay_utført", mapOf(
            "fødselsnummer" to fødselsnummer.toString(),
            "aktørId" to aktørId,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to "$vedtaksperiodeId"
        )).toJson())
    }

    private fun createReplayMessage(message: JsonNode, vedtaksperiodeId: UUID) {
        message as ObjectNode
        message.put("@replay", true)
        message.put("@event_name", "inntektsmelding_replay")
        message.put("vedtaksperiodeId", "$vedtaksperiodeId")
        replays.add(vedtaksperiodeId to message.toString())
    }

    override fun inntektsmeldingFørSøknad(event: PersonObserver.InntektsmeldingFørSøknadEvent) {
        queueMessage(JsonMessage.newMessage("inntektsmelding_før_søknad", mapOf(
            "inntektsmeldingId" to event.inntektsmeldingId,
            "overlappende_sykmeldingsperioder" to event.overlappendeSykmeldingsperioder.map { periode ->
                mapOf(
                    "fom" to periode.start,
                    "tom" to periode.endInclusive
                )
            },
            "organisasjonsnummer" to event.organisasjonsnummer
        )))
    }

    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String, harPeriodeInnenfor16Dager: Boolean) {
        queueMessage(JsonMessage.newMessage("inntektsmelding_ikke_håndtert", mapOf(
            "inntektsmeldingId" to inntektsmeldingId,
            "organisasjonsnummer" to organisasjonsnummer,
            "harPeriodeInnenfor16Dager" to harPeriodeInnenfor16Dager
        )))
    }

    override fun inntektsmeldingHåndtert(inntektsmeldingId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        queueMessage(JsonMessage.newMessage("inntektsmelding_håndtert", mapOf(
            "inntektsmeldingId" to inntektsmeldingId,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId
        )))
    }
    override fun søknadHåndtert(søknadId: UUID, vedtaksperiodeId: UUID, organisasjonsnummer: String) {
        queueMessage(JsonMessage.newMessage("søknad_håndtert", mapOf(
            "søknadId" to søknadId,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId
        )))
    }

    override fun vedtaksperiodeAnnullert(vedtaksperiodeAnnullertEvent: PersonObserver.VedtaksperiodeAnnullertEvent) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_annullert", mapOf(
            "fom" to vedtaksperiodeAnnullertEvent.fom,
            "tom" to vedtaksperiodeAnnullertEvent.tom,
            "vedtaksperiodeId" to vedtaksperiodeAnnullertEvent.vedtaksperiodeId,
            "behandlingId" to vedtaksperiodeAnnullertEvent.behandlingId,
            "organisasjonsnummer" to vedtaksperiodeAnnullertEvent.organisasjonsnummer
        )))
    }

    override fun overlappendeInfotrygdperioder(event: PersonObserver.OverlappendeInfotrygdperioder) {
        queueMessage(
            JsonMessage.newMessage(
                "overlappende_infotrygdperioder", mutableMapOf(
                    "infotrygdhistorikkHendelseId" to event.infotrygdhistorikkHendelseId,
                    "vedtaksperioder" to event.overlappendeInfotrygdperioder.map { it ->
                        mapOf("organisasjonsnummer" to it.organisasjonsnummer,
                            "vedtaksperiodeId" to it.vedtaksperiodeId,
                            "vedtaksperiodeFom" to it.vedtaksperiodeFom,
                            "vedtaksperiodeTom" to it.vedtaksperiodeTom,
                            "vedtaksperiodetilstand" to it.vedtaksperiodetilstand,
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
        )
    }

    override fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, påminnelse: Påminnelse) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_påminnet", påminnelse.toOutgoingMessage()))
    }

    override fun vedtaksperiodeIkkePåminnet(vedtaksperiodeId: UUID, organisasjonsnummer: String, nåværendeTilstand: TilstandType) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_ikke_påminnet", mapOf(
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "tilstand" to nåværendeTilstand
        )))
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        queueMessage(JsonMessage.newMessage("utbetaling_annullert", mutableMapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "utbetalingId" to event.utbetalingId,
            "korrelasjonsId" to event.korrelasjonsId,
            "fom" to event.fom,
            "tom" to event.tom,
            "tidspunkt" to event.annullertAvSaksbehandler,
            "epost" to event.saksbehandlerEpost,
            "ident" to event.saksbehandlerIdent,
            "arbeidsgiverFagsystemId" to event.arbeidsgiverFagsystemId,
            "personFagsystemId" to event.personFagsystemId
        )))
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        queueMessage(JsonMessage.newMessage("utbetaling_endret", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "utbetalingId" to event.utbetalingId,
            "type" to event.type,
            "forrigeStatus" to event.forrigeStatus,
            "gjeldendeStatus" to event.gjeldendeStatus,
            "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
            "personOppdrag" to event.personOppdrag,
            "korrelasjonsId" to event.korrelasjonsId
        )))
    }

    override fun nyVedtaksperiodeUtbetaling(
        personidentifikator: Personidentifikator,
        aktørId: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID
    ) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_ny_utbetaling", mapOf(
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "utbetalingId" to utbetalingId
        )))
    }

    override fun overstyringIgangsatt(event: PersonObserver.OverstyringIgangsatt) {
        queueMessage(JsonMessage.newMessage("overstyring_igangsatt", mapOf(
            "revurderingId" to UUID.randomUUID(),
            "kilde" to message.id,
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
                    "orgnummer" to it.orgnummer,
                    "typeEndring" to it.typeEndring,
                )
            }
        )))
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(utbetalingAvsluttet("utbetaling_utbetalt", event))
    }

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage(utbetalingAvsluttet("utbetaling_uten_utbetaling", event))
    }

    private fun utbetalingAvsluttet(eventName: String, event: PersonObserver.UtbetalingUtbetaltEvent) =
        JsonMessage.newMessage(eventName, mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
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
            "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
            "personOppdrag" to event.personOppdrag,
            "utbetalingsdager" to event.utbetalingsdager,
            "vedtaksperiodeIder" to event.vedtaksperiodeIder // TODO: denne kan slettes når spesialsaker ikke trengs automatiseres mer (eneste avhengighet er sporbar)
        ))

    override fun feriepengerUtbetalt(event: PersonObserver.FeriepengerUtbetaltEvent) =
        queueMessage(JsonMessage.newMessage("feriepenger_utbetalt", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
            "personOppdrag" to event.personOppdrag,
        )))

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_endret", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "gjeldendeTilstand" to event.gjeldendeTilstand,
            "forrigeTilstand" to event.forrigeTilstand,
            "hendelser" to event.hendelser,
            "makstid" to event.makstid,
            "fom" to event.fom,
            "tom" to event.tom
        )))
    }

    override fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_venter", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "hendelser" to event.hendelser,
            "ventetSiden" to event.ventetSiden,
            "venterTil" to event.venterTil,
            "venterPå" to mapOf(
                "vedtaksperiodeId" to event.venterPå.vedtaksperiodeId,
                "skjæringstidspunkt" to event.venterPå.skjæringstidspunkt,
                "organisasjonsnummer" to event.venterPå.organisasjonsnummer,
                "venteårsak" to mapOf(
                    "hva" to event.venterPå.venteårsak.hva,
                    "hvorfor" to event.venterPå.venteårsak.hvorfor
                )
            )
        )))
    }

    override fun vedtaksperiodeOpprettet(event: PersonObserver.VedtaksperiodeOpprettet) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_opprettet", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "fom" to event.periode.start,
            "tom" to event.periode.endInclusive
        )))
    }

    override fun vedtaksperiodeForkastet(event: PersonObserver.VedtaksperiodeForkastetEvent) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_forkastet", event.toJsonMap()))
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        queueMessage(JsonMessage.newMessage("behandling_opprettet", mutableMapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "type" to event.type,
            "fom" to event.fom,
            "tom" to event.tom,
            "kilde" to mapOf(
                "meldingsreferanseId" to event.kilde.meldingsreferanseId,
                "innsendt" to event.kilde.innsendt,
                "registrert" to event.kilde.registert,
                "avsender" to event.kilde.avsender
            )
        )))
    }

    override fun behandlingForkastet(event: PersonObserver.BehandlingForkastetEvent) {
        queueMessage(JsonMessage.newMessage("behandling_forkastet", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "automatiskBehandling" to event.automatiskBehandling
        )))
    }

    override fun behandlingLukket(event: PersonObserver.BehandlingLukketEvent) {
        queueMessage(JsonMessage.newMessage("behandling_lukket", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId
        )))
    }

    override fun avsluttetUtenVedtak(event: PersonObserver.AvsluttetUtenVedtakEvent) {
        queueMessage(JsonMessage.newMessage("avsluttet_uten_vedtak", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "fom" to event.periode.start,
            "tom" to event.periode.endInclusive,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "hendelser" to event.hendelseIder,
            "avsluttetTidspunkt" to event.avsluttetTidspunkt
        )))
    }

    override fun avsluttetMedVedtak(event: PersonObserver.AvsluttetMedVedtakEvent) {
        queueMessage(JsonMessage.newMessage("avsluttet_med_vedtak", mutableMapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "behandlingId" to event.behandlingId,
            "fom" to event.periode.start,
            "tom" to event.periode.endInclusive,
            "hendelser" to event.hendelseIder,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "sykepengegrunnlag" to event.sykepengegrunnlag,
            "grunnlagForSykepengegrunnlag" to event.beregningsgrunnlag,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver" to event.omregnetÅrsinntektPerArbeidsgiver,
            "begrensning" to event.sykepengegrunnlagsbegrensning,
            "inntekt" to event.inntekt,
            "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt,
            "tags" to event.tags
        ).apply {
            event.utbetalingId?.let { this["utbetalingId"] = it }
            event.sykepengegrunnlagsfakta?.let {
                this["sykepengegrunnlagsfakta"] = when (it) {
                    is PersonObserver.AvsluttetMedVedtakEvent.FastsattISpeil -> mutableMapOf(
                        "fastsatt" to it.fastsatt,
                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                        "6G" to it.`6G`,
                        "tags" to it.tags,
                        "arbeidsgivere" to it.arbeidsgivere.map { arbeidsgiver ->
                            val skjønnsfastsatt = if (arbeidsgiver.skjønnsfastsatt != null) mapOf("skjønnsfastsatt" to arbeidsgiver.skjønnsfastsatt) else emptyMap()
                            mapOf(
                                "arbeidsgiver" to arbeidsgiver.arbeidsgiver,
                                "omregnetÅrsinntekt" to arbeidsgiver.omregnetÅrsinntekt
                            ) + skjønnsfastsatt
                        }
                    ).apply {
                        compute("skjønnsfastsatt") { _, _ -> it.skjønnsfastsatt }
                    }
                    is PersonObserver.AvsluttetMedVedtakEvent.FastsattIInfotrygd -> mapOf(
                        "fastsatt" to it.fastsatt,
                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt
                    )
                }
            }
        }))
    }

    override fun vedtaksperiodeIkkeFunnet(event: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
        queueMessage(JsonMessage.newMessage("vedtaksperiode_ikke_funnet", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId
        )))
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        queueMessage(JsonMessage.newMessage("trenger_inntektsmelding", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "fom" to event.fom,
            "tom" to event.tom,
            "søknadIder" to event.søknadIder
        )))
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        queueMessage(JsonMessage.newMessage("trenger_ikke_inntektsmelding", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "fom" to event.fom,
            "tom" to event.tom,
            "søknadIder" to event.søknadIder
        )))
    }

    override fun trengerArbeidsgiveropplysninger(event: PersonObserver.TrengerArbeidsgiveropplysningerEvent) {
        queueMessage(JsonMessage.newMessage("trenger_opplysninger_fra_arbeidsgiver", event.toJsonMap()))
    }

    override fun trengerPotensieltArbeidsgiveropplysninger(event: PersonObserver.TrengerPotensieltArbeidsgiveropplysningerEvent) {
        queueMessage(JsonMessage.newMessage("trenger_potensielt_opplysninger_fra_arbeidsgiver", event.toJsonMap()))
    }

    override fun trengerIkkeArbeidsgiveropplysninger(event: PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent) {
        queueMessage(JsonMessage.newMessage("trenger_ikke_opplysninger_fra_arbeidsgiver", event.toJsonMap()))
    }

    override fun arbeidsgiveropplysningerKorrigert(event: PersonObserver.ArbeidsgiveropplysningerKorrigertEvent) {
        queueMessage(JsonMessage.newMessage("arbeidsgiveropplysninger_korrigert", event.toJsonMap()))
    }

    private fun leggPåStandardfelter(outgoingMessage: JsonMessage) = outgoingMessage.apply {
        this["aktørId"] = hendelse.aktørId()
        this["fødselsnummer"] = hendelse.fødselsnummer()
    }

    private fun queueMessage(outgoingMessage: JsonMessage) {
        loggHvisTomHendelseliste(outgoingMessage)
        queueMessage(hendelse.fødselsnummer(), leggPåStandardfelter(outgoingMessage).toJson())
    }

    private fun loggHvisTomHendelseliste(outgoingMessage: JsonMessage) {
        objectMapper.readTree(outgoingMessage.toJson()).let {
            if (it.path("hendelser").isArray && it["hendelser"].isEmpty) {
                logg.warn("Det blir sendt ut et event med tom hendelseliste - ooouups")
            }
        }
    }

    private data class Pakke (
        private val fødselsnummer: String,
        private val eventName: String,
        private val blob: String,
    ) {
        fun publish(context: MessageContext) {
            context.publish(fødselsnummer, blob.also { sikkerLogg.info("sender $eventName: $it") })
        }

        companion object {
            private const val VedtaksperiodeVenter = "vedtaksperiode_venter"

            fun MutableList<Pakke>.fjernVedtaksperiodeVenter() {
                removeIf { it.eventName == VedtaksperiodeVenter }
            }
            fun List<Pakke>.loggAntallVedtaksperioderVenter() {
                val antall = filter { it.eventName == VedtaksperiodeVenter }.takeUnless { it.isEmpty() }?.size ?: return
                sikkerLogg.info("Sender $antall vedtaksperiode_venter eventer", keyValue("fødselsnummer", first().fødselsnummer))
            }
        }
    }

}
