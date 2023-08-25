package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadFrilansMessage

internal class SendtFrilansSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_frilans"
    override val riverName = "Sendt søknad Frilans"

    override fun validate(message: JsonMessage) {
        message.requireKey("id")
        message.forbid("arbeidsgiver.orgnummer")
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
        message.interestedIn("arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadFrilansMessage(packet)
}
