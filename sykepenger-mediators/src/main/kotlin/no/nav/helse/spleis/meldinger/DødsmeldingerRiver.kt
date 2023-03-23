package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.DødsmeldingMessage
import no.nav.helse.spleis.meldinger.model.InfotrygdendringMessage

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