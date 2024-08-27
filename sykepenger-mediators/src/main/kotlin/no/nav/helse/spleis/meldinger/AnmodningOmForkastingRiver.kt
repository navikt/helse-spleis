package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.util.UUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AnmodningOmForkastingMessage

internal open class AnmodningOmForkastingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "anmodning_om_forkasting"
    override val riverName = "anmodningOmForkasting"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "organisasjonsnummer",
            "fødselsnummer",
            "aktørId"
        )
        message.require("vedtaksperiodeId") { UUID.fromString(it.asText()) }
        message.interestedIn("force")
    }

    override fun createMessage(packet: JsonMessage) = AnmodningOmForkastingMessage(packet)
}
