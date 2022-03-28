package no.nav.helse.spleis.meldinger.model

import java.util.UUID
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing an Inntektsmelding replay
internal class InntektsmeldingReplayMessage(packet: JsonMessage) : InntektsmeldingMessage(packet) {
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    override val skalDuplikatsjekkes = false

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, InntektsmeldingReplay(inntektsmelding, vedtaksperiodeId), context)
    }
}
