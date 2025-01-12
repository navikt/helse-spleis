package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage

internal class LpsOgAltinnInntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Lps- og Altinn-inntektsmeldinger"
    override fun precondition(packet: JsonMessage) {
        packet.forbidValues("avsenderSystem.navn", listOf("NAV_NO", "NAV_NO_SELVBESTEMT"))
        packet.forbid("vedtaksperiodeId")
    }

    override fun validate(message: JsonMessage) {
        standardInntektsmeldingvalidering(message)
        message.require("fødselsdato", JsonNode::asLocalDate)
        message.interestedIn("dødsdato", JsonNode::asLocalDate)
        message.interestedIn("harFlereInntektsmeldinger", "avsenderSystem")
        message.interestedIn("foersteFravaersdag", JsonNode::asLocalDate)
    }

    override fun createMessage(packet: JsonMessage): InntektsmeldingMessage {
        val fødselsdato = packet["fødselsdato"].asLocalDate()
        val dødsdato = packet["dødsdato"].asOptionalLocalDate()
        val meldingsporing = Meldingsporing(
            id = packet["@id"].asText().toUUID(),
            fødselsnummer = packet["arbeidstakerFnr"].asText()
        )
        return InntektsmeldingMessage(
            packet = packet,
            personopplysninger = Personopplysninger(
                Personidentifikator(meldingsporing.fødselsnummer),
                fødselsdato, dødsdato
            ),
            meldingsporing = meldingsporing
        )
    }
}

internal fun standardInntektsmeldingvalidering(message: JsonMessage, pathPrefix: String? = null) {
    fun p(key: String) = pathPrefix?.let { "$pathPrefix.$key" } ?: key
    message.requireKey(p("arbeidstakerFnr"), p("virksomhetsnummer"), p("beregnetInntekt"), p("opphoerAvNaturalytelser"))
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
