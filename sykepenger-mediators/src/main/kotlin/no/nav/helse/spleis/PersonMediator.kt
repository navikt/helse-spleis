package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.time.LocalDateTime
import java.time.ZoneId
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
        EventBusOversetter(eventBus, message)
            .jsonMessages()
            .map { jsonMessage -> mapTilPakke(jsonMessage) }
            .sendUtgåendeMeldinger(context)
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

    private data class Pakke(
        private val fødselsnummer: String,
        private val blob: String,
        private val førSending: () -> Unit
    ) {
        fun tilUtgåendeMelding(): OutgoingMessage {
            førSending()
            return OutgoingMessage(
                body = blob,
                key = fødselsnummer
            )
        }
    }
}
