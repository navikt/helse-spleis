package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage

internal class PersonPåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "person_påminnelse"
    override val riverName = "Person påminnelse"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("fødselsnummer", "aktørId")
    }

    override fun createMessage(packet: JsonMessage) = PersonPåminnelseMessage(JsonMessageDelegate(packet))
}
