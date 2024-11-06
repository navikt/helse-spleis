package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage

internal class OverstyrArbeidsgiveropplysningerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
): HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "overstyr_inntekt_og_refusjon"

    override val riverName = "Overstyr inntekt og refusjon"

    override fun createMessage(packet: JsonMessage) = OverstyrArbeidsgiveropplysningerMessage(packet, Meldingsporing(
        id = packet["@id"].asText().toUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        aktørId = packet["aktørId"].asText()
    ))

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.requireArbeidsgiveropplysninger()
    }

    private companion object {
        private val JsonNode.gyldigTekst get() = isTextual && asText().isNotBlank()
        private val JsonNode.gyldigDouble get() = isNumber || asText().toDoubleOrNull() != null
        private val JsonNode.gyldigInt get() = isInt || asText().toIntOrNull() != null

        private fun JsonMessage.requireArbeidsgiveropplysninger() {
            require("arbeidsgivere") { require(it.size() > 0) { "Må settes minst en arbeidsgiver" } }
            requireArray("arbeidsgivere") {
                require("organisasjonsnummer") { require(it.gyldigTekst) }
                require("månedligInntekt") { require(it.gyldigDouble) }
                require("forklaring") { require(it.gyldigTekst) }
                interestedIn("fom", JsonNode::asLocalDate)
                interestedIn("tom", JsonNode::asLocalDate)
                interestedIn("subsumsjon") { require(it.path("paragraf").gyldigTekst) }
                interestedIn("subsumsjon.paragraf")
                interestedIn("subsumsjon.ledd")  { require(it.gyldigInt) }
                interestedIn("subsumsjon.bokstav") { require(it.gyldigTekst) }
                requireArray("refusjonsopplysninger") {
                    require("fom", JsonNode::asLocalDate)
                    interestedIn("tom", JsonNode::asLocalDate)
                    require("beløp") { require(it.gyldigDouble)}
                }
            }
            require("arbeidsgivere") { arbeidsgiveropplysning ->
                val organisasjonsnummer = arbeidsgiveropplysning.map { it.path("organisasjonsnummer").asText() }
                require(organisasjonsnummer.size == organisasjonsnummer.toSet().size) { "Duplikate organisasjonsnummer $organisasjonsnummer" }
            }
        }
    }
}