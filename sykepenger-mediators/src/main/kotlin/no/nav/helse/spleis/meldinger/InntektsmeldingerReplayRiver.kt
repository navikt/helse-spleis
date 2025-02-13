package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InntektsmeldingerReplayMessage

internal class InntektsmeldingerReplayRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmeldinger_replay"
    override val riverName = "Inntektsmeldinger Replay"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "organisasjonsnummer")
        message.requireKey("vedtaksperiodeId")
        message.requireArray("inntektsmeldinger") {
            require("internDokumentId") { it.asText().toUUID() }
            standardInntektsmeldingvalidering(this, "inntektsmelding")
            requireKey("inntektsmelding.beregnetInntekt")
            interestedIn("inntektsmelding.harFlereInntektsmeldinger", "inntektsmelding.avsenderSystem")
            interestedIn("inntektsmelding.foersteFravaersdag", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingerReplayMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
