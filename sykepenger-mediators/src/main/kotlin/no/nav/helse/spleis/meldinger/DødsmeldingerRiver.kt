package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.DødsmeldingMessage

internal class DødsmeldingerRiver (
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
    ) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "dødsmelding"
    override val riverName = "Dødsmelding"


    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer")
        message.require("dødsdato", JsonNode::asLocalDate)
    }

    override fun createMessage(packet: JsonMessage) = DødsmeldingMessage(packet)
}