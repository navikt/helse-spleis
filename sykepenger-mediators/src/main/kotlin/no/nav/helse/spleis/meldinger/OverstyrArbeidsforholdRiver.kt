package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsforholdMessage

internal class OverstyrArbeidsforholdRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_arbeidsforhold"

    override val riverName = "Overstyr arbeidsforhold"

    override fun createMessage(packet: JsonMessage) = OverstyrArbeidsforholdMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.requireArray("overstyrteArbeidsforhold") {
            requireKey("orgnummer", "forklaring")
            require("deaktivert", JsonNode::asBoolean)
        }
        message.require("overstyrteArbeidsforhold") { require(!it.isEmpty) }
    }
}
