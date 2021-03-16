package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.InntektsmeldingReplayMessage

internal open class InntektsmeldingerReplayRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : InntektsmeldingerRiver(rapidsConnection, messageMediator) {
    override val eventName = "inntektsmelding_replay"
    override val riverName = "Inntektsmelding Replay"

    override fun validate(message: JsonMessage) {
        super.validate(message)
        message.requireKey("vedtaksperiodeId")
    }

    override fun createMessage(packet: JsonMessage) = InntektsmeldingReplayMessage(packet)
}
