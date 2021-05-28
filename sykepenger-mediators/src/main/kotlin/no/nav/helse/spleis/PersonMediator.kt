package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class PersonMediator(
    private val person: Person,
    private val message: HendelseMessage,
    private val hendelse: PersonHendelse,
    private val hendelseRepository: HendelseRepository
) : PersonObserver {
    private val meldinger = mutableListOf<Pakke>()
    private val replays = mutableListOf<String>()
    private var vedtak = false

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }


    init {
        person.addObserver(this)
    }

    fun finalize(rapidsConnection: RapidsConnection, lagrePersonDao: LagrePersonDao) {
        lagrePersonDao.lagrePerson(message, person, hendelse, vedtak)
        sendUtgåendeMeldinger(rapidsConnection)
        sendReplays(rapidsConnection)
    }

    private fun sendUtgåendeMeldinger(rapidsConnection: RapidsConnection) {
        if (meldinger.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, meldinger.size)
        meldinger.forEach { (fødselsnummer, eventName, melding) ->
            rapidsConnection.publish(fødselsnummer, melding.also { sikkerLogg.info("sender $eventName: $it") })
        }
    }

    private fun sendReplays(rapidsConnection: RapidsConnection) {
        if (replays.isEmpty()) return
        message.logReplays(sikkerLogg, replays.size)
        replays.forEach { melding ->
            rapidsConnection.queueReplayMessage(hendelse.fødselsnummer(), melding)
        }
    }

    private fun queueMessage(fødselsnummer: String, eventName: String, message: String) {
        meldinger.add(Pakke(fødselsnummer, eventName, message))
    }

    override fun inntektsmeldingReplay(event: PersonObserver.InntektsmeldingReplayEvent) {
        hendelseRepository.finnInntektsmeldinger(event.fnr).forEach { inntektsmelding ->
            createReplayMessage(inntektsmelding, mapOf(
                "@event_name" to "inntektsmelding_replay",
                "vedtaksperiodeId" to event.vedtaksperiodeId
            ))
        }
    }

    private fun createReplayMessage(message: JsonNode, extraFields: Map<String, Any>) {
        message as ObjectNode
        message.put("@replay", true)
        extraFields.forEach { (key, value), -> message.replace(key, objectMapper.convertValue<JsonNode>(value)) }
        replays.add(message.toString())
    }

    override fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {
        queueMessage("vedtaksperiode_påminnet", JsonMessage.newMessage(påminnelse.toOutgoingMessage()))
    }

    override fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, vedtaksperiodeId: UUID, nåværendeTilstand: TilstandType) {
        queueMessage(
            "vedtaksperiode_ikke_påminnet", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "tilstand" to nåværendeTilstand
                )
            )
        )
    }

    override fun annullering(event: PersonObserver.UtbetalingAnnullertEvent) {
        queueMessage(
            "utbetaling_annullert", JsonMessage.newMessage(
                objectMapper.convertValue(event)
            )
        )
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        queueMessage(
            "utbetaling_endret", JsonMessage.newMessage(
                mapOf(
                    "utbetalingId" to event.utbetalingId,
                    "type" to event.type,
                    "forrigeStatus" to event.forrigeStatus,
                    "gjeldendeStatus" to event.gjeldendeStatus,
                    "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
                    "personOppdrag" to event.personOppdrag
                )
            )
        )
    }

    override fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage("utbetaling_utbetalt", utbetalingAvsluttet(event))
    }

    override fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        queueMessage("utbetaling_uten_utbetaling", utbetalingAvsluttet(event))
    }

    private fun utbetalingAvsluttet(event: PersonObserver.UtbetalingUtbetaltEvent) =
        JsonMessage.newMessage(mapOf(
            "utbetalingId" to event.utbetalingId,
            "type" to event.type,
            "fom" to event.fom,
            "tom" to event.tom,
            "maksdato" to event.maksdato,
            "forbrukteSykedager" to event.forbrukteSykedager,
            "gjenståendeSykedager" to event.gjenståendeSykedager,
            "ident" to event.ident,
            "epost" to event.epost,
            "tidspunkt" to event.tidspunkt,
            "automatiskBehandling" to event.automatiskBehandling,
            "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
            "personOppdrag" to event.personOppdrag,
            "utbetalingsdager" to event.utbetalingsdager
        ))

    override fun feriepengerUtbetalt(event: PersonObserver.FeriepengerUtbetaltEvent) =
        queueMessage(
            "feriepenger_utbetalt", JsonMessage.newMessage(
                mapOf(
                    "arbeidsgiverOppdrag" to event.arbeidsgiverOppdrag,
                    "personOppdrag" to event.personOppdrag,
                )
            )
        )

    override fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {
        queueMessage(
            "vedtaksperiode_reberegnet", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId
                )
            )
        )
    }

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        queueMessage(
            "vedtaksperiode_endret", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "organisasjonsnummer" to event.organisasjonsnummer,
                    "gjeldendeTilstand" to event.gjeldendeTilstand,
                    "forrigeTilstand" to event.forrigeTilstand,
                    "aktivitetslogg" to event.aktivitetslogg.toMap(),
                    "harVedtaksperiodeWarnings" to event.harVedtaksperiodeWarnings,
                    "hendelser" to event.hendelser,
                    "makstid" to event.makstid
                )
            )
        )
    }

    override fun vedtaksperiodeAvbrutt(event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        queueMessage(
            "vedtaksperiode_forkastet", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "tilstand" to event.gjeldendeTilstand
                )
            )
        )
    }

    override fun vedtakFattet(
        event: PersonObserver.VedtakFattetEvent) {
        vedtak = true
        queueMessage("vedtak_fattet", JsonMessage.newMessage(
            mutableMapOf(
                "vedtaksperiodeId" to event.vedtaksperiodeId,
                "fom" to event.periode.start,
                "tom" to event.periode.endInclusive,
                "hendelser" to event.hendelseIder,
                "skjæringstidspunkt" to event.skjæringstidspunkt,
                "sykepengegrunnlag" to event.sykepengegrunnlag,
                "inntekt" to event.inntekt
            ).apply {
                event.utbetalingId?.let { this["utbetalingId"] = it }
            }
        ))
    }

    override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        queueMessage(
            "utbetalt", JsonMessage.newMessage(
                mapOf(
                    "utbetalingId" to event.utbetalingId,
                    "hendelser" to event.hendelser,
                    "utbetalt" to event.oppdrag.map { utbetalt ->
                        mapOf(
                            "mottaker" to utbetalt.mottaker,
                            "fagområde" to utbetalt.fagområde,
                            "fagsystemId" to utbetalt.fagsystemId,
                            "totalbeløp" to utbetalt.totalbeløp,
                            "utbetalingslinjer" to utbetalt.utbetalingslinjer.map { linje ->
                                mapOf(
                                    "fom" to linje.fom,
                                    "tom" to linje.tom,
                                    "sats" to linje.sats,
                                    "dagsats" to linje.sats,
                                    "beløp" to linje.beløp,
                                    "grad" to linje.grad,
                                    "sykedager" to linje.sykedager
                                )
                            }
                        )
                    },
                    "ikkeUtbetalteDager" to event.ikkeUtbetalteDager.map {
                        mapOf(
                            "dato" to it.dato,
                            "type" to it.type.name
                        )
                    },
                    "fom" to event.fom,
                    "tom" to event.tom,
                    "forbrukteSykedager" to event.forbrukteSykedager,
                    "gjenståendeSykedager" to event.gjenståendeSykedager,
                    "godkjentAv" to event.godkjentAv,
                    "automatiskBehandling" to event.automatiskBehandling,
                    "opprettet" to event.opprettet,
                    "sykepengegrunnlag" to event.sykepengegrunnlag,
                    "månedsinntekt" to event.månedsinntekt,
                    "maksdato" to event.maksdato
                )
            )
        )
    }

    override fun avstemt(result: Map<String, Any>) {
        queueMessage("person_avstemt", JsonMessage.newMessage(result))
    }

    override fun vedtaksperiodeIkkeFunnet(vedtaksperiodeEvent: PersonObserver.VedtaksperiodeIkkeFunnetEvent) {
        queueMessage(
            "vedtaksperiode_ikke_funnet", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeEvent.vedtaksperiodeId
                )
            )
        )
    }

    override fun manglerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        queueMessage(
            "trenger_inntektsmelding", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "fom" to event.fom,
                    "tom" to event.tom
                )
            )
        )
    }

    override fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        queueMessage(
            "trenger_ikke_inntektsmelding", JsonMessage.newMessage(
                mapOf(
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "fom" to event.fom,
                    "tom" to event.tom
                )
            )
        )
    }

    override fun hendelseIkkeHåndtert(event: PersonObserver.HendelseIkkeHåndtertEvent) {
        queueMessage("hendelse_ikke_håndtert", JsonMessage.newMessage(
            mapOf("hendelseId" to event.hendelseId)
        ))
    }

    private fun leggPåStandardfelter(eventName: String, outgoingMessage: JsonMessage) = outgoingMessage.apply {
        this["@event_name"] = eventName
        this["@id"] = UUID.randomUUID()
        this["@opprettet"] = LocalDateTime.now()
        this["@forårsaket_av"] = message.tracinginfo()
        this["aktørId"] = hendelse.aktørId()
        this["fødselsnummer"] = hendelse.fødselsnummer()
        if (hendelse is ArbeidstakerHendelse) {
            this["organisasjonsnummer"] = hendelse.organisasjonsnummer()
        }
    }

    private fun queueMessage(eventName: String, outgoingMessage: JsonMessage) {
        queueMessage(hendelse.fødselsnummer(), eventName, leggPåStandardfelter(eventName, outgoingMessage).toJson())
    }

    private data class Pakke (
        val fødselsnummer: String,
        val eventName: String,
        val blob: String,
    )

}
