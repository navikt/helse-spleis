package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayMessage
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayUtførtMessage

internal open class InntektsmeldingReplayUtførtRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding_replay_utført"
    override val riverName = "Inntektsmelding Replay Utført"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "aktørId", "organisasjonsnummer", "vedtaksperiodeId")
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingReplayUtførtMessage(packet)
}
