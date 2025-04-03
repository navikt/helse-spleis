package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage

internal class LpsOgAltinnInntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Lps- og Altinn-inntektsmeldinger"

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
        message.requireKey("beregnetInntekt")
        message.interestedIn("harFlereInntektsmeldinger")
        message.interestedIn("foersteFravaersdag")
        message.require("arbeidsgiverperioder") { agp ->
            if (agp.size() == 0) {
                message.requireKey("foersteFravaersdag")
            }
        }
    }

    override fun createMessage(packet: JsonMessage): InntektsmeldingMessage {
        val meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["arbeidstakerFnr"].asText()
        )
        return InntektsmeldingMessage(
            packet = packet,
            meldingsporing = meldingsporing
        )
    }
}

internal fun standardInntektsmeldingvalidering(message: JsonMessage, pathPrefix: String? = null) {
    fun p(key: String) = pathPrefix?.let { "$pathPrefix.$key" } ?: key
    message.requireKey(p("arbeidstakerFnr"), p("virksomhetsnummer"), p("opphoerAvNaturalytelser"))
    message.requireArray(p("arbeidsgiverperioder")) {
        require("fom", JsonNode::asLocalDate)
        require("tom", JsonNode::asLocalDate)
    }
    message.requireArray(p("endringIRefusjoner")) {
        require("endringsdato", JsonNode::asLocalDate)
        requireKey("beloep")
    }
    message.require(p("mottattDato"), JsonNode::asLocalDateTime)
    message.interestedIn(p("refusjon.opphoersdato"), JsonNode::asLocalDate)
    message.interestedIn(p("refusjon.beloepPrMnd"), p("begrunnelseForReduksjonEllerIkkeUtbetalt"))
}
