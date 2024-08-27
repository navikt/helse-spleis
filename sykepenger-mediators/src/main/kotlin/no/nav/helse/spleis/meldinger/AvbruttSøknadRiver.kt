package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AvbruttSøknadMessage

internal class AvbruttSøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "avbrutt_søknad"
    override val riverName = "avbrutt_søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "@id",
            "aktorId",
            "fnr",
            "arbeidsgiver.orgnummer"
        )
        message.require("fom", JsonNode::asLocalDate)
        message.require("tom", JsonNode::asLocalDate)
    }

    override fun createMessage(packet: JsonMessage) = AvbruttSøknadMessage(packet)
}
