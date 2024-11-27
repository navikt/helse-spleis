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

internal open class InntektsmeldingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding"
    override val riverName = "Inntektsmelding"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "virksomhetsnummer",
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
            requireKey("beloep")
        }
        message.require("mottattDato", JsonNode::asLocalDateTime)
        message.require("fødselsdato", JsonNode::asLocalDate)
        message.interestedIn("dødsdato", JsonNode::asLocalDate)
        message.interestedIn("foersteFravaersdag", JsonNode::asLocalDate)
        message.interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
        message.interestedIn(
            "refusjon.beloepPrMnd",
            "begrunnelseForReduksjonEllerIkkeUtbetalt",
            "opphoerAvNaturalytelser",
            "harFlereInntektsmeldinger",
            "historiskeFolkeregisteridenter",
            "avsenderSystem",
            "inntektsdato",
            "vedtaksperiodeId"
        )
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
