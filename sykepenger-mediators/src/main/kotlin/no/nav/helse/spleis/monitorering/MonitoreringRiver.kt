package no.nav.helse.spleis.monitorering

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal class MonitoreringRiver(
    rapidsConnection: RapidsConnection,
    vararg sjekker: Sjekk
) : River.PacketListener {
    private val sjekker = sjekker.toList()

    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "minutt") }
            validate {
                it.require("@opprettet") { node -> LocalDateTime.parse(node.asText()) }
                it.requireKey("system_participating_services")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val nå = packet["@opprettet"].asLocalDateTime()
        val systemParticipatingServices = packet["system_participating_services"]
        try {
            sjekker
                .filter { it.skalSjekke(nå) }
                .mapNotNull { it.sjekk() }
                .forEach { (level, melding) ->
                    context.publish(slackmelding(melding, level, systemParticipatingServices))
                }
            sikkerlogg.info("Gjennomført alle monitoreringssjekker $nå")
        } catch (throwable: Throwable) {
            sikkerlogg.error("Feil ved monitoreringssjekker $nå", throwable)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun slackmelding(melding: String, level: Level, systemParticipatingServices: JsonNode): String {
            return JsonMessage.newMessage(
                "slackmelding", mapOf(
                "melding" to melding,
                "level" to level,
                "system_participating_services" to systemParticipatingServices
            )
            ).toJson()
        }
    }
}
