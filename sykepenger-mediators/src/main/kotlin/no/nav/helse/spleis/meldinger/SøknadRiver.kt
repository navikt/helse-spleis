package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator

internal abstract class SøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    init {
        river.validate(::validateSøknad)
    }

    private fun validateSøknad(packet: JsonMessage) {
        packet.requireKey("fnr", "aktorId", "status")
        packet.require("sykmeldingSkrevet", JsonNode::asLocalDateTime)
        packet.require("opprettet", JsonNode::asLocalDateTime)
        packet.require("fom", JsonNode::asLocalDate)
        packet.require("tom", JsonNode::asLocalDate)
        packet.require("fødselsdato", JsonNode::asLocalDate)
        packet.interestedIn("dødsdato", JsonNode::asLocalDate)
        packet.requireArray("soknadsperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
            requireKey("sykmeldingsgrad")
        }
        packet.interestedIn("historiskeFolkeregisteridenter", "arbeidUtenforNorge", "yrkesskade")
    }
}
