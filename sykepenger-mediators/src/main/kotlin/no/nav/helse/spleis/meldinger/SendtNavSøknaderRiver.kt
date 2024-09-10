package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadNavMessage

internal class SendtNavSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_nav"
    override val riverName = "Sendt søknad Nav"

    override fun validate(message: JsonMessage) {
        message.requireKey("id", "arbeidsgiver.orgnummer")
        message.requireArray("papirsykmeldinger") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("fravar") {
            requireAny("type", listOf("UTDANNING_FULLTID", "UTDANNING_DELTID", "PERMISJON", "FERIE", "UTLANDSOPPHOLD"))
            require("fom", JsonNode::asLocalDate)
            interestedIn("tom") { it.asLocalDate() }
        }
        message.require("sendtNav", JsonNode::asLocalDateTime)
        message.interestedIn("egenmeldingsdagerFraSykmelding") { egenmeldinger -> egenmeldinger.map { it.asLocalDate() } }
        message.interestedIn("tilkomneInntekter") {
            message.requireArray("tilkomneInntekter") {
                require("orgnummer") { it.isTextual }
                require("beløp") { it.isNumber }
                require("fom") { JsonNode::asLocalDate }
                require("tom") { JsonNode::asLocalDate }
            }
        }
        message.interestedIn("sporsmal", "arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadNavMessage(packet)
}
