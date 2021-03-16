package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage

internal class OverstyrTidlinjeRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_tidslinje"
    override val riverName = "Overstyr tidslinje"

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer")
        message.requireArray("dager") {
            requireKey("dato")
            requireKey("type")
            interestedIn("grad")
        }
        message.require("dager") { require(!it.isEmpty) }
    }

    override fun createMessage(packet: JsonMessage) = OverstyrTidslinjeMessage(packet)
}
