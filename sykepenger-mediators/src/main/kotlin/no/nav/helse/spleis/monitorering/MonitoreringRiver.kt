package no.nav.helse.spleis.monitorering

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal class MonitoreringRiver(
    rapidsConnection: RapidsConnection,
    vararg sjekker: Sjekk
): River.PacketListener {
    private val sjekker = sjekker.toList()

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "minutt")
                it.require("@opprettet") { node -> LocalDateTime.parse(node.asText()) }
                it.requireKey("system_participating_services")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
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
            return JsonMessage.newMessage("slackmelding", mapOf(
                "melding" to melding,
                "level" to level,
                "system_participating_services" to systemParticipatingServices
            )).toJson()
        }
    }
}