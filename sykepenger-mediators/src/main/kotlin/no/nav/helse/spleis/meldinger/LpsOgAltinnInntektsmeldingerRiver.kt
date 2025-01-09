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
    }

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt",
            "status", "arkivreferanse", "opphoerAvNaturalytelser"
        )
        message.requireArray("arbeidsgiverperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        message.requireArray("endringIRefusjoner") {
            require("endringsdato", JsonNode::asLocalDate)
            requireKey("beloep")
        }
        message.require("mottattDato", JsonNode::asLocalDateTime)
        message.require("fødselsdato", JsonNode::asLocalDate)

        message.forbid("vedtaksperiodeId")

        message.interestedIn("dødsdato", JsonNode::asLocalDate)
        message.interestedIn("foersteFravaersdag", JsonNode::asLocalDate)
        message.interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
        message.interestedIn(
            "refusjon.beloepPrMnd",
            "begrunnelseForReduksjonEllerIkkeUtbetalt",
            "harFlereInntektsmeldinger",
            "historiskeFolkeregisteridenter",
            "avsenderSystem",
            "inntektsdato",
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
