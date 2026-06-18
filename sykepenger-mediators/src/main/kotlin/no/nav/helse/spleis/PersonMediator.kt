package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.time.LocalDateTime
import java.time.ZoneId
import no.nav.helse.Toggle
import no.nav.helse.person.EventBus
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.EventBusOversetter
import org.slf4j.LoggerFactory

internal class PersonMediator(
    private val message: HendelseMessage
) {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun ferdigstill(context: MessageContext, eventBus: EventBus) {
        val oversetter = EventBusOversetter(eventBus, message)

        when (Toggle.BrukUtgåendeMelding.enabled) {
            true -> {
                oversetter.utgåendeMeldinger()
                    .map { utgåendeMelding ->
                        when (utgåendeMelding.eventName) {
                            "behov" -> {
                                val behov = utgåendeMelding.json.path("@behov").map { it.asText() }
                                sikkerLogg.info("sender behov (${behov.joinToString()}):\n\t${utgåendeMelding.json}")
                            }
                            else -> sikkerLogg.info("sender ${utgåendeMelding.eventName}:\n\t${utgåendeMelding.json}")
                        }
                        OutgoingMessage(body = utgåendeMelding.json.toString(), key = utgåendeMelding.key)
                    }
                    .sendUtgåendeMeldinger(context)
            }
            false -> {
                oversetter.jsonMessages()
                    .map { jsonMessage -> mapTilPakke(jsonMessage) }
                    .map { pakke -> pakke.somOutgoingMessage() }
                    .sendUtgåendeMeldinger(context)
            }
        }
    }

    private fun LocalDateTime.tilUtc() = atZone(ZoneId.systemDefault()).toInstant()

    private fun mapTilPakke(jsonMessage: JsonMessage): Pakke {
        jsonMessage.requireKey("@event_name", "@opprettet")
        val outgoingMessage = jsonMessage.apply {
            this["fødselsnummer"] = message.meldingsporing.fødselsnummer
            this["@opprettetUTC"] = jsonMessage["@opprettet"].asLocalDateTime().tilUtc()
        }.toJson()
        val eventName = jsonMessage["@event_name"].asText()
        return Pakke(message.meldingsporing.fødselsnummer, outgoingMessage) {
            when (eventName.lowercase() == "behov") {
                true -> {
                    jsonMessage.requireKey("@behov")
                    val behov = jsonMessage["@behov"].map { it.asText() }
                    sikkerLogg.info("sender behov (${behov.joinToString()}):\n\t$outgoingMessage")
                }
                false -> sikkerLogg.info("sender $eventName:\n\t$outgoingMessage")
            }
        }
    }


    private fun List<OutgoingMessage>.sendUtgåendeMeldinger(context: MessageContext) {
        if (this.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, this.size)
        val (ok, failed) = context.publish(this)

        if (failed.isEmpty()) return
        val førsteFeil = failed.first().error
        val feilmelding = "Feil ved sending av ${failed.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
            "Disse meldingene feilet:\n" +
            failed.joinToString(separator = "\n") { "#${it.index}: ${it.error.message}\n\t${it.message}" }
        throw RuntimeException(feilmelding, førsteFeil)
    }

    private data class Pakke(
        private val fødselsnummer: String,
        private val blob: String,
        private val førSending: () -> Unit
    ) {
        fun somOutgoingMessage(): OutgoingMessage {
            førSending()
            return OutgoingMessage(
                body = blob,
                key = fødselsnummer
            )
        }
    }
}
