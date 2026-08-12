package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.opptjening.application.OpptjeningService

class OpptjeningsvurderingRiver(rapidsConnection: RapidsConnection, private val opptjeningService: OpptjeningService): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("@event_name") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("skjæringstidspunkt") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
//        opptjeningService.vurderOpptjening(fødselsnummer, skjæringstidspunkt, KafkaMeldingssender(context))
    }
}
