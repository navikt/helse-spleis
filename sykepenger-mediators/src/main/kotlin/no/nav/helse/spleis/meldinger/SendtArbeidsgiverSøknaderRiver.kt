package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SendtSøknadArbeidsgiverMessage

internal class SendtArbeidsgiverSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "sendt_søknad_arbeidsgiver"
    override val riverName = "Sendt søknad arbeidsgiver"

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
        message.interestedIn("sporsmal", "arbeidGjenopptatt", "andreInntektskilder", "permitteringer", "merknaderFraSykmelding", "korrigerer", "opprinneligSendt", "utenlandskSykmelding", "sendTilGosys", "egenmeldingsdagerFraSykmelding", "inntektFraNyttArbeidsforhold")
        message.requireValue("status", "SENDT")
        message.require("sendtArbeidsgiver", JsonNode::asLocalDateTime)
        message.forbid("sendtNav")
    }

    override fun createMessage(packet: JsonMessage) = SendtSøknadArbeidsgiverMessage(packet)
}
