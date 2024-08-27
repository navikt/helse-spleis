package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.InntektsmeldingerReplayMessage

internal open class InntektsmeldingerReplayRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmeldinger_replay"
    override val riverName = "Inntektsmeldinger Replay"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "aktørId", "organisasjonsnummer")
        message.requireKey("vedtaksperiodeId")
        message.requireArray("inntektsmeldinger") {
            require("internDokumentId") { it.asText().toUUID() }
            requireKey(
                "inntektsmelding.inntektsmeldingId",
                "inntektsmelding.arbeidstakerFnr",
                "inntektsmelding.arbeidstakerAktorId",
                "inntektsmelding.virksomhetsnummer",
                "inntektsmelding.arbeidsgivertype",
                "inntektsmelding.beregnetInntekt",
                "inntektsmelding.status",
                "inntektsmelding.arkivreferanse"
            )
            requireArray("inntektsmelding.arbeidsgiverperioder") {
                require("fom", JsonNode::asLocalDate)
                require("tom", JsonNode::asLocalDate)
            }
            requireArray("inntektsmelding.ferieperioder") {
                require("fom", JsonNode::asLocalDate)
                require("tom", JsonNode::asLocalDate)
            }
            requireArray("inntektsmelding.endringIRefusjoner") {
                require("endringsdato", JsonNode::asLocalDate)
                requireKey("beloep")
            }
            require("inntektsmelding.mottattDato", JsonNode::asLocalDateTime)
            interestedIn("inntektsmelding.fødselsdato", JsonNode::asLocalDate)
            interestedIn("inntektsmelding.dødsdato", JsonNode::asLocalDate)
            interestedIn("inntektsmelding.foersteFravaersdag", JsonNode::asLocalDate)
            interestedIn("inntektsmelding.refusjon.opphoersdato", JsonNode::asLocalDate)
            interestedIn(
                "inntektsmelding.refusjon.beloepPrMnd",
                "inntektsmelding.arbeidsforholdId",
                "inntektsmelding.begrunnelseForReduksjonEllerIkkeUtbetalt",
                "inntektsmelding.opphoerAvNaturalytelser",
                "inntektsmelding.harFlereInntektsmeldinger",
                "inntektsmelding.historiskeFolkeregisteridenter",
                "inntektsmelding.avsenderSystem",
                "inntektsmelding.inntektsdato"
            )
        }
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingerReplayMessage(packet)
}
