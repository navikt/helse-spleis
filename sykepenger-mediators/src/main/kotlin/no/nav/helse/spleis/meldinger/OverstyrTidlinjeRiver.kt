package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.OverstyrTidslinjeMessage

internal class OverstyrTidlinjeRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_tidslinje"
    override val riverName = "Overstyr tidslinje"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "organisasjonsnummer")
        message.requireArray("dager") {
            requireKey("dato")
            requireAny("type", Dagtype.gyldigeTyper)
            interestedIn("grad", "tom")
        }
        message.require("dager") { require(!it.isEmpty) }
    }

    override fun createMessage(packet: JsonMessage) = OverstyrTidslinjeMessage(
        packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
