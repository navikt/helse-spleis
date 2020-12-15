package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage

internal class AvstemmingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "avstemming"
    override val riverName = "Avstemming"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("fødselsnummer", "aktørId")
    }

    override fun createMessage(packet: JsonMessage) = AvstemmingMessage(JsonMessageDelegate(packet))
}
