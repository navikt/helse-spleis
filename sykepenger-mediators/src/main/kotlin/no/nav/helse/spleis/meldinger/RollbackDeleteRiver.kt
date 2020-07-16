package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.RollbackDeleteMessage

internal class RollbackDeleteRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "rollback_person_delete"
    override val riverName = "Rollback person delete"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("aktørId", "fødselsnummer")
    }

    override fun createMessage(packet: JsonMessage) = RollbackDeleteMessage(JsonMessageDelegate(packet))
}
