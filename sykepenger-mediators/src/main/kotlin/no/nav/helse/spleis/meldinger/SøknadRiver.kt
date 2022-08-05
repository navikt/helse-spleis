package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator

internal abstract class SøknadRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    init {
        river.validate(::validateSøknad)
    }

    private fun validateSøknad(packet: JsonMessage) {
        packet.requireKey("fnr", "aktorId", "arbeidsgiver.orgnummer", "status")
        packet.require("sykmeldingSkrevet", JsonNode::asLocalDateTime)
        packet.require("opprettet", JsonNode::asLocalDateTime)
        packet.require("fom", JsonNode::asLocalDate)
        packet.require("tom", JsonNode::asLocalDate)
        packet.require("fødselsdato", JsonNode::asLocalDate)
        packet.requireArray("soknadsperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
            requireKey("sykmeldingsgrad")
        }
    }
}
