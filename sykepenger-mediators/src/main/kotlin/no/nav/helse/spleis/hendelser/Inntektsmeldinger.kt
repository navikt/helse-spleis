package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage

internal class Inntektsmeldinger(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "inntektsmelding")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "inntektsmeldingId", "arbeidstakerFnr",
                    "arbeidstakerAktorId", "virksomhetsnummer",
                    "arbeidsgivertype", "beregnetInntekt",
                    "status", "arkivreferanse"
                )
                it.requireArray("arbeidsgiverperioder") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                }
                it.requireArray("ferieperioder") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                }
                it.requireArray("endringIRefusjoner") {
                    require("endringsdato", JsonNode::asLocalDate)
                }
                it.require("mottattDato", JsonNode::asLocalDateTime)
                it.require("foersteFravaersdag", JsonNode::asLocalDate)
                it.interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
                it.interestedIn("refusjon.beloepPrMnd", "arbeidsforholdId", "begrunnelseForReduksjonEllerIkkeUtbetalt")
            }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(InntektsmeldingMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Inntektsmelding", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Inntektsmelding", problems, context)
    }
}
