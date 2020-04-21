package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage

internal class SendtArbeidsgiverSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_arbeidsgiver"
    override val riverName = "Sendt søknad arbeidsgiver"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("id", "egenmeldinger", "fravar")
        packet.requireValue("status", "SENDT")
        packet.require("sendtArbeidsgiver", JsonNode::asLocalDateTime)
        packet.forbid("sendtNav")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadArbeidsgiverMessage(packet)
}
