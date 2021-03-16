package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage

internal open class InntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Inntektsmelding"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "arbeidstakerAktorId", "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt",
            "status", "arkivreferanse"
        )
        message.requireArray("arbeidsgiverperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("ferieperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("endringIRefusjoner") {
            require("endringsdato", JsonNode::asLocalDate)
        }
        message.require("mottattDato", JsonNode::asLocalDateTime)
        message.interestedIn("foersteFravaersdag", JsonNode::asLocalDate)
        message.interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
        message.interestedIn(
            "refusjon.beloepPrMnd",
            "arbeidsforholdId",
            "begrunnelseForReduksjonEllerIkkeUtbetalt",
            "opphoerAvNaturalytelser"
        )
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingMessage(packet)
}
