package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.UUID
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

    protected abstract fun createMessage(packet: JsonMessage): HendelseMessage

    private inner class RiverImpl(river: River) : River.PacketListener {
        init {
            river.precondition { it.requireValue("@event_name", eventName) }
            river.validate { packet ->
                packet.require("@opprettet", JsonNode::asLocalDateTime)
                packet.require("@id") { UUID.fromString(it.asText()) }
            }
            river.validate(this@HendelseRiver)
            river.register(this)
        }

        override fun name() = this@HendelseRiver::class.simpleName ?: "ukjent"

        override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
            withMDC(
                mapOf(
                    "river_name" to riverName,
                    "melding_type" to eventName,
                    "melding_id" to packet["@id"].asText()
                )
            ) {

                val timer = Timer.start(meterRegistry)

                try {
                    messageMediator.onRecognizedMessage(createMessage(packet), context)
                } catch (e: Exception) {
                    sikkerLogg.error("Klarte ikke å lese melding, innhold: ${packet.toJson()}", e)
                    throw e
                } finally {
                    timer.stop(
                        Timer.builder("behandlingstid_seconds")
                            .description("hvor lang tid spleis bruker på behandling av en melding")
                            .tag("river_name", riverName)
                            .tag("event_name", eventName)
                            .register(meterRegistry)
                    )
                }
            }
        }

        override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
            messageMediator.onRiverError(riverName, problems, context, metadata)
        }
    }

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
