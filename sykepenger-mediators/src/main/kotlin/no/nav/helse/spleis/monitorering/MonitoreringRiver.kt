package no.nav.helse.spleis.monitorering

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
                it.demandValue("@event_name", "midnatt")
            }
        }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            sjekker.mapNotNull { it.sjekk() }.forEach { context.publish(it.slackmelding) }
            sikkerlogg.info("Gjennomført alle monitoreringssjekker")
        } catch (throwable: Throwable) {
            sikkerlogg.error("Feil ved monitoreringssjekker", throwable)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val Pair<Level, String>.slackmelding get(): String {
            val (level, melding) = this
            return """{"@event_name":  "slackmelding", "melding": "$melding", "level": "${level.name}"}"""
        }
    }
}