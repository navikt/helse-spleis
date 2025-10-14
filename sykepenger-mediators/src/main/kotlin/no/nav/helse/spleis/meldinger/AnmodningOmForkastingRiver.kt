package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.AnmodningOmForkastingMessage

internal open class AnmodningOmForkastingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "anmodning_om_forkasting"
    override val riverName = "anmodningOmForkasting"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "yrkesaktivitetstype")
        message.require("vedtaksperiodeId") { UUID.fromString(it.asText()) }
        message.interestedIn("force", "organisasjonsnummer")
    }

    override fun createMessage(packet: JsonMessage) = AnmodningOmForkastingMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
