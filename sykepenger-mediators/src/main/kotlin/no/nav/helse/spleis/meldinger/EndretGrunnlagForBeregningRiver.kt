package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.EndretGrunnlagForBeregningMessage
import no.nav.helse.spleis.meldinger.model.EndretGrunnlagForBeregningMessage.Grunnlagsformat.GraderteAndreYtelser
import no.nav.helse.spleis.meldinger.model.EndretGrunnlagForBeregningMessage.Grunnlagsformat.Inntektsendringer

internal class EndretGrunnlagForBeregningRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    private val grunnlag = setOf(
        GraderteAndreYtelser,
        Inntektsendringer
    )

    override val eventNames = grunnlag.map { it.eventName }.toSet()

    override val riverName = "Endret grunnlag for beregning"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
        message.interestedIn("manuellVurdering")
        message.require(message.grunnlagsformat.fomPath, JsonNode::asLocalDate)
    }

    override fun precondition(packet: JsonMessage) {
        packet.requireKey("@event_name")
        packet.grunnlagsformatOrNull?.precondition(packet)
    }

    override fun createMessage(packet: JsonMessage) = EndretGrunnlagForBeregningMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        ),
        grunnlagsformat = packet.grunnlagsformat
    )

    private val JsonMessage.eventNameOrNull
        get() =
            objectMapper.readTree(toJson())
                .takeIf { it.has("@event_name") }
                ?.path("@event_name")
                ?.asText()

    private val JsonMessage.grunnlagsformatOrNull
        get() = eventNameOrNull?.let { eventName ->
            grunnlag.singleOrNull { it.eventName == eventName }
        }

    private val JsonMessage.grunnlagsformat
        get() =
            grunnlagsformatOrNull ?: error("Ukjent grunnlag for eventName ${eventNameOrNull ?: "mangler @event_name"}")
}
