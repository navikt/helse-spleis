package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.EndretVurderingPåSkjæringstidspunktMessage
import no.nav.helse.spleis.meldinger.model.EndretVurderingPåSkjæringstidspunktMessage.Vurderingsformat.Forsikringsvudering
import no.nav.helse.spleis.meldinger.model.EndretVurderingPåSkjæringstidspunktMessage.Vurderingsformat.Opptjeningsvurdering

internal class EndretVurderingPåSkjæringstidspunktRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    private val vurderinger = setOf(
        Forsikringsvudering,
        Opptjeningsvurdering
    )

    override val eventNames = vurderinger.map { it.eventName }.toSet()

    override val riverName = "Endret vurdering på skjæringstidspunkt"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.interestedIn("manuellVurdering")
        message.require(message.vurderingsformat.idPath) { UUID.fromString(it.asText()) }
    }

    override fun createMessage(packet: JsonMessage) = EndretVurderingPåSkjæringstidspunktMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        ),
        vurderingsformat = packet.vurderingsformat
    )

    private val JsonMessage.vurderingsformat get() = vurderinger.singleOrNull {
        it.eventName == get("@event_name").asText()
    } ?: error("Ukjent vurdering for eventName ${get("@event_name").asText()}")
}
