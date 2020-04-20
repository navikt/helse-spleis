package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage

internal class Inntektsmeldinger(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Inntektsmelding"

    override fun validate(packet: JsonMessage) {
        packet.requireKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "arbeidstakerAktorId", "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt",
            "status", "arkivreferanse"
        )
        packet.requireArray("arbeidsgiverperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        packet.requireArray("ferieperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        packet.requireArray("endringIRefusjoner") {
            require("endringsdato", JsonNode::asLocalDate)
        }
        packet.require("mottattDato", JsonNode::asLocalDateTime)
        packet.require("foersteFravaersdag", JsonNode::asLocalDate)
        packet.interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
        packet.interestedIn("refusjon.beloepPrMnd", "arbeidsforholdId", "begrunnelseForReduksjonEllerIkkeUtbetalt")
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingMessage(packet)
}
