package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.RollbackMessage

internal class RollbackRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "rollback_person"
    override val riverName = "Rollback person"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@id", "aktørId", "fødselsnummer", "personVersjon")
    }

    override fun createMessage(packet: JsonMessage) = RollbackMessage(packet)
}
