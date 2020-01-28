package no.nav.helse.rapids_rivers

import com.fasterxml.jackson.databind.JsonNode

class HeartbeatRiver(rapidsConnection: RapidsConnection,
                     private val serviceId: String) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.path("@event_name").asText() == "heartbeat" }
            validate { it.path("service_id").let { it.isMissingNode || it.isNull } }
        }.register(this)
    }

    override fun onPacket(packet: JsonNode, context: RapidsConnection.MessageContext) {
        packet.put("service_id", serviceId)
        context.send(packet.toJson())
    }
}
