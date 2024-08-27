package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.PersonPåminnelseMessage

internal class PersonPåminnelserRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "person_påminnelse"
    override val riverName = "Person påminnelse"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "aktørId")
    }

    override fun createMessage(packet: JsonMessage) = PersonPåminnelseMessage(packet)
}
