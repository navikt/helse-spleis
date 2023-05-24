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
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.SykefraværstilfelleeventyrObserver
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

    private fun queueMessage(fødselsnummer: String, eventName: String, message: String) {
        meldinger.add(Pakke(fødselsnummer, eventName, message))
    }

    override fun inntektsmeldingReplay(personidentifikator: Personidentifikator, aktørId: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, sammenhengendePeriode: Periode) {
        // en IM overlapper dersom dagene fra IM er "rett før" vedtaksperioden eller dersom
        // vedtaksperioden er "rett før" inntektsmeldingen. utvider derfor søkeområdet med to dager
        // i begge retninger for å hensynta evt. helg-forskyvning
        val søkeområde = sammenhengendePeriode.start.minusDays(2) til sammenhengendePeriode.endInclusive.plusDays(2)
        hendelseRepository.finnInntektsmeldinger(personidentifikator)
            .filter { it.path("virksomhetsnummer").asText() == organisasjonsnummer }
            .filter { inntektsmelding ->
                val førsteFraværsdag = inntektsmelding.path("foersteFravaersdag").asOptionalLocalDate()
                val redusertUtbetaling = inntektsmelding.path("begrunnelseForReduksjonEllerIkkeUtbetalt").asText().isNotBlank()
                val arbeidsgiverperioder = inntektsmelding.path("arbeidsgiverperioder").map(::asPeriode).periode()
                val overlapperMedRedusertUtbetaltAGP = (redusertUtbetaling && arbeidsgiverperioder == null && førsteFraværsdag in sammenhengendePeriode)

                // inntekt fra inntektsmelding håndteres på arbeidsgivernivå når IM ankommer; derfor trenger vi i utgangspunktet
                // kun replay av tidligere IM for å håndtere dager.
                // en Vedtaksperiode overlapper med IM slik:
                // 1) vedtaksperioden, eller den sammenhengende perioden, overlapper med dagene (AGP)
                // 2) dersom IM har oppgitt reduksjon, og AGP er tom, da benyttes første fraværsdag som en nødløsning (TM)
                arbeidsgiverperioder?.overlapperMed(søkeområde) == true || overlapperMedRedusertUtbetaltAGP
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

    override fun sykefraværstilfelle(sykefraværstilfeller: List<SykefraværstilfelleeventyrObserver.SykefraværstilfelleeventyrObserverEvent>) {
        queueMessage(JsonMessage.newMessage("sykefraværstilfeller", mapOf(
            "tilfeller" to sykefraværstilfeller.map { tilfelle ->
                mapOf(
                    "dato" to tilfelle.dato,
                    "perioder" to tilfelle.perioder.map { periode ->
                        mapOf(
                            "vedtaksperiodeId" to periode.id,
                            "organisasjonsnummer" to periode.organisasjonsnummer,
                            "fom" to periode.fom,
                            "tom" to periode.tom
                        )
                    }
                )
            }
        )))
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

    override fun inntektsmeldingIkkeHåndtert(inntektsmeldingId: UUID, organisasjonsnummer: String) {
        queueMessage(JsonMessage.newMessage("inntektsmelding_ikke_håndtert", mapOf(
            "inntektsmeldingId" to inntektsmeldingId,
            "organisasjonsnummer" to organisasjonsnummer
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

    override fun overlappendeInfotrygdperiodeEtterInfotrygdendring(event: PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring) {
        queueMessage(JsonMessage.newMessage("overlappende_infotrygdperiode_etter_infotrygdendring", mapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "vedtaksperiodeFom" to event.vedtaksperiodeFom,
            "vedtaksperiodeTom" to event.vedtaksperiodeTom,
            "vedtaksperiodetilstand" to event.vedtaksperiodetilstand,
            "infotrygdhistorikkHendelseId" to (event.infotrygdhistorikkHendelseId ?: ""),
            "infotrygdperioder" to event.infotrygdperioder.map {
                mapOf(
                    "fom" to it.fom,
                    "tom" to it.tom,
                    "type" to it.type,
                    "organisasjonsnummer" to (it.orgnummer ?: "")
                )
            }
        )))
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
        val eventName = "vedtaksperiode_ny_utbetaling"
        queueMessage(personidentifikator.toString(), eventName, JsonMessage.newMessage(eventName, mapOf(
            "fødselsnummer" to personidentifikator.toString(),
            "aktørId" to aktørId,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "utbetalingId" to utbetalingId
        )).toJson())
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
            "vedtaksperiodeIder" to event.vedtaksperiodeIder
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
            "hendelser" to event.hendelser,
            "ventetSiden" to event.ventetSiden,
            "venterTil" to event.venterTil,
            "venterPå" to mapOf(
                "vedtaksperiodeId" to event.venterPå.vedtaksperiodeId,
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

    override fun vedtakFattet(event: PersonObserver.VedtakFattetEvent) {
        queueMessage(JsonMessage.newMessage("vedtak_fattet", mutableMapOf(
            "organisasjonsnummer" to event.organisasjonsnummer,
            "vedtaksperiodeId" to event.vedtaksperiodeId,
            "fom" to event.periode.start,
            "tom" to event.periode.endInclusive,
            "hendelser" to event.hendelseIder,
            "skjæringstidspunkt" to event.skjæringstidspunkt,
            "sykepengegrunnlag" to event.sykepengegrunnlag,
            "grunnlagForSykepengegrunnlag" to event.beregningsgrunnlag,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver" to event.omregnetÅrsinntektPerArbeidsgiver,
            "begrensning" to event.sykepengegrunnlagsbegrensning,
            "inntekt" to event.inntekt,
            "vedtakFattetTidspunkt" to event.vedtakFattetTidspunkt
        ).apply {
            event.utbetalingId?.let { this["utbetalingId"] = it }
        }))
    }

    override fun avstemt(result: Map<String, Any>) {
        queueMessage(JsonMessage.newMessage("person_avstemt", result))
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

    override fun arbeidsgiveropplysningerKorrigert(event: PersonObserver.ArbeidsgiveropplysningerKorrigertEvent) {
        queueMessage(JsonMessage.newMessage("arbeidsgiveropplysninger_korrigert", event.toJsonMap()))
    }

    private fun leggPåStandardfelter(outgoingMessage: JsonMessage) = outgoingMessage.apply {
        this["aktørId"] = hendelse.aktørId()
        this["fødselsnummer"] = hendelse.fødselsnummer()
    }

    private fun queueMessage(outgoingMessage: JsonMessage) {
        loggHvisTomHendelseliste(outgoingMessage)
        queueMessage(hendelse.fødselsnummer(), outgoingMessage.also { it.interestedIn("@event_name") }["@event_name"].asText(), leggPåStandardfelter(outgoingMessage).toJson())
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
