package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
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

    override fun createMessage(packet: JsonMessage) = EndretGrunnlagForBeregningMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        ),
        grunnlagsformat = packet.grunnlagsformat
    )

    private val JsonMessage.grunnlagsformat get() = grunnlag.singleOrNull {
        it.eventName == get("@event_name").asText()
    } ?: error("Ukjent grunnlag for eventName ${get("@event_name").asText()}")
}
