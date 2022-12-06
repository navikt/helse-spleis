package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage

internal class OverstyrArbeidsgiveropplysningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
): HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "overstyr_arbeidsgiveropplysninger"

    override val riverName = "Overstyr arbeidsgiveropplysninger"

    override fun createMessage(packet: JsonMessage) = OverstyrArbeidsgiveropplysningerMessage(packet)

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("arbeidsgiveropplysninger") { arbeidsgiveropplysninger ->
            require(arbeidsgiveropplysninger.fieldNames().hasNext())
            arbeidsgiveropplysninger.fieldNames().forEach { orgnr -> "arbeidsgiveropplysninger.$orgnr".let { arbeidsgiver ->
                message.require("$arbeidsgiver.månedligInntekt") { require(it.gyldigDouble) }
                message.require("$arbeidsgiver.forklaring") { require(it.gyldigTekst) }
                message.interestedIn("$arbeidsgiver.subsumsjon") { require(it.path("paragraf").gyldigTekst) }
                message.interestedIn("$arbeidsgiver.subsumsjon.paragraf")
                message.interestedIn("$arbeidsgiver.subsumsjon.ledd")  { require(it.gyldigInt) }
                message.interestedIn("$arbeidsgiver.subsumsjon.bokstav") { require(it.gyldigTekst) }
                message.requireArray("$arbeidsgiver.refusjonsopplysninger") {
                    require("fom", JsonNode::asLocalDate)
                    interestedIn("tom", JsonNode::asLocalDate)
                    require("beløp") { require(it.gyldigDouble)}
                }
            }}
        }
    }

    private companion object {
        private val JsonNode.gyldigTekst get() = isTextual && asText().isNotBlank()
        private val JsonNode.gyldigDouble get() = isNumber || asText().toDoubleOrNull() != null
        private val JsonNode.gyldigInt get() = isInt || asText().toIntOrNull() != null
    }
}