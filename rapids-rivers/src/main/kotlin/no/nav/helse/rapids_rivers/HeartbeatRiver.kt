package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.databind.JsonNode

class HeartbeatRiver(private val serviceId: String) : River() {
    init {
        validate { it.path("@event_name").asText() == "heartbeat" }
        validate { it.path("service_id").let { it.isMissingNode || it.isNull } }
    }

    override fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext) {
        packet.put("service_id", serviceId)
        context.send(packet.toJson())
    }
}
