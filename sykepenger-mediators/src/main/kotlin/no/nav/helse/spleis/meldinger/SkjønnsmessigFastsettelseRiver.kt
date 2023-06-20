package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.SkjønnsmessigFastsettelseMessage

internal class SkjønnsmessigFastsettelseRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
): HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "skjønnsmessig_fastsettelse"

    override val riverName = "Skjønnsmessig fastsettelse"

    override fun createMessage(packet: JsonMessage) = SkjønnsmessigFastsettelseMessage(packet)

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer")
        message.require("skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("arbeidsgivere") { require(it.size() > 0) { "Må settes minst en arbeidsgiver" } }
        message.requireArray("arbeidsgivere") {
            require("organisasjonsnummer") { require(it.gyldigTekst) }
            require("månedligInntekt") { require(it.gyldigDouble) }
        }
        message.require("arbeidsgivere") { arbeidsgiveropplysning ->
            val organisasjonsnummer = arbeidsgiveropplysning.map { it.path("organisasjonsnummer").asText() }
            require(organisasjonsnummer.size == organisasjonsnummer.toSet().size) { "Duplikate organisasjonsnummer $organisasjonsnummer" }
        }
    }

    private companion object {
        private val JsonNode.gyldigTekst get() = isTextual && asText().isNotBlank()
        private val JsonNode.gyldigDouble get() = isNumber || asText().toDoubleOrNull() != null
    }
}