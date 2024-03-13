package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Histogram
import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.withMDC
import org.slf4j.LoggerFactory

internal abstract class HendelseRiver(rapidsConnection: RapidsConnection, private val messageMediator: IMessageMediator) : River.PacketValidation {
    protected val river = River(rapidsConnection)
    protected abstract val eventName: String
    protected abstract val riverName: String

    init {
        RiverImpl(river)
    }

    private fun validateHendelse(packet: JsonMessage) {
        packet.demandValue("@event_name", eventName)
        packet.require("@opprettet", JsonNode::asLocalDateTime)
        packet.require("@id") { UUID.fromString(it.asText()) }
    }

    protected abstract fun createMessage(packet: JsonMessage): HendelseMessage

    private inner class RiverImpl(river: River) : River.PacketListener {
        init {
            river.validate(::validateHendelse)
            river.validate(this@HendelseRiver)
            river.register(this)
        }

        override fun name() = this@HendelseRiver::class.simpleName ?: "ukjent"

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            withMDC(mapOf(
                "river_name" to riverName,
                "melding_type" to eventName,
                "melding_id" to packet["@id"].asText()
            )) {
                behandlingstid.labels(riverName, eventName).time {
                    try {
                        messageMediator.onRecognizedMessage(createMessage(packet), context)
                    } catch (e: Exception) {
                        sikkerLogg.error("Klarte ikke å lese melding, innhold: ${packet.toJson()}", e)
                        throw e
                    }
                }
            }
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            messageMediator.onRiverError(riverName, problems, context)
        }
    }

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val behandlingstid = Histogram.build("behandlingstid_seconds", "hvor lang tid spleis bruker på behandling av en melding")
            .labelNames("river_name", "event_name")
            .buckets(0.05, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 10.0, 20.0, 60.0, 120.0)
            .register()
    }
}
