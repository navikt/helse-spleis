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

    override fun validate(packet: JsonMessage) {
        packet.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer")
        packet.requireArray("dager") {
            requireKey("dato")
            requireKey("dagtype")
            interestedIn("grad")
        }
    }

    override fun createMessage(packet: JsonMessage) = OverstyrTidslinjeMessage(packet)
}
